<br />


Applicable XR devices This guidance helps you build experiences for these types of XR devices. [Learn about XR device types →](https://developer.android.com/develop/xr/devices) ![](https://developer.android.com/static/images/develop/xr/ai-glasses-icon.svg) AI Glasses [](https://developer.android.com/develop/xr/devices#ai-glasses) [Learn about XR device types →](https://developer.android.com/develop/xr/devices)

<br />

All Jetpack Compose Glimmer components are designed to work with standard input
methods similar to phones, such as a tap or swipe on the AI glasses' touchpad,
while also being receptive to input commands that are specific to AI glasses
hardware, such as [camera and display buttons](https://developer.android.com/design/ui/ai-glasses/guides/interaction/inputs). Jetpack Compose Glimmer
components automatically handle the necessary input events. For custom
components, you can utilize existing Compose APIs like
[`Modifier.draggable`](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier#(androidx.compose.ui.Modifier).draggable(androidx.compose.foundation.gestures.DraggableState,androidx.compose.foundation.gestures.Orientation,kotlin.Boolean,androidx.compose.foundation.interaction.MutableInteractionSource,kotlin.Boolean,kotlin.coroutines.SuspendFunction2,kotlin.coroutines.SuspendFunction2,kotlin.Boolean)) or [`Modifier.scrollable`](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier#(androidx.compose.ui.Modifier).scrollable(androidx.compose.foundation.gestures.ScrollableState,androidx.compose.foundation.gestures.Orientation,kotlin.Boolean,kotlin.Boolean,androidx.compose.foundation.gestures.FlingBehavior,androidx.compose.foundation.interaction.MutableInteractionSource)) to implement specific
interaction behaviors.

On AI glasses with a display, pointer input can affect focus:

- **Tap**: Direct interaction for activating element. Focus moves to an element when a user interacts with it.
- **Swipe**: Used for navigation and scrolling. Unhandled swipe gestures automatically translate into focus movements, enabling seamless UI navigation without direct pointer input.

<br />

> [!WARNING]
> **(Temporary requirement) Enable required input flag** :
>
> <br />
>
> Using a feature of Jetpack Compose, the system can automatically set the initial
> focus to the very-first focusable element when the screen loads, which is often
> the top-left item on the screen. This feature is still in development and isn't
> enabled by default. To activate this feature, set the
> `isInitialFocusOnFocusableAvailable` flag to `true` in your activity's
> [`onCreate()`](https://developer.android.com/reference/kotlin/android/app/Activity#onCreate(android.os.Bundle)) method.
>
>     class GlassesActivityExample : ComponentActivity() {
>         override fun onCreate(savedInstanceState: Bundle?) {
>             @OptIn(ExperimentalComposeUiApi::class)
>             ComposeUiFlags.isInitialFocusOnFocusableAvailable = true
>             super.onCreate(savedInstanceState)
>         }
>     }
>
> <br />
>
<br />

## Navigation behavior and order

Focus movement and order change as a user navigates your app.

### Focus movement

On a scrollable container, focus moves continuously with a swipe on the
touchpad. For discrete elements like a row of buttons, each swipe moves the
focus one element at a time.

### Focus order

Just like in Jetpack Compose, Jetpack Compose Glimmer uses one-dimensional focus
search. To learn more about the order of focus traversal, see [Change focus
traversal order](https://developer.android.com/develop/ui/compose/touch-input/focus/change-focus-traversal-order#override-one-dimensional).

To change the initially-focused item, you can add a top-level
[`Modifier.focusGroup()`](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier#(androidx.compose.ui.Modifier).focusGroup()) and specify a custom `onEnter`
[`focusProperty`](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier#(androidx.compose.ui.Modifier).focusProperties(kotlin.Function1)):

    Modifier.focusProperties {
        onEnter = {
            initialFocus.requestFocus()
            cancelFocusChange()
        }
    }
    .focusGroup()

### Scrolling containers

For an optimal user experience, scrolling containers like lists should be the
only major component on a screen. Avoid placing a scrollable list directly above
or below other interactive elements, such as buttons, to prevent navigation
confusion and promote smooth, predictable focus movement.

## Default focus states

Jetpack Compose Glimmer implements default focus states across its interactable
components, including surfaces, cards, and list items, promoting consistent and
clear visual feedback during user interaction.
![](https://developer.android.com/static/images/design/ui/glasses/guides/glasses_ixd_inputs_focus.png) **Figure 1.** Three focus states in Jetpack Compose Glimmer, which are differentiated using outline-based visual feedback.

- **Default** : The button's background color is derived from
  [`GlimmerTheme.colors.surface`](https://developer.android.com/reference/kotlin/androidx/xr/glimmer/Colors#surface()), its main content calculates the content
  color of that surface, and icons are [`GlimmerTheme.colors.primary`](https://developer.android.com/reference/kotlin/androidx/xr/glimmer/Colors#primary()).

- **Focused**: The border width is increased to communicate focus.

- **Focused + Pressed** : The background is set to
  `GlimmerTheme.colors.surface` at full opacity to communicate its selected
  state.