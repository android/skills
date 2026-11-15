> [!IMPORTANT]
> **Important:** **ORCHESTRATION GATEWAY PREREQUISITE** : This document is a supplemental reference guide for [`../SKILL.md`](https://developer.android.com/agents/skills/migration/ios-to-android/skill). You **MUST** read and follow the primary orchestration protocol in `../SKILL.md` before applying any patterns from this guide. `SKILL.md` enforces mandatory incremental compilation gates (`./gradlew compileDebugKotlin`), vertical feature slicing, and strict anti-premature-exit execution discipline.

This skill provides instructions and patterns for migrating automated screenshot
functionality built with Fastlane Snapshot on iOS to the Android platform (e.g.,
using Fastlane Screengrab or native Android testing tools).

## Inputs and Outputs

- **Input:** An existing iOS Fastlane Snapshot setup (Snapfile, Fastfile, and UI Test target).
- **Output:** An equivalent Android setup for automated screenshots and a security configuration for sensitive views.

## Prerequisites

Before using this skill, you **Must** ensure the following prerequisites are
met:

- **Source Context:** Access to the existing iOS Fastlane configuration is **Mandatory**.
- **Android Environment:** The Android SDK command-line tools (`adb`, build-tools) **Must** be installed and accessible via PATH.
- **Fastlane:** Fastlane **Must** be installed if using Screengrab for Android.

## Limitations

This skill has the following limitations:

- Focuses on the **migration** and mapping of concepts, not a full standalone tutorial for Android screenshots.
- Assumes basic familiarity with both platforms.

## Glossary

| Term | Definition |
|---|---|
| `Snapshot` | Fastlane tool for automated iOS screenshots. |
| `Screengrab` | Fastlane tool for automated Android screenshots. |
| `Snapfile` | Configuration file for iOS Snapshot. |
| `Screengrabfile` | Configuration file for Android Screengrab. |
| `FLAG_SECURE` | Android window flag used to prevent screenshots and screen recordings. |
| `HIDE_OVERLAY_WINDOWS` | Android permission to prevent application overlays from being drawn over the app. |

## Setup \& Dependencies

When migrating, you **Must** establish the Android environment equivalent to the
iOS Ruby gem isolation. You **Must** add the necessary UI testing dependencies
to your app's `build.gradle` (or `build.gradle.kts`) file.

### Example `build.gradle` Dependencies

```groovy
dependencies {
   // Provides core APIs for Android testing, including ApplicationProvider.
   androidTestImplementation 'androidx.test:core:1.5.0'


   // The instrumentation-based test runner for executing tests on-device.
   androidTestImplementation 'androidx.test:runner:1.5.2'

   // Provides JUnit Rules for managing test setup, e.g., ActivityScenarioRule.
   androidTestImplementation 'androidx.test:rules:1.5.0'


   // Core Espresso library for UI interactions and assertions.
   androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'

   // Resource counting library to synchronize background tasks with Espresso.
   androidTestImplementation 'androidx.test.espresso:espresso-idling-resource:3.5.1'

   // UI Automator for cross-app interactions and system-level UI testing.
   androidTestImplementation 'androidx.test.uiautomator:uiautomator:2.2.0'
}
```

To initialize Fastlane Screengrab, you **Must** run `fastlane screengrab init`
in the Android project directory to generate the initial `Screengrabfile`.

## Instructions: Mapping Concepts

### 1. Automated Target Configuration

Map the iOS build settings to Android equivalents in `build.gradle` and manifest
files.

| iOS Concept | iOS Setting | Android Equivalent | Migration Action |
|---|---|---|---|
| Target Name | `PRODUCT_NAME` | `applicationId` / `testApplicationId` | Ensure unique test application ID in `build.gradle`. |
| Identifier | `PRODUCT_BUNDLE_IDENTIFIER` | Package Name | Align package names according to project conventions. |
| Host App | `TEST_TARGET_NAME` | `targetSdkVersion` / Instrumentation | Configure the test instrumentation runner correctly. |
| Build Number | `CURRENT_PROJECT_VERSION` | `versionCode` | Map iOS build number to Android version code in `build.gradle`. |
| App Version | `MARKETING_VERSION` | `versionName` | Map iOS marketing version to Android version name. |

### 2. Configuration File Mapping: Snapfile to Screengrabfile

Explicitly map common iOS Snapfile parameters to their Android Screengrabfile
equivalents.

| iOS Snapfile Parameter | Android Screengrabfile Parameter | Migration Action |
|---|---|---|
| `devices([ ... ])` | `device_type("phone")` | Map device classes to a specific `device_type` from the exhaustive list: "phone", "seven_inch_tablet", "ten_inch_tablet", "tv", or "wear". |
| `languages([ ... ])` | `locales([ ... ])` | Copy the array of locale strings directly. |
| `scheme("Staging")` | (Not applicable) | Must be handled via Gradle tasks (e.g., `connectedStagingAndroidTest`), not in the `Screengrabfile`. |
| `output_directory("./screenshots")` | `output_directory("./fastlane/screenshots")` | Ensure the path is updated to reflect the Android project's directory structure. |
| `clear_previous_screenshots(true)` | `clear_previous_screenshots(true)` | Parameter name and function are identical. |

### 3. Scheme Architecture to Build Variants

iOS uses shared schemes for testing. Android uses Build Variants (combinations
of build types and product flavors).

- **Branching Logic:**
- **IF** the source iOS project uses multiple schemes for different environments (e.g., Staging, Production), **THEN** you **Must** define corresponding `productFlavors` in your `build.gradle` file.
- **ELSE** , you can proceed using only the default `buildTypes` (debug/release).

### Example `productFlavors` Configuration

```groovy
android {
   ...
   flavorDimensions "environment"

   productFlavors {
       staging {
           dimension "environment"
           applicationIdSuffix ".staging"
           versionNameSuffix "-staging"
       }
       production {
           dimension "environment"
       }
   }
}
```

- **Test Execution:** You **Must** ensure the Android UI tests are run against the correct build variant using `./gradlew connectedStagingAndroidTest` or `./gradlew connectedProductionAndroidTest`.

### 4. UI Test Concurrency \& Isolation

Map iOS `@MainActor` and async tests to Android testing rules.

- **Thread Safety:** While iOS uses `@MainActor` to ensure UI interaction on the main thread, Android Espresso automatically synchronizes with the main thread.
- **Idling Resources:** If your app performs asynchronous operations (like network calls) that Espresso does not automatically track, you **Must** implement `IdlingResource` to prevent flaky tests and race conditions.

#### Code Example for `IdlingResource` and Test Rule

Implement a singleton to manage the counting resource:

```kotlin
import androidx.test.espresso.idling.CountingIdlingResource


object EspressoIdlingResource {
   private const val RESOURCE = "GLOBAL"
   @JvmField val countingIdlingResource = CountingIdlingResource(RESOURCE)


   fun increment() {
       countingIdlingResource.increment()
   }


   fun decrement() {
       if (!countingIdlingResource.isIdleNow) {
           countingIdlingResource.decrement()
       }
   }
}
```

To ensure the IdlingResource is registered before each test and unregistered
after, you Must use a custom JUnit TestRule:
\`\`\`kotlin
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class IdlingResourceRule(private val idlingResource: IdlingResource) : TestRule {
override fun apply(base: Statement, description: Description): Statement {
return object : Statement() {
override fun evaluate() {
IdlingRegistry.getInstance().register(idlingResource)
try {
base.evaluate()
} finally {
IdlingRegistry.getInstance().unregister(idlingResource)
}
}
}
}
}
\`\`\`

Then, apply the rule in your test class:

```kotlin
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class MyScreenshotTest {


   @get:Rule
   val idlingRule = IdlingResourceRule(EspressoIdlingResource.countingIdlingResource)


   @Test
   fun testTakeScreenshot() {
       // Your test steps here
       // Screengrab.screenshot("main_screen")
   }
}
```

- **Compose Testing:** If using Jetpack Compose, you **Must** use `createComposeRule()` or `createAndroidComposeRule()` to manage synchronization.

#### Code Example for Jetpack Compose

When testing Jetpack Compose interfaces, you **Must** use
`createAndroidComposeRule` to ensure proper synchronization with the Compose
runtime.

```kotlin
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.locale.LocaleTestRule


class ComposeScreenshotTest {


   @get:Rule
   val composeTestRule = createAndroidComposeRule<MainActivity>()


   @get:Rule
   val localeTestRule = LocaleTestRule()


   @Test
   fun testCaptureComposeScreen() {
       // Interact with Compose elements
       composeTestRule.onNodeWithText("Welcome").assertExists()

       // Capture screenshot using Screengrab
       Screengrab.screenshot("compose_main_screen")
   }
}
```

### 5. Security \& Fraud Prevention (Overlay \& Screenshot Protection)

When handling sensitive views (e.g., login, payments), both platforms offer
mechanisms to prevent data leakage, but they differ in approach.

#### Android (Target)

- **Screenshot Prevention:** You **Must** use `WindowManager.LayoutParams.FLAG_SECURE` in your Activity to prevent screenshots and screen recordings. This also prevents content from appearing in background screenshots and non-secure displays (e.g., casting).
- **Overlay Protection (Android 12+):** **IF** your app is vulnerable to overlay attacks (e.g., handles financial transactions or sensitive user input), **THEN** you **Must** add the `<uses-permission android:name="android.permission.HIDE_OVERLAY_WINDOWS" />` permission to your `AndroidManifest.xml`.
- **Reference:** Follow the official Android guidelines: [Secure in-app
  communication](https://developer.android.com/security/fraud-prevention/activities).

#### Code Example for `FLAG_SECURE`

```kotlin
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity


class SecureActivity : AppCompatActivity() {
   override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)

       // Prevent screenshots and screen recordings for this secure view
       window.setFlags(
           WindowManager.LayoutParams.FLAG_SECURE,
           WindowManager.LayoutParams.FLAG_SECURE
       )

       setContentView(R.layout.activity_secure)
   }
}
```

#### iOS (Source Comparison)

- **Automatic Hiding:** iOS automatically hides content in fields marked with `isSecureTextEntry` (e.g., passwords) during screenshots and recordings.
- **Detection:** iOS does not provide a direct "disable screenshot" API. The standard practice is to observe `UIScreen.capturedDidChangeNotification` to detect when screen recording starts and programmatically blur or hide sensitive UI elements.

### 6. Maintenance \& Scoped Debugging

- **iOS vs Android Logs:** iOS logs are in `~/Library/Logs/snapshot/`. For Android, you **Must** use `logcat` and check the output of `screengrab` in the terminal.
- **Scoped Runs:** Similar to iOS, you **Must** scope your runs on Android by targeting specific devices or languages in the `Screengrabfile` to minimize turn-around time.

### 7. Advanced Android Testing Requirements

While Fastlane Screengrab provides a direct migration path, you **Must**
implement the following Android-native best practices for a robust testing
strategy.

#### Native Screenshot Alternatives

- **UI Automator APIs:** For component-level testing or cross-app interactions, you can use UI Automator to take screenshots of the active window (`UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(file)`) or individual elements.
- **ADB Screencap:** For quick debugging or custom scripting outside the test suite, use `adb shell screencap -p /sdcard/screenshot.png` to capture the current screen.

#### Screenshot Testing Strategy

- **Minimizing Test Combinations:** To keep the test suite manageable, you **Must** avoid testing every possible UI state combination. Select representative combinations of themes, locales, and screen sizes that provide unique feedback.
- **Handling Platform Rendering Differences:** UI rendering (especially text and shadows) can differ between developer machines and CI servers. You **Must** configure your screenshot comparison tools with a reasonable tolerance threshold to prevent false positives caused by anti-aliasing or rendering engine differences.

#### Reference Image Management

- **Git LFS:** Screenshot reference images are large binary files. To avoid bloating your Git repository, you **Must** use Git LFS (Large File Storage) or an external cloud storage solution to manage "golden" images.

#### Consistent Environment

- **Cold Boot Emulators:** To ensure consistent results, you **Must** configure your Android Virtual Devices (AVDs) to use "Cold Boot" instead of "Quick Boot". Quick Boot can carry over state from previous runs, leading to flaky tests.

#### Enhanced Debugging

- **Failure Screenshots:** When using UI Automator, capture screenshots on failure and export them to a dedicated test-results output directory (e.g., `build/outputs/connected_android_test_additional_output`) for CI/CD inspection.

## Quality Control: Antipatterns

### DO

- **DO** map iOS schemes to Android build variants explicitly using `productFlavors`.
- **DO** use `FLAG_SECURE` for sensitive screens on Android to prevent data leakage.
- **DO** filter logcat by the test process name or specific tags like "Screengrab" to isolate relevant errors during test execution.
- **DO** implement `IdlingResource` for async operations in Espresso tests.
- **DO** use Git LFS for managing large reference images to prevent repository bloat.
- **DO** configure AVDs for Cold Boot to ensure a consistent test environment.

### DON'T

- **DON'T** assume iOS `@MainActor` constraints apply directly to Android Espresso (Espresso handles sync automatically).
- **DON'T** ignore Android's `FLAG_SECURE` capability when handling banking or sensitive user data views.
- **DON'T** use soft or optional language like "consider" when defining security or architecture constraints.

### NEVER

- **NEVER** proceed with migration without a clear mapping of iOS test cases to Android test cases.
- **NEVER** run snapshot tests without setting `FASTLANE_SKIP_CONFIRMATION=1` on CI environments. Failure to do so will cause the automation to halt and await interactive user input, resulting in a timeout or build failure.

## Examples

### Example Screengrabfile (Android Equivalent of Snapfile)

```ruby
# fastlane/Screengrabfile


# The package name of your Android application
app_package_name("com.yourcompany.app")


# The path to the main application APK. This path MUST match the build variant
# you intend to test (e.g., staging, production).
# Example for 'staging' flavor, 'debug' build type:
app_apk_path("./app/build/outputs/apk/staging/debug/app-staging-debug.apk")


# The path to the test APK containing the UI tests. This path MUST also match
# the build variant being tested.
tests_apk_path("./app/build/outputs/apk/androidTest/staging/debug/app-staging-debug-androidTest.apk")


# A list of locales for which to capture screenshots
locales(["en-US", "fr-FR", "de-DE"])


# Output directory where screenshots will be saved
output_directory("./fastlane/screenshots")


# Clear previous screenshots before running new tests
clear_previous_screenshots(true)


# Advanced configuration


# If true, attempts to run adb root before taking screenshots.
# This might be necessary for certain devices or to perform actions requiring elevated permissions.
use_adb_root(true)


# The test runner used to execute the tests
test_instrumentation_runner('androidx.test.runner.AndroidJUnitRunner')
```

### Example Fastfile Lane (Android)

```ruby
# fastlane/Fastfile


lane :take_screenshots_for_variant do |options|
 # Example: fastlane take_screenshots_for_variant variant:staging
 variant = options[:variant] || "staging"
  # Ensure the test APK is built for the specific variant
 gradle(task: "assemble#{variant.capitalize}AndroidTest")


 screengrab(
   app_apk_path: "./app/build/outputs/apk/#{variant}/debug/app-#{variant}-debug.apk",
   tests_apk_path: "./app/build/outputs/apk/androidTest/#{variant}/debug/app-#{variant}-debug-androidTest.apk",
   locales: ["en-US"],
   device_type: "phone"
 )
end
```