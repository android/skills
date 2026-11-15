> [!IMPORTANT]
> **Important:** **ORCHESTRATION GATEWAY PREREQUISITE** : This document is a supplemental reference guide for [`../SKILL.md`](https://developer.android.com/agents/skills/migration/ios-to-android/skill). You **MUST** read and follow the primary orchestration protocol in `../SKILL.md` before applying any patterns from this guide. `SKILL.md` enforces mandatory incremental compilation gates (`./gradlew compileDebugKotlin`), vertical feature slicing, and strict anti-premature-exit execution discipline.

This skill maps iOS architectural constructs and lifecycle management to their
Android equivalents, focusing on Jetpack libraries and modern development
practices.

**Official Reference Links:**

- [ViewModel overview \| Android Developers](https://developer.android.com/topic/libraries/architecture/viewmodel)
- [Guide to app architecture \| Android Developers](https://developer.android.com/topic/architecture)
- [Saved State module for ViewModel \| Android Developers](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate)

## Prerequisites

Before using this skill, you **Must** ensure the following prerequisites are
met:

- **Source Context:** Access to the existing iOS implementation using MVVM or MVC is **Mandatory**.
- **Software Requirements:** Android Gradle Plugin (AGP) 8.0+, Kotlin 1.8+, Jetpack Compose BOM 2023.08.00+, and Jetpack Compose Lifecycle/ViewModel (`androidx.lifecycle:lifecycle-viewmodel-compose:2.6+`) are **Mandatory**.
- **Knowledge Requirements:** Familiarity with Kotlin Coroutines and Flow is **Mandatory**.

## Limitations

This skill has the following limitations:

- **Jetpack Focus:** Examples are tailored exclusively for standard Jetpack libraries (ViewModel, StateFlow). It does not cover custom or third-party architecture frameworks in depth.
- **Clean Architecture:** While layer-by-layer translation is discussed, full Clean Architecture implementations are not covered in depth.

## Glossary

| iOS Term (`StoreKit` / `AVFoundation` equivalent) | Android Equivalent (`Jetpack`) | Definition |
|---|---|---|
| `ObservableObject` | `ViewModel` | A class that stores and manages UI-related data in a lifecycle-aware way. |
| `@Published` | `StateFlow` | A state-holder observable flow that emits the current and new state updates. |
| `AppDelegate` | `Application` | The base class for maintaining global application state. |
| `UIViewController` | `Activity` / `Fragment` | The UI component that manages a screen or a portion of a screen. |
| `.onAppear` | `LaunchedEffect` | A side-effect that runs when a Composable enters the composition. |

## Setup \& Dependencies

You **Must** add the following dependencies to your `build.gradle.kts` file to
enable modern architecture components:

```kotlin
dependencies {
   val lifecycleVersion = "2.8.0"
   implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
   implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")
}
```

> [!CAUTION]
> **Caution:** **The Golden Rule of Android Architecture** : **NEVER store pure Application State inside an Activity or Component.** In iOS, rotating the device just calls a layout redraw event. In Android, rotating the device completely destroys the Activity and rebuilds it natively from scratch. If you place `val loggedIn = false` in a class field or `remember { mutableStateOf(false) }`, it will wipe when the user rotates the phone!

## Instructions: Mapping Concepts

### 1. ViewModels

Map iOS `ObservableObject` classes paired with `@StateObject` to Android
`ViewModel` subclasses combined with `StateFlow`.

- **Input:** iOS ViewModel with `@Published` properties.
- **Output:** Android `ViewModel` with `StateFlow`.

**iOS**
\`\`\`swift
class ProfileViewModel: ObservableObject {
@Published var name: String = ""

func fetch() { name = "John" }
}

struct ProfileView: View {
@StateObject var vm = ProfileViewModel()
// ...
}
\`\`\`

**Android**
\`\`\`kotlin
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProfileViewModel : ViewModel() {
private val _name = MutableStateFlow("")
val name: StateFlow = _name.asStateFlow()

fun fetch() {
_name.value = "John"
}
}

@Composable
fun ProfileView(viewModel: ProfileViewModel = viewModel()) {
// collectAsStateWithLifecycle is Mandatory for lifecycle awareness
val name by viewModel.name.collectAsStateWithLifecycle()
// Display UI
}
\`\`\`

### 2. App Lifecycle

Map iOS `AppDelegate` / `SceneDelegate` events to Android
`LifecycleEventObserver` or Activity callbacks.

- **Input:** iOS lifecycle event (e.g., `sceneDidBecomeActive`).
- **Output:** Android lifecycle callback or observer.

**iOS**
`swift
func sceneDidBecomeActive(_ scene: UIScene) { ... }`

**Android**
You **Must** implement a `LifecycleEventObserver` or use the specific callback
in the `Activity`.

```kotlin
override fun onResume() {
   super.onResume()
   // "sceneDidBecomeActive" equivalent
}
```

### 3. Launched Tasks (onAppear)

Map SwiftUI `.onAppear` / `.task` to Android Compose `LaunchedEffect`.

- **Input:** SwiftUI side-effect.
- **Output:** Compose side-effect.

**iOS**
`swift
.task {
await viewModel.loadData()
}`

**Android**
`kotlin
LaunchedEffect(Unit) { // Unit means it runs exactly once when the Composable enters the hierarchy
viewModel.loadData()
}`

To run code every time a specific variable changes, pass the variable into the
`LaunchedEffect` key:

```kotlin
LaunchedEffect(userId) {
   // Runs automatically whenever userId changes
}
```

### 4. Architectural Value Mapping

| iOS Architecture | Android Target Architecture | Migration Effort |
|---|---|---|
| **MVC** (older apps) | **MVVM** or **Clean Architecture** | High (Requires full refactoring of business logic from UI controllers into separate ViewModel/UseCase classes, affecting \>50% of code). |
| **MVVM** (modern SwiftUI) | **MVVM** | Low (Primarily consists of syntax and dependency mapping with \<10% change in logic). |
| **VIPER** | **Clean Architecture** with MVI or MVVM | Medium (Requires mapping VIPER modules to Android Features and UseCases, affecting \~30% of code). |

### 5. Layer-by-Layer Translation

#### Presentation Layer (The UI)

- **iOS** : `UIViewController` or SwiftUI `View`.
- **Android** : `@Composable` functions.
- **State** : `StateFlow` or Compose `State`.

#### Business Logic Layer (The ViewModel)

- **iOS** : `class ViewModel: ObservableObject`.
- **Android** : `class MyViewModel : ViewModel()`.
- **Must** use standard Android ViewModels to persist state through configuration changes.

#### Domain Layer (UseCases/Interactors)

- **iOS Interactor** (VIPER) -\> **Android UseCase**.
- **Scope** : Domain objects **Must** be pure Kotlin (no Android dependencies).

#### Data Layer (Repositories/Datasources)

- **iOS Network Client** (Alamofire) -\> **Android Repository +
  Ktor/Retrofit**.
- **iOS Database** (CoreData) -\> **Android Room**.

### 6. Dependency Injection (DI)

Map iOS DI solutions to Android DI frameworks.

- **Input:** iOS DI implementation (e.g., Swinject).
- **Output:** Hilt or Koin setup.

**Decision Table:**

| iOS DI | Recommended Android DI | Rationale |
|---|---|---|
| Swinject | **Dagger-Hilt** | Google-recommended standard; provides compile-time dependency validation, reducing runtime errors. |
| Custom Service Locator | **Koin** | Uses runtime resolution, requires no annotation processing, and has minimal boilerplate setup. |

**Hilt Example:**
`kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
@Provides
@Singleton
fun provideRetrofit(): Retrofit {
return Retrofit.Builder()
.baseUrl("https://api.example.com/")
.build()
}
}`

### 7. Persisting State across Process Death (SavedStateHandle)

Unlike iOS, where background apps are frozen or terminated but state is often
restored via state restoration APIs, Android apps can be killed by the system to
reclaim memory. You **Must** handle this to ensure a good user experience.

- **Input:** State that must survive process death.
- **Output:** `SavedStateHandle` integration in ViewModel.

**Android Code Example:**
\`\`\`kotlin
class SavedStateViewModel(
private val savedStateHandle: SavedStateHandle
) : ViewModel() {

// Expose state that survives process death
val userId: StateFlow = savedStateHandle.getStateFlow("userId", null)

fun setUserId(id: String) {
savedStateHandle\["userId"\] = id
}
}
\`\`\`

### 8. Model-View-Intent (MVI) Architecture

For complex screens in Jetpack Compose, MVI is often preferred over pure MVVM to
manage state unidirectionally.

- **Input:** User actions and UI state.
- **Output:** Unidirectional data flow.

**Decision Table for Architecture Choice:**

| Screen Complexity | Recommended Architecture | Key Benefit |
|---|---|---|
| Simple (1-2 states) | **MVVM** | Less boilerplate, quick setup. |
| Complex (Many states, side effects) | **MVI** | Predictable state transitions, single source of truth. |

**Android MVI Example:**
\`\`\`kotlin
data class UserState(
val name: String = "",
val isLoading: Boolean = false,
val error: String? = null
)

sealed class UserIntent {
object LoadUser : UserIntent()
data class UpdateName(val name: String) : UserIntent()
}

class UserViewModel : ViewModel() {
private val _state = MutableStateFlow(UserState())
val state: StateFlow = _state.asStateFlow()

fun processIntent(onIntent: UserIntent) {
when (onIntent) {
is UserIntent.LoadUser -\> loadUser()
is UserIntent.UpdateName -\> updateName(onIntent.name)
}
}

private fun loadUser() {
// Implementation
}

private fun updateName(name: String) {
// Implementation
}
}
\`\`\`

### 9. Navigation Mapping

Map iOS Navigation controllers to Jetpack Navigation.

- **Input:** iOS Navigation action (push/pop).
- **Output:** Jetpack Navigation call.

**iOS**
`swift
navigationController?.pushViewController(detailVC, animated: true)`

**Android**
\`\`\`kotlin
val navController = rememberNavController()
NavHost(navController = navController, startDestination = "profile") {
composable("profile") { ProfileScreen(navController) }
composable("detail") { DetailScreen() }
}

// To navigate:
navController.navigate("detail")
\`\`\`

### 10. Testing Strategy Mapping

You **Must** map iOS testing strategies to Android equivalents to ensure
quality.

| iOS (`XCTest`) | Android (`JUnit`, `Compose Test`) | Notes |
|---|---|---|
| `XCTestCase` | `JUnit4` / `JUnit5` | For unit testing ViewModels and UseCases. **Must** use `mockk` for mocking. |
| `XCUIApplication` | `ComposeTestRule` | For UI testing Compose screens. |

**Android Code Example:**
\`\`\`kotlin
@get:Rule
val composeTestRule = createComposeRule()

@Test
fun myTest() {
composeTestRule.setContent {
MyScreen()
}
composeTestRule.onNodeWithText("Submit").performClick()
}
\`\`\`

## Antipatterns

### DO

- **DO** use `collectAsStateWithLifecycle()` in Compose to collect flows safely.
- **DO** use Hilt for dependency injection in large production apps.
- **DO** separate concerns into Presentation, Domain, and Data layers.
- **DO** use `SavedStateHandle` to survive process death for critical data.

### DON'T

- **DON'T** store UI state directly in the Activity.
- **DON'T** use `collectAsState()` if `collectAsStateWithLifecycle()` is available, to avoid resource leaks.
- **DON'T** put business logic in Composable functions.

### NEVER

- **NEVER** block the main thread with heavy database or network operations. Use Coroutines.
- **NEVER** pass Activity or View contexts into ViewModels to avoid memory leaks.
- **NEVER** assume state in `remember` will survive configuration changes (rotation) unless using `rememberSaveable`.