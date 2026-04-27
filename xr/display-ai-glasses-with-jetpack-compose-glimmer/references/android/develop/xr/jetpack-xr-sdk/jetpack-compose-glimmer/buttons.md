<br />


Applicable XR devices This guidance helps you build experiences for these types of XR devices. [Learn about XR device types →](https://developer.android.com/develop/xr/devices) ![](https://developer.android.com/static/images/develop/xr/ai-glasses-icon.svg) AI Glasses [](https://developer.android.com/develop/xr/devices#ai-glasses) [Learn about XR device types →](https://developer.android.com/develop/xr/devices)

<br />

In Jetpack Compose Glimmer, the [`Button`](https://developer.android.com/reference/kotlin/androidx/xr/glimmer/Button.composable#Button(kotlin.Function0,androidx.compose.ui.Modifier,kotlin.Boolean,androidx.xr.glimmer.ButtonSize,kotlin.Function0,kotlin.Function0,androidx.compose.ui.graphics.Shape,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,androidx.compose.foundation.BorderStroke,androidx.compose.foundation.layout.PaddingValues,androidx.compose.foundation.interaction.MutableInteractionSource,kotlin.Function1)) component is an interactive
component that's optimized for AI glasses input, offering clear visual feedback
for their unfocused, focused, and pressed states to guide user actions.
![](https://developer.android.com/static/images/design/ui/glasses/guides/glasses_components_buttons.png) **Figure 1.** An example of some different styles of buttons in Jetpack Compose Glimmer.

## Example: Button variations

    @Composable
    fun GlimmerButtonExample() {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Basic Button
            Button(onClick = { /* Do something */ }) {
                Text("Test Button 1")
            }

            // Button with a leading icon
            Button(
                onClick = { /* Do something */ },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_favorite),
                        contentDescription = "Favorite icon"
                    )
                }
            ) {
                Text("Test Button 2")
            }

            // Button with leading and trailing icons
            Button(
                onClick = { /* Do something */ },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_favorite),
                        contentDescription = "Favorite icon"
                    )
                },
                trailingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_favorite),
                        contentDescription = "Favorite icon"
                    )
                }
            ) {
                Text("Test Button 3")
            }
        }
    }

### Key points about the code

- The button icons source local XML vector drawables (`R.drawable.ic_favorite`) using [`painterResource`](https://developer.android.com/reference/kotlin/androidx/compose/ui/res/painterResource.composable#painterResource(kotlin.Int)), replacing the `Icons.Default` library dependency for optimized asset loading.
- The `leadingIcon` and `trailingIcon` parameters are utilized to inject icon Composables into the button layout, demonstrating Jetpack Compose Glimmer's support for flexible icon positioning.
- The buttons use the default sizing configuration, which automatically manages internal padding and text scaling to align with standard Jetpack Compose Glimmer design specifications without explicit size modifiers.