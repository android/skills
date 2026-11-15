> [!IMPORTANT]
> **Important:** **ORCHESTRATION GATEWAY PREREQUISITE** : This document is a supplemental reference guide for [`../SKILL.md`](https://developer.android.com/agents/skills/migration/ios-to-android/skill). You **MUST** read and follow the primary orchestration protocol in `../SKILL.md` before applying any patterns from this guide. `SKILL.md` enforces mandatory incremental compilation gates (`./gradlew compileDebugKotlin`), vertical feature slicing, and strict anti-premature-exit execution discipline.

This skill outlines how to map iOS SwiftUI state wrappers (`@State`, `@Binding`,
`@StateObject`, `@EnvironmentObject`) to Jetpack Compose reactive state
management primitives. You **Must** use this skill when porting an app's UI
layer to ensure data flow consistency and avoid memory leaks or lost state on
configuration changes.

**Official Reference Links:**

- [State in Jetpack Compose \| Android Developers](https://developer.android.com/develop/ui/compose/state)
- [State holders and UI state \| Android guide \| Android Developers](https://developer.android.com/topic/architecture/ui-layer/stateholders)
- [Kotlin flows on Android \| Android Developers](https://developer.android.com/kotlin/flow)
- [Side-effects in Compose \| Jetpack Compose \| Android Developers](https://developer.android.com/develop/ui/compose/side-effects)

## Prerequisites

Before initiating the state migration, you **Must** ensure the following
prerequisites are met:

- **Source Context:** Access to the iOS project's SwiftUI views and ViewModels is **Mandatory**.
- **Jetpack Compose:** Familiarity with recomposition, state hoisting, and side effects is required.
- **Software Requirements:** Android Gradle Plugin (AGP) 8.0+, Kotlin 1.8+, Jetpack Compose BOM 2023.08.00+, and Jetpack Compose Lifecycle (`androidx.lifecycle:lifecycle-runtime-compose:2.8.0`, required for `collectAsStateWithLifecycle()`) are **Mandatory**.
- **Kotlin Coroutines:** Understanding of `Flow`, `StateFlow`, and coroutine scopes is required.

## Limitations

This skill has the following limitations:

- **Legacy Architecture:** This skill focuses on SwiftUI to Compose. Legacy UIKit to Android Views state mapping is outside its scope.
- **Complex Redux/MVI:** While basic state holders are covered, mapping complex Redux or MVI architectures requires additional custom mapping not detailed here.

## Setup/Dependencies

To use the state APIs shown in this skill, you **Must** ensure the following
dependencies are included in your module's `build.gradle.kts` file. This skill
assumes the use of **Version Catalogs** for dependency management.

```kotlin
// app/build.gradle.kts
dependencies {
   // For remember, mutableStateOf
   // Catalog alias maps to: androidx.compose.runtime:runtime
   implementation(libs.androidx.compose.runtime)
   // For collectAsStateWithLifecycle
   // Catalog alias maps to: androidx.lifecycle:lifecycle-runtime-compose
   implementation(libs.androidx.lifecycle.runtime.compose)
   // For viewModel()
   // Catalog alias maps to: androidx.lifecycle:lifecycle-viewmodel-compose
   implementation(libs.androidx.lifecycle.viewmodel.compose)
}
```

## Glossary

| iOS Concept | Android Equivalent | Definition |
|---|---|---|
| `@State` | `remember { mutableStateOf() }` | Local, component-scoped state. |
| `@Binding` | Callback lambda (`(T) -> Unit`) | Two-way communication with parent. |
| `@StateObject` | `viewModel()` | Lifecycle-aware state holder ownership. |
| `@EnvironmentObject` | `CompositionLocal` | Implicit data passing down the tree. |

## 1. Local State (@State -\> remember / rememberSaveable)

SwiftUI uses `@State` for local property storage. Compose uses `mutableStateOf`
combined with `remember` or `rememberSaveable`.

**Logic:**

- **IF** the state must survive configuration changes (like screen rotation or dark mode toggles), **THEN** you **Must** use `rememberSaveable`.
- **ELSE** (component-scoped state that can reset on rotation), use `remember`.

**Example: Local State Mapping**

- **Input (SwiftUI):** `swift
  @State private var text: String = "Hello"`
- **Output (Compose):** `kotlin
  // Requires import androidx.compose.runtime.getValue and setValue
  var text by rememberSaveable { mutableStateOf("Hello") }`

### Advanced State: rememberUpdatedState

When a lambda captures a value that changes, but you do not want the effect
(like `LaunchedEffect`) to restart, you **Must** use `rememberUpdatedState`.

**Example: rememberUpdatedState**

- **Input:** A callback lambda that references changing state.
- **Output:** A stable reference to the latest lambda value.
- **Side Effect:** Prevents effect restart while ensuring the latest value is used.

```kotlin
@Composable
fun TimeoutComponent(onTimeout: () -> Unit) {
   // Reference to the latest onTimeout lambda
   val currentOnTimeout by rememberUpdatedState(onTimeout)


   LaunchedEffect(Unit) {
       delay(5000)
       // Uses the latest version of onTimeout
       currentOnTimeout()
   }
}
```

## 2. Two-Way Bindings (@Binding -\> State Hoisting)

SwiftUI uses `@Binding` to create a two-way connection to state owned by a
parent. Compose enforces unidirectional data flow via **State Hoisting**.

**Antipattern:** DO NOT pass `MutableState<T>` to children as a 1:1 map for
`@Binding` unless explicitly required. Instead, pass the value and a callback.

**Example: State Hoisting Mapping**

- **Input (SwiftUI Child):** `swift
  struct ChildView: View {
  @Binding var isEnabled: Bool
  var body: some View {
  Toggle("Toggle", isOn: $isEnabled)
  }
  }`
- **Output (Compose Child):** `kotlin
  @Composable
  fun ChildView(
  isEnabled: Boolean,
  onEnabledChanged: (Boolean) -> Unit
  ) {
  Switch(checked = isEnabled, onCheckedChange = onEnabledChanged)
  }`

## 3. Observable Objects (@StateObject / @ObservedObject -\> ViewModel \& StateFlow)

SwiftUI uses `@StateObject` to instantiate an `ObservableObject` and
`@ObservedObject` to reference it. Android uses `ViewModel` as the state holder
and `StateFlow` to expose state.

**Example: State Holder Mapping**

- **Input (SwiftUI ObservableObject):**
  \`\`\`swift
  class UserViewModel: ObservableObject {
  @Published var username: String = ""
  }

  struct UserView: View {
  @StateObject var viewModel = UserViewModel()
  var body: some View {
  Text(viewModel.username)
  }
  }
  \`\`\`
- **Output (Compose ViewModel):**
  \`\`\`kotlin
  class UserViewModel : ViewModel() {
  private val _username = MutableStateFlow("")
  val username: StateFlow = _username.asStateFlow() }

  @Composable
  fun UserView(viewModel: UserViewModel = viewModel()) {
  val username by viewModel.username.collectAsStateWithLifecycle()
  Text(text = username)
  }
  \`\`\`

## 4. Environment Object (@EnvironmentObject -\> CompositionLocal)

SwiftUI uses `@EnvironmentObject` to read data provided higher up in the view
hierarchy. Compose uses `CompositionLocal` for the same purpose.

**Example: Environment Mapping**

- **Input (SwiftUI Environment):** `swift
  struct GrandchildView: View {
  @EnvironmentObject var settings: SettingsConfig
  }`
- **Output (Compose CompositionLocal):**
  \`\`\`kotlin
  val LocalSettings = staticCompositionLocalOf { error("No settings") }

  @Composable
  fun GrandchildView() {
  val settings = LocalSettings.current
  }
  \`\`\`

## 5. Combine to Kotlin Flow Mapping

When migrating from Combine (iOS) to Kotlin Flow (Android), you **Must** map the
types according to the following table:

| Swift Combine Concept | Kotlin Flow Equivalent | Definition |
|---|---|---|
| `AnyPublisher` | `Flow` | The base stream type. |
| `CurrentValueSubject` | `MutableStateFlow` | Holds a value and emits updates. |
| `PassthroughSubject` | `MutableSharedFlow` | Emits events without holding a value. |
| `.sink` | `.collect` | Terminal operator to consume emissions. |
| `.map` | `.map` | Transforms emissions. |

### Advanced Combine Operator Mapping

You **Must** map advanced Combine operators to their Kotlin Flow equivalents
when handling complex streams:

| Swift Combine Operator | Kotlin Flow Equivalent | Definition |
|---|---|---|
| `.debounce` | `.debounce` | Time-based rate limiting. |
| `.flatMap` | `.flatMapLatest` | Flattens streams, canceling previous ones. |
| `.combineLatest` | `combine` | Combines latest values from multiple flows. |

**Example: Advanced Stream Mapping**

- **Input (SwiftUI combineLatest):** `swift
  Publishers.CombineLatest(publisherA, publisherB)`
- **Output (Compose combine):** `kotlin
  combine(flowA, flowB) { a, b -> Pair(a, b) }`

**Example: Stream Mapping**

- **Input (SwiftUI Combine):** `swift
  let subject = CurrentValueSubject<String, Never>("Initial")
  subject.send("New Value")`
- **Output (Compose Flow):** `kotlin
  val stateFlow = MutableStateFlow("Initial")
  stateFlow.value = "New Value"`

## 6. Side Effects (onAppear / onChange -\> LaunchedEffect)

SwiftUI uses modifiers like `.onAppear` and `.onChange` to perform side effects.
Compose uses effect APIs like `LaunchedEffect`.

**Example: Side Effect Mapping**

- **Input (SwiftUI onAppear):** `swift
  .onAppear {
  viewModel.loadData()
  }`
- **Output (Compose LaunchedEffect):**
  `kotlin
  LaunchedEffect(Unit) {
  viewModel.loadData()
  }`

- **Input (SwiftUI onChange):**
  `swift
  .onChange(of: text) { newValue in
  print(newValue)
  }`

- **Output (Compose Side Effect):**
  `kotlin
  LaunchedEffect(text) {
  print(text)
  }`

- **Input (SwiftUI onDisappear):**
  `swift
  .onDisappear {
  viewModel.cleanup()
  }`

- **Output (Compose DisposableEffect):**
  `kotlin
  DisposableEffect(Unit) {
  onDispose {
  viewModel.cleanup()
  }
  }`

## 7. Advanced State: Architecture Mapping

You **Must** map the standard iOS ViewModel pattern to the Android standard
architecture.

**Logic:**

- **IF** the iOS app uses a ViewModel observing a `@Published` data source, **THEN** you **Must** expose it as a `StateFlow` or `LiveData` on Android.
- **IF** the iOS app relies on diffing frameworks like `DifferenceKit` for dynamic collections, **THEN** you **Must** use Android's `DiffUtil` or Compose's inherent recomposition diffing.

## 8. Advanced State: Kotlin Multiplatform (KMP) Integration

When adopting a Kotlin Multiplatform strategy to share state management logic:

**Logic:**

- **IF** a project uses a Kotlin Multiplatform shared module containing a ViewModel, **THEN** you **Must** consume the Kotlin `StateFlow` directly on iOS as an `AsyncSequence` and **NEVER** create a duplicate ViewModel in Swift.
- **ELSE** (Native UI), maintain parallel state machines as defined in previous sections.

### KMP Shared ViewModel Example

When sharing state logic, you **Must** define the ViewModel in the shared module
and consume it in platform-specific UIs.

**Shared Module (Kotlin):**
\`\`\`kotlin
class SharedHomeViewModel : ViewModel() {
private val _state = MutableStateFlow(HomeState())
val state: StateFlow = _state.asStateFlow()

fun loadData() { /\* ... \*/ }
}
\`\`\`

**Android Consumption:**
`kotlin
@Composable
fun HomeScreen(viewModel: SharedHomeViewModel = viewModel()) {
val state by viewModel.state.collectAsStateWithLifecycle()
// Render UI
}`

**iOS Consumption (Swift):**
\`\`\`swift
struct iOSHomeScreen: View {
@StateObject var viewModel = SharedHomeViewModel()

var body: some View {
VStack {
// Render UI
}
.task {
for await state in viewModel.state {
// Update local Swift state
}
}
}
}
\`\`\`

## 9. State Management Testing

You **Must** verify state transitions and recomposition behavior in your tests.

### Unit Testing ViewModels

When testing `ViewModel` state driven by `StateFlow`, you **Must** use `collect`
or `first` within a coroutine scope to observe emissions.

> [!NOTE]
> **Note:** **StateFlow Testing with `stateIn`** : When using `stateIn` in ViewModels, ensure your tests collect the flow (e.g. using `launch { viewModel.uiState.collect() }`) before asserting `.value`. Otherwise, Robolectric tests may read the initial state due to the lack of active subscribers. Alternatively, use `MutableStateFlow` and collect in the `init` block of the ViewModel if it simplifies testing.

**Example: ViewModel Test**
\`\``kotlin
@Test
fun`when loadData is called, state is updated to loaded\`() = runTest {
val viewModel = UserViewModel()

// Start collecting to keep the flow active
backgroundScope.launch(UnconfinedTestDispatcher()) {
viewModel.username.collect()
}

viewModel.updateUsername("NewUser")

assertEquals("NewUser", viewModel.username.value)
}
\`\`\`

### UI Testing State (Compose)

Use `ComposeTestRule` to verify that UI updates correctly when state changes.

**Example: Compose UI Test**
\`\``kotlin
@Test
fun`when text state changes, text is displayed\`() {
val textState = mutableStateOf("Initial")
composeTestRule.setContent {
Text(text = textState.value)
}

composeTestRule.onNodeWithText("Initial").assertExists()

textState.value = "Updated"

composeTestRule.onNodeWithText("Updated").assertExists()
}
\`\`\`

## Tooling Integration: Standard Actions

To enable automated assessment and migration tracking, this skill defines the
following standard actions. Any automation tool built on this skill **Must**
implement these actions with the specified parameters to ensure benchmark
compliance.

### Verification Commands \& Agnostic Agent Instructions

To audit SwiftUI state decorators and verify Compose state hoisting:

1. **State Decorator Scan (CLI):** `bash
   grep -rnE "@State|@Binding|@StateObject|@ObservedObject|@EnvironmentObject" .`
2. **State Hoisting Audit:** *Inspect Compose UI components to ensure child composables receive immutable
   state (`value: T`) and event lambdas (`onValueChanged: (T) -> Unit`) rather
   than passing mutable state holders downwards.*

**Example: Action Invocation**
`bash
migration-assistant --run-action 'scan-ios-state' --onSourcePath './ios-project'`

## Antipatterns

### DO

- **DO** use `rememberSaveable` for state that must survive screen rotations.
- **DO** hoist state to parent composables to make children stateless and reusable.
- **DO** use `collectAsStateWithLifecycle()` to consume flows in Compose to avoid resource leaks.

### DON'T

- **DON'T** pass `MutableState` instances directly into child composables.
- **DON'T** use `remember` for state that needs to survive configuration changes.
- **DON'T** use `LiveData` in new Compose projects; prefer `StateFlow`.

### NEVER

- **NEVER** modify state directly inside a composable function without wrapping it in an event callback or side effect.
- **NEVER** use `collectAsState()` without lifecycle awareness in production, as it can waste resources when the app is in the background.