The following sections describe strategies for saving your back stack and
storing state associated with entries on your back stack.

## Save your back stack

Ensuring your app's navigation state persists across various lifecycle events,
including configuration changes and process death, is crucial for a good user
experience. In Navigation 3, you own your back stack, so there aren't strict
guidelines on how you should create or save it. However, Navigation 3 does offer
a convenience method that provides you with a saveable back stack:
[`rememberNavBackStack`](https://developer.android.com/reference/kotlin/androidx/navigation3/runtime/rememberNavBackStack.composable).

### Use `rememberNavBackStack`

The `rememberNavBackStack` composable function is designed to create a back
stack that persists across configuration changes and process death.

For `rememberNavBackStack` to function correctly, each key in your back stack
must adhere to specific requirements:

- **Implement `NavKey` interface** : Every key in the back stack must implement the [`NavKey`](https://developer.android.com/reference/kotlin/androidx/navigation3/runtime/NavKey) interface. This acts as a marker interface that signals to the library that the key can be saved.
- **Have the `@Serializable` annotation** : In addition to implementing `NavKey`, your key classes and objects must be marked with the [`@Serializable`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-serializable/) annotation.

The following snippet shows a correct implementation of `rememberNavBackStack`:


```kotlin
@Serializable
data object Home : NavKey

@Composable
fun NavBackStack() {
    val backStack = rememberNavBackStack(Home)
}
```

<br />

#### Remember a back stack with subtypes of `NavKey`

The `rememberNavBackStack` composable function returns a `NavBackStack<NavKey>`.
If your app defines its own subtype of `NavKey` from which all of its keys
inherit, you can preserve that typing by implementing a custom remember
function, as follows:


```kotlin
@Serializable
sealed interface MyAppNavKey : NavKey

@Serializable
data object ScreenA: MyAppNavKey

@Serializable
data class ScreenB(val id: String): MyAppNavKey

@Composable
fun rememberMyAppNavBackStack(vararg elements: MyAppNavKey): NavBackStack<MyAppNavKey> {
    return rememberSerializable(serializer = serializer()) {
        NavBackStack(*elements)
    }
}

@Composable
fun MyApp() {
    // defaultNavBackStack is NavBackStack<NavKey>
    val defaultNavBackStack = rememberNavBackStack(ScreenA)
    // myAppNavBackStack is NavBackStack<MyAppNavKey>
    val myAppNavBackStack = rememberMyAppNavBackStack(ScreenA)
}
```

<br />

For more examples---including how to handle open polymorphism---see
[`NavBackStackSamples`](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:navigation3/navigation3-runtime/samples/src/main/kotlin/androidx/navigation3/runtime/samples/NavBackStackSamples.kt).

### Alternative: Storing in a `ViewModel`

Another approach to managing your back stack is to store it in a `ViewModel`.
For persistence through process death when using a `ViewModel` or any other
custom storage, you need to:

- **Ensure your keys are serializable** : Just like with `rememberNavBackStack`, your navigation keys must be serializable.
- **Handle serialization and deserialization manually** : You're responsible for manually saving the serialized representation of each key to, and deserializing it from, persistent storage (e.g., `SharedPreferences`, a database, or a file) when your app is going into the background or being restored.

## Scoping `ViewModel` instances to `NavEntry` objects

`ViewModels` are used to retain UI-related state across configuration changes,
such as screen rotations. By default, `ViewModels` are scoped to the nearest
`ViewModelStoreOwner`, which is typically your `Activity` or `Fragment`.

However, you might want to scope a `ViewModel` to a specific `NavEntry` (i.e., a
specific screen or destination) on the back stack, rather than the entire
`Activity`. This ensures that the `ViewModel`'s state is retained only while
that particular `NavEntry` is part of the back stack, and is cleared when the
`NavEntry` is popped.

The `androidx.lifecycle:lifecycle-viewmodel-navigation3` add-on library provides
a [`NavEntryDecorator`](https://developer.android.com/guide/navigation/navigation-3/naventrydecorators) that facilitates this. This decorator provides a
`ViewModelStoreOwner` for each `NavEntry`. When you create a `ViewModel` inside a
`NavEntry`'s content (e.g., using `viewModel()` in Compose), it is automatically
scoped to that specific `NavEntry`'s key on the back stack. This means the
`ViewModel` is created when the `NavEntry` is added to the back stack, and
cleared when it's removed.

To use [`NavEntryDecorator`](https://developer.android.com/guide/navigation/navigation-3/naventrydecorators#apply-decorators) for scoping `ViewModel`s to `NavEntry`s, follow
these steps:

1. Add the `androidx.lifecycle:lifecycle-viewmodel-navigation3` dependency to your `app/build.gradle.kts` file.
2. Add the default [`rememberSaveableStateHolderNavEntryDecorator()`](https://developer.android.com/reference/kotlin/androidx/navigation3/runtime/rememberSaveableStateHolderNavEntryDecorator.composable) to the list of `entryDecorators` when constructing a `NavDisplay`.
3. Add [`rememberViewModelStoreNavEntryDecorator()`](https://developer.android.com/reference/kotlin/androidx/lifecycle/viewmodel/navigation3/rememberViewModelStoreNavEntryDecorator.composable) to the list of `entryDecorators`.


```kotlin
NavDisplay(
    entryDecorators = listOf(
        // Add the default decorators for managing scenes and saving state
        rememberSaveableStateHolderNavEntryDecorator(),
        // Then add the view model store decorator
        rememberViewModelStoreNavEntryDecorator()
    ),
    backStack = backStack,
    entryProvider = entryProvider { },
)
```

<br />