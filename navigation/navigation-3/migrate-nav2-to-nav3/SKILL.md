---
name: migrate-from-navigation-2-to-navigation-3
description: Provides a step-by-step workflow to migrate an Android Compose project from Navigation 2 to Navigation 3. It contains dependency information, code for custom state holders (NavigationState, Navigator), and detailed instructions for replacing NavController and NavHost. Use this skill when the user explicitly asks to "Migrate from Navigation 2 to Navigation 3". It solves the problem of updating the navigation architecture to the modern, state-driven Navigation 3 pattern.
license: Complete terms in LICENSE.txt
---

# Android Navigation 2 to Navigation 3 Migration

This skill guides the migration of an Android project from Navigation 2 to Navigation 3, based on the official migration guide. The process involves updating dependencies, refactoring routes and destinations, and replacing `NavController`/`NavHost` with a state-driven approach using `NavigationState`, `Navigator`, and `NavDisplay`.

## Glossary

| Term | Definition |
| --- | --- |
| `NavKey` | An interface that navigation routes must implement to support state saving via `rememberNavBackStack`. |
| `NavigationState` | A state holder class that holds top-level routes, their back stacks, and the current top-level route. It persists configuration changes and process death. |
| `Navigator` | A class that handles navigation events (`navigate`, `goBack`) and updates the `NavigationState` in a unidirectional data flow pattern. |
| `entryProvider` | A DSL that resolves a route (`NavKey`) to a `NavEntry`. It replaces the `NavGraphBuilder` DSL used within `NavHost`. |
| `NavDisplay` | A composable that displays the current navigation entries provided by the state holder. It replaces `NavHost`. |
| `DialogSceneStrategy` | A metadata and scene strategy component used to handle dialog destinations in Navigation 3. |

## 1. Pre-Migration Analysis

Before changing any code, you MUST perform these checks.

### 1.1. Verify Prerequisites

Confirm the project meets the following mandatory conditions:
- `compileSdk` is 36 or later.
- All navigation destinations are composable functions.
- All navigation routes are strongly typed. If the project uses string-based routes, you must first migrate to type-safe routes.

### 1.2. Verify Project Assumptions

This migration guide is built on a specific set of assumptions about the project's structure.
**IF** any of the following are false, **THEN** you must stop the migration and ask the user how to proceed.

- The project has one or several top-level routes, typically displayed in a bottom navigation bar.
- Each top-level route has its own back stack.
- When switching between back stacks, the state of the stack and all its destinations is retained.
- The app is always exited through the **Home** screen, which is the first screen displayed on launch.
- The migration from Navigation 2 to Navigation 3 will be performed in a single, atomic change.

### 1.3. Analyze Project Features

Analyze the project for the following feature categories. Your migration plan depends on this analysis.

#### Supported Features
The core guide directly supports migrating:
- Destinations defined as composable functions.
- Dialogs (a destination shown on top of another destination).

#### Features Supported via Recipes
**IF** the project contains any of the features below, **THEN** you must consult the specified recipe from the [code recipes repository](https://github.com/android/nav3-recipes). Create a migration plan based on the recipe's README and source code. Do not proceed without confirming the plan with the user.
- [Bottom sheets](https://github.com/android/nav3-recipes/tree/main/app/src/main/java/com/example/nav3recipes/bottomsheet)
- [Modularized navigation code and injected destinations](https://github.com/android/nav3-recipes/tree/main/app/src/main/java/com/example/nav3recipes/modular/hilt)
- [Using and passing arguments to ViewModels](https://github.com/android/nav3-recipes?tab=readme-ov-file#passing-navigation-arguments-to-viewmodels)
- [Returning results from a screen](https://github.com/android/nav3-recipes?tab=readme-ov-file#returning-results)

#### Unsupported Features
**IF** the project contains any of the features below, **THEN** you MUST NOT proceed with the migration. Inform the user of the specific unsupported feature and ask for further instructions.
- More than one level of nested navigation
- Shared destinations (screens that can move between different back stacks)
- [Custom destination types](https://developer.android.com/guide/navigation/design/kotlin-dsl#custom)
- Deep links

## 2. Migration Workflow

Execute these steps sequentially after completing the pre-migration analysis.

### Step 1: Add Navigation 3 Dependencies

1.  Update the project's `minSdk` to 23 and `compileSdk` to 36. These are typically in `app/build.gradle.kts` or `lib.versions.toml`.
2.  Add the Navigation 3 dependencies to your project files.

**`lib.versions.toml`**
```toml
[versions]
nav3Core = "1.0.0"

# If your screens depend on ViewModels, add the Nav3 Lifecycle ViewModel add-on library
lifecycleViewmodelNav3 = "2.10.0-rc01"

[libraries]
# Core Navigation 3 libraries
androidx-navigation3-runtime = { module = "androidx.navigation3:navigation3-runtime", version.ref = "nav3Core" }
androidx-navigation3-ui = { module = "androidx.navigation3:navigation3-ui", version.ref = "nav3Core" }

# Add-on libraries (only add if you need them)
androidx-lifecycle-viewmodel-navigation3 = { module = "androidx.lifecycle:lifecycle-viewmodel-navigation3", version.ref = "lifecycleViewmodelNav3" }
```

**`app/build.gradle.kts`**
```kotlin
dependencies {
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)

    // If using the ViewModel add-on library
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
}
```

### Step 2: Update Routes to Implement `NavKey`

Modify every navigation route data class/object to implement the `NavKey` interface. This is mandatory for state saving.

**Before:**
```kotlin
@Serializable data object RouteA
```

**After:**
```kotlin
import androidx.navigation3.runtime.NavKey

@Serializable data object RouteA : NavKey
```

### Step 3: Create State Management Classes

Create two new files, `NavigationState.kt` and `Navigator.kt`, to manage navigation state.

1.  Create `NavigationState.kt` with the following content. Add the correct package name.

    ```kotlin
    // package com.example.project

    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.MutableState
    import androidx.compose.runtime.getValue
    import androidx.compose.runtime.mutableStateOf
    import androidx.compose.runtime.remember
    import androidx.compose.runtime.saveable.rememberSerializable
    import androidx.compose.runtime.setValue
    import androidx.compose.runtime.snapshots.SnapshotStateList
    import androidx.compose.runtime.toMutableStateList
    import androidx.navigation3.runtime.NavBackStack
    import androidx.navigation3.runtime.NavEntry
    import androidx.navigation3.runtime.NavKey
    import androidx.navigation3.runtime.rememberDecoratedNavEntries
    import androidx.navigation3.runtime.rememberNavBackStack
    import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
    import androidx.navigation3.runtime.serialization.NavKeySerializer
    import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer

    @Composable
    fun rememberNavigationState(
        startRoute: NavKey,
        topLevelRoutes: Set<NavKey>
    ): NavigationState {

        val topLevelRoute = rememberSerializable(
            startRoute, topLevelRoutes,
            serializer = MutableStateSerializer(NavKeySerializer())
        ) {
            mutableStateOf(startRoute)
        }

        val backStacks = topLevelRoutes.associateWith { key -> rememberNavBackStack(key) }

        return remember(startRoute, topLevelRoutes) {
            NavigationState(
                startRoute = startRoute,
                topLevelRoute = topLevelRoute,
                backStacks = backStacks
            )
        }
    }

    class NavigationState(
        val startRoute: NavKey,
        topLevelRoute: MutableState<NavKey>,
        val backStacks: Map<NavKey, NavBackStack<NavKey>>
    ) {
        var topLevelRoute: NavKey by topLevelRoute
        val stacksInUse: List<NavKey>
            get() = if (topLevelRoute == startRoute) {
                listOf(startRoute)
            } else {
                listOf(startRoute, topLevelRoute)
            }
    }

    @Composable
    fun NavigationState.toEntries(
        entryProvider: (NavKey) -> NavEntry<NavKey>
    ): SnapshotStateList<NavEntry<NavKey>> {

        val decoratedEntries = backStacks.mapValues { (_, stack) ->
            val decorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
            )
            rememberDecoratedNavEntries(
                backStack = stack,
                entryDecorators = decorators,
                entryProvider = entryProvider
            )
        }

        return stacksInUse
            .flatMap { decoratedEntries[it] ?: emptyList() }
            .toMutableStateList()
    }
    ```

2.  Create `Navigator.kt` with the following content. Add the correct package name.

    ```kotlin
    // package com.example.project

    import androidx.navigation3.runtime.NavKey

    class Navigator(val state: NavigationState){
        fun navigate(route: NavKey){
            if (route in state.backStacks.keys){
                // This is a top level route, just switch to it.
                state.topLevelRoute = route
            } else {
                state.backStacks[state.topLevelRoute]?.add(route)
            }
        }

        fun goBack(){
            val currentStack = state.backStacks[state.topLevelRoute] ?:
            error("Stack for ${state.topLevelRoute} not found")
            val currentRoute = currentStack.last()

            // If we're at the base of the current route, go back to the start route stack.
            if (currentRoute == state.topLevelRoute){
                state.topLevelRoute = state.startRoute
            } else {
                currentStack.removeLastOrNull()
            }
        }
    }
    ```

3.  Create instances of `NavigationState` and `Navigator` in the same scope where `NavController` was previously created.

    ```kotlin
    val navigationState = rememberNavigationState(
        startRoute = <Insert your starting route>,
        topLevelRoutes = <Insert your set of top level routes>
    )

    val navigator = remember { Navigator(navigationState) }
    ```

### Step 4: Replace `NavController`

Refactor all usages of `NavController` to use `Navigator` for events and `NavigationState` for state observation.

| `NavController` field or method | `Navigator` / `NavigationState` equivalent |
| --- | --- |
| `navigate()` | `navigator.navigate()` |
| `popBackStack()` | `navigator.goBack()` |
| `currentBackStack` | `navigationState.backStacks[navigationState.topLevelRoute]` |
| `currentBackStackEntry`, `currentBackStackEntryAsState()`, `currentBackStackEntryFlow`, `currentDestination` | `navigationState.backStacks[navigationState.topLevelRoute].last()` |
| Get the top level route | `navigationState.topLevelRoute` |

To determine the currently selected item in a navigation bar, replace hierarchy checks with a direct comparison.

**Before:**
```kotlin
val isSelected = navController.currentBackStackEntryAsState().value?.destination.isRouteInHierarchy(key::class)

fun NavDestination?.isRouteInHierarchy(route: KClass<*>) =
    this?.hierarchy?.any {
        it.hasRoute(route)
    } ?: false
```

**After:**
```kotlin
val isSelected = key == navigationState.topLevelRoute
```

Verify that all references to `NavController` and its associated imports have been removed.

### Step 5: Refactor Destinations into `entryProvider`

1.  Create an `entryProvider` at the same scope as `NavigationState`.
    ```kotlin
    val entryProvider = entryProvider {
        // Destinations will be moved here
    }
    ```
2.  Move each destination from the `NavHost` `NavGraphBuilder` into the `entryProvider`, applying the following transformations:
    - **`navigation`:** Delete the `navigation` block and its associated route. There is no need for "base routes." When deleting a route used to identify a nested graph (e.g., `BaseRouteA`), you must replace all references to it with the type of the first child in that graph (e.g., `RouteA`).
    - **`composable<T>`:** Rename to `entry<T>`, retaining the type parameter.
    - **`dialog<T>`:** Rename to `entry<T>` and add dialog metadata: `entry<T>(metadata = DialogSceneStrategy.dialog())`.
    - **`bottomSheet`:** Follow the [bottom sheet recipe](https://github.com/android/nav3-recipes/tree/main/app/src/main/java/com/example/nav3recipes/bottomsheet).
3.  Obtain navigation arguments from the key provided to the `entry`'s trailing lambda.

**Example Refactor:**

**Before (`NavHost`):**
```kotlin
// Assume NavController and NavGraphBuilder imports
@Serializable data object BaseRouteA
@Serializable data class RouteA(val id: String)
@Serializable data object BaseRouteB
@Serializable data object RouteB
@Serializable data object RouteD

NavHost(navController = navController, startDestination = BaseRouteA){
    composable<RouteA>{ entry ->
        val id = entry.toRoute<RouteA>().id
        ScreenA(title = "Screen has ID: $id")
    }
    featureBSection()
    dialog<RouteD>{ ScreenD() }
}

fun NavGraphBuilder.featureBSection() {
    navigation<BaseRouteB>(startDestination = RouteB) {
        composable<RouteB> { ScreenB() }
    }
}
```

**After (`entryProvider`):**
```kotlin
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.scene.DialogSceneStrategy

// Note: BaseRouteA and BaseRouteB are deleted. Routes now implement NavKey.
@Serializable data class RouteA(val id: String) : NavKey
@Serializable data object RouteB : NavKey
@Serializable data object RouteD : NavKey

val entryProvider = entryProvider {
    entry<RouteA>{ key -> ScreenA(title = "Screen has ID: ${key.id}") }
    featureBSection()
    entry<RouteD>(metadata = DialogSceneStrategy.dialog()){ ScreenD() }
}

// Extension function now targets EntryProviderScope<NavKey>
fun EntryProviderScope<NavKey>.featureBSection() {
    entry<RouteB> { ScreenB() }
}
```

### Step 6: Replace `NavHost` with `NavDisplay`

Replace the `NavHost` composable with `NavDisplay`.

1.  Delete `NavHost`.
2.  Add `NavDisplay` and configure its parameters:
    - `entries`: Set to `navigationState.toEntries(entryProvider)`.
    - `onBack`: Connect to the back handler: `onBack = { navigator.goBack() }`.
    - `sceneStrategy`: **IF** you have dialog destinations, you must add `remember { DialogSceneStrategy() }`.

**Example:**
```kotlin
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.compose.runtime.remember

NavDisplay(
    entries = navigationState.toEntries(entryProvider),
    onBack = { navigator.goBack() },
    // Only add sceneStrategy if dialogs are used
    sceneStrategy = remember { DialogSceneStrategy() }
)
```

### Step 7: Final Cleanup

Remove all remaining Navigation 2 library dependencies from your Gradle files and any unused `androidx.navigation` imports from your Kotlin files.

## Antipatterns

### DO
- **DO** implement the `NavKey` interface on all serializable route objects.
- **DO** use `rememberSerializable` for persisting the `topLevelRoute` in `NavigationState.kt`.
- **DO** use `entry<T>(metadata = DialogSceneStrategy.dialog())` for dialog destinations.
- **DO** connect `NavDisplay.onBack` to `navigator.goBack()`.
- **DO** replace references to deleted `navigation` base routes with the first child route of that graph.

### DON'T
- **DON'T** change `rememberSerializable` to `rememberSaveable` in the provided `NavigationState.kt` code. It is correct as is.
- **DON'T** leave any references to `NavController`, its methods, or its imports in the project.
- **DON'T** forget to add `DialogSceneStrategy` to `NavDisplay`'s `sceneStrategy` parameter if the project uses dialogs.

### NEVER
- **NEVER** proceed with the migration if the project contains unsupported features like deep links or shared destinations. Stop and inform the user.
- **NEVER** proceed if the project's structure does not match the assumptions (e.g., it lacks a single home/start screen). Stop and ask the user for clarification.
- **NEVER** use any code or dependencies from Navigation 2 after the migration is complete.