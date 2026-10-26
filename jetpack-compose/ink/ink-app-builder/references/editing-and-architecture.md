Reference for structuring an Ink Compose app with MVVM and Hilt, implementing a
linear undo and redo history stack, and adding a geometry-based stroke eraser.

## MVVM architecture overview

    Compose UI Layer (DrawingScreen -> DrawingSurface + DrawingToolbox)
      │  Observes StateFlows via collectAsStateWithLifecycle
      ▼
    ViewModel Layer (DrawingViewModel)
      │  Manages uiState, currentBrush, isEraserMode, canUndo/canRedo, history
      ▼
    Repository Layer (StrokeRepository)
      │  Runs serializeStroke / deserializeStroke on Dispatchers.IO
      ▼
    Data Layer (Room AppDatabase + DocumentDao)

Define a single immutable UI state class and a `ViewModel` that owns the stroke
history, active brush, and eraser state:


```kotlin
data class DrawingUiState(
    val strokes: List<Stroke> = emptyList(),
    val documentTitle: String = "",
)
```

<br />

## Linear undo and redo history stack

Maintain a snapshot history list (`MutableList<List<Stroke>>`) and a
`historyIndex` pointer inside the `ViewModel`. Because `Stroke` instances are
immutable, each snapshot only allocates a lightweight list reference without
duplicating stroke geometry.

### `DrawingViewModel` with undo, redo, and eraser

When the user adds, erases, or clears strokes after an `undo`, truncate all
entries after `historyIndex` before appending the new snapshot. Cap history
growth with `maxHistorySize` (such as `50`) for long sessions, and persist
strokes on `Dispatchers.IO` after every mutation (`onStrokesFinished`,
`endErase`, `undo`, `redo`, and `clearStrokes`):


```kotlin
class DrawingViewModel(
    private val repository: StrokeRepository,
    val textureStore: AppTextureBitmapStore,
    private val documentId: Long = 1L,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DrawingUiState())
    val uiState: StateFlow<DrawingUiState> = _uiState.asStateFlow()

    private val _selectedBrush = MutableStateFlow(
        Brush.createWithComposeColor(
            family = StockBrushes.pressurePen(),
            color = Color.Black,
            size = 5f,
            epsilon = 0.1f,
        )
    )
    val currentBrush: StateFlow<Brush> = _selectedBrush.asStateFlow()

    fun getCurrentBrush(): Brush = _selectedBrush.value

    private val _isEraserMode = MutableStateFlow(false)
    val isEraserMode: StateFlow<Boolean> = _isEraserMode.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val history = mutableListOf<List<Stroke>>()
    private var historyIndex = -1
    private val maxHistorySize = 50

    private var previousPoint: MutableVec? = null
    private val eraserPadding = 50f

    init {
        viewModelScope.launch {
            val initialStrokes = repository.loadStrokes(documentId)
            _uiState.update { it.copy(strokes = initialStrokes) }
            history.clear()
            history.add(initialStrokes)
            historyIndex = 0
            updateUndoRedoState()
        }
    }

    private fun updateStrokes(newStrokes: List<Stroke>) {
        // Truncate any future redo states when a new edit occurs after undo
        if (historyIndex < history.size - 1) {
            history.subList(historyIndex + 1, history.size).clear()
        }
        history.add(newStrokes)
        historyIndex++

        while (history.size > maxHistorySize) {
            history.removeAt(0)
            historyIndex--
        }

        _uiState.update { it.copy(strokes = newStrokes) }
        updateUndoRedoState()
    }

    private fun updateUndoRedoState() {
        _canUndo.value = historyIndex > 0
        _canRedo.value = historyIndex < history.size - 1
    }

    fun onStrokesFinished(finishedStrokes: List<Stroke>) {
        val current = history.getOrElse(historyIndex) { emptyList() }
        updateStrokes(current + finishedStrokes)
        viewModelScope.launch { saveStrokes() }
    }

    fun undo() {
        if (_canUndo.value) {
            historyIndex--
            _uiState.update { it.copy(strokes = history[historyIndex]) }
            updateUndoRedoState()
            viewModelScope.launch { saveStrokes() }
        }
    }

    fun redo() {
        if (_canRedo.value) {
            historyIndex++
            _uiState.update { it.copy(strokes = history[historyIndex]) }
            updateUndoRedoState()
            viewModelScope.launch { saveStrokes() }
        }
    }

    fun clearStrokes() {
        if (_uiState.value.strokes.isNotEmpty()) {
            updateStrokes(emptyList())
            viewModelScope.launch { saveStrokes() }
        }
    }

    fun setEraserMode(enabled: Boolean) {
        _isEraserMode.value = enabled
    }

    fun startErase() {
        previousPoint = null
    }

    fun endErase() {
        previousPoint = null
        viewModelScope.launch { saveStrokes() }
    }

    fun erase(x: Float, y: Float) {
        val strokesBefore = history.getOrElse(historyIndex) { emptyList() }
        val strokesAfter = eraseIntersectingStrokes(x, y, strokesBefore)
        if (strokesAfter.size != strokesBefore.size) {
            updateStrokes(strokesAfter)
        }
    }

    private fun eraseIntersectingStrokes(
        currentX: Float,
        currentY: Float,
        currentStrokes: List<Stroke>,
    ): List<Stroke> {
        val prev = previousPoint
        previousPoint = MutableVec(currentX, currentY)
        if (prev == null) return currentStrokes

        val segment = MutableSegment(prev, MutableVec(currentX, currentY))
        val parallelogram = MutableParallelogram()
            .populateFromSegmentAndPadding(segment, eraserPadding)

        val strokesToRemove = currentStrokes.filter { stroke ->
            stroke.shape.intersects(parallelogram, AffineTransform.IDENTITY)
        }
        return if (strokesToRemove.isNotEmpty()) {
            currentStrokes - strokesToRemove.toSet()
        } else {
            currentStrokes
        }
    }

    private suspend fun saveStrokes() {
        val strokesToSave = history.getOrElse(historyIndex) { emptyList() }
        repository.saveStrokes(documentId, strokesToSave)
    }
}
```

<br />

## Geometry-based stroke eraser details

The eraser tests whether each stroke's `PartitionedMesh` (`stroke.shape`)
intersects a padded `MutableParallelogram` swept between consecutive drag
points (`MutableSegment`).

- **Eraser padding (`eraserPadding`)** : Use `10f` to `30f` for precise erasing, `40f` to `60f` (with `50f` by default) for finger or stylus erasing, or `80f` and higher for wide-area erasing.
- **Lifecycle** : Reset `previousPoint = null` in both `startErase` and `endErase` so a new drag doesn't connect to the final point of the preceding drag.

### Geometry conversions and pan or zoom erasing

Use `ink-geometry-compose` and `ink-geometry` extensions to convert between
Compose or Android types and Ink geometry types, or to transform eraser hit
tests when the canvas is panned or zoomed:


```kotlin
fun populatePointFromOffset(reusableVec: MutableVec, offset: Offset): Offset {
    reusableVec.populateFrom(offset)
    return reusableVec.toOffset()
}

fun eraseWithPanZoom(
    strokes: List<Stroke>,
    screenEraserBox: MutableParallelogram,
    worldToScreenAndroidMatrix: AndroidMatrix,
    worldToScreenComposeMatrix: ComposeMatrix? = null,
): List<Stroke> {
    val meshToEraserTransform: AffineTransform =
        if (worldToScreenComposeMatrix != null) {
            ImmutableAffineTransform.from(worldToScreenComposeMatrix) ?: AffineTransform.IDENTITY
        } else {
            MutableAffineTransform().populateFrom(worldToScreenAndroidMatrix)
        }

    return strokes.filterNot { stroke ->
        stroke.shape.intersects(screenEraserBox, meshToEraserTransform)
    }
}

fun getStrokeBounds(stroke: Stroke): Pair<Rect?, RectF?> {
    val boundingBox = stroke.shape.computeBoundingBox() ?: return null to null
    return boundingBox.toRect() to boundingBox.toRectF()
}
```

<br />

## Composable screen wiring and repository interface

Observe `ViewModel` flows with `collectAsStateWithLifecycle` so collection
pauses when the app goes into the background, and recreate
`CanvasStrokeRenderer` in the composable layer when `textureStore.generation`
changes:


```kotlin
interface StrokeRepository {
    suspend fun saveStrokes(documentId: Long, strokes: List<Stroke>)
    suspend fun loadStrokes(documentId: Long): List<Stroke>
}

@Composable
fun DrawingScreen(
    viewModel: DrawingViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentBrush by viewModel.currentBrush.collectAsStateWithLifecycle()
    val isEraserMode by viewModel.isEraserMode.collectAsStateWithLifecycle()

    val textureStore = viewModel.textureStore
    val cacheGen by textureStore.generation.collectAsStateWithLifecycle()
    val renderer = remember(textureStore, cacheGen) {
        CanvasStrokeRenderer.create(textureStore)
    }

    CompositionLocalProvider(LocalTextureStore provides textureStore) {
        DrawingSurface(
            strokes = uiState.strokes,
            canvasStrokeRenderer = renderer,
            currentBrush = currentBrush,
            onGetNextBrush = { viewModel.getCurrentBrush() },
            onStrokesFinished = { viewModel.onStrokesFinished(it) },
            isEraserMode = isEraserMode,
            modifier = modifier,
            onErase = { x, y -> viewModel.erase(x, y) },
            onEraseStart = { viewModel.startErase() },
            onEraseEnd = { viewModel.endErase() },
        )
    }
}
```

<br />

## Common pitfalls

- **Launching `saveStrokes` inside `ViewModel.onCleared`** : `viewModelScope` is already cancelled when `onCleared` runs, so coroutines launched there cancel immediately. Persist strokes after each mutation instead.
- **Not resetting `previousPoint` in `startErase` and `endErase`** : Leaving a stale `previousPoint` creates a long segment across the canvas on the next touch down, erasing unintended strokes.
- **Not calling `change.consume` in the eraser gesture**: Unconsumed drag events propagate to parent or sibling gesture handlers.
- **Forgetting to seed `history` on `init`** : Always add `initialStrokes`, even when empty, and set `historyIndex = 0` so that the first user stroke can be undone.