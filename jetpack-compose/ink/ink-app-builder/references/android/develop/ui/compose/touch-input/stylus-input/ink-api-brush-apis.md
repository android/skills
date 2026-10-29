The [`Brush`](https://developer.android.com/reference/kotlin/androidx/ink/brush/Brush) APIs provide you with the tools to
define the visual style of your strokes. You can create brushes with different
colors, sizes, and families to achieve a variety of looks.


## Android skills

[View on GitHub](https://github.com/android/skills/tree/main/jetpack-compose/ink/ink-custom-brush-builder)

### Create custom Ink brushes

Use the `ink-custom-brush-builder` [Android skill](https://developer.android.com/tools/agents/android-skills) to design custom BrushFamily styles in Jetpack Compose with custom tips, dynamic behaviors, textures, and coats. To install the skill from the [Android CLI](https://developer.android.com/tools/agents/android-cli), run:

    android skills add ink-custom-brush-builder

<br />

## Create a brush

To create a brush, use the Compose [`Brush`](https://developer.android.com/reference/kotlin/androidx/ink/brush/Brush)
companion methods with named arguments such as
[`Brush.Companion.createWithComposeColor`](https://developer.android.com/reference/kotlin/androidx/ink/brush/Brush.Companion#(androidx.ink.brush.Brush.Companion).createWithComposeColor(androidx.ink.brush.BrushFamily,androidx.compose.ui.graphics.Color,kotlin.Float,kotlin.Float)).
This class lets you set the following properties:

- `family`: The style of the brush, analogous to a typeface or font in text. See [`StockBrushes`](https://developer.android.com/reference/kotlin/androidx/ink/brush/StockBrushes) for available [`BrushFamily`](https://developer.android.com/reference/kotlin/androidx/ink/brush/BrushFamily) values.
- `color`: The color of the brush. You can set the color using a [`ColorLong`](https://developer.android.com/reference/kotlin/androidx/annotation/ColorLong).
- `size`: The overall thickness of strokes created with the brush.
- `epsilon`: The smallest distance for which two points should be considered visually distinct for stroke generation geometry purposes. The ratio of epsilon and stroke points controls how much a stroke can be zoomed in on without artifacts, at the cost of memory. A good starting point for stroke units is 1 px, and a good starting point for epsilon is 0.1. Higher epsilon values use less memory but allow for less zoom before triangle artifacts appear. Experiment to find the right value for your use case.


```kotlin
val brush = Brush.createWithComposeColor(
    family = StockBrushes.pressurePen(),
    color = Color.Black,
    size = 5F,
    epsilon = 0.1F
)
```

<br />

## Modify brush properties

You can create a copy of an existing brush using the
[`copyWithComposeColor()`](https://developer.android.com/reference/kotlin/androidx/ink/brush/Brush#(androidx.ink.brush.Brush).copyWithComposeColor(androidx.compose.ui.graphics.Color,androidx.ink.brush.BrushFamily,kotlin.Float,kotlin.Float)) method, which lets you
change any of the brush's properties.


```kotlin
val redBrush = Brush.createWithComposeColor(
    family = StockBrushes.pressurePen(),
    color = Color.Red,
    size = 5F,
    epsilon = 0.1F
)

val blueBrush = redBrush.copyWithComposeColor(color = Color.Blue)
```

<br />

## Custom Brushes

While [`StockBrushes`](https://developer.android.com/reference/kotlin/androidx/ink/brush/StockBrushes) provides a versatile set of
common brushes, Ink API also offers an advanced path for creating entirely new
brush behaviors for unique artistic effects or to replicate specific existing
brushes for backward compatibility.

A custom `BrushFamily` is loaded from its serialized format. The required format
is the gzipped binary encoding of the
[BrushFamily protocol buffer](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:ink/ink-storage/src/commonMain/proto/brush_family.proto;l=1?q=brush_family.pr&sq). This lets you load and use
custom brush files today. Once deserialized, the custom `BrushFamily` can be
used to create a new `Brush` with a specific color and size, just like any of
the `StockBrushes` families.

> [!NOTE]
> **Note:** This interface is experimental and is subject to change.


```kotlin
class CustomBrushes(val context: Context) {

    companion object {
        private const val TAG = "CustomBrushes"
    }

    val brushes by lazy { loadCustomBrushes() }

    @OptIn(ExperimentalInkCustomBrushApi::class)
    private fun loadCustomBrushes(): List<CustomBrush> {
        val brushFiles = mapOf(
            "Calligraphy" to (R.raw.calligraphy to R.drawable.draw_24px),
            "Flag Banner" to (R.raw.flag_banner to R.drawable.flag_24px),
            "Graffiti" to (R.raw.graffiti to R.drawable.format_paint_24px),
            // ...
        )

        val loadedBrushes = brushFiles.mapNotNull { (name, pair) ->
            val (resourceId, icon) = pair
            val brushFamily = context.resources.openRawResource(resourceId).use { inputStream ->
                BrushFamily.decode(inputStream)
            }
            CustomBrush(name, icon, brushFamily)
        }
        return loadedBrushes
    }
}

data class CustomBrush(
    val name: String,
    val icon: Int,
    val brushFamily: BrushFamily
)
```

<br />