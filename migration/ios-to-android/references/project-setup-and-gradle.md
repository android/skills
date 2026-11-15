> [!IMPORTANT]
> **Important:** **ORCHESTRATION GATEWAY PREREQUISITE** : This document is a supplemental reference guide for [`../SKILL.md`](https://developer.android.com/agents/skills/migration/ios-to-android/skill). You **MUST** read and follow the primary orchestration protocol in `../SKILL.md` before applying any patterns from this guide. `SKILL.md` enforces mandatory incremental compilation gates (`./gradlew compileDebugKotlin`), vertical feature slicing, and strict anti-premature-exit execution discipline.

This skill guides the initial setup of an Android Jetpack Compose project,
providing a direct comparison to Xcode project creation. You **Must** use this
skill when starting a new Android port of an existing iOS application or setting
up a greenfield project intended to mirror an iOS app's structure.

**Official Reference Links:**

- [Create an Android project](https://developer.android.com/studio/projects/create-project)
- [Migrate your build to version catalogs \| Android Developers](https://developer.android.com/build/migrate-to-catalogs)
- [Configure build variants \| Android Developers](https://developer.android.com/build/build-variants)
- [Work with multiple manifest files \| Android Developers](https://developer.android.com/build/manage-manifests)

## Prerequisites

Before initiating the project setup, you **Must** ensure the following
prerequisites are met:

- **Software Requirements:** Android Gradle Plugin (AGP) 8.3+, Kotlin 1.9+, and Jetpack Compose BOM 2023.08.00+ are **Mandatory** . You can use any modern IDE (VS Code, Cursor, IntelliJ IDEA Community, Android Studio) or the command line (`./gradlew`).
- **JDK:** Java Development Kit (JDK 17 or higher) must be configured.
- **Source Context:** Access to the iOS project structure (including the Xcode project file, Podfile, and Package.swift) is **Mandatory** to understand the source structure being mapped.

## Limitations

This skill has the following limitations:

- **Multi-Module Projects:** This skill focuses on a single-module application setup. Complex multi-module architecture setup is covered in `architecture-and-viewmodels.md`.
- **CI/CD Pipelines:** Setting up CI/CD pipelines (e.g., GitHub Actions, Bitrise) is outside the scope of this skill.

## Glossary

| iOS Concept | Android Equivalent | Definition |
|---|---|---|
| **Xcode Project (.xcodeproj)** | **Gradle Project** | The container for source code, resources, and build configurations. |
| **Bundle Identifier** | **Package Name / Application ID** | The unique identifier for the application on the store. |
| **Info.plist** | **AndroidManifest.xml** | The file containing essential metadata about the application. |
| **Build Phases** | **Gradle Tasks** | The steps executed during the build process. |
| **CocoaPods / SPM** | **Gradle Dependencies** | The package management systems used to include external libraries. |
| **Build Configurations (Debug/Release)** | **Build Types \& Product Flavors** | Mechanisms for creating different versions of the app (e.g., staging, production). |

## Info.plist to AndroidManifest Mapping

You **Must** map common iOS `Info.plist` keys to the appropriate declarations in
the `AndroidManifest.xml` or `build.gradle.kts`.

| iOS Key | Android Equivalent | Location |
|---|---|---|
| `CFBundleIdentifier` | `applicationId` | `build.gradle.kts` |
| `CFBundleShortVersionString` | `versionName` | `build.gradle.kts` |
| `CFBundleVersion` | `versionCode` | `build.gradle.kts` |
| `NSCameraUsageDescription` | `android.permission.CAMERA` | `AndroidManifest.xml` |
| `NSLocationWhenInUseUsageDescription` | `android.permission.ACCESS_COARSE_LOCATION` | `AndroidManifest.xml` |
| `NSPhotoLibraryUsageDescription` | `android.permission.READ_MEDIA_IMAGES` | `AndroidManifest.xml` |
| `UISupportedInterfaceOrientations` | `android:screenOrientation` | `AndroidManifest.xml` |
| `UIBackgroundModes` | `ForegroundService` / `WorkManager` | `AndroidManifest.xml` |
| `LSApplicationQueriesSchemes` | `<queries>` | `AndroidManifest.xml` |
| `UIRequiredDeviceCapabilities` | `<uses-feature>` | `AndroidManifest.xml` |

## Framework Mapping: iOS to Android

When setting up the project, you **Must** plan the replacement of core iOS
frameworks with their Android equivalents.

| iOS Framework | Android Equivalent | Gradle Dependency | Notes |
|---|---|---|---|
| **MapKit** | **Google Maps SDK** | `com.google.android.gms:play-services-maps` | Requires API key setup. |
| **CoreData** | **Room** | `androidx.room:room-runtime` | SQLite-based ORM. |
| **CoreLocation** | **Play Services Location** | `com.google.android.gms:play-services-location` | Use FusedLocationProviderClient. |
| **Combine** | **Coroutines \& Flow** | `org.jetbrains.kotlinx:kotlinx-coroutines-android` | Reactive model. |
| **UIKit** | **Jetpack Compose** | `androidx.compose.ui:ui` | Declarative UI framework. |

## Setup \& Dependencies: Version Catalogs

Modern Android projects **Must** use Gradle Version Catalogs
(`libs.versions.toml`) for dependency management. This is equivalent to managing
dependencies in a centralized `Podfile` or `Package.swift`.

**Example: libs.versions.toml**
\`\`\`toml
\[versions\]
agp = "8.3.0"
kotlin = "1.9.22"
coreKtx = "1.12.0"
lifecycleRuntimeKtx = "2.8.0"
activityCompose = "1.8.2"
composeBom = "2024.02.00"

\[libraries\]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }

\[plugins\]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
\`\`\`

## Instructions: Project Creation Workflow

### Step 1: Structuring the Android Project \& Boilerplate Architecture from iOS Source

When porting an existing iOS application, you **Must** structure the new Android
Gradle project and its baseline architecture directly from the patterns read out
of the iOS source tree (`.xcodeproj`, `.xcworkspace`, `Info.plist`, and source
directories).

#### 1. Root \& Build Layout (from iOS Project Metadata)

- **`settings.gradle.kts`** : Set `rootProject.name` matching the iOS App Name and include `:app` (plus any modular feature targets read from Xcode sub-projects).
- **`gradle/libs.versions.toml`** : Define plugins and dependencies mapped from `Podfile` / `Package.swift`.
- **`app/build.gradle.kts`** : Configure `applicationId` (mapped from iOS Bundle ID), `minSdk` (mapped from iOS Deployment Target), and build types/flavors (mapped from Xcode build configurations).

#### 2. Boilerplate Package \& Architectural Structure (`app/src/main/java/<package_name>/`)

Read the iOS source directory organization and map it to standard Android
Jetpack architecture:

- **`ui/theme/` (`Theme.kt`, `Color.kt`, `Type.kt`)** : Read iOS color catalogs (`Asset Catalog`) and typography rules, creating Compose `MaterialTheme`, `ColorScheme`, and `Typography` definitions.
- **`ui/navigation/` (`AppNavigation.kt`)** : Read iOS routing (`NavigationStack`, `.storyboard`, or coordinator flow) and generate the top-level Compose `NavHost` and route hierarchy.
- **`ui/<feature>/`** : For each functional module in the iOS source (e.g., `Login/`, `Home/`, `Settings/`), create a matching feature package containing composable screens (`*Screen.kt`) and their associated state holders (`*ViewModel.kt`).
- **`data/` (`repository/`, `model/`, `remote/`)** : Map iOS `Codable` structs, network clients (`URLSession`), and persistence layers (`CoreData`/`UserDefaults`) to Kotlin data classes, repository interfaces, and data source implementations.
- **`app/src/main/res/`** : Map `.xcassets` to `res/drawable/` and `res/mipmap-anydpi-v26/`, and `.strings` to `res/values/strings.xml`.

### Step 2: Mapping Environments (Build Types \& Flavors)

iOS apps often use different targets or configurations (Debug, Staging, Release)
for different environments. On Android, you **Must** use `buildTypes` and
`productFlavors`.

- **iOS:** `Debug`, `Staging`, `Release` schemes.
- **Android:** `debug`, `release` build types + custom flavors.

**Example: Configuring Build Variants**

- **Input:** Environment names from iOS.
- **Output:** Configured Gradle build variants.
- **Side Effect:** Generates different APKs for different environments.

```kotlin
// app/build.gradle.kts
android {
   buildTypes {
       getByName("debug") {
           applicationIdSuffix = ".debug"
           isDebuggable = true
       }
       getByName("release") {
           isMinifyEnabled = true
           proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
       }
   }


   flavorDimensions.add("environment")
   productFlavors {
       create("staging") {
           dimension = "environment"
           applicationIdSuffix = ".staging"
           versionNameSuffix = "-staging"
           // Example: Different API URL for staging
           buildConfigField("String", "API_URL", "\"https://staging.api.example.com\"")
       }
       create("production") {
           dimension = "environment"
           buildConfigField("String", "API_URL", "\"https://api.example.com\"")
       }
   }
}
```

## Instructions: Mapping Xcode Build Phases to Gradle

You **Must** map custom build scripts or phases from Xcode to custom Gradle
tasks.

- **iOS:** Xcode Build Phases -\> New Run Script Phase.
- **Android:** Register a new Gradle task in `build.gradle.kts`.

**Example: Custom Task**

- **Input:** Task name and action.
- **Output:** Executed action during build.
- **Side Effect:** Modifies files or generates resources.

```kotlin
tasks.register("generateBuildInfo") {
   doLast {
       val buildInfoFile = file("src/main/assets/build_info.txt")
       buildInfoFile.writeText("Build Time: ${java.util.Date()}")
   }
}


// Ensure it runs before the app is compiled
tasks.named("preBuild") {
   dependsOn("generateBuildInfo")
}
```

## Instructions: App Icon Setup

You **Must** map iOS app icons from `Assets.xcassets` to Android `res/mipmap`
directories. Android supports adaptive icons (introduced in API 26), which
consist of a background and a foreground layer.

- **Input:** A 1024x1024px source icon file (e.g., `icon.png`) or separate layers.
- **Output:** `ic_launcher.xml` in `res/mipmap-anydpi-v26/` and multiple `ic_launcher.png` files in the corresponding `res/mipmap-*` directories.
- **Side Effect:** Generates all density-specific bitmaps.

**Logic:**

- **IF** the iOS app has a vector or simple icon, **THEN** you **Must** define an adaptive icon in `res/mipmap-anydpi-v26/ic_launcher.xml` referencing vector drawables (`@drawable/ic_launcher_foreground` and `@color/ic_launcher_background`).
- **ELSE** (complex bitmap art), generate PNG fallbacks for all densities (`mdpi` through `xxxhdpi`) using a CLI image processing utility (such as ImageMagick or an open-source Android asset generation CLI tool).

**Example: Adaptive Icon XML** (`res/mipmap-anydpi-v26/ic_launcher.xml`)
`xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
<background android:drawable="@color/ic_launcher_background"/>
<foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>`

**Legacy Mipmap Densities**
To support older devices, you **Must** provide legacy bitmaps in the following
sizes:

| Density | Folder | Size (px) |
|---|---|---|
| mdpi | `mipmap-mdpi` | 48 x 48 |
| hdpi | `mipmap-hdpi` | 72 x 72 |
| xhdpi | `mipmap-xhdpi` | 96 x 96 |
| xxhdpi | `mipmap-xxhdpi` | 144 x 144 |
| xxxhdpi | `mipmap-xxxhdpi` | 192 x 192 |

## Mapping Asset Catalogs to Resource Qualifiers

When mapping iOS assets from `Assets.xcassets` to Android, you **Must** use
Android's resource qualifier system to handle different device configurations
(e.g., dark mode, screen size).

| iOS Asset Trait | Android Qualifier | Example Directory |
|---|---|---|
| **Light Mode** | Default | `res/drawable/` |
| **Dark Mode** | `-night` | `res/drawable-night/` |
| **iPad (Tablet)** | `-sw600dp` | `res/layout-sw600dp/` |
| **Right-to-Left (RTL)** | `-ldrtl` | `res/layout-ldrtl/` |
| **High Resolution** | `-xxhdpi` | `res/drawable-xxhdpi/` |

## Instructions: Secrets Management

For API keys and secrets, iOS apps often use hardcoded values in code
(antipattern) or specialized build scripts to inject them. On Android, you
**Must** use the `Secrets Gradle Plugin` to read secrets from a
`local.properties` file.

**Example: Secrets Setup**

- **Input:** Secret key defined in `local.properties` as `API_KEY=value`.
- **Output:** Accessible string in code via `BuildConfig.API_KEY`.
- **Side Effect:** Keeps secrets out of version control and prevents accidental exposure.

1. Add the plugin to `libs.versions.toml`:
   `toml
   [plugins]
   secrets = { id = "com.google.android.libraries.mapsplatform.secrets-gradle-plugin", version = "2.0.1" }`

2. Apply the plugin in `app/build.gradle.kts`:
   `kotlin
   plugins {
   alias(libs.plugins.secrets)
   }`

3. Add secret to `local.properties`:
   `properties
   API_KEY=your_secret_key`

4. Access the secret in Kotlin code:
   `kotlin
   val apiKey = BuildConfig.API_KEY`

## Tooling Integration: Standard Actions

To enable automated assessment and migration tracking, this skill defines the
following standard actions. Any automation tool built on this skill **Must**
implement these actions with the specified parameters to ensure benchmark
compliance.

### Verification Commands \& Agnostic Agent Instructions

To verify Version Catalog adoption and build flavor configuration without
proprietary tool runners:

1. **Version Catalog Audit (CLI):** `bash
   # Verify that gradle/libs.versions.toml exists and is referenced in build.gradle.kts
   ls -l gradle/libs.versions.toml
   grep -rn "libs\." app/build.gradle.kts`
2. **Build Flavors Verification (CLI):** `bash
   # Verify configured build flavors and types via Gradle
   ./gradlew tasks --all | grep "assemble"`
3. **Agent Audit Instruction:** *Read `gradle/libs.versions.toml` and `app/build.gradle.kts` to verify that
   all iOS dependencies and target environments have corresponding Version
   Catalog entries and product flavor definitions.*

## Antipatterns

### DO

- **DO** use Version Catalogs for all dependencies.
- **DO** use `Empty Activity` for new Compose projects.
- **DO** map iOS environments to Android Product Flavors.
- **DO** use `Secrets Gradle Plugin` for managing API keys.

### DON'T

- **DON'T** hardcode dependency versions in `build.gradle.kts` files.
- **DON'T** use a minimum SDK below API 24 (Android 7.0). You **Must** set the minimum SDK to API 24 or higher to support adaptive icons and modern resource handling while covering \~95% of active devices.

### NEVER

- **NEVER** commit sensitive API keys or secrets directly into the `build.gradle.kts` or `AndroidManifest.xml`.
- **NEVER** perform blocking network calls or heavy IO during the custom Gradle task execution.