Reference for persisting Ink strokes to a Room database using `ink-storage`,
exporting drawings to an offscreen `Bitmap` or PNG file, and sharing pointer
input with sibling gesture handlers.

## Stroke serialization and Room persistence

Decompose each `Stroke` into two components for compact storage:

1. **Stroke inputs (`StrokeInputBatch`)** : Encode and decode binary input points using `stroke.inputs.encode(outputStream)` and `StrokeInputBatch.decode(inputStream)` from `androidx.ink.storage`. (In `1.1.0-alpha` and higher, direct `ByteArray` overloads `stroke.inputs.encode` and `StrokeInputBatch.decode(bytes)` are also available.)
2. **Brush metadata** : Store `size`, `colorLong`, `epsilon`, and either a stock brush enum or a custom brush identifier. Never call `brush.family.clientBrushFamilyId` directly---that property is `@RestrictTo(LIBRARY_GROUP)`. Look up custom brushes in your app's `Map<String, BrushFamily>` using structural `==` equality.

### Serialized data classes and stroke converters


```kotlin
@Serializable
data class SerializedStroke(
    val inputs: ByteArray,
    val brush: SerializedBrush,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SerializedStroke) return false
        return inputs.contentEquals(other.inputs) && brush == other.brush
    }

    override fun hashCode(): Int = 31 * inputs.contentHashCode() + brush.hashCode()
}

@Serializable
data class SerializedBrush(
    val size: Float,
    val color: Long,
    val epsilon: Float,
    val stockBrush: SerializedStockBrush,
    val clientBrushFamilyId: String? = null,
)

@Serializable
enum class SerializedStockBrush {
    MarkerLatest,
    PressurePenLatest,
    HighlighterLatest,
    DashedLineLatest,
}

private val stockBrushToEnum = mapOf(
    StockBrushes.marker() to SerializedStockBrush.MarkerLatest,
    StockBrushes.pressurePen() to SerializedStockBrush.PressurePenLatest,
    StockBrushes.highlighter() to SerializedStockBrush.HighlighterLatest,
    StockBrushes.dashedLine() to SerializedStockBrush.DashedLineLatest,
)

private val enumToStockBrush = stockBrushToEnum.entries.associate { (k, v) -> v to k }

fun serializeStroke(
    stroke: Stroke,
    customBrushes: Map<String, BrushFamily> = emptyMap(),
): String {
    val encodedInputs = ByteArrayOutputStream().use { outputStream ->
        stroke.inputs.encode(outputStream)
        outputStream.toByteArray()
    }
    val customBrushId = customBrushes.entries.firstOrNull { it.value == stroke.brush.family }?.key
    val serializedBrush = SerializedBrush(
        size = stroke.brush.size,
        color = stroke.brush.colorLong,
        epsilon = stroke.brush.epsilon,
        stockBrush = stockBrushToEnum[stroke.brush.family] ?: SerializedStockBrush.MarkerLatest,
        clientBrushFamilyId = customBrushId,
    )
    return Json.encodeToString(SerializedStroke(inputs = encodedInputs, brush = serializedBrush))
}

fun deserializeStroke(
    data: String,
    customBrushes: Map<String, BrushFamily> = emptyMap(),
): Stroke? {
    return try {
        val serialized = Json.decodeFromString<SerializedStroke>(data)
        val inputs = ByteArrayInputStream(serialized.inputs).use { inputStream ->
            StrokeInputBatch.decode(inputStream)
        }
        val family = serialized.brush.clientBrushFamilyId?.let { customBrushes[it] }
            ?: enumToStockBrush[serialized.brush.stockBrush]
            ?: StockBrushes.marker()
        val brush = Brush.createWithColorLong(
            family = family,
            colorLong = serialized.brush.color,
            size = serialized.brush.size,
            epsilon = serialized.brush.epsilon,
        )
        Stroke(brush = brush, inputs = inputs)
    } catch (e: Exception) {
        null
    }
}
```

<br />

### Room database entity, converter, and repository

When using Room in Kotlin, always apply the `com.google.devtools.ksp` plugin and
use `ksp(libs.androidx.room.compiler)`---never `annotationProcessor`. Run stroke
encoding and decoding on `Dispatchers.IO` so large drawings don't block the UI
thread:


```kotlin
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "strokes_data")
    val strokesData: List<String> = emptyList(),
)

class RoomConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringList(list: List<String>?): String? = list?.let { json.encodeToString(it) }

    @TypeConverter
    fun toStringList(jsonString: String?): List<String> {
        if (jsonString.isNullOrEmpty()) return emptyList()
        return try {
            json.decodeFromString<List<String>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }
}

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getById(id: Long): DocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DocumentEntity)
}

@Database(entities = [DocumentEntity::class], version = 1)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
}

class StrokeRepositoryImpl(
    private val dao: DocumentDao,
) : StrokeRepository {
    override suspend fun saveStrokes(documentId: Long, strokes: List<Stroke>) {
        withContext(Dispatchers.IO) {
            val serializedList = strokes.map { serializeStroke(it) }
            dao.upsert(DocumentEntity(id = documentId, strokesData = serializedList))
        }
    }

    override suspend fun loadStrokes(documentId: Long): List<Stroke> =
        withContext(Dispatchers.IO) {
            val doc = dao.getById(documentId) ?: return@withContext emptyList()
            doc.strokesData.mapNotNull { deserializeStroke(it) }
        }
}
```

<br />

## Bitmap and PNG export

Render finished strokes onto an offscreen `Bitmap` using `Canvas(bitmap)` and
`CanvasStrokeRenderer.create(textureStore)`. Compress the `Bitmap` with
`Bitmap.CompressFormat.PNG`. In version `1.0.0` and higher,
`CanvasStrokeRenderer.create(textureStore)` automatically falls back to path
rendering (`CanvasPathRenderer`) when drawing onto a software `Canvas(bitmap)`.
> **Path rendering caveat on software `Canvas(bitmap)`** : Mesh-only features
> (`SelfOverlap.ACCUMULATE`, `BrushPaint.StampingTexture` such as
> `StockBrushes.emojiHighlighter`'s mini-emoji trail, and per-vertex opacity or
> color `BrushBehavior` instances) can't be drawn by `CanvasPathRenderer` and
> are skipped unless a path-compatible fallback `BrushPaint` is supplied.
> Standard `StockBrushes` (`pressurePen`, `marker`, `highlighter`, `dashedLine`)
> export to software `Canvas(bitmap)` without issue.


```kotlin
suspend fun exportDrawingToPng(
    width: Int,
    height: Int,
    strokes: List<Stroke>,
    textureStore: TextureBitmapStore,
    outputFile: File,
    backgroundColor: Int = android.graphics.Color.WHITE,
): Uri = withContext(Dispatchers.IO) {
    val exportBitmap = createBitmap(width, height)
    val canvas = Canvas(exportBitmap)
    canvas.drawColor(backgroundColor)

    val renderer = CanvasStrokeRenderer.create(textureStore = textureStore)
    strokes.forEach { stroke ->
        canvas.withSave {
            renderer.draw(
                canvas = this,
                stroke = stroke,
                strokeToScreenTransform = Matrix()
            )
        }
    }

    FileOutputStream(outputFile).use { output ->
        exportBitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
    }
    Uri.fromFile(outputFile)
}
```

<br />

## Motion prediction and gesture coexistence

### Low-latency motion prediction

Adding `implementation("androidx.input:input-motionprediction:<version>")` to
`app/build.gradle.kts` enables `InProgressStrokes` to predict future stylus and
touch points automatically on the classpath---reducing perceived stylus latency
from 20 to 50 ms to near zero with no extra Kotlin code required.

### Drag coexistence (`pointerInputWithSiblingFallthrough`)

Standard Compose `pointerInput` modifiers don't share pointer events with
sibling composables. To let a long-press drag handler coexist with
`InProgressStrokes`, define a `PointerInputModifierNode` that returns
`sharePointerInputWithSiblings() = true` and place its `Box` *after*
`InProgressStrokes` inside the parent `Box` (Compose hit-tests `Box` children in
reverse declaration order, from last child to first):


```kotlin
internal fun Modifier.pointerInputWithSiblingFallthrough(
    pointerInputEventHandler: PointerInputEventHandler
) = this then PointerInputSiblingFallthroughElement(pointerInputEventHandler)

private class PointerInputSiblingFallthroughModifierNode(
    pointerInputEventHandler: PointerInputEventHandler
) : PointerInputModifierNode, DelegatingNode() {

    var pointerInputEventHandler: PointerInputEventHandler
        get() = delegateNode.pointerInputEventHandler
        set(value) {
            delegateNode.pointerInputEventHandler = value
        }

    val delegateNode = delegate(
        SuspendingPointerInputModifierNode(pointerInputEventHandler)
    )

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        delegateNode.onPointerEvent(pointerEvent, pass, bounds)
    }

    override fun onCancelPointerInput() {
        delegateNode.onCancelPointerInput()
    }

    override fun sharePointerInputWithSiblings() = true
}

private data class PointerInputSiblingFallthroughElement(
    val pointerInputEventHandler: PointerInputEventHandler
) : ModifierNodeElement<PointerInputSiblingFallthroughModifierNode>() {

    override fun create() =
        PointerInputSiblingFallthroughModifierNode(pointerInputEventHandler)

    override fun update(node: PointerInputSiblingFallthroughModifierNode) {
        node.pointerInputEventHandler = pointerInputEventHandler
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "pointerInputWithSiblingFallthrough"
        properties["pointerInputEventHandler"] = pointerInputEventHandler
    }
}
```

<br />

## Common pitfalls

- **Rasterizing strokes to bitmaps for database storage** : Always persist `StrokeInputBatch` binary bytes and brush metadata rather than PNGs so strokes stay editable, erasable, and resolution-independent.
- **Re-deserializing on every Room `Flow` emission** : Collecting a Room `Flow` while saving after every stroke re-decodes the entire document on each stroke. Load once on `init` and treat in-memory `history` as the session source of truth.
- **Placing `pointerInputWithSiblingFallthrough` before `InProgressStrokes`** : Place the fallthrough `Box` *after* `InProgressStrokes` so it's hit-tested first and shares events downward to `InProgressStrokes`.