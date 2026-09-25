> [!WARNING]
> **Experimental:** Compose Preview Screenshot Testing with AGP test suites is in active development. APIs and DSL syntax are subject to change. Report feedback and issues through the [issue tracker](https://issuetracker.google.com/issues/new?component=192708&template=840533).

Starting with Android Gradle Plugin (AGP) 9.5.0-alpha03 and Compose Preview
Screenshot Testing engine `0.0.1-alpha16`, screenshot testing is integrated with
AGP's native [test suites](https://developer.android.com/reference/tools/gradle-api/9.5/com/android/build/api/dsl/AgpTestSuite) framework.

This approach replaces the standalone screenshot plugin
(`com.android.compose.screenshot`). We recommend adopting AGP test suites for
the following reasons:

- **Native Gradle task lifecycle**: Screenshot tests integrate directly into standard Gradle and AGP testing lifecycles, improving task isolation and test execution reliability.
- **Multi-variant and custom suite support** : You can create multiple distinct screenshot test suites (such as `screenshotTest`, `uiTests`, or `smokeTests`) within a single module and target specific build variants (such as `demoDebug` or `release`), rather than being restricted to a single pre-configured source set.
- **Enhanced build performance and isolation**: AGP test suites use built-in artifact transforms (such as Layoutlib runtime extraction) and isolated classloading, with full support for Gradle Configuration Caching and Project Isolation.


## Android skills

[View on GitHub](https://github.com/android/skills/blob/main/testing/testing-setup)

### Create screenshot tests

Use the `testing-setup` [Android skill](https://developer.android.com/tools/agents/android-skills) to develop a testing strategy and create screenshot tests. To install the skill from the [Android CLI](https://developer.android.com/tools/agents/android-cli), run:

    android skills add testing-setup

<br />

## Requirements

To use Compose Screenshot Testing with test suites, ensure your environment
meets the following requirements:

- Android Studio Rabbit 1 Canary 4 or higher.
- Android Gradle Plugin (AGP) version 9.5.0-alpha03 or higher.
- Compose Screenshot Engine version 0.0.1-alpha16 or higher.
- JDK version 17 or higher.
- Compose enabled for your project. We recommend enabling Compose using the [Compose Compiler Gradle plugin](https://developer.android.com/develop/ui/compose/compiler).

## Setup and configuration

To configure Compose screenshot testing with test suites, complete the
following steps:

### 1. Enable experimental flags

In your project's root `gradle.properties` file, enable screenshot testing and
test suite support:

    android.experimental.enableScreenshotTest=true
    android.experimental.testSuiteSupport=true

### 2. Configure the test suite in the `build.gradle.kts` file

In your module's `build.gradle.kts` file, define a screenshot test suite within
the `testOptions` block:

    android {
        testOptions {
            screenshotTests.create("screenshotTest") { // suiteName can be customized (for example, "uiTests")
                engineVersion = "0.0.1-alpha16"
                targetVariants.add("demoDebug") // Add specific variants to test

                dependencies {
                    implementation(libs.androidx.compose.ui.tooling)
                    implementation("com.android.tools.screenshot:screenshot-validation-api:0.0.1-alpha16")
                }
            }
        }
    }

### 3. Create the test source set

Create a dedicated source set directory matching your suite name:

`{module}/src/{suiteName}/kotlin/`

For example, for a suite named `screenshotTest`:

`feature/foryou/impl/src/screenshotTest/kotlin/com/example/app/ForYouScreenTest.kt`

### 4. Define composable preview tests

Annotate composables with `@PreviewTest` and standard `@Preview` or
multi-preview annotations:

    package com.example.app

    import androidx.compose.runtime.Composable
    import androidx.compose.ui.tooling.preview.Preview
    import com.android.tools.screenshot.PreviewTest
    import com.example.app.ui.theme.AppTheme

    @PreviewTest
    @Preview(showBackground = true)
    @Composable
    fun ForYouScreenPreview() {
        AppTheme {
            ForYouScreen(isSyncing = false)
        }
    }

## Run screenshot tests

AGP test suites generate dedicated Gradle tasks based on your suite name,
target, and variants.

### 1. Generate or update reference images

Render composable previews and store the golden baseline reference images:

- **Linux and macOS** : `./gradlew update{SuiteName}{Target}{Variant}TestSuite` (for example, `./gradlew updateScreenshotTestDefaultDemoDebugTestSuite`)
- **Windows** : `gradlew updateScreenshotTestDefaultDemoDebugTestSuite`

Reference images are generated and saved at:

`{module}/src/{suiteName}{Target}{Variant}/reference/`

### 2. Verify and run tests

Render fresh screenshots and compare them against reference images:

- **Linux and macOS** : `./gradlew test{SuiteName}{Target}{Variant}TestSuite` (for example, `./gradlew testScreenshotTestDefaultDemoDebugTestSuite`)
- **Windows** : `gradlew testScreenshotTestDefaultDemoDebugTestSuite`

## Inspect test reports

If differences are detected or tests fail, AGP generates an HTML test report.

- **Report location** : `{module}/build/reports/tests/{taskName}/index.html` (for example, `app/build/reports/tests/testScreenshotTestDefaultDemoDebugTestSuite/index.html`)

The updated report includes:

- **Header metadata card**: Displays test name, preview method, variant, suite, and status badge.
- **Error categorization** : Clearly flags `Reference Image Missing`, `Image Size Mismatch`, or `Pixel Mismatch` with copyable stack traces.
- **Dynamic visual diff**: Highlights subtle modifications with lower intensity and major changes with high-contrast emphasis to prevent nested element swallowing.

## Migrate from the legacy standalone plugin

To migrate from the legacy standalone screenshot plugin to AGP test suites,
update your Gradle configuration and task commands.

### Build configuration DSL comparison

#### Legacy standalone plugin (deprecated)

    // In build.gradle.kts
    plugins {
        alias(libs.plugins.screenshot)
    }

    dependencies {
        screenshotTestImplementation(libs.androidx.compose.ui.tooling)
        screenshotTestImplementation("com.android.tools.screenshot:screenshot-validation-api:0.0.1-alpha16")
    }

#### AGP test suites (recommended)

    // In build.gradle.kts
    android {
        testOptions {
            screenshotTests.create("screenshotTest") {
                engineVersion = "0.0.1-alpha16"
                targetVariants.add("demoDebug")

                dependencies {
                    implementation(libs.androidx.compose.ui.tooling)
                    implementation("com.android.tools.screenshot:screenshot-validation-api:0.0.1-alpha16")
                }
            }
        }
    }

### Tasks and paths mapping

| Concept | Legacy setup (deprecated) | AGP test suites (recommended) |
|---|---|---|
| **Update task** | `./gradlew updateDebugScreenshotTest` | `./gradlew update{SuiteName}{Target}{Variant}TestSuite` |
| **Test task** | `./gradlew validateDebugScreenshotTest` | `./gradlew test{SuiteName}{Target}{Variant}TestSuite` |
| **Reference path** | `src/screenshotTestDebug/reference` | `src/{suiteName}{Target}{Variant}/reference` |
| **Report path** | `build/reports/screenshotTest/debug/` | `build/reports/tests/{taskName}/` |