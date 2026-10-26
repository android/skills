Reference for adding `BrushPaint.TilingTexture` and `BrushPaint.StampingTexture`
layers with `TextureBitmapStore`, and encoding or decoding custom `BrushFamily`
definitions (`androidx.ink` `1.1.0-alpha03` and higher).

## `TextureBitmapStore` and texture assets

`androidx.ink` doesn't bundle texture image assets. When generating a textured
brush, inform the user to add the referenced PNG or WebP texture file to their
project (such as `app/src/main/res/drawable-nodpi/pencil_grain.png` to prevent
density scaling) and return its `Bitmap` from `TextureBitmapStore`. If
`TextureBitmapStore[clientTextureId]` returns `null`, the stroke silently
renders as a flat, untextured fill.


```kotlin
class CustomBrushTextureStore(context: Context) : TextureBitmapStore {
    private val resources = context.resources
    private val textureResources: Map<String, Int> = mapOf(
        "pencil-grain" to R.drawable.pencil_grain,
        "watercolor-wash" to R.drawable.watercolor_wash,
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

    fun registerBitmap(textureId: String, bitmap: Bitmap) {
        loadedBitmaps[textureId] = bitmap
        _generation.update { it + 1 }
    }
}
```

<br />

When `generation` increments after loading a texture dynamically (such as from a
decoded `.brush` stream), recreate `CanvasStrokeRenderer` with
`remember(textureStore, generation)` and re-key `InProgressStrokes` with
`key(generation)` so cached `BitmapShader` instances refresh.

## `TextureLayer` subclasses (`TilingTexture` and `StampingTexture`)

In `1.1.0-alpha03` and higher, `BrushPaint.TextureLayer` is an abstract class
with two concrete subclasses:

- **`BrushPaint.TilingTexture`** : Repeats a seamless texture across stroke geometry (ideal for pencil grain, paper texture, or watercolor wash). Set `sizeUnit = BrushPaint.TextureLayer.SizeUnit.BRUSH_SIZE` so tile dimensions scale with `Brush.size`.
- **`BrushPaint.StampingTexture`** : Stamps a texture onto each discrete particle along a stroke (`BrushTip.particleGapDistanceScale > 0f` or `particleGapDurationMillis > 0L`). Requires `CanvasMeshRenderer` on Android 14 (API level 34) and higher hardware `Canvas`; supply a second path-compatible fallback `BrushPaint` in `BrushCoat.paintPreferences` if the stroke must also render on Android 13 (API level 33) and lower or software `Canvas(bitmap)`.

### Choose `BlendMode` for texture layers

The **final (or only) `TextureLayer`** blends the texture (`src`) against the
brush color (`dst`, which carries edge anti-aliasing and per-vertex
`OPACITY_MULTIPLIER`). Use a blend mode whose output alpha is proportional to
`Alpha_dst`---such as **`BlendMode.MODULATE`** (default), **`BlendMode.DST_IN`** ,
**`BlendMode.DST_OUT`** , **`BlendMode.SRC_ATOP`** , or **`BlendMode.SRC_IN`** .
Never use `BlendMode.SRC_OVER` on the final `TextureLayer` because it overrides
edge anti-aliasing and ignores `OPACITY_MULTIPLIER` wherever the texture is
opaque:


```kotlin
fun createTexturedBrushFamily(): BrushFamily {
    val grainPaint = BrushPaint(
        selfOverlap = SelfOverlap.ANY,
        textureLayers = listOf(
            BrushPaint.TilingTexture(
                clientTextureId = "pencil-grain",
                sizeX = 1.0f,
                sizeY = 1.0f,
                sizeUnit = BrushPaint.TextureLayer.SizeUnit.BRUSH_SIZE,
                blendMode = BrushPaint.TextureLayer.BlendMode.MODULATE,
            ),
        ),
        colorFunctions = listOf(BrushPaint.ColorFunction.OpacityMultiplier(0.85f)),
    )

    return BrushFamily(
        tip = BrushTip(scaleX = 1f, scaleY = 1f, cornerRounding = 1f),
        paint = grainPaint,
    )
}
```

<br />

## Serialize and deserialize custom `BrushFamily` definitions

`androidx.ink.storage` provides two equivalent gzipped-protobuf serialization
APIs (unannotated in `1.1.0-alpha02` and higher; requires
`@OptIn(ExperimentalInkCustomBrushApi::class)` only on `1.0.0` stable):

1. **Kotlin extensions** : `brushFamily.encode(outputStream, textureBitmapStore)` and `BrushFamily.decode(inputStream, maxVersion, getClientTextureId)`.
2. **Java-friendly `@JvmStatic` methods** : `AndroidBrushFamilySerialization.encode` and `AndroidBrushFamilySerialization.decode`.

### Save and load a `BrushFamily` with embedded textures

When decoding with a texture callback in `1.1.0-alpha`, always wrap the lambda
in **`BrushFamilyDecodeCallback { textureId, bitmap -> ... }`** to disambiguate
from the multiplatform `OnDecodeTexturePngBytes` overload. Pass
`maxVersion = Version.MAX_SUPPORTED` for production brushes or
`Version.DEVELOPMENT` when testing experimental brush features:


```kotlin
suspend fun saveCustomBrushToFile(
    context: Context,
    uri: Uri,
    brushFamily: BrushFamily,
    textureBitmapStore: TextureBitmapStore,
) = withContext(Dispatchers.IO) {
    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
        brushFamily.encode(outputStream, textureBitmapStore)
    }
}

suspend fun loadCustomBrushFromFile(
    inputStream: InputStream,
    textureStore: CustomBrushTextureStore,
    allowDevelopmentFeatures: Boolean = false,
): BrushFamily = withContext(Dispatchers.IO) {
    val maxVersion = if (allowDevelopmentFeatures) {
        Version.DEVELOPMENT
    } else {
        Version.MAX_SUPPORTED
    }
    inputStream.use { stream ->
        BrushFamily.decode(
            input = stream,
            maxVersion = maxVersion,
            getClientTextureId = BrushFamilyDecodeCallback { textureId, bitmap ->
                if (bitmap != null) {
                    textureStore.registerBitmap(textureId, bitmap)
                }
                textureId
            },
        )
    }
}
```

<br />

### Store custom brushes in a Room database (`BLOB`)

Persist encoded `BrushFamily` bytes in a `ColumnInfo.BLOB` `ByteArray` column:


```kotlin
@Entity(tableName = "custom_brushes")
data class CustomBrushEntity(
    @PrimaryKey val name: String,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val brushBytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CustomBrushEntity) return false
        return name == other.name && brushBytes.contentEquals(other.brushBytes)
    }

    override fun hashCode(): Int = 31 * name.hashCode() + brushBytes.contentHashCode()
}

fun encodeBrushForDatabase(
    brushFamily: BrushFamily,
    textureBitmapStore: TextureBitmapStore = TextureBitmapStore { null },
): ByteArray {
    return ByteArrayOutputStream().use { stream ->
        brushFamily.encode(stream, textureBitmapStore)
        stream.toByteArray()
    }
}
```

<br />

## Common pitfalls

- **Instantiating abstract `BrushPaint.TextureLayer` directly** : In `1.1.0-alpha03` and higher, instantiate `BrushPaint.TilingTexture` or `BrushPaint.StampingTexture` (`SizeUnit`, `BlendMode`, and `Wrap` remain nested on `BrushPaint.TextureLayer.*`, while `Origin` is nested on `BrushPaint.TilingTexture.Origin`).
- **Passing an untyped trailing lambda to `BrushFamily.decode`** : Kotlin can't disambiguate between `BrushFamilyDecodeCallback` (`Bitmap?`) and `OnDecodeTexturePngBytes` (`ByteArray?`). Always wrap with `BrushFamilyDecodeCallback { id, bitmap -> ... }`.
- **Oversized texture bitmaps** : Keep tiled textures around 256×256 or 512×512 pixels in `res/drawable-nodpi/` to avoid unnecessary GPU memory consumption.