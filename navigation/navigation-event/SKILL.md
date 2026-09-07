---
name: navigation-event
description: Intercept back gestures and run Predictive Back animations using the
  NavigationEvent (androidx.navigationevent) library in Compose Android. Handles Activity
  setup, parent-child dispatcher scoping in `ViewPagers` or tabs, Compose `NavigationBackHandler`,
  and migration from legacy `BackHandler` on SDK 36+.
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-09-01'
  keywords:
  - Android
  - Navigation Event
  - Jetpack Compose
  - Back Navigation
  - Dispatcher
  - Guidelines
  - Troubleshooting
  - ComponentActivity
  - Dialog
  - ViewPager
---

## Common guidelines

- **For architecture concepts** : To understand the foundational architecture, continuous gesture event lifecycles, or class definitions of the Navigation Event library, read [Navigation Event overview](references/android/guide/navigation/navigation-event/index.md).
- **For Android target** : If compile SDK is lower than 36, set it to `36` or higher in `build.gradle.kts`.
- **For Compose Android target**: The project must use Jetpack Compose for Compose-specific APIs. This skill is scoped exclusively to Compose Android (Android Views and non-Compose implementations are excluded).
- **For activity dispatchers** : `ComponentActivity` automatically implements `NavigationEventDispatcherOwner` out-of-the-box. You must use the built-in `navigationEventDispatcher` without creating anonymous delegate owners or overriding member properties.
- **For dialog scoping** : Floating windows (Compose `Dialog`, `ModalBottomSheet`, `ComponentDialog`) automatically provide a `NavigationEventDispatcherOwner`. You don't need manual `CompositionLocalProvider` propagation for dialogs.
- **For parent-child dispatcher hierarchies** : When scoping navigation handling to `ViewPagers`, tabbed interfaces, or nested navigation containers in Compose, use `rememberNavigationEventDispatcherOwner()` to create a child owner linked to the parent. Disabling the owner (`enabled = false`) automatically cascades to disable all child handlers.
- **For Compose handlers** : A one-to-one relationship between `NavigationEventState` and handlers is strictly enforced. Never bind the same `NavigationEventState` to multiple active `NavigationBackHandler` instances (`IllegalArgumentException`).

## Step 1: Plan

To complete this step, you **MUST** ensure the following:

1. **Identify the target platform** : Verify the app is targeting Compose Android. If `compileSdk` is lower than 36, set it to `36` or higher in `build.gradle.kts`.
2. **Navigation check**: Check if Navigation 3 is in use. If it is in use, use Navigation 3's built-in back navigation support rather than manually implementing low-level dispatchers from this skill.
3. **Hierarchy check** : Identify host Activities, `ViewPagers`, tabbed interfaces, or nested navigation hosts that require back gesture interception or parent-child dispatcher linking.
4. **Migration check** : Check if the project is migrating from back handling (`OnBackPressedCallback`, `BackHandler`, `onBackPresser`) to `NavigationEvent` and `NavigationBackHandler`.
5. **Input interception** : Detect where the app is intercepting navigation events from gestures or hardware button presses requiring translation to `NavigationEvent`.

## Step 2: Set up dependencies

To complete this step, you **MUST** ensure the following:

- For setting up compile SDKs, declaring catalog versions, and adding dependencies, follow [setup guide](references/android/guide/navigation/navigation-event/setup.md).

## Step 3: Configure dispatcher and inputs

To complete this step, you **MUST** ensure the following:

- To configure your dispatcher, leverage automatic `ComponentActivity` or `ComponentDialog` owner resolution.
- Link parent-child dispatchers in Compose following [dispatcher guide](references/android/guide/navigation/navigation-event/dispatcher.md).

## Step 4: Handle back navigation and UI transitions

To complete this step, you **MUST** ensure the following:

- To create navigation event handlers, integrate back gesture interception in Compose, animate UI components during swipes, and migrate from legacy back handlers, follow [handle back guide](references/android/guide/navigation/navigation-event/handle-back.md).

## Step 5: Clean up resources

> [!WARNING]
> **Warning:** Compose APIs perform teardown automatically. When using Compose APIs such as `NavigationBackHandler` and `rememberNavigationEventDispatcherOwner()`, handler removal and dispatcher disposal occur automatically when the composable leaves the composition.

You **MUST** perform explicit manual cleanup only when managing custom
dispatchers or non-Compose handlers:

- Call `remove()` on active handlers during teardown.
- Call `isEnabled = false` to temporarily disable navigation subtrees.
- Call `dispose()` on dispatcher instances when hosting components are destroyed. Disposing a parent dispatcher automatically cascades to all child dispatchers.

## Core troubleshooting guidelines

### 1. Activity dispatcher setup (StackOverflowError recursion)

`ComponentActivity` implements `NavigationEventDispatcherOwner` automatically
out-of-the-box. Don't override `navigationEventDispatcher` or wrap it in an
anonymous delegate owner.

#### RIGHT

**Why this is RIGHT** : Compose apps use `ComponentActivity` as the host.
`LocalNavigationEventDispatcherOwner.current` automatically resolves the
Activity's built-in dispatcher.


```kotlin
// RIGHT
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationContent()
        }
    }
}
```

<br />

#### WRONG

**Why this is WRONG** : Implementing `NavigationEventDispatcherOwner` directly on
`MainActivity` and overriding `navigationEventDispatcher` with a new instance
shadows the library's extension property, causing a recursive infinite loop
crash on launch (`StackOverflowError`). Creating redundant anonymous delegate
owners (`object : NavigationEventDispatcherOwner`) is unnecessary.


```kotlin
// WRONG
class MainActivity : ComponentActivity(), NavigationEventDispatcherOwner {
    override val navigationEventDispatcher = NavigationEventDispatcher() // Shadow loop crash
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationContent()
        }
    }
}
```

<br />

### 2. Floating window and dialog scoping (automatic ComponentDialog owner)

Floating windows (Compose `Dialog`, `ModalBottomSheet`, and any window backed by
`ComponentDialog`) automatically provide a `NavigationEventDispatcherOwner`.
Don't manually re-provide `LocalNavigationEventDispatcherOwner` using
`CompositionLocalProvider` inside dialogs.

#### RIGHT

**Why this is RIGHT** : `ComponentDialog` handles navigation dispatchers
automatically. Compose `Dialog` components resolve their dispatcher owner
out-of-the-box without manual propagation.


```kotlin
// RIGHT
@Composable
fun MyDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
        NavigationBackHandler(
            state = navigationState,
            onBackCompleted = onDismiss
        )
    }
}
```

<br />

#### WRONG

**Why this is WRONG** : Wrapping dialog content in a manual
`CompositionLocalProvider` creates redundant boilerplate and obscures the
automatic dispatcher resolution provided by `ComponentDialog`.


```kotlin
// WRONG
@Composable
fun MyDialog(onDismiss: () -> Unit) {
    val dispatcherOwner = LocalNavigationEventDispatcherOwner.current!!
    Dialog(onDismissRequest = onDismiss) {
        // Redundant: ComponentDialog provides NavigationEventDispatcherOwner automatically
        CompositionLocalProvider( LocalNavigationEventDispatcherOwner provides dispatcherOwner) {
            val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
            NavigationBackHandler(
                state = navigationState,
                onBackCompleted = onDismiss
            )
        }
    }
}
```

<br />

### 3. Parent-child dispatcher hierarchy (`ViewPagers` and nested navigation)

When managing nested UI hierarchies such as `ViewPagers`, tabbed
interfaces, or custom navigation containers in Compose, use
`rememberNavigationEventDispatcherOwner()` to create a child owner linked to the
composition hierarchy. Setting `enabled = false` on the child owner
automatically disables its dispatcher and all registered child handlers.

#### RIGHT

**Why this is RIGHT** : Using `rememberNavigationEventDispatcherOwner(enabled =
isSelected)` creates a scoped child dispatcher linked to the parent from
`LocalNavigationEventDispatcherOwner.current`. Providing it using
`CompositionLocalProvider` ensures non-visible tabs or pages automatically stop
intercepting back gestures without leaking handlers.


```kotlin
// RIGHT: Scoping child navigation in a ViewPager or Tab interface
@Composable
fun TabPage(isSelected: Boolean) {
    val childOwner = rememberNavigationEventDispatcherOwner(enabled = isSelected)
    CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides childOwner) {
        val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
        NavigationBackHandler(
            state = navigationState,
            onBackCompleted = { /* Handle page back navigation */ }
        )
        // Page content
    }
}
```

<br />

#### WRONG

**Why this is WRONG** : Creating unlinked standalone dispatchers, instantiating
raw dispatchers without remembering them across recompositions, or attempting to
use non-existent methods like `.addChild()` breaks hierarchy routing and leaves
child handlers active even when the page is inactive.


```kotlin
// WRONG
@Composable
fun TabPage(isSelected: Boolean) {
    val parentDispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher
    val childDispatcher = NavigationEventDispatcher() // Unlinked and not remembered across recompositions
    // WRONG: Method does not exist
    parentDispatcher?.addChild(childDispatcher)
}
```

<br />

### 4. Compose multi-handler registration (IllegalArgumentException)

You must not bind the same `NavigationEventState` to multiple active
`NavigationBackHandler` instances, as this throws an `IllegalArgumentException`
at runtime. To handle conditional workflows (such as checking for unsaved
changes versus navigating back immediately), you must register a single unified
handler and branch logic inside `onBackCompleted`.

#### RIGHT

**Why this is RIGHT** : Using a single `NavigationBackHandler` with internal
branching logic inside `onBackCompleted` maintains a strict 1:1 mapping between
`NavigationEventState` and the handler, preventing state collisions.


```kotlin
// RIGHT
val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
NavigationBackHandler(
    state = navigationState,
    isBackEnabled = true,
    onBackCompleted = {
        if (hasUnsavedChanges) {
            showDiscardDialog()
        } else {
            onNavigateUp()
        }
    }
)
```

<br />

#### WRONG

**Why this is WRONG** : Attaching multiple `NavigationBackHandler` composables to
the same `navigationState` instance attempts to bind duplicate handlers to a
single state object, which throws an `IllegalArgumentException` at runtime.


```kotlin
// WRONG
val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
NavigationBackHandler(
    state = navigationState,
    isBackEnabled = hasUnsavedChanges,
    onBackCompleted = { /* Discard changes */ }
)
NavigationBackHandler(
    state = navigationState,
    isBackEnabled = !hasUnsavedChanges,
    onBackCompleted = { /* Navigate up */ }
)
```

<br />

## Checklist

**For Compose Android targets:**

- \[ \] Is compile SDK set to `36` or higher? (If compile SDK is lower than 36, set it to `36` or higher in `build.gradle.kts`).
- \[ \] Is `android:enableOnBackInvokedCallback` NOT explicitly set to `"false"` in `AndroidManifest.xml`? (On API 36+, it defaults to `"true"`; on API 33--35, ensure it is set to `"true"`).
- \[ \] Does the Activity rely on the built-in `ComponentActivity` dispatcher owner without redundant anonymous delegate wrapping?
- \[ \] Do dialogs or sheets rely on automatic `ComponentDialog` dispatcher resolution without redundant `CompositionLocalProvider` wrapping?
- \[ \] Are parent-child dispatcher relationships in Compose scoped using `rememberNavigationEventDispatcherOwner()` when managing nested hierarchies?
- \[ \] Is conditional back logic handled within a single unified `NavigationBackHandler` to avoid duplicate registration (`IllegalArgumentException`)?
- \[ \] Are legacy `BackHandler` usages migrated to `NavigationBackHandler` with predictive progress support?
- \[ \] Does the project build and pass tests successfully?
