> [!IMPORTANT]
> **Important:** **ORCHESTRATION GATEWAY PREREQUISITE** : This document is a supplemental reference guide for [`../SKILL.md`](https://developer.android.com/agents/skills/migration/ios-to-android/skill). You **MUST** read and follow the primary orchestration protocol in `../SKILL.md` before applying any patterns from this guide. `SKILL.md` enforces mandatory incremental compilation gates (`./gradlew compileDebugKotlin`), vertical feature slicing, and strict anti-premature-exit execution discipline.

This skill maps iOS SwiftUI navigation constructs to their Android Jetpack
Compose equivalents.

**Official Reference Links:**

- [Navigation in Jetpack Compose \| Android Developers](https://developer.android.com/guide/navigation)

## Prerequisites

Before using this skill, you **Must** ensure the following prerequisites are
met:

- **Source Context:** Access to the existing iOS SwiftUI implementation (using `NavigationStack` or `NavigationView`) is **Mandatory**.
- **Software Requirements:** Kotlin 1.9.20+ (or Kotlin 2.0+), Kotlin Serialization Plugin (`org.jetbrains.kotlin.plugin.serialization`), Android Gradle Plugin (AGP) 8.0+, and Jetpack Compose Navigation (`androidx.navigation:navigation-compose:2.8.0+`, required for type-safe route serialization) are **Mandatory**.
- **Kotlin Knowledge:** Familiarity with Kotlin Serialization is **Mandatory** for defining type-safe routes.

## Limitations

This skill has the following limitations:

- **Basic Stack Navigation:** This skill primarily covers stack-based navigation (Push/Pop). It does not cover complex split views in detail.
- **Version Focus:** The examples are tailored for Jetpack Navigation 2.8.0+ which supports type-safe routing via Kotlin Serialization.

## Glossary

| iOS Term | Android/Compose Equivalent | Definition |
|---|---|---|
| `NavigationStack` | `NavHost` | The component that renders the UI based on the current navigation state and defines the navigation graph. |
| `NavigationLink` | `navController.navigate()` | The action that triggers a transition to a new screen. |
| `UINavigationController` | `NavController` | The central API that manages app navigation, holding the backstack. |
| `navigationDestination` | `composable<T>` destination | The mapping of a route key to a specific Composable screen in the graph. |
| `Environment(\.dismiss)` | `navController.popBackStack()` | The action to navigate back or dismiss the current screen. |
| `.navigationDestination(for:)` | `@Serializable` data class | The definition of a route and its parameters using serialization. |

## Setup \& Dependencies

You **Must** add the following dependencies to your `build.gradle.kts` file to
enable type-safe navigation:

```kotlin
dependencies {
   implementation("androidx.navigation:navigation-compose:2.8.0")
   implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
   implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
}
```

You **Must** also apply the Kotlin Serialization plugin in your
`build.gradle.kts`:
`kotlin
plugins {
kotlin("plugin.serialization") version "1.9.20" // Match your Kotlin version
}`

## Instructions: Mapping Concepts

### 1. Defining Routes

Map iOS dynamic destination resolution to Android `@Serializable` data classes
for type safety.

- **Input:** A destination identifier and its parameters.
- **Output:** A type-safe route definition.

**iOS**
`swift
// In iOS, routes are often resolved dynamically via types
.navigationDestination(for: Profile.self) { profile in
ProfileView(id: profile.id)
}`

**Android**
\`\`\`kotlin
// In Compose 2.8.0+, routes are defined by serializable objects
@Serializable
data class ProfileRoute(val id: String)

@Serializable
object HomeRoute
\`\`\`

### 2. Setting up the Navigation Graph

Map iOS `NavigationStack` to Android `NavHost`.

- **Input:** A `NavController` instance and a start destination.
- **Output:** A rendered UI based on the current destination in the graph.

**iOS**
`swift
NavigationStack {
ContentView()
}`

**Android**
\`\`\`kotlin
val navController = rememberNavController()

NavHost(navController = navController, startDestination = HomeRoute) {
composable { HomeScreen( onProfileClick = { id -\> navController.navigate(ProfileRoute(id)) } ) } composable { backStackEntry -\> // Extract arguments type-safely val profile = backStackEntry.toRoute() ProfileScreen(id = profile.id) } } \`\`\`

### 3. Navigating to a Destination

Map iOS `NavigationLink` to `navController.navigate()`.

- **Input:** The target route object with arguments.
- **Output:** The backstack is updated, and the app navigates to the new screen.

**iOS**
`swift
NavigationLink(value: user) {
Text("View Profile")
}`

**Android**
`kotlin
// Triggered via callback from the View to maintain decoupling
onProfileClick = { id ->
navController.navigate(ProfileRoute(id))
}`

### 4. Back Navigation

Map iOS dismiss environment to Android `popBackStack()`.

- **Input:** None.
- **Output:** The top screen is removed from the backstack.

**iOS**
`swift
@Environment(\.dismiss) var dismiss
// ...
dismiss()`

**Android**
`kotlin
navController.popBackStack()`

### 5. Handling Deep Links

Map iOS deep link handling to Compose type-safe deep links.

- **Input:** A deep link URL pattern.
- **Output:** Automatic navigation to the screen when the app is opened via link.

**iOS**
`swift
.onOpenURL { url in
// Handle URL manually
}`

**Android**
\`\`\`kotlin
val deepLinkUri = "https://example.com/profile/{id}"

composable( deepLinks = listOf( navDeepLink(basePath = "https://example.com/profile") ) ) { backStackEntry -\> val profile = backStackEntry.toRoute() ProfileScreen(id = profile.id) } \`\`\`

### 6. Tab-Based Navigation (Bottom Navigation)

Map iOS `TabView` to Android Compose `NavigationBar` with state preservation.

- **Input:** A list of top-level destinations and the `NavController`.
- **Output:** A persistent bottom bar that switches screens while retaining their scroll state.

**iOS**
`swift
TabView {
HomeView()
.tabItem { Label("Home", systemImage: "house") }
SettingsView()
.tabItem { Label("Settings", systemImage: "gear") }
}`

**Android**
\`\`\`kotlin
val items = listOf(HomeRoute, SettingsRoute)
val navController = rememberNavController()

Scaffold(
bottomBar = {
NavigationBar {
val navBackStackEntry by navController.currentBackStackEntryAsState()
val currentDestination = navBackStackEntry?.destination

           items.forEach { screen ->
               NavigationBarItem(
                   icon = { /* Add appropriate icon */ },
                   label = { Text(stringResource(screen.labelRes)) }, // @StringRes on each route
                   selected = currentDestination?.hierarchy?.any {
                       it.hasRoute(screen::class)
                   } == true,
                   onClick = {
                       navController.navigate(screen) {
                           // Pop up to the start destination of the graph to
                           // avoid building up a large stack of destinations
                           // on the back stack as users select items
                           popUpTo(navController.graph.findStartDestination().id) {
                               saveState = true
                           }
                           // Avoid multiple copies of the same destination when
                           // reselecting the same item
                           launchSingleTop = true
                           // Restore state when reselecting a previously selected item
                           restoreState = true
                       }
                   }
               )
           }
       }

}
) { innerPadding -\>
NavHost(navController, startDestination = HomeRoute, Modifier.padding(innerPadding)) {
composable { HomeScreen() } composable { SettingsScreen() } } } \`\`\`

### 7. Custom Transitions

Map iOS transition animations to Compose Navigation transition parameters.

- **Input:** Navigation action triggering a screen change.
- **Output:** Visual transition effect (e.g., slide, fade) applied to the entering and exiting screens.

**iOS**
`swift
// iOS typically uses system defaults or complex UIViewControllerTransitioningDelegate
NavigationStack {
// ...
}
// SwiftUI transitions are often defined on the view itself
.transition(.slide)`

**Android**
`kotlin
composable<ProfileRoute>(
enterTransition = {
slideIntoContainer(
AnimatedContentTransitionScope.SlideDirection.Left,
animationSpec = tween(500)
)
},
exitTransition = {
slideOutOfContainer(
AnimatedContentTransitionScope.SlideDirection.Left,
animationSpec = tween(500)
)
},
popEnterTransition = {
slideIntoContainer(
AnimatedContentTransitionScope.SlideDirection.Right,
animationSpec = tween(500)
)
},
popExitTransition = {
slideOutOfContainer(
AnimatedContentTransitionScope.SlideDirection.Right,
animationSpec = tween(500)
)
}
) { backStackEntry ->
val profile = backStackEntry.toRoute<ProfileRoute>()
ProfileScreen(id = profile.id)
}`

### 8. Nested Navigation Graphs

Map iOS child coordinators or separate flows to Compose nested navigation
graphs.

- **Input:** A group of related destinations.
- **Output:** A modular, nested graph within the main navigation graph.

**iOS**
`swift
// iOS handles this via child coordinators or separate NavigationStacks`

**Android**
\`\`\`kotlin
// Define a route for the graph itself
@Serializable
object ProfileGraph

NavHost(navController, startDestination = HomeRoute) {
composable { /\* ... \*/ }

// Nested graph
navigation(startDestination = ProfileRoute) { composable { backStackEntry -\> val profile = backStackEntry.toRoute() ProfileScreen(id = profile.id) } composable { ProfileSettingsScreen() } } } \`\`\`

### 9. Returning a Result to the Previous Screen

Map iOS callback patterns or delegates to Compose saved state handle results.

- **Input:** A screen that needs to return data.
- **Output:** Data is passed back to the previous screen in the backstack.

**iOS**
`swift
// iOS typically uses delegates or closures to return data`

**Android**
\`\`\`kotlin
// In the destination screen that returns a result
val result = "New Data"
navController.previousBackStackEntry?.savedStateHandle?.set("key", result)
navController.popBackStack()

// In the source screen that is waiting for a result
val navBackStackEntry = navController.currentBackStackEntry
val resultFlow = navBackStackEntry?.savedStateHandle?.getStateFlow("key", null) val result by resultFlow.collectAsStateWithLifecycle()

LaunchedEffect(result) {
if (result != null) {
// Process the result
// ...
// Clear the result after consuming it
navBackStackEntry?.savedStateHandle?.remove("key") } } \`\`\`

## Quality Control: Antipatterns

### DO

- **DO** use `@Serializable` data classes or objects for all routes to ensure type safety and prevent runtime crashes from missing arguments.
- **DO** pass lambdas for navigation events (e.g., `onNavigate`) instead of passing the `NavController` instance directly to child Composables.
- **DO** use `toRoute<T>()` to extract arguments in the destination lambda.

### DON'T

- **DON'T** assume navigation state is automatically preserved across process death without saving the backstack state properly.
- **DON'T** hardcode string paths for routes (e.g., `navigate("profile/\(id)")`); always use the type-safe object approach.
- **DON'T** forget to declare an `<intent-filter>` in your `AndroidManifest.xml` for each deep link. The `navDeepLink` composable parameter only handles in-app routing; the manifest entry is mandatory for the app to respond to external URLs.

### NEVER

- **NEVER** attempt to share navigation routing graphs in Kotlin Multiplatform (KMP) if it forces you to compromise on using platform-native capabilities like `NavigationSplitView` on iOS. Failure to do so may lead to degraded user experience.
- **NEVER** pass the `NavController` instance directly as a parameter to generic composable screens to ensure proper decoupling and testability.