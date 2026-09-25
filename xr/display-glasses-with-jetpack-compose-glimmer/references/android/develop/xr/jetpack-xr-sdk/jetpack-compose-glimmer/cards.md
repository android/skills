<br />


Applicable XR devices This guidance helps you build experiences for these types of XR devices. [Learn about XR device types →](https://developer.android.com/develop/xr/devices) ![](https://developer.android.com/static/images/develop/xr/ai-glasses-icon.svg) Display Glasses [](https://developer.android.com/develop/xr/devices#audio-display) [Learn about XR device types →](https://developer.android.com/develop/xr/devices)

<br />

In Jetpack Compose Glimmer, the [`Card`](https://developer.android.com/reference/kotlin/androidx/xr/glimmer/Card.composable) component serves as the primary
container for related content, creating a clear visual boundary for digestible
units of information. Cards are highly adaptable, supporting combinations of
main content, optional titles, subtitles, and leading or trailing icons. Cards
fill the maximum available width by default, are focusable, and you can also
make them clickable. Cards support a vertical arrangement where the header image
is top-most, followed by the title, subtitle, and body content.

Cards are built on the Jetpack Compose Glimmer [surface system](https://developer.android.com/develop/xr/jetpack-xr-sdk/jetpack-compose-glimmer/surface), so they
inherit physical properties like depth effects, clipping, and consistent border
highlights.
![](https://developer.android.com/static/images/design/ui/glasses/guides/glasses_components_cards.png) **Figure 1.** An example of some different styles of cards in Jetpack Compose Glimmer.

## Anatomy and slots

A Jetpack Compose Glimmer Card is built from several specialized elements that
let you customize its content and layout.

| Slot | Description |
|---|---|
| Header | The top section of the card, designed to hold an image. |
| Title and subtitle | These text fields provide the main and secondary labels for the card. The title is placed above the subtitle. |
| Leading icon | An optional icon (typically an [`Icon`](https://developer.android.com/reference/kotlin/androidx/xr/glimmer/Icon.composable)) that appears at the beginning of the card's content area. |
| Trailing icon | An optional icon that appears at the end of the card's content area. |
| Main content | The core body of the card for primary [`Text`](https://developer.android.com/reference/kotlin/androidx/xr/glimmer/Text.composable) or content. |

## Card defaults

The following defaults apply to cards:

- **Width**: Cards fill the maximum available width of the display to optimize legibility on display glasses.
- **Minimum height** : `80.dp`
- **Text vertical spacing** : `3.dp`
- **Header shape** : Uses [`RoundedCornerShape`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/shape/package-summary#RoundedCornerShape(androidx.compose.ui.unit.Dp)) with `24.dp` corners
- **Content padding** : Defaults to [`GlimmerTheme.componentSpacingValues.medium`](https://developer.android.com/reference/kotlin/androidx/xr/glimmer/ComponentSpacingValues#medium()). This affects the outermost padding around header images and the content container.
- **Shape** : Defaults to [`GlimmerTheme.shapes.medium`](https://developer.android.com/reference/kotlin/androidx/xr/glimmer/Shapes#medium()).
- **Text rendering** : Uses the default values from
  [`GlimmerTheme.typography`](https://developer.android.com/reference/kotlin/androidx/xr/glimmer/GlimmerTheme#typography()) for the following slots:

  - Title: [`bodyMedium`](https://developer.android.com/reference/kotlin/androidx/xr/glimmer/Typography#bodyMedium())
  - Subtitle: [`caption`](https://developer.android.com/reference/kotlin/androidx/xr/glimmer/Typography#caption())
  - Main content: [`bodySmall`](https://developer.android.com/reference/kotlin/androidx/xr/glimmer/Typography#bodySmall())

## Example: Basic card

The following code shows the minimal code needed to build a card:


```kotlin
Card { Text("This is a card") }
```

<br />

## Example: Card with multiple slots

The following code shows how to use multiple slots together to build a card:


```kotlin
Card(
    title = { Text("Card Title", style = GlimmerTheme.typography.titleMedium) },
    subtitle = { Text("Sub-heading text", style = GlimmerTheme.typography.titleSmall) },
    leadingIcon = { Icon(FavoriteIcon, contentDescription = "Favorite") },
    trailingIcon = { Icon(BookmarkIcon, contentDescription = "Bookmark") }
) {
    Text("This is a complete card")
}
```

<br />

### Key points about the code

- Shows how to use various card elements, such as [`title`](https://developer.android.com/reference/kotlin/androidx/xr/glimmer/Card.composable), [`subtitle`](https://developer.android.com/reference/kotlin/androidx/xr/glimmer/Card.composable), and [`leadingIcon`](https://developer.android.com/reference/kotlin/androidx/xr/glimmer/Card.composable), to customize the card's content and structure.
- Uses the [`Card`](https://developer.android.com/reference/kotlin/androidx/xr/glimmer/Card.composable#Card(kotlin.Function0,androidx.compose.ui.Modifier,kotlin.Function0,kotlin.Function0,kotlin.Function0,kotlin.Function0,kotlin.Function0,androidx.compose.ui.graphics.Shape,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,androidx.compose.foundation.BorderStroke,androidx.compose.foundation.layout.PaddingValues,androidx.compose.foundation.interaction.MutableInteractionSource,kotlin.Function0)) overload with `onClick` to make the entire card surface interactive.

## Action cards

If you want to include an action button with a card, use an
[`ActionCard`](https://developer.android.com/reference/kotlin/androidx/xr/glimmer/ActionCard.composable). Action cards have the same elements as standard cards and
also include an additional `action` parameter that defines the bottom button.
![](https://developer.android.com/static/images/design/ui/xr/guides/actioncard.png) **Figure 2.** An example of an `ActionCard` in Jetpack Compose Glimmer.

### Example: Basic action card

This code shows how to create an `ActionCard`:


```kotlin
ActionCard(
    action = {
        Button(onClick = {}, trailingIcon = { Icon(FavoriteIcon, "Localized description") }) {
            Text("Send")
        }
    }
) {
    Text("This is a card with a title, leading icon, header image, and action")
}
```

<br />

### Example: Action card with an icon

This code shows how to create a more complex `ActionCard` with icons:


```kotlin
ActionCard(
    title = { Text("Card Title", style = GlimmerTheme.typography.titleMedium) },
    subtitle = { Text("Sub-heading text", style = GlimmerTheme.typography.titleSmall) },
    leadingIcon = { Icon(FavoriteIcon, contentDescription = "Favorite") },
    action = {
        Button(onClick = { /* Handle action */ }) {
            Text("Action")
        }
    }
) {
    Text(
        "This is the main body content of the card, utilizing theme-tokens for consistent styling.",
        style = GlimmerTheme.typography.bodyMedium
    )
}
```

<br />