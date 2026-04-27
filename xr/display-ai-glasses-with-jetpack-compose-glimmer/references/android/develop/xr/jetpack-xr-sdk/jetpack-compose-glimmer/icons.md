<br />


Applicable XR devices This guidance helps you build experiences for these types of XR devices. [Learn about XR device types →](https://developer.android.com/develop/xr/devices) ![](https://developer.android.com/static/images/develop/xr/ai-glasses-icon.svg) AI Glasses [](https://developer.android.com/develop/xr/devices#ai-glasses) [Learn about XR device types →](https://developer.android.com/develop/xr/devices)

<br />

In Jetpack Compose Glimmer, the [`Icon`](https://developer.android.com/reference/kotlin/androidx/xr/glimmer/Icon.composable#Icon(androidx.compose.ui.graphics.ImageBitmap,kotlin.String,androidx.compose.ui.Modifier)) component is specifically designed
for rendering single-color icons. `Icon` can accept [`ImageVector`](https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/vector/ImageVector),
[`ImageBitmap`](https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/ImageBitmap), or [`Painter`](https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/painter/Painter) as its source. `Icon`, similar to
[`Text`](https://developer.android.com/reference/kotlin/androidx/xr/glimmer/Text.composable#Text(kotlin.String,androidx.compose.ui.Modifier,androidx.compose.ui.graphics.Color,androidx.compose.foundation.text.TextAutoSize,androidx.compose.ui.unit.TextUnit,androidx.compose.ui.text.font.FontStyle,androidx.compose.ui.text.font.FontWeight,androidx.compose.ui.text.font.FontFamily,androidx.compose.ui.unit.TextUnit,androidx.compose.ui.text.style.TextDecoration,androidx.compose.ui.text.style.TextAlign,androidx.compose.ui.unit.TextUnit,androidx.compose.ui.text.style.TextOverflow,kotlin.Boolean,kotlin.Int,kotlin.Int,kotlin.Function1,androidx.compose.ui.text.TextStyle)), can intelligently apply a tint based on the surrounding UI theme.
While it defaults to a size provided by the [`LocalIconSize`](https://developer.android.com/reference/kotlin/androidx/xr/glimmer/package-summary#LocalIconSize()), you can also
set custom icon sizes.

## Example: Create a box with a large star icon

    @Composable
    fun GlimmerIconSample() {
        GlimmerTheme {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_star),
                        contentDescription = "A star icon from Google Symbols",
                        modifier = Modifier.size(GlimmerTheme.iconSizes.large),
                        tint = GlimmerTheme.colors.primary
                    )
                }
            }
        }
    }

### Key points about the code

- The icon's source loads a local XML vector drawable (`R.drawable.ic_star`) using [`painterResource`](https://developer.android.com/reference/kotlin/androidx/compose/ui/res/painterResource.composable#painterResource(kotlin.Int)), demonstrating the recommended approach for integrating icons into a Jetpack Compose Glimmer UI without external library overhead.
- The icon's size is customized by setting [`GlimmerTheme.iconSizes.large`](https://developer.android.com/reference/kotlin/androidx/xr/glimmer/IconSizes#large()) with a modifier, demonstrating how to override Jetpack Compose Glimmer's predefined sizing.
- The icon's tint color is customized by setting [`GlimmerTheme.colors.primary`](https://developer.android.com/reference/kotlin/androidx/xr/glimmer/Colors#primary()) using the tint parameter, applying single-color icon tinting for visual consistency.