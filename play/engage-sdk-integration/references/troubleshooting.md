This guide helps resolve common compilation, import, and dependency errors
encountered during the Play Engage SDK integration.

## 1. Verify compilation with full build tasks

When verifying your Play Engage SDK integration, run a full compilation task
rather than a Kotlin-only compile shortcut:

- **Use full compilation** : Run `./gradlew compileDebugUnitTestSources` or `./gradlew assembleDebug` (plus the project's relevant test tasks).
- **Don't use compilation shortcuts** : Never use commands like `./gradlew :app:compileDebugKotlin` or other compile-only tasks. These shortcuts bypass manifest merging and resource packaging, which hides critical errors like namespace conflicts or XML syntax errors.

## 2. Resolve missing imports

If you're upgrading the SDK version, for example, to 1.6.0, or if you get `Unresolved reference` errors during the initial compilation, make sure you've added the correct import statements:

| Class name | Package or import statement | Note |
|---|---|---|
| `ImageTheme` | `import com.google.android.engage.common.datamodel.ImageTheme` |   |
| `PlatformSpecificUri` | `import com.google.android.engage.common.datamodel.PlatformSpecificUri` |   |
| `DisplayTimeWindow` | `import com.google.android.engage.common.datamodel.DisplayTimeWindow` |   |
| `RatingSystem` | `import com.google.android.engage.video.datamodel.RatingSystem` | Note: It's under `video.datamodel`, not `common.datamodel`. |

## 3. Resolve namespace conflicts

You might receive an error message like this one:

    Namespace 'com.google.android.engage' is used in multiple modules and/or libraries: com.google.android.engage:engage-core:1.6.0, com.google.android.engage:engage-tv:1.1.0

This conflict occurs because both packages declare the same namespace.
\* **Resolution** : Remove the explicit `engage-core` dependency from your `build.gradle` or `build.gradle.kts` and keep *only* `engage-tv`. The `engage-tv` library transitively pulls in the compatible version of `engage-core` automatically.