---
name: ios-to-android
description: "Use for ANY task that ports or implements Android functionality from\
  \ an iOS/Swift codebase \u2014 including single-feature tasks (BLE scanning, location/GPS\
  \ tracking, billing, Health Connect, background work, database, one screen), not\
  \ just full-app migrations. Primary coordinating skill for migrating iOS apps and\
  \ subsystems to Android (Jetpack Compose, Room, Kotlin Coroutines and Flow, Architecture\
  \ Components). Handles both full-app greenfield migrations and scoped sub-component\
  \ feature migrations: Info.plist permissions and AndroidManifest.xml mapping (Android\
  \ 14+ runtime permissions and foreground service types), SwiftUI views and screen\
  \ layouts, CoreData/Room, StoreKit/Google Play Billing (in-app subscriptions and\
  \ purchases), HealthKit/Health Connect, CoreBluetooth/BLE, CoreLocation/GPS, BGTaskScheduler/WorkManager,\
  \ and UserDefaults/DataStore. Don't use for Android-only work with no iOS source,\
  \ for converting Android XML Views to Compose, or for building or changing iOS apps."
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-11-14'
  keywords:
  - Android
  - iOS
  - Swift
  - SwiftUI
  - migration
  - Jetpack Compose
  - Kotlin
  - Coroutines
  - Flow
  - Room
  - Core Data
  - DataStore
  - UserDefaults
  - Google Play Billing
  - StoreKit
  - Health Connect
  - HealthKit
  - Bluetooth Low Energy
  - Core Bluetooth
  - location
  - Core Location
  - WorkManager
  - BGTaskScheduler
  - runtime permissions
  - Info.plist
  - AndroidManifest
---

This skill guides you through the structured process of migrating an Apple iOS
application to modern Android. It provides an end-to-end 10-step engineering
methodology that performs deep pre-flight project discovery, establishes Gradle
infrastructure, and conditionally loads specialized reference documents from
`references/` based on the specific subsystems detected in the source codebase.

## Objective

To systematically convert an iOS codebase into a clean, modern, and idiomatic
Android application powered by **Jetpack Compose** , **Kotlin Coroutines \&
Flow** , and **Android Architecture Components**, maintaining architectural
integrity and visual parity without legacy Android view bloat, unverified
stubbing, or proprietary vendor lock-in.

*** ** * ** ***

## Operational Scope Modes

Before taking action, determine which operational mode matches the user's
request and workspace:

### Mode A: Full Application Migration (End-to-End Greenfield)

- **Trigger**: The user asks to migrate an entire iOS application from start to finish, or the target directory is empty / contains only a clean starter skeleton.
- **Workflow**: Execute the full 10-step methodology sequentially (Steps 1 through 10), decomposing the app into multi-package architecture, establishing build configuration, porting all subsystems, and verifying against the final consolidated build gate.
- **Mandatory Pre-Migration Subsystem Inspection Gate** : Before implementing Android code for any detected iOS domain capability, you **MUST** explicitly view and inspect the corresponding reference guide in `references/` for each subsystem present in the source codebase:
  1. **Architecture \& State** : Inspect [references/architecture-and-viewmodels.md](references/architecture-and-viewmodels.md) and [references/state-and-reactive-flows.md](references/state-and-reactive-flows.md).
  2. **Screen Layouts \& Declarative UI** : Inspect [references/ui-layout-swiftui-to-compose.md](references/ui-layout-swiftui-to-compose.md).
  3. **Navigation** : Inspect [references/navigation-compose.md](references/navigation-compose.md).
  4. **Database \& Entities** : Inspect [references/database-coredata-to-room.md](references/database-coredata-to-room.md).
  5. **Preferences \& Settings** : Inspect [references/preferences-datastore.md](references/preferences-datastore.md).
  6. **Runtime Permissions** : Inspect [references/permissions-runtime.md](references/permissions-runtime.md).
  7. **Health \& Fitness** : Inspect [references/health-connect.md](references/health-connect.md).
  8. **In-App Billing \& Store** : Inspect [references/in-app-billing.md](references/in-app-billing.md).
  9. **Bluetooth LE Sensors** : Inspect [references/bluetooth-ble.md](references/bluetooth-ble.md).
  10. **Location \& GPS Tracking** : Inspect [references/location-fusedprovider.md](references/location-fusedprovider.md).
  11. **Background Tasks** : Inspect [references/background-tasks-workmanager.md](references/background-tasks-workmanager.md).

> [!IMPORTANT]
> **Important:** **This gate is mandatory.** Reading `SKILL.md` alone is **not** sufficient to complete a migration --- this root document is an index and a set of invariants; the reference guides hold the actual API-level implementation patterns. Skipping them is the single most common cause of mock-stub managers, missing runtime permissions, and non-idiomatic layouts that compile cleanly and then fail review.

You **MUST** record the list of reference guides you actually opened in
`.migration/migration_walkthrough.md` under a `## Reference Guides Consulted`
heading. If a subsystem is present in the source app and its guide is absent
from that list, the migration is **incomplete** --- go back and read it before
concluding.

### Mode B: Scoped Sub-Component \& Feature Migration (Contextual Integration)

- **Trigger** : The user asks to migrate a specific subsystem, feature, screen, or capability (e.g., CoreData to Room, HealthKit to Health Connect, StoreKit to Play Billing, BLE, or a specific screen) into an **existing Android
  project**.
- **Bypass Macro Overhead** : Do **NOT** perform a full 10-step audit of the entire iOS repository or generate a full `.migration/feature_specification.md`.
- **Execute the 4-Phase Micro Workflow** :
  1. **Target Codebase Convention Discovery**: Inspect the target Android project to discover and adopt established architecture, package structure, dependency injection, and state patterns.
  2. **Surgical Subsystem Routing** : Jump directly to the relevant supplemental reference guide in `references/` for the requested capability.
  3. **Scoped Migration \& Integration**: Implement the subsystem matching the discovered target conventions and wire it to replace any existing Fakes or stubs.
  4. **Targeted Build \& Test Verification** : Verify `./gradlew assembleDebug` or run specific unit tests without mutating unrelated modules.
- **Anti-Premature-Exit \& Execution Liveness Mandate** :
  - **NEVER exit or yield prematurely on background tasks** : If a build or test command (`./gradlew assembleDebug`, `./gradlew test`) runs asynchronously, do NOT output a passive waiting message (such as *"Waiting for tests to complete..."* ) without pending tool calls. In non-interactive runs, ending a turn with no pending tool call ends the task before the build finishes. Either wait synchronously (`WaitMsBeforeAsync: 10000`) or monitor the task to completion.
  - **A task is NEVER finished without the implementation diff** : You must author or update the target Kotlin source files (`write_to_file`, `replace_file_content`) replacing mock/stub implementations with real Android Jetpack/platform APIs *before* concluding.

*** ** * ** ***

## Target Codebase Convention Discovery Protocol (Mandatory for Mode B)

When performing a sub-component migration into an existing Android codebase, you
**MUST** inspect 2--3 existing files in `app/src/main/java/` and
`gradle/libs.versions.toml` before creating or modifying files. Adopt the target
project's established conventions across these 5 dimensions:

1. **Package Organization** :
   - *Layer-based* (`data/`, `domain/`, `ui/`) vs. *Feature-based* (`feature/home/`, `feature/catalog/`). Place new files into the existing package hierarchy rather than inventing an incompatible tree.
2. **Dependency Injection \& Service Locator** :
   - Discover how dependencies are provided: **Hilt** (`@Inject`, `@HiltViewModel`, `@InstallIn`), **Koin** , or manual **AppContainer** / Service Locator pattern.
   - **Do NOT introduce a foreign DI framework** . If the target app uses manual DI or an `AppContainer`, provide clean constructor injections that plug directly into that container. If it uses Hilt, provide Hilt `@Module` and `@Provides` bindings.
3. **State Management \& Concurrency** :
   - Observe how ViewModels expose UI state: unified `StateFlow<UiState>` data classes, individual StateFlows, or Compose `MutableState`.
   - Observe how Coroutine dispatchers are provided (e.g., injected `@IoDispatcher` vs `Dispatchers.IO`).
4. **Design System \& UI Components** :
   - Inspect `ui/theme/` and common component directories. If the codebase defines custom composable wrappers (e.g. `AppButton`, `AppCard`, custom typography tokens), reuse them rather than falling back to generic default primitives.
5. **Version Catalog \& Dependencies** :
   - Inspect `gradle/libs.versions.toml` before adding dependencies. If a library family (e.g. Room, Coroutines, Serialization) already exists, reuse the project's existing versions and aliases instead of adding duplicates.

*** ** * ** ***

## Summary of the 10-Step Migration Methodology (Mode A)

1. **Deep Source Audit, Domain Discovery \& Migration Brief**
2. **Subsystem \& Capability Decomposition**
3. **Formulate and Approve Phased Migration Plan**
4. **Project Infrastructure, Gradle \& DI Initialization** *(Mandatory Build
   Gate 1)*
5. **Resource Catalog \& Localization Migration**
6. **Local Persistence \& Storage Migration** *(Mandatory Build Gate 2)*
7. **Core Architecture, Concurrency, State Hoisting \& Secrets Handling**
8. **Declarative UI \& Navigation Porting to Jetpack Compose** *(Mandatory Build
   Gate 3)*
9. **Hardware, Platform Services \& Background Subsystems**
10. **Build Verification, Test Porting, Quality Triage \& Walkthrough
    Generation** *(Mandatory Build Gate 4)*

*** ** * ** ***

## Detailed Migration Steps

### Step 1: Deep Source Audit, Domain Discovery \& Migration Brief

Before writing any Android code, you **Must** perform an exhaustive audit of the
iOS source tree.

- **Domain Grounding** : Deduce the application's true functional purpose (e.g., finance, healthcare, social, productivity) from actual code, data models, and API integrations. **Do NOT** guess visual motifs or architectures based solely on the app's name.
- **Deep Functional Extraction**: Pay close attention to all functionality implemented in the source application, including non-obvious logic, background tasks, state synchronization, data flows, and lifecycle edge cases. Do not skim the surface.
- **Structure \& Dependencies** : Discover targets with `find . -maxdepth 3 -type d` and inventory dependencies via `Podfile`, `Package.swift`, or `Cartfile`.
- **Generate Migration Brief** : Produce a formal `feature_specification.md` in `.migration/` summarizing all features, entry/exit points, data flows, and Android target equivalents.
- Consult [references/analysis-and-project-audit.md](references/analysis-and-project-audit.md) for full audit checklists and the Migration Brief specification template.

### Step 2: Subsystem \& Capability Decomposition

Map detected iOS capabilities to Android replacement frameworks. Use the
**Heuristic Routing Table** below to determine which supplemental reference
guides are required for the project. When conducting a full-app migration (Mode
A), you **MUST** inspect the corresponding reference guide for every subsystem
detected in the iOS app before writing implementation code.

#### Heuristic Routing Table (Progressive Disclosure)

| Detected iOS Indicator / Code Pattern | Supplemental Reference Guide | Android Target Architecture |
|---|---|---|
| `import SwiftUI` / `List` / `VStack` | [references/ui-layout-swiftui-to-compose.md](references/ui-layout-swiftui-to-compose.md) | Jetpack Compose (LazyColumn, Row, Column, Box) |
| `import CoreData` / `.xcdatamodeld` | [references/database-coredata-to-room.md](references/database-coredata-to-room.md) | Jetpack Room, SQLite, KSP |
| `import CoreLocation` / `NSLocation*` | [references/location-fusedprovider.md](references/location-fusedprovider.md) | FusedLocationProviderClient / LocationManager |
| `import StoreKit` | [references/in-app-billing.md](references/in-app-billing.md) | Google Play Billing Library 8 |
| `import HealthKit` / `HKHealthStore` | [references/health-connect.md](references/health-connect.md) | Jetpack Health Connect |
| `import CoreBluetooth` / `CBCentralManager` | [references/bluetooth-ble.md](references/bluetooth-ble.md) | Android BLE GATT \& Scanner |
| `import AVFoundation` / `AVCaptureSession` | [references/camera-camerax.md](references/camera-camerax.md) | Jetpack CameraX (PreviewView, ImageAnalysis) |
| `import UserNotifications` / APNs | [references/push-notifications-fcm.md](references/push-notifications-fcm.md) | NotificationManager \& FCM |
| `BGTaskScheduler` / `BGAppRefreshTask` | [references/background-tasks-workmanager.md](references/background-tasks-workmanager.md) | Android WorkManager (CoroutineWorker) |
| `UserDefaults` / `@AppStorage` | [references/preferences-datastore.md](references/preferences-datastore.md) | Jetpack Preferences DataStore |
| `NavigationStack` / `NavigationView` | [references/navigation-compose.md](references/navigation-compose.md) | Navigation Compose with Type-Safe Routing |
| `withAnimation` / `.animation(` | [references/ui-animations.md](references/ui-animations.md) | Jetpack Compose Animation APIs |
| `import XCTest` / `XCUITest` | [references/testing-unit-and-compose.md](references/testing-unit-and-compose.md) | JUnit5, MockK, ComposeTestRule |
| `fastlane/Snapfile` / `snapshot(` | [references/screenshots-screengrab.md](references/screenshots-screengrab.md) | Fastlane Screengrab, UI Automator |

#### Universal Platform Capability \& ActivityResultContract Matrix

On Android, different system capabilities require distinct runtime invocation
mechanisms. When porting iOS system dialogs and authorization flows, you
**MUST** use the dedicated Android contract rather than generic permission
requests:

| iOS Framework / Capability | Android Runtime Mechanism | Dedicated Android Contract / API |
|---|---|---|
| Standard Runtime Permissions (`Camera`, `Location`, `Bluetooth`) | Activity Result Launcher | `rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions())` |
| HealthKit (`HKHealthStore.requestAuthorization`) | Health Connect Permission Contract | `rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract())` |
| Photo Library (`PHPickerViewController`) | Activity Result Photo Picker | `rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia())` |
| Document Picker (`UIDocumentPickerViewController`) | Storage Access Framework | `rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument())` |
| In-App Purchases / Subscriptions (`StoreKit`) | Play Billing Flow | `BillingClient.launchBillingFlow(activity, billingFlowParams)` |
| Android 14+ Foreground Services | Manifest Service Declaration | `<service android:foregroundServiceType="health|location|dataSync|connectedDevice" ... />` |

> [!IMPORTANT]
> **Important:** **Android 14+ (API 34+) Manifest \& Foreground Service Rules** : 1. **Foreground Service Types** : Any `<service>` tag in `AndroidManifest.xml` that runs in the foreground MUST specify `android:foregroundServiceType` matching the exact permissions and functionality used (e.g. `health`, `location`, `dataSync`, `camera`, `connectedDevice`). Omitting this attribute will trigger a runtime `SecurityException` on Android 14+. 2. **`Info.plist` HealthKit Mapping (`NSHealthShareUsageDescription` /
> `NSHealthUpdateUsageDescription`)** : Always map iOS HealthKit privacy keys in `AndroidManifest.xml` to Android 14+ **Health Connect permissions** (`android.permission.health.READ_STEPS`, `android.permission.health.READ_HEART_RATE`, `android.permission.health.READ_ACTIVE_CALORIES_BURNED`, etc.) alongside `<queries><package android:name="com.google.android.apps.healthdata" /></queries>`.

### Step 3: Formulate and Approve Phased Migration Plan

Generate a structured Migration Assessment Report and migration backlog based on
Steps 1 and 2.

- Detail the module breakdown, dependencies in `libs.versions.toml`, and sequential execution milestones.
- Explicitly identify any proprietary or closed-source iOS APIs that lack 1:1 Android mappings, and document the fallback strategy (see Unsupported Element Protocol in [references/analysis-and-project-audit.md](references/analysis-and-project-audit.md)).
- **IF** interactive user approval is supported, ask for confirmation before modifying or generating code.
- **ELSE**, proceed directly to Step 4 following the generated plan.

#### Contextual Architectural Scaling (By App Size \& Complexity)

Before generating Android source files, evaluate the scale and complexity of the
iOS source project and select the appropriate structural tier:

1. **Tier 1: Micro-Apps, Single-Feature Utilities, or Scoped Prototypes (\<
   1,000 LOC / 1--2 Screens)**
   - *Consolidation Permitted*: When migrating a small, self-contained utility or single-screen component, consolidating domain models, state holders, and Compose UI into a compact package or single cohesive file is acceptable when it improves readability and avoids over-engineering.
2. **Tier 2: Multi-Feature Production Applications (\> 1,000 LOC / 5+ Screens /
   Multiple Subsystems such as CoreData, BLE, HealthKit, Maps)**
   - *Full Architectural Decomposition Mandatory* : When migrating a complex, multi-journey application (such as a production app), you **MUST NOT** collapse the application into a single monolithic file (`MainActivity.kt`) or substitute functional screens with barebones test-tag stubs.
   - **Mandatory Multi-Package Execution Protocol for Complex Apps** :
     - **Package Decomposition** : Organize code into logical packages (`data/local/`, `data/preferences/`, `domain/model/`, `ui/navigation/`, `ui/screens/`, `ui/theme/`). `MainActivity.kt` must only initialize `enableEdgeToEdge()`, runtime permission launchers, and the root `NavHost` scaffold.
     - **Complete Local Persistence (`data/local/` \&
       `data/preferences/`)** : Implement concrete `@Entity` classes for **all** core domain tables (e.g., both the catalog entity and the user-history entity), a `@Dao` interface exposing `Flow<List<T>>` queries, and a `RoomDatabase`. Implement asynchronous `DataStore<Preferences>` for user settings and authentication tokens.
     - **Extracted Android Resources (`res/values/strings.xml` \&
       `ui/theme/Theme.kt`)** : Extract user-facing strings into `res/values/strings.xml` and define both Light and Dark Material 3 `ColorScheme`s in `Theme.kt`.
     - **Complete Navigation Graph (`ui/navigation/NavGraph.kt`)** : Build a `NavHost` covering both top-level bottom navigation tabs (`launchSingleTop = true`, `restoreState = true`) AND distinct sub-flow destinations (e.g. onboarding, detail screens, full-screen task flows). Model onboarding / first-run flows as destinations inside that `NavHost` (choose the start destination from the persisted "onboarding complete" flag); never render them as a separate composable tree outside the `NavHost` that calls `navController.navigate(...)` before the graph has been set, which throws at runtime.
     - **Runtime Permission Launchers** : In addition to declaring the Android 14+ permissions for each capability the source app uses in `AndroidManifest.xml`, invoke `rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions())` at runtime in the Compose UI layer.
     - **Feature-Complete Material 3 Screens (`ui/screens/*.kt`)** : Every screen must collect state from a Jetpack `ViewModel` via **`collectAsStateWithLifecycle()`** (never raw `collectAsState()`), render scrollable lists using **`LazyColumn` with explicit
       `key = { ... }`** parameters, and present Material 3 content showing the real domain data from the source app's models (not placeholder text), matching the functional specification.

### Step 4: Project Infrastructure, Gradle \& DI Initialization

Inspect the target Android directory before touching build configuration files:

- **IF an Android project shell already exists** (i.e. `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`, and `MainActivity.kt` are present):
  - **DO NOT** generate wholly new project files or overwrite `settings.gradle.kts`, root `build.gradle.kts`, or `app/build.gradle.kts` from scratch.
  - **MUST PRESERVE** the existing `namespace`, `applicationId`, and root package name (e.g., keep `MainActivity.kt` in its existing root package and place all new sub-packages---`data/local/`, `data/preferences/`, `domain/model/`, `ui/navigation/`, `ui/screens/`, `ui/theme/`---under that same root package).
  - **Incremental Gradle Updates Only** : Modify `app/build.gradle.kts` and `gradle/libs.versions.toml` to add all Jetpack and Google Play libraries required for the detected iOS capabilities **immediately in Step 4** before authoring manager classes. Never remove existing dependencies, plugins, or test configuration you did not add.
  - **Mandatory Subsystem Dependencies in `app/build.gradle.kts`** :
    - If **HealthKit** (`import HealthKit`) is detected: set `minSdk = 26` in `defaultConfig {}` and add `implementation("androidx.health.connect:connect-client:1.1.0-alpha07")` and `implementation("com.google.guava:guava:33.0.0-android")` to `dependencies {}`.
    - If **StoreKit** (`import StoreKit`) is detected: add `implementation("com.android.billingclient:billing-ktx:8.3.0")` to `dependencies {}`.
    - If **BGTaskScheduler** (`import BackgroundTasks`) is detected: add `implementation("androidx.work:work-runtime-ktx:2.9.0")` to `dependencies {}`.
    - If **CoreLocation** (`import CoreLocation`) is detected: add `implementation("com.google.android.gms:play-services-location:21.2.0")` to `dependencies {}`.
- **ELSE (if starting in an empty directory with no Gradle files)** :
  - Initialize the target Android project structure with Kotlin DSL (`settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`) and establish module boundaries matching iOS targets.
- Consult [references/project-setup-and-gradle.md](references/project-setup-and-gradle.md) and [references/dependency-injection-hilt.md](references/dependency-injection-hilt.md).

### Step 5: Resource Catalog \& Localization Migration

Extract and convert iOS asset catalogs and strings into Android resource
directories.

- Convert image assets and vector drawings in `.xcassets` into Android XML Drawables.
- Map app icons to `res/mipmap-anydpi-v26/ic_launcher.xml` adaptive icons.
- Convert `Localizable.strings` into `res/values/strings.xml`.
- Consult [references/resources-and-assets.md](references/resources-and-assets.md).

### Step 6: Local Persistence \& Storage Migration

Port data models, local relational databases, and key-value preferences.

- **IF** Core Data / SwiftData is detected, map entities, type converters, and DAOs to Room using [references/database-coredata-to-room.md](references/database-coredata-to-room.md).
- **IF** `UserDefaults` or `@AppStorage` is detected, replace with Preferences DataStore using [references/preferences-datastore.md](references/preferences-datastore.md).
- **Initial Database Seeding** : If the iOS app ships bundled or preloaded data (sample records, catalog items, defaults), you **MUST** seed it on first launch. Otherwise list and detail screens render empty states and the app looks broken on first launch.
  - **Default strategy --- `RoomDatabase.Callback` seeding** : Read the iOS seed data (bundled JSON/plist, Swift sample-data arrays, or rows extracted at migration time from a bundled database such as a Core Data / SwiftData / GRDB / FMDB `.sqlite` or a Realm file) and insert equivalent domain rows through your DAOs in `onCreate()`. This cannot fail Room's schema validation.
  - **NEVER copy a bundled iOS database file (Core Data / SwiftData / GRDB /
    FMDB `.sqlite`) verbatim into `assets/` and pass it to
    `createFromAsset()`** unless every `@Entity` was deliberately written to mirror that file's exact `Z*` tables, columns, types, nullability and indices (see [references/database-coredata-to-room.md](references/database-coredata-to-room.md)). Room validates pre-packaged databases on first open and throws `IllegalStateException: Pre-packaged database has an invalid schema`, crashing the app on launch. Adding extra tables to the copied file does **not** fix this.
  - **You usually cannot observe this crash** (no device/emulator; `assembleDebug` still succeeds). If you do use `createFromAsset()`, you **MUST** statically prove schema parity before finishing, using the no-device verification in section 0, "Choose a seeding strategy before writing entities", of [references/database-coredata-to-room.md](references/database-coredata-to-room.md).
- **Room Threading** : Room throws `IllegalStateException` for any query on the main thread. Make DAO methods `suspend` or return `Flow`; never call the database from a ViewModel `init {}` body, from composition, or inside `runBlocking` on the main thread --- launch the work in `viewModelScope` (or a repository coroutine on `Dispatchers.IO`) instead. Run seeding from the `RoomDatabase.Callback` on a background dispatcher. **Never** use `allowMainThreadQueries()` in production code (it is acceptable only in tests).

### Step 7: Core Architecture, Concurrency, State Hoisting \& Secrets Handling

Translate business logic, network services, reactive state management, and
external API configurations.

- Map CocoaPods / Swift Package Manager libraries and Swift `async`/`await` to Kotlin Coroutines using [references/dependencies-and-concurrency.md](references/dependencies-and-concurrency.md).
- Convert ObservableObjects into Android `ViewModel` instances with `SavedStateHandle` using [references/architecture-and-viewmodels.md](references/architecture-and-viewmodels.md).
- Convert `@State`, `@Binding`, and Combine publishers into `mutableStateOf`, `StateFlow`, and `SharedFlow` with `collectAsStateWithLifecycle` using [references/state-and-reactive-flows.md](references/state-and-reactive-flows.md).
- **MANDATORY LIFECYCLE AWARENESS** : When observing ViewModel `StateFlow` or `SharedFlow` streams inside Jetpack Compose screens, **ALWAYS** use `collectAsStateWithLifecycle()` from `androidx.lifecycle.compose.collectAsStateWithLifecycle`. **NEVER** use standard `collectAsState()`. `collectAsState()` does not pause collection when the application is backgrounded or the screen is off, leaking resources and draining battery by keeping sensors, database queries, and network flows active.
- **Mandatory Named Arguments for Dependency Wiring** : When instantiating repositories, use cases, and ViewModels in `AppContainer.kt` or `MainActivity.kt`, **always use Kotlin named arguments** (e.g. `ItemRepository(context = this, api = api)`). This eliminates swapped parameter compilation errors between the class definition and its call site.
- **API Keys \& Secrets Rules** : Never hardcode secrets in source code. Place API keys into `local.properties` and read them via Gradle `buildConfigField` or secure credential providers.

### Step 8: Declarative UI \& Navigation Porting to Jetpack Compose

Rebuild user interface screens and navigation hierarchies in Jetpack Compose.

- **Material 3 Semantic Theming \& Color Encapsulation** :
  - **NEVER** reference un-scoped top-level color constants directly inside screen composables (e.g. referencing loose `val CustomBrandBlue = Color(...)`). This causes unresolved reference errors across packages and breaks theme adaptability.
  - Map all branding and palette colors extracted from iOS asset catalogs (`Assets.xcassets`) directly into the `ColorScheme` definition in `ui/theme/Theme.kt` (using standard slots like `primary`, `secondary`, `tertiary`, `surface`, `surfaceVariant`, `error`) or into a custom `ExtendedColorScheme` provided via `CompositionLocalProvider`.
  - In screen composables, access colors strictly via `MaterialTheme.colorScheme.<slot>` or semantic design system tokens. This guarantees zero missing imports across any package and automated Dark/Light theme switching.
- **Mandatory Edge-to-Edge** : Implement `enableEdgeToEdge()` on the host Activity and properly handle `WindowInsets` across all composable screens.
- **Compose Previews** : Every top-level composable screen **MUST** include a matching `@Preview` function to enable visual validation.
- Implement type-safe screen routing and bottom navigation using [references/navigation-compose.md](references/navigation-compose.md).
- Replicate layout transitions, physics-based springs, and shared elements using [references/ui-animations.md](references/ui-animations.md).
- **Jetpack Compose Runtime Permission Protocol** :
  - In Jetpack Compose applications, runtime permissions **MUST** be requested inside `@Composable` screens using `rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions())` (or `PermissionController.createRequestPermissionResultContract()` for Health Connect).
  - **NEVER** fall back to legacy Activity-level `registerForActivityResult()` inside `MainActivity.kt` **unless there is
    no composable-compliant permission alternative** available for that specific platform capability.
  - **NEVER** simulate platform permissions with mock boolean flags in repositories or bypass real permission checks.
- **Compose Layout Anti-Squeeze Invariant** : In every `Row` that places flexible content (a title/description `Text` or `Column`) beside fixed-intent elements (a `Button`, price or badge `Text`, `FilterChip`, `SegmentedButton`, `IconButton`, `Switch`), apply `Modifier.weight(1f)` to the flexible side and `maxLines = 1` to the fixed element's `Text`; otherwise Compose squeezes the fixed element and wraps its label one character per line on a typical phone width (roughly 360--411dp). `Arrangement.SpaceBetween` is not a substitute for a weight. Put multi-option selectors (segmented buttons, chip groups) on their own full-width row below the section title, each option `Modifier.weight(1f)`. Worked examples: [references/ui-layout-swiftui-to-compose.md](references/ui-layout-swiftui-to-compose.md).
- **String Resources Are Referenced, Not Just Declared** : SwiftUI `Text("Literal")` is an implicit `LocalizedStringKey` lookup; Compose `Text("Literal")` is not. Every user-facing string --- text, button and chip labels, titles, `contentDescription`, snackbar/dialog text, and custom-component `text`/`title`/`label` arguments, including strings the iOS code wrote inline --- must come from `stringResource(R.string....)`. A `strings.xml` the UI never references does not count. Also:
  - **Strings first, per screen** : before writing each composable, add that screen's user-facing strings to `strings.xml` --- the iOS view's inline literals as well as its localized keys (`String(localized:)`, `NSLocalizedString`, or generated accessors such as SwiftGen's `L10n` / R.swift), reusing existing entries --- then write the composable against `R.string` keys. Don't defer extraction to a clean-up pass.
  - **Dynamic text uses format arguments** , never concatenation or string templates (`<string name="trail_distance">Distance: %1$s</string>` → `stringResource(R.string.trail_distance, distance)`); concatenated fragments cannot be translated or reordered.
  - **Display names on models and enums** (goals, categories, levels) hold a `@StringRes Int` resolved in the UI, not a raw `String`.
  - **Text outside composition** (notifications, toasts, foreground-service notifications, widgets) uses `context.getString(...)`.
  - Previews, log messages and non-user-facing keys (routes, test tags, channel IDs) are exempt.

### Step 9: Hardware, Platform Services \& Background Subsystems

Conditionally load and execute domain-specific platform services when identified
during Step 2:

- **Runtime Permissions** : Consult [references/permissions-runtime.md](references/permissions-runtime.md) for the iOS `Info.plist` → Android 14+ permission mapping table, the `rememberPermissionRequest` hook pattern, Health Connect's dedicated permission contract, and the two-step background location flow. **Every
  dangerous permission declared in `AndroidManifest.xml` requires a matching
  runtime request** --- a manifest entry alone grants nothing, and the request must surface the real system dialog rather than an in-app substitute.
- **Camera** : Consult [references/camera-camerax.md](references/camera-camerax.md) for CameraX preview (`PreviewView` inside Compose `AndroidView`) and `ImageCapture`.
- **Location \& Geofencing** : Consult [references/location-fusedprovider.md](references/location-fusedprovider.md) for Google Play Fused Location Provider and runtime permission flows.
- **Bluetooth LE** : Consult [references/bluetooth-ble.md](references/bluetooth-ble.md) for BLE scanning and GATT callbacks.
- **In-App Billing** : Consult [references/in-app-billing.md](references/in-app-billing.md) for Google Play Billing Library 8.
- **Health \& Fitness** : Consult [references/health-connect.md](references/health-connect.md) for Health Connect data types and permissions.
- **Push \& Local Notifications** : Consult [references/push-notifications-fcm.md](references/push-notifications-fcm.md) for Notification Channels, Android 13+ permissions, and FCM.
- **Background Tasks** : Consult [references/background-tasks-workmanager.md](references/background-tasks-workmanager.md) for `WorkManager` constraints and periodic jobs.

> [!CAUTION]
> **Caution:** **Non-Negotiable Real Platform SDK Contract (Never Write Pure Mock
> Managers)** : iOS codebases often include Simulator fallbacks or dummy data inside their platform-service managers (for example HealthKit, CoreBluetooth, StoreKit, CoreLocation, background tasks, camera, push notifications, MapKit or CloudKit). When migrating (including full-app Mode A migrations), you **MUST NOT** replace platform SDKs with pure mock timers or `SharedPreferences`. Implement the real Android SDK in each manager/service class. Wrap hardware/SDK calls in `try/catch` so JVM unit tests and devices without the hardware or SDK don't crash; this covers missing *hardware* , never missing *permissions* --- check the permission first, then call inside `try/catch`. Sample/demo data is allowed **only** as a fallback when the SDK is unavailable or the user denies permission: never mixed into real results, and never granting an entitlement. If the source gates sample/demo data behind a user setting, debug flag or `#if targetEnvironment(simulator)`, port that gate (a DataStore preference or `BuildConfig.DEBUG`); never call a demo-data loader unconditionally from `init` or from a scan/read entry point. 1. **Runtime permissions** : see *Runtime Permission Invariants* below. `@SuppressLint("MissingPermission")` is not a permission implementation; it is allowed only on a call site guarded by `ContextCompat.checkSelfPermission(...) == PackageManager.PERMISSION_GRANTED`. 2. **Health Connect** : `HealthConnectClient.getOrCreate(context)`, `HealthPermission.getReadPermission(...)` / `getWritePermission(...)` for the record types matching the HealthKit types the source app reads and writes, `readRecords(ReadRecordsRequest(...))` and `insertRecords(...)`. See [references/health-connect.md](references/health-connect.md). 3. **Google Play Billing** : `BillingClient.newBuilder(context).setListener(purchasesUpdatedListener).enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()).build()`; a `BillingClientStateListener` whose `onBillingServiceDisconnected()` **actually reconnects** (for example with exponential backoff, or reconnect-on-next-request) rather than only logging; `queryProductDetailsAsync`, `launchBillingFlow(activity, params)`, and `acknowledgePurchase`. Placeholder prices may be displayed as a fallback, but a purchase with no ready client or no Activity fails --- delete mock purchase code rather than keeping it as an `else` branch. See [references/in-app-billing.md](references/in-app-billing.md). 4. **Bluetooth LE** : `BluetoothManager` → `BluetoothLeScanner.startScan(filters, settings, scanCallback)` with a concrete `ScanCallback`, and a concrete `BluetoothGattCallback` (`onConnectionStateChange`, `onServicesDiscovered`, `onCharacteristicChanged` parsing the GATT characteristics the iOS code reads). Manifest: `BLUETOOTH_SCAN` (with `android:usesPermissionFlags="neverForLocation"` unless location is derived from scans) and `BLUETOOTH_CONNECT`, both requested at runtime; legacy `BLUETOOTH` / `BLUETOOTH_ADMIN` and any scan-only `ACCESS_FINE_LOCATION` limited with `android:maxSdkVersion="30"`. See [references/bluetooth-ble.md](references/bluetooth-ble.md). 5. **Location** : `LocationServices.getFusedLocationProviderClient(context)` with `LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)` and a `LocationCallback` (`requestLocationUpdates` / `removeLocationUpdates`). When tracking must continue after the UI leaves the screen (iOS `allowsBackgroundLocationUpdates` / `UIBackgroundModes: location`), add a `Service` that calls `startForeground` with an ongoing notification, declared with `android:foregroundServiceType="location"` plus `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_LOCATION`. Don't declare `ACCESS_BACKGROUND_LOCATION` unless a code path reads location with no visible UI and no foreground service; never declare permissions the migrated code doesn't use. See [references/location-fusedprovider.md](references/location-fusedprovider.md). 6. **WorkManager** : a `CoroutineWorker` scheduled with `WorkManager.getInstance(context).enqueueUniquePeriodicWork(..., ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<...>(...).setConstraints(...).build())`, mirroring the iOS task's cadence and constraints. See [references/background-tasks-workmanager.md](references/background-tasks-workmanager.md).

#### Runtime Permission Invariants

The permission layer is the most frequently omitted part of an iOS→Android
migration, because a manifest-only port compiles cleanly and fails only at
runtime. The full pattern --- a `rememberPermissionRequest` state hook, call-site
guards, Health Connect's contract, two-step background location, and a
verification checklist --- is in [references/permissions-runtime.md](references/permissions-runtime.md). Read it
before writing permission code.

> [!CAUTION]
> **Caution:** **The runtime prompt MUST be the real Android system dialog, launched
> through `rememberLauncherForActivityResult` from the user action that needs the
> permission, and a screen MUST still render its content when a permission is not
> granted.** These shapes are a **failed migration** : \* ❌ A utility object of static `hasXPermission()` helpers. Checking is not requesting. \* ❌ A permission helper or launcher that is defined or imported but never invoked from a screen. \* ❌ `if (granted) ScreenContent() else PermissionScreen()`. Permission state may disable a control or populate an empty state; it must never replace the screen.

- **Point of use** : request each dangerous permission from the action that needs it --- e.g. the control that starts location tracking → fine/coarse location; the control that scans for BLE devices → `BLUETOOTH_SCAN` + `BLUETOOTH_CONNECT`; the screen or sheet that opens the camera → `CAMERA`, before binding CameraX (the most commonly missed request); app entry → `POST_NOTIFICATIONS` on API 33+ (from a `LaunchedEffect`, without gating content).
- **Health Connect** uses `PermissionController.createRequestPermissionResultContract()`, not `RequestMultiplePermissions()`. The action that reads health data must first call `permissionController.getGrantedPermissions()` and launch the request if anything is missing. Fall back to sample data **only** after denial or when the SDK is unavailable --- never instead of requesting.
- **Background location** is a second, separate request after foreground location is granted, and only if a code path needs it.
- If the source app has its own explanatory permission UI, port it as a non-blocking banner, card, or empty state that calls the request action.
- A declared dangerous permission with no runtime request reached from a screen is a failed migration (verified in the Step 10 Pre-Completion Checklist).

### Step 10: Build Verification, Test Porting, Quality Triage \& Walkthrough Generation

Verify that the migrated Android application is fully functional, tested, and
documented.

- **INCREMENTAL LAYER COMPILATION \& VERTICAL SLICING GATES** :
  - To prevent late-stage compilation cascades across large multi-file migrations, adhere to incremental compilation gates:
    1. **Data \& Domain Gate** : After implementing domain models, Room entities, DAOs, and repositories, execute `./gradlew compileDebugKotlin` to verify the foundational data layer compiles cleanly before touching the UI.
    2. **Vertical Feature Slicing for UI** : Author UI feature-by-feature (`ViewModel` ➡️ `Screen Composable` ➡️ `Route`). Before implementing a screen, inspect the corresponding `ViewModel` state data class and domain models to ensure 100% property name and type fidelity.
    3. **Per-Slice Verification** : Run `./gradlew compileDebugKotlin` after completing each feature slice (or every 2 screens). Catching a property mismatch or syntax error immediately takes seconds and guarantees a clean final build.
  - At the conclusion of migration, execute `./gradlew :app:assembleDebug` and `./gradlew :app:testDebugUnitTest`. Both **MUST** exit 0.
- **Pre-Completion Checklist (run every item before finishing; each is a
  search, not a recollection)** :
  1. **Debugging stubs restored** : anything you stubbed, moved or commented out to isolate a failure (e.g. a minimal `setContent { Text("test") }`) is restored; `MainActivity.kt` hosts the real theme and navigation graph; no temporary directories or placeholder composables remain.
  2. **Permissions** : every permission helper has a call site outside its own definition, and every dangerous permission in `AndroidManifest.xml` (e.g. `CAMERA`, location, Bluetooth, notifications, Health Connect) appears in a runtime request reached from a screen.
  3. **Strings** : search UI and notification code for `Text("`, `text = "`, `title = "`, `label = "`, `contentDescription = "`, `setContentTitle("` / `setContentText("`, and string templates containing `$` in UI files; move every user-facing hit into `strings.xml` per Step 8.
  4. **Platform managers**: no demo/sample-data loader is called unconditionally (Step 9 contract); every hardware/SDK path calls the real Android API.
  5. **Database** : if `createFromAsset()` is used, schema parity is proven per Step 6; no Room query runs on the main thread.
  6. Re-run `./gradlew :app:assembleDebug` and `./gradlew :app:testDebugUnitTest`; both exit 0.
- **Turn Completion** : Do **NOT** end your turn or output a waiting message after starting a background build; keep working or wait for it to finish.
- Port XCTest unit tests to JUnit5 / MockK, and XCUITest journeys to Compose UI tests using [references/testing-unit-and-compose.md](references/testing-unit-and-compose.md).
- If the iOS project automates screenshots (fastlane `snapshot`), port it using [references/screenshots-screengrab.md](references/screenshots-screengrab.md).
- **Prioritized Quality Gate \& Refinement Triage** : Follow this strict 3-tier feedback protocol when validating:
  1. **Priority #1: Stability (Crashes)**: Fix all unhandled exceptions, KSP annotation crashes, or runtime crashes immediately. Iterate until the app is stable.
  2. **Priority #2: Core Feature Parity** : Check that all CUJs and top-level navigation tabs in `feature_specification.md` are implemented with complete, functional Material 3 screens across the full navigation graph. Trigger at most a **single (one-time) refinement loop** per feature.
  3. **Priority #3: Critical UI Defects** : Resolve obvious layout breakage (padding, clipping, margins), jarring unstyled default colors, missing drawables, and Edge-to-Edge compliance. **Ignore minor cosmetic or
     typography nitpicks** to avoid thrashing.
- **Generate Migration Walkthrough** : Write `.migration/migration_walkthrough.md` documenting all transformed components, verification outcomes, and architectural decisions. Every "implemented X" claim **MUST** name the file where X is actually *used* (not just defined) --- e.g. the screen/button that invokes a permission request. If you cannot point to a call site, the feature is not implemented; go back and wire it.

*** ** * ** ***

## Antipatterns

### DO

- **DO** run the automated project audit before writing any code.
- **DO** preserve the existing `namespace`, `applicationId`, and root package structure when migrating into an existing Android project shell---only generate wholly new project/Gradle files if none exist to start, and only update Gradle configuration (`app/build.gradle.kts`, `libs.versions.toml`) to add libraries/plugins required for functionality being ported.
- **DO** scale your package architecture contextually: compact files for small single-feature utilities (\< 1,000 LOC); full multi-package architecture for complex multi-journey apps (\> 1,000 LOC / 5+ screens), with every screen of the source app reachable in the `NavHost` graph.
- **DO** declare in `AndroidManifest.xml` the Android 14+ permission for every iOS `Info.plist` privacy key the app actually uses (see the mapping table in [references/permissions-runtime.md](references/permissions-runtime.md)).
- **DO** ensure all added permissions are requested at runtime according to Jetpack Compose best practices using `rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions())` (and Health Connect permission request contracts) rather than only declaring them in the manifest.
- **DO** keep UI strings in `res/values/strings.xml` and colors in a Light + Dark Material 3 theme, migrate relational data to Room (`@Entity`, `@Dao`, `RoomDatabase` exposing `Flow`), and use `DataStore<Preferences>` for key-value settings.
- **DO** produce a formal `feature_specification.md` capturing non-obvious logic and data flows.
- **DO** verify the complete migration with a consolidated compile and test check (`./gradlew assembleDebug` and `./gradlew testDebugUnitTest`) at the conclusion of code generation.
- **DO** load only the specific reference files flagged during the initial audit.
- **DO** use Jetpack Compose, Material Design 3, and `enableEdgeToEdge()` for all UI screens, collecting state via `collectAsStateWithLifecycle()` and using explicit `key = { ... }` in `LazyColumn` items.
- **DO** isolate all secrets and API keys in `local.properties`.
- **DO** generate `@Preview` functions for every top-level composable screen.

### DON'T

- **DON'T** claim migration success without real, verified CLI execution output showing exit code 0.
- **DON'T** overwrite existing `settings.gradle.kts`, root `build.gradle.kts`, or `app/build.gradle.kts` from scratch or change the root `namespace`/package name when migrating into an existing Android project shell.
- **DON'T** collapse a complex multi-screen application into a single monolithic `MainActivity.kt` file or use barebones test-tag stub buttons instead of functional Material 3 screens.
- **DON'T** declare permissions in `AndroidManifest.xml` without also implementing runtime permission requests (`rememberLauncherForActivityResult`) in the UI layer.
- **DON'T** enter infinite refinement loops for minor cosmetic or typography differences.
- **DON'T** load all supplemental reference files simultaneously into the agent's context.
- **DON'T** access files in `references/` without first reading and following the primary orchestration protocol in `SKILL.md`.
- **DON'T** translate Swift code line-by-line; translate concepts, architectural boundaries, and behaviors.
- **DON'T** use legacy Android XML Views, `findViewById`, or `AsyncTask` in new migrations.
- **DON'T** fail silently on unsupported or proprietary iOS APIs.

### NEVER

- **NEVER** finalize a migration without verifying that `./gradlew assembleDebug` exits 0.
- **NEVER** exit a turn or emit messages like "Waiting for tests/build to complete..." without authoring the requested Kotlin implementation diff.
- **NEVER** hardcode API keys, secrets, or tokens in source code or version control.
- **NEVER** leave empty placeholder functions or `TODO` comments in production Kotlin source files.
- **NEVER** ignore iOS `Info.plist` permissions during the initial audit phase.
