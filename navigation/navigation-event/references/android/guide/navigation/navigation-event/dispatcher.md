To implement a robust navigation system, your app needs a centralized way to
handle back gestures and other navigation signals. This page describes how to
use [`NavigationEventDispatcher`](https://developer.android.com/reference/kotlin/androidx/navigationevent/NavigationEventDispatcher) to coordinate and distribute these
navigation events across your application.

## Declare a `NavigationEventDispatcher`

The `NavigationEventDispatcher` is the central component of the
`NavigationEvent` library. It acts as an event hub that dispatches
navigation-related events, such as back gestures and navigation transitions, to
registered listeners within your app. Components can subscribe to these events
to react to navigation changes or other system-driven navigation actions.

You should provide `NavigationEventDispatcher` instances through a
[`NavigationEventDispatcherOwner`](https://developer.android.com/reference/androidx/navigationevent/NavigationEventDispatcherOwner). This ensures that different parts of your
app can access the same dispatcher and observe navigation events in a consistent
and coordinated way.


```kotlin
class MyComponent: NavigationEventDispatcherOwner {
    override val navigationEventDispatcher: NavigationEventDispatcher =
        NavigationEventDispatcher()
}
```

<br />

If you are inside of a `ComponentActivity`, instead of implementing your own
dispatcher, you can retrieve the one provided for you.


```kotlin
class MyCustomActivity : ComponentActivity() {
    fun addMyHandler() {
        // navigationEventDispatcher provided by the ComponentActivity
        navigationEventDispatcher.addHandler(myNavigationEventHandler)
    }
}
```

<br />

## Add a `NavigationEventInput`

Now that you've registered the handler, you are set up to receive events.
However, you need to provide a source from which the events are generated with
`NavigationEventInput`.

`NavigationEventInput` is the platform-specific component that receives
raw system input and translates it into a standard `NavigationEvent` to be sent
to the `NavigationEventDispatcher`.

The following example is a custom implementation of a `NavigationEventInput`:


```kotlin
public class MyInput : NavigationEventInput() {
    @MainThread
    public fun backStarted(event: NavigationEvent) {
        dispatchOnBackStarted(event)
    }

    @MainThread
    public fun backProgressed(event: NavigationEvent) {
        dispatchOnBackProgressed(event)
    }

    @MainThread
    public fun backCancelled() {
        dispatchOnBackCancelled()
    }

    @MainThread
    public fun backCompleted() {
        dispatchOnBackCompleted()
    }
}
```

<br />

Next, provide that input to your dispatcher:


```kotlin
navigationEventDispatcher.addInput(MyInput())
```

<br />

> [!NOTE]
> **Note:** To provide a simple input, use the [`DirectNavigationEventInput`](https://developer.android.com/reference/androidx/navigationevent/DirectNavigationEventInput) class.

## Clean up resources with `dispose()`

To prevent memory leaks in a dynamic UI, every created
`NavigationEventDispatcher` instance must be explicitly removed from the
hierarchy using the `dispose()` method when the component it is tied to is
destroyed:


```kotlin
navigationEventDispatcher.dispose()
```

<br />

The `dispose()` method ensures a *cascading cleanup* by iteratively removing
the dispatcher and all of its descendants (children and grandchildren),
guaranteeing that all associated handlers are unregistered from the shared
system.

### Dispatcher hierarchy and control

The `NavigationEventDispatcher` supports a parent-child hierarchy, enabling
components nested deep within a UI (such as nested `NavHost`s or dialogs) to
participate in navigation event handling.

#### Create a child dispatcher

A child dispatcher is created by passing a reference to its parent dispatcher
during construction. All dispatchers in a hierarchy share the same
`NavigationEventProcessor` to maintain a global **Last-In, First-Out (LIFO)**
event ordering based on priority.

#### Hierarchical enabling

The dispatcher includes an `isEnabled` property that allows developers to enable
or disable an entire subtree of handlers at once.

When a parent dispatcher is disabled (`isEnabled = false`), all handlers
associated with that parent and any of its children will be ignored, regardless
of their individual enabled state.