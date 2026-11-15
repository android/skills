> [!IMPORTANT]
> **Important:** **ORCHESTRATION GATEWAY PREREQUISITE** : This document is a supplemental reference guide for [`../SKILL.md`](https://developer.android.com/agents/skills/migration/ios-to-android/skill). You **MUST** read and follow the primary orchestration protocol in `../SKILL.md` before applying any patterns from this guide. `SKILL.md` enforces mandatory incremental compilation gates (`./gradlew compileDebugKotlin`), vertical feature slicing, and strict anti-premature-exit execution discipline.

This skill guides the setup of Dagger-Hilt for Dependency Injection in an
Android project, specifically using **Gradle Kotlin DSL (`gradle.kts`)** ,
**Version Catalogs (`libs.versions.toml`)** , and **KSP (Kotlin Symbol
Processing)**.

Setting up Hilt in `gradle.kts` can be error-prone. Using KSP is the modern best
practice as it is faster than KAPT.

> [!IMPORTANT]
> **Important:** **Knowledge Base Lookup** : Before applying this skill, you **MUST** check the Knowledge Base (KB) for repository-specific dependency management patterns and Hilt usage guidelines. KIs may contain updated versions or specific conventions for the target project.

## Prerequisites

- The project must be using Gradle Version Catalogs (`libs.versions.toml`).
- The project must be using Kotlin DSL (`.gradle.kts` files).

## Step 1: Update `libs.versions.toml`

Add the Hilt and KSP versions, libraries, and plugins to your
`gradle/libs.versions.toml` file.

> [!NOTE]
> **Note:** **Check Existing KSP**: Before adding KSP, check if the project already uses it. If it does, use the existing version and ensure it matches the project's Kotlin version.

```toml
[versions]
# Note: These are example versions. Verify against project needs or use latest stable.
hilt = "2.51.1"
ksp = "2.0.0-1.0.21" # MUST match your Kotlin version! (e.g., 2.0.0 -> 2.0.0-1.0.21)
androidxHilt = "1.2.0"

[libraries]
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "androidxHilt" }

[plugins]
hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

## Step 2: Update Project-level `build.gradle.kts`

Register the Hilt and KSP plugins in the root `build.gradle.kts` file.

```kotlin
// Root build.gradle.kts
plugins {
    // Note that the Android plugin MUST come first
    // ...

    // Then hilt
    alias(libs.plugins.hilt.android) apply false

    // Then other plugins (e.g. KSP).
    alias(libs.plugins.ksp) apply false
}
```

## Step 3: Update App-level `build.gradle.kts`

Apply the plugins and add the dependencies in your app module's
`build.gradle.kts` (usually `app/build.gradle.kts`).

```kotlin
// app/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose) // Essential for hiltViewModel() in Compose
    ksp(libs.hilt.compiler)
}
```

## Step 3.5: Multi-module Setup (Optional)

If your project has multiple modules (e.g., `feature`, `data`, `core`), follow
these rules:

- **Root** : Ensure plugins are declared with `apply false` in the root `build.gradle.kts` (Step 2).
- **Feature/Library Modules** : In each module that uses Hilt annotations (like `@Inject` or `@Provides`):
  - Apply the KSP plugin: `alias(libs.plugins.ksp)`
  - If it's an Android module, apply Hilt: `alias(libs.plugins.hilt.android)`
  - Add the compiler: `ksp(libs.hilt.compiler)`
- **Avoid Duplicate Applications** : Only the main app module should have the `@HiltAndroidApp` class.

## Step 4: Perform Gradle Sync

After modifying the build files, you **MUST** perform a Gradle sync to enable
Hilt dependencies.

## Step 5: Create Application Class

Hilt requires a custom `Application` class annotated with `@HiltAndroidApp`.

```kotlin
package com.example.myapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApp : Application()
```

## Step 6: Update `AndroidManifest.xml`

Register your custom `Application` class in the `AndroidManifest.xml`.

```xml
<application
    android:name=".MyApp"
    ...>
</application>
```

## Step 7: Annotate Hosting Activity

Any Activity that hosts Hilt-injected Composables or Fragments must be annotated
with `@AndroidEntryPoint`.

```kotlin
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.ComponentActivity

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // ...
}
```

## Step 8: Basic Usage (iOS Analogy)

After setup, use these annotations to inject dependencies. In iOS, you might
have used Resolver or Swinject; in Android, Hilt automates this.

### ViewModel Injection

```kotlin
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: MyRepository
) : ViewModel()
```

### Usage in Compose

```kotlin
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun MyScreen(viewModel: MyViewModel = hiltViewModel()) {
    // ...
}
```

## Antipatterns

### DO

- **DO** use `alias(libs.plugins.hilt.android)` and `alias(libs.plugins.ksp)` in `build.gradle.kts`.
- **DO** use `ksp` for `hilt-compiler` dependency.
- **DO** ensure KSP version matches your Kotlin version exactly.
- **DO** remove the `kotlin-kapt` plugin if it is no longer needed to avoid build overhead.
- **DO** check Knowledge Base for project-specific patterns.

### DON'T

- **DON'T** use KAPT if you can use KSP.
- **DON'T** hardcode versions in `build.gradle.kts`.
- **DON'T** forget to add `@AndroidEntryPoint` to your Activity if it hosts Hilt-injected Composables.