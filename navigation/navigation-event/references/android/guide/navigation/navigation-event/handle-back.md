You can extend the abstract class `NavigationEventHandler` to handle navigation
events across platforms. This class provides methods corresponding to the
lifecycle of a navigation gesture.


```kotlin
val myHandler = object: NavigationEventHandler<NavigationEventInfo>(
    initialInfo = NavigationEventInfo.None,
    isBackEnabled = true
) {
    override fun onBackStarted(event: NavigationEvent) {
        // Prepare for the back event
    }

    override fun onBackProgressed(event: NavigationEvent) {
        // Use event.progress for predictive animations
    }

    // This is the required method for final event handling
    override fun onBackCompleted() {
        // Complete the back event
    }

    override fun onBackCancelled() {
        // Cancel the back event
    }
}
```

<br />

The `addHandler` function connects the handler to the dispatcher:


```kotlin
navigationEventDispatcher.addHandler(myHandler)
```

<br />

Call `myHandler.remove()` to remove the handler from the dispatcher:


```kotlin
myHandler.remove()
```

<br />

Handlers are invoked based on priority, and then by recency. All
[`PRIORITY_OVERLAY`](https://developer.android.com/reference/kotlin/androidx/navigationevent/NavigationEventDispatcher#PRIORITY_OVERLAY()) handlers are called before any [`PRIORITY_DEFAULT`](https://developer.android.com/reference/kotlin/androidx/navigationevent/NavigationEventDispatcher#PRIORITY_DEFAULT())
handlers. Within each priority group, handlers are invoked in a Last-In,
First-Out (LIFO) order --- the most recently added handler is called first.

## Intercept back with Jetpack Compose

For Jetpack Compose, the library provides a utility composable to manage the
dispatcher hierarchy.

The `NavigationBackHandler` composable creates a `NavigationEventHandler` for
its content and links it to the `LocalNavigationEventDispatcherOwner`. It uses
Compose's `DisposableEffect` to automatically call the dispatcher's `dispose()`
method when the composable leaves the screen, safely managing resources.


```kotlin
@Composable
public fun NavigationBackHandler(
    state: NavigationEventState<out NavigationEventInfo>,
    isBackEnabled: Boolean = true,
    onBackCancelled: () -> Unit = {},
    onBackCompleted: () -> Unit,
){

}
```

<br />

This function lets you control event handling precisely within localized UI
subtrees.


```kotlin
@Composable
fun HandlingBackWithTransitionState(
    onNavigateUp: () -> Unit
) {
    val navigationState = rememberNavigationEventState(
        currentInfo = NavigationEventInfo.None
    )
    val transitionState = navigationState.transitionState
    // React to predictive back transition updates
    when (transitionState) {
        is NavigationEventTransitionState.InProgress -> {
            val progress = transitionState.latestEvent.progress
            // Use progress (0f..1f) to update UI during the gesture
        }
        is NavigationEventTransitionState.Idle -> {
            // Reset any temporary UI state if the gesture is cancelled
        }
    }
    NavigationBackHandler(
        state = navigationState,
        onBackCancelled = {
            // Called if the back gesture is cancelled
        },
        onBackCompleted = {
            // Called when the back gesture fully completes
            onNavigateUp()
        }
    )
}
```

<br />

This example shows how to observe predictive back gesture updates using
[`NavigationEventTransitionState`](https://developer.android.com/reference/kotlin/androidx/navigationevent/NavigationEventTransitionState). The `progress` value can be used to
update UI elements in response to the back gesture, while handling completion
and cancellation through `NavigationBackHandler`.

### Access the back gesture or swipe edge in Compose

> [!NOTE]
> **Note:** For Android, if you're already using a navigation library with built-in Predictive Back support, like [Navigation 3](https://developer.android.com/guide/navigation/navigation-3/animate-destinations), use that instead of implementing the guidance here. The following section shows how to create a Predictive Back animation using only `NavigationEvent` and Compose.

**Figure 1** . A predictive back animation built with `NavigationEvent` and Compose.

To animate the screen while the user swipes back, you'll need to (a) check if
the `NavigationEventTransitionState` is `InProgress`, and (b) observe the
progress and swipe edge state with `rememberNavigationEventState`:

- `progress`: A Float from `0.0` to `1.0` indicating how far the user has swiped.
- `swipeEdge`: An integer constant (`EDGE_LEFT` or `EDGE_RIGHT`) indicating where the gesture started.

The following snippet is a simplified example of how to implement a scale and
shift animation:


```kotlin
object Routes {
    const val SCREEN_A = "Screen A"
    const val SCREEN_B = "Screen B"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var state by remember { mutableStateOf(Routes.SCREEN_A) }
            val backEventState = rememberNavigationEventState<NavigationEventInfo>(currentInfo = NavigationEventInfo.None)
            when (state) {
                Routes.SCREEN_A -> {
                    ScreenA(onNavigate = { state = Routes.SCREEN_B })
                }
                else -> {
                    if (backEventState.transitionState is NavigationEventTransitionState.InProgress) {
                        ScreenA(onNavigate = { })
                    }
                    ScreenB(
                        backEventState = backEventState,
                        onBackCompleted = { state = Routes.SCREEN_A }
                    )
                }
            }
        }
    }
}

@Composable
fun ScreenB(
    backEventState: NavigationEventState<NavigationEventInfo>,
    onBackCompleted: () -> Unit = {},
) {
    val transitionState = backEventState.transitionState
    val latestEvent =
        (transitionState as? NavigationEventTransitionState.InProgress)
            ?.latestEvent
    val backProgress = latestEvent?.progress ?: 0f
    val swipeEdge = latestEvent?.swipeEdge ?: NavigationEvent.EDGE_LEFT
    if (transitionState is NavigationEventTransitionState.InProgress) {
        Log.d("BackGesture", "Progress: ${transitionState.latestEvent.progress}")
    } else if (transitionState is NavigationEventTransitionState.Idle) {
        Log.d("BackGesture", "Idle")
    }
    val animatedScale by animateFloatAsState(
        targetValue = 1f - (backProgress * 0.1f),
        label = "ScaleAnimation"
    )
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val maxShift = remember(windowInfo, density) {
        val widthDp = with(density) { windowInfo.containerSize.width.toDp() }
        (widthDp.value / 20f) - 8
    }
    val offsetX = when (swipeEdge) {
        NavigationEvent.EDGE_LEFT -> (backProgress * maxShift).dp
        NavigationEvent.EDGE_RIGHT -> (-backProgress * maxShift).dp
        else -> 0.dp
    }
    NavigationBackHandler(
        state = backEventState,
        onBackCompleted = onBackCompleted,
        isBackEnabled = true
    )
    Box(
        modifier = Modifier
            .offset(x = offsetX)
            .scale(animatedScale)
    ){
        // Rest of UI
    }
}
```

<br />