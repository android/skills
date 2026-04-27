<br />


Applicable XR devices This guidance helps you build experiences for these types of XR devices. [Learn about XR device types →](https://developer.android.com/develop/xr/devices) ![](https://developer.android.com/static/images/develop/xr/ai-glasses-icon.svg) AI Glasses [](https://developer.android.com/develop/xr/devices#ai-glasses) [Learn about XR device types →](https://developer.android.com/develop/xr/devices)

<br />

In Jetpack Compose Glimmer, the [`TitleChip`](https://developer.android.com/reference/kotlin/androidx/xr/glimmer/TitleChip.composable#TitleChip(androidx.compose.ui.Modifier,kotlin.Function0,androidx.compose.ui.graphics.Shape,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,androidx.compose.foundation.BorderStroke,androidx.compose.foundation.layout.PaddingValues,kotlin.Function1)) component is designed to
provide brief, non-interactive label for associated content, such as a Card. Use
title chips to display concise information like a short title, a name, or a
status. Since title chips are not focusable or interactive, they serve a purely
informational role within the a Jetpack Compose Glimmer UI. For example, you
might provide a title chip labeled "Ingredients" next to a scrollable list of
ingredients.
![](https://developer.android.com/static/images/design/ui/glasses/guides/glasses_components_titlechip_anatomy.png) **Figure 1.** An example of some different styles of title chips in Jetpack Compose Glimmer.

Default title chip and Sticky title chip shown. Sticky title chips are displayed
with an outline.

1. Title chips label
2. Optional leading icon or entity

## Basic example: Display a short title chip

You can create a short title chip with very little code:

    TitleChip { Text("Messages") }

## Detailed example: Display a title chip with a card

To use a title chip with another component, place the title chip
[`TitleChipDefaults.AssociatedContentSpacing`](https://developer.android.com/reference/kotlin/androidx/xr/glimmer/TitleChipDefaults#AssociatedContentSpacing()) above the other component in
the composable. The following code shows how to use a title chip with a card:

    @Composable
    fun TitleChipExample() {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TitleChip { Text("Title Chip") }
            Spacer(Modifier.height(TitleChipDefaults.AssociatedContentSpacing))
            Card(
                title = { Text("Title") },
                subtitle = { Text("Subtitle") },
                leadingIcon = { Icon(FavoriteIcon, "Localized description") },
            ) {
                Text("Card Content")
            }
        }
    }

### Key points about the code

- The [`Spacer`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/Spacer.composable#Spacer(androidx.compose.ui.Modifier)) has a fixed height to provide the correct vertical spacing, defined by [`TitleChipDefaults.AssociatedContentSpacing`](https://developer.android.com/reference/kotlin/androidx/xr/glimmer/TitleChipDefaults#AssociatedContentSpacing()), between the two components.