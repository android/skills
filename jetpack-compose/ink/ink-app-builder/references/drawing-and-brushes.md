Reference for building a flicker-free Compose drawing surface, rendering dry
strokes with `CanvasStrokeRenderer`, and configuring or switching
`StockBrushes`.

## `InProgressStrokes` parameters

When configuring `InProgressStrokes`, pass these parameters:

| Parameter | Type | Default value | Description |
|---|---|---|---|
| `defaultBrush` | `Brush?` | None (required) | Brush applied when a stroke begins, or `null` to disable wet-ink input while keeping rendering resources active. |
| `nextBrush` | `() -> Brush?` | `{ defaultBrush }` | Lambda returning the brush for the next stroke. |
| `pointerEventToWorldTransform` | `Matrix` | `IDENTITY_MATRIX` | `androidx.compose.ui.graphics.Matrix` mapping pointer coordinates to world or stroke space. |
| `strokeToWorldTransform` | `Matrix` | `IDENTITY_MATRIX` | `androidx.compose.ui.graphics.Matrix` mapping stroke coordinates to world space. |
| `maskPath` | `Path?` | `null` | `androidx.compose.ui.graphics.Path` masking regions where wet ink doesn't render, such as floating toolbars. |
| `textureBitmapStore` | `TextureBitmapStore` | `TextureBitmapStore { null }` | Non-nullable bitmap store for textured brushes (don't pass `null`). |
| `onStrokesFinished` | `(List<Stroke>) -> Unit` | None (required) | UI-thread callback that passes completed `Stroke` objects to the app's dry-ink state when the pointer lifts. |

## Render dry strokes and highlighter blend modes

Create a `CanvasStrokeRenderer` with `CanvasStrokeRenderer.create(textureStore)`
and render each `Stroke` onto `drawContext.canvas.nativeCanvas` inside a
`nativeCanvas.withSave` block so transform and clip state stays associated with
a single stroke.
> **Offscreen and software `Canvas` note** : As of version `1.0.0`,
> `CanvasStrokeRenderer.create(textureStore)` automatically detects software
> canvases (`!canvas.isHardwareAccelerated`, such as `Canvas(bitmap)`), and
> falls back to path rendering. Never call the `@RestrictTo(LIBRARY_GROUP)`
> 2-argument overloaded version of
> `CanvasStrokeRenderer.create(forcePathRendering = true, textureStore)`.

### Render highlighter strokes with `BlendMode.Multiply`

To make overlapping highlights darken underlying ink realistically without
self-blending artifacts, use translucent color alpha (`< 1f`) and
`BlendMode.Multiply` composited per stroke using
`drawContext.canvas.withSaveLayer`:


```kotlin
@Composable
fun StrokeCanvas(
    strokes: List<Stroke>,
    canvasStrokeRenderer: CanvasStrokeRenderer,
    modifier: Modifier = Modifier,
) {
    val multiplyPaint = remember { Paint().apply { blendMode = BlendMode.Multiply } }
    val srcOverPaint = remember { Paint().apply { blendMode = BlendMode.SrcOver } }
    val identityMatrix = remember { Matrix() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val nativeCanvas = drawContext.canvas.nativeCanvas
        val bounds = drawContext.size.toRect()

        strokes.fastForEach { stroke ->
            val paint = if (stroke.brush.family == StockBrushes.highlighter()) {
                multiplyPaint
            } else {
                srcOverPaint
            }

            drawContext.canvas.withSaveLayer(bounds, paint) {
                nativeCanvas.withSave {
                    canvasStrokeRenderer.draw(
                        canvas = this,
                        stroke = stroke,
                        strokeToScreenTransform = identityMatrix,
                    )
                }
            }
        }
    }
}
```

<br />

## Stock brushes and brush configuration

`StockBrushes` (from `ink-brush`) provides built-in `BrushFamily` factories.

> [!NOTE]
> **Note:** All `StockBrushes` factories work on Android 8.0 (API level 26) and higher, including `StockBrushes.emojiHighlighter` with `showMiniEmojiTrail = false`. Mesh-rendered features, such as `SelfOverlap.ACCUMULATE` and mini-emoji trails (`showMiniEmojiTrail = true`), require Android 14 (API level 34) and higher.

| Factory method | Description | Typical use |
|---|---|---|
| `StockBrushes.pressurePen` | Pressure-sensitive pen with variable width | Primary writing and drawing tool |
| `StockBrushes.marker` | Uniform-width marker | Bold drawing and flat strokes |
| `StockBrushes.highlighter(selfOverlap = SelfOverlap.ANY)` | Semi-transparent highlighter (`SelfOverlap.ANY` uses `SelfOverlap.ACCUMULATE` on Android 14 (API level 34) and higher; pass `SelfOverlap.DISCARD` to suppress self-overlap darkening) | Highlighting over existing content |
| `StockBrushes.dashedLine` | Dashed line pattern | Borders, guides, and annotations |
| `StockBrushes.emojiHighlighter(clientTextureId, showMiniEmojiTrail = false, selfOverlap = SelfOverlap.ANY)` | Textured highlighter with an emoji tip (`showMiniEmojiTrail = false` works on all supported API levels; `showMiniEmojiTrail = true` requires Android 14 (API level 34) and higher) | Decorative highlighting |

### Emoji highlighter requirements

`StockBrushes.emojiHighlighter` requires two configurations:

1. **Android 14 (API level 34) and higher for `showMiniEmojiTrail`** : The mini-emoji particle trail uses `CanvasMeshRenderer`, which requires Android 14 (`Build.VERSION_CODES.UPSIDE_DOWN_CAKE`, API level 34) and higher. On Android 13 (API level 33) and lower, set `showMiniEmojiTrail = false`.
2. **User-supplied texture bitmaps** : `androidx.ink` doesn't bundle emoji bitmap files. Prompt the user to add the texture bitmap (for example, in `app/src/main/res/drawable-nodpi/`) and register its `clientTextureId` in `TextureBitmapStore`.


```kotlin
val supportsMiniEmojiTrail = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

val heartHighlighter = StockBrushes.emojiHighlighter(
    clientTextureId = "emoji-heart",
    showMiniEmojiTrail = supportsMiniEmojiTrail,
)
```

<br />

### Modify and switch brushes with alpha preservation

`Brush` is immutable. Derive updated brushes with `copy` and
`copyWithComposeColor`, read the Compose color with `brush.composeColor`, and
compare `BrushFamily` instances with structural equality (`==`, never `===`).
When switching to or from `StockBrushes.highlighter`, apply translucent alpha
(such as `0.3f`) for highlighters and restore `alpha = 1f` for opaque brushes:


```kotlin
private const val HIGHLIGHTER_ALPHA = 0.3f

fun switchBrushFamily(currentBrush: Brush, targetFamily: BrushFamily): Brush {
    val baseColor = currentBrush.composeColor
    val adjustedColor = if (targetFamily == StockBrushes.highlighter()) {
        baseColor.copy(alpha = HIGHLIGHTER_ALPHA)
    } else {
        baseColor.copy(alpha = 1f)
    }
    return currentBrush
        .copy(family = targetFamily)
        .copyWithComposeColor(color = adjustedColor)
}

fun recolorStroke(stroke: Stroke, newColor: Color): Stroke {
    val updatedBrush = stroke.brush.copyWithComposeColor(color = newColor)
    return stroke.copy(brush = updatedBrush)
}
```

<br />

## `TextureBitmapStore` and generation invalidation

Provide a singleton `TextureBitmapStore` with a reactive `generation` counter.
Because `CanvasStrokeRenderer` and `InProgressStrokes` cache `BitmapShader`
instances per `BrushPaint` on first draw, observe `generation` to recreate
`CanvasStrokeRenderer` with `remember(textureStore, cacheGen)` and use
`key(cacheGen)` to update the cache for `InProgressStrokes` whenever textures
load dynamically:


```kotlin
class AppTextureBitmapStore(context: Context) : TextureBitmapStore {
    private val resources = context.resources
    private val textureResources: Map<String, Int> = mapOf(
        "emoji-heart" to R.drawable.emoji_heart,
    )
    private val loadedBitmaps = mutableMapOf<String, Bitmap>()

    private val _generation = MutableStateFlow(0)
    val generation: StateFlow<Int> = _generation.asStateFlow()

    override operator fun get(clientTextureId: String): Bitmap? {
        return loadedBitmaps.getOrPut(clientTextureId) {
            textureResources[clientTextureId]?.let { resId ->
                BitmapFactory.decodeResource(resources, resId)
            } ?: return null
        }
    }

    fun loadTexture(textureId: String, bitmap: Bitmap) {
        loadedBitmaps[textureId] = bitmap
        _generation.update { it + 1 }
    }
}

val LocalTextureStore = staticCompositionLocalOf<AppTextureBitmapStore> {
    error("No AppTextureBitmapStore provided")
}
```

<br />

## Common pitfalls

- **Keeping `InProgressStrokes` active during eraser mode** : Remove `InProgressStrokes` from composition when `isEraserMode` is `true` so pointer events don't conflict.
- **Omitting `withSave` around `CanvasStrokeRenderer.draw`** : Always wrap each `draw` call in `nativeCanvas.withSave { }` to prevent matrix or clip leakage (where transformations or clipping bounds from one stroke accidentally bleed over and affect subsequent draws).
- **Violating `0f < epsilon <= size`** : Scaling a brush's `size` smaller than its `epsilon` throws `IllegalArgumentException`. Keep `epsilon >= 0.01f` to avoid excessive memory usage for stroke mesh data.
- **Using `BlendMode.SrcOver` for highlighters** : Without `BlendMode.Multiply` inside `withSaveLayer` and reduced color alpha (`0.3f`), highlighters occlude existing strokes instead of highlighting them.