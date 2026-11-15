> [!IMPORTANT]
> **Important:** **ORCHESTRATION GATEWAY PREREQUISITE** : This document is a supplemental reference guide for [`../SKILL.md`](https://developer.android.com/agents/skills/migration/ios-to-android/skill). You **MUST** read and follow the primary orchestration protocol in `../SKILL.md` before applying any patterns from this guide. `SKILL.md` enforces mandatory incremental compilation gates (`./gradlew compileDebugKotlin`), vertical feature slicing, and strict anti-premature-exit execution discipline.

## 1. Project Health \& Discovery

Before initiating any code translation, you **Must** perform an exhaustive
automated audit of the iOS codebase.

- **Structure Discovery** : Run `find . -maxdepth 3 -type d` to inspect the project layout and determine target boundaries.
- **Dependency Inventory** : Read `Podfile`, `Package.swift`, or `Cartfile` to identify third-party libraries and SDK integrations.
- **UI Framework Identification** : Run grep scans to quantify UI paradigms: `bash
  grep -rn "import SwiftUI" . | wc -l
  grep -rn "import UIKit" . | wc -l`

*** ** * ** ***

## 2. Automated Subsystem Heuristic Matrix

You **Must** scan the iOS codebase for the following indicators and map them to
their required supplemental reference guides:

| Detected iOS Indicator / Code Pattern | Required Supplemental Reference Guide | Primary Android Technology |
|---|---|---|
| `import CoreData` OR `import SwiftData` OR `*.xcdatamodeld` | [database-coredata-to-room.md](https://developer.android.com/agents/skills/migration/ios-to-android/references/database-coredata-to-room) | Jetpack Room, SQLite, KSP |
| `import CoreLocation` OR `NSLocation*UsageDescription` | [location-fusedprovider.md](https://developer.android.com/agents/skills/migration/ios-to-android/references/location-fusedprovider) | Google Play Fused Location Provider |
| `import StoreKit` | [in-app-billing.md](https://developer.android.com/agents/skills/migration/ios-to-android/references/in-app-billing) | Google Play Billing Library 8 |
| `import HealthKit` OR `HKHealthStore` | [health-connect.md](https://developer.android.com/agents/skills/migration/ios-to-android/references/health-connect) | Jetpack Health Connect |
| `import CoreBluetooth` OR `CBCentralManager` | [bluetooth-ble.md](https://developer.android.com/agents/skills/migration/ios-to-android/references/bluetooth-ble) | Android BLE GATT \& Scanner |
| `import AVFoundation` OR `AVCaptureSession` | [camera-camerax.md](https://developer.android.com/agents/skills/migration/ios-to-android/references/camera-camerax) | Jetpack CameraX (PreviewView, ImageAnalysis) |
| `import UserNotifications` OR APNs handlers | [push-notifications-fcm.md](https://developer.android.com/agents/skills/migration/ios-to-android/references/push-notifications-fcm) | NotificationManager \& Firebase Cloud Messaging |
| `BGTaskScheduler` OR `BGAppRefreshTask` | [background-tasks-workmanager.md](https://developer.android.com/agents/skills/migration/ios-to-android/references/background-tasks-workmanager) | Android WorkManager (CoroutineWorker) |
| `UserDefaults` OR `@AppStorage` | [preferences-datastore.md](https://developer.android.com/agents/skills/migration/ios-to-android/references/preferences-datastore) | Jetpack Preferences DataStore |
| `NavigationStack` OR `NavigationView` OR `NavigationLink` | [navigation-compose.md](https://developer.android.com/agents/skills/migration/ios-to-android/references/navigation-compose) | Navigation Compose with Type-Safe Routing |
| `withAnimation` OR `.animation(` OR `matchedGeometryEffect` | [ui-animations.md](https://developer.android.com/agents/skills/migration/ios-to-android/references/ui-animations) | Jetpack Compose Animation APIs |
| `import XCTest` | [testing-unit-and-compose.md](https://developer.android.com/agents/skills/migration/ios-to-android/references/testing-unit-and-compose) | JUnit5, MockK, ComposeTestRule |
| `fastlane/Snapfile` OR `snapshot(` | [screenshots-screengrab.md](https://developer.android.com/agents/skills/migration/ios-to-android/references/screenshots-screengrab) | Fastlane Screengrab, UI Automator |

*** ** * ** ***

## 3. Project Structure Mapping

| iOS Concept | Android Equivalent | Architectural Rule |
|---|---|---|
| **Xcode Project** (`.xcodeproj`) | **Root Gradle Project** | Root folder containing `settings.gradle.kts` and `build.gradle.kts`. |
| **Xcode Target** | **Gradle Module** | Discrete buildable unit (`:app`, `:core:database`, `:feature:profile`). |
| **Schemes \& Configurations** | **Build Types \& Flavors** | Debug/Release map to Build Types; custom environments map to Product Flavors. |
| **Info.plist** | **AndroidManifest.xml** | Package metadata, exported activities, and runtime permission declarations. |
| **Assets.xcassets** | **res/ (Resources)** | Vector drawables (`res/drawable`), app icons (`res/mipmap`), colors/strings (`res/values`). |

*** ** * ** ***

## 4. Deep Functional Extraction \& Domain Grounding

Before generating any Android architecture, theme, or code, you **Must** deduce
the application's actual functional domain from source code, data entities, and
service integrations.

- **Domain Grounding**: Never rely on the app's title or package name alone to infer motifs or visual design. Analyze models and endpoints to determine whether the app is financial, healthcare, social, productivity, media, or e-commerce.
- **Non-Obvious Logic Extraction** : Inspect for:
  - Background sync mechanisms, observers, and periodic refresh tasks.
  - State recovery logic during process death (`scenePhase`, `willResignActive`, `didEnterBackground`).
  - Implicit data cascading (e.g. Core Data relationships with cascade deletes).
  - Auth token refreshes and interceptors.
  - Edge cases and failure states (offline mode, network retry backoff, validation rules).

*** ** * ** ***

## 5. Migration Brief Specification (`feature_specification.md`)

Before writing any target code, the agent **Must** generate and persist a formal
Migration Brief at `.migration/feature_specification.md`.

```markdown
# Migration Brief: [App Name]

## 1. Domain & Core Purpose
- **Inferred Domain**: [e.g. Personal Finance & Budgeting]
- **Primary Value Proposition**: [Brief description grounded in code analysis]

## 2. Source Feature Inventory & Technical Mappings
| Feature Name | Source Implementation (iOS) | Target Implementation (Android) | Data/Backend Access | Complexity |
| :--- | :--- | :--- | :--- | :--- |
| User Authentication | `AuthenticationServices`, Keychain | Credential Manager, DataStore + Tink | REST `/auth/login` | Medium |
| Expense Ledger | `CoreData` (`TransactionEntity`) | Jetpack Room (`TransactionEntity`, `Dao`) | Local SQLite | High |
| Real-Time Exchange | Combine `URLSession.dataTaskPublisher` | Kotlin Coroutines, Flow, Retrofit | WebSocket / SSE | Medium |

## 3. Navigation & Screen Topology
- **Top-Level Navigation**: [Bottom navigation with 3 tabs: Dashboard, Transactions, Settings]
- **Flow Depth & Arguments**: [Detail views using typed navigation routes implementing `@Serializable`]

## 4. Non-Obvious Logic & Lifecycle Nuances
- [Auto-lock timer when app transitions to background]
- [Local notification scheduling upon reaching monthly spending limit]

## 5. Unsupported Elements & Fallback Strategy
- [Document any closed-source SDKs, proprietary Apple APIs, or missing direct equivalents]
```

*** ** * ** ***

## 6. Unsupported Element \& Graceful Degradation Protocol

When encountering iOS APIs or third-party dependencies with no direct Android
equivalent (e.g. proprietary Apple frameworks like CloudKit, Handoff, Apple
Sign-In, or closed-source `.xcframework` binaries):

1. **Do NOT Fail Silently**: Never omit functionality without explicit documentation.
2. **Define a Clean Abstraction Boundary**: Create a Kotlin interface in the domain layer that encapsulates the expected capability.
3. **Provide a Typed Stub with Actionable Instructions** :
   \`\`\`kotlin
   /\*\*
   \* STUB(migration): iOS used Apple CloudKit for automatic key-value sync.
   \* Android does not provide a direct 1:1 CloudKit equivalent.
   \*
   \* RECOMMENDED ALTERNATIVES:
   \* 1. Firebase Firestore / Cloud Storage for cross-platform cloud sync.
   \* 2. Google Drive AppData folder for private user backup.
   \*
   \* ACTION REQUIRED: Implement this interface using your selected cloud sync provider.
   \*/
   interface CloudSyncService {
   suspend fun syncUserData(userId: String): Result }

   class CloudSyncServiceStub @Inject constructor() : CloudSyncService {
   override suspend fun syncUserData(userId: String): Result { // TODO(migration): Replace stub with production cloud sync provider. return Result.success(Unit) } } \`\`\`
4. **Log in Walkthrough** : Record the stubbed component in
   `.migration/migration_walkthrough.md` under **Manual Action Items
   Required**.

*** ** * ** ***

## Glossary

| iOS Concept | Android Equivalent | Definition |
|---|---|---|
| **Xcode Project** | **Root Gradle Project** | Root folder containing build scripts and module settings. |
| **Xcode Target** | **Gradle Module** | Discrete buildable unit (`:app`, `:core`, `:feature`). |
| **SwiftUI View** | **@Composable Function** | Declarative UI building block. |
| **ObservableObject** | **ViewModel + StateFlow** | Architecture state holder surviving configuration changes. |
| **NavigationStack** | **NavHost + Compose Navigation** | Backstack manager and destination router. |
| **Core Data / SwiftData** | **Jetpack Room** | Object-relational SQLite abstraction layer. |
| **UserDefaults / @AppStorage** | **Preferences DataStore** | Asynchronous key-value storage. |
| **BGTaskScheduler** | **Jetpack WorkManager** | Deferrable, guaranteed background task scheduler. |
| **AVFoundation (Capture)** | **Jetpack CameraX** | Camera lifecycle-aware abstraction layer. |
| **CoreLocation** | **FusedLocationProviderClient** | High-accuracy, battery-optimized location client. |