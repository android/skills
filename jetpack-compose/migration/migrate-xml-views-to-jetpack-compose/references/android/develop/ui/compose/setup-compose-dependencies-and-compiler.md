## Set up the Compose Compiler Gradle plugin

For Gradle users, you can use the Compose Compiler Gradle plugin to make setting
up and configuring Compose easier.

> [!NOTE]
> **Note:** The Compose Compiler Gradle Plugin is only available from Kotlin 2.0+. For migration instructions, see ["Jetpack Compose compiler moving to the Kotlin
> repository"](https://android-developers.googleblog.com/2024/04/jetpack-compose-compiler-moving-to-kotlin-repository.html). For an example migration, see the [Compose Samples
> PR](https://github.com/android/compose-samples/pull/1354) in the Compose samples.

### Set up with Gradle version catalogs

The following instructions outline how you can set up the Compose Compiler
Gradle plugin:

1. In your `libs.versions.toml` file, remove any reference to the Compose Compiler.
2. In the `versions` and `plugins` section, add the following new dependency:

    [versions]
    kotlin = "2.3.10"

    [plugins]
    org-jetbrains-kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }

    // Add this line
    compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }

1. In your project's root `build.gradle.kts` file, add the following snippet to the `plugins` section.

    plugins {
       // Existing plugins
       alias(libs.plugins.compose.compiler) apply false
    }

1. In each module that uses Compose, apply the plugin as follows.

    plugins {
       // Existing plugins
       alias(libs.plugins.compose.compiler)
    }

Your app should now build and compile if you are using the default set up. If
you had configured custom options on the Compose compiler, see the following
section.

### Set up the Compose Compiler without Gradle version catalogs

Add the
following plugin to `build.gradle.kts` files associated with modules you use
Compose:

    plugins {
        id("org.jetbrains.kotlin.plugin.compose") version "2.3.10" // this version matches your Kotlin version
    }

You might also need to add this classpath to your top-level project
`build.gradle.kts` file:

    buildscript {
        dependencies {
            classpath("org.jetbrains.kotlin.plugin.compose:org.jetbrains.kotlin.plugin.compose.gradle.plugin:2.3.10")
        }
    }

### Configuration options with the Compose Compiler Gradle Plugin

To configure the Compose compiler using the Gradle plugin, add the
`composeCompiler` block to the module's `build.gradle.kts` file at the top
level.

    android { ... }

    composeCompiler {
        reportsDestination = layout.buildDirectory.dir("compose_compiler")
        stabilityConfigurationFile = rootProject.layout.projectDirectory.file("stability_config.conf")
    }

For the full list of available options, see the [documentation](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-compiler.html#compose-compiler-options-dsl).

## Set up Compose dependencies

Add the following definition to your app's `build.gradle` file:

### Groovy

    android {
        buildFeatures {
            compose true
        }
    }

### Kotlin

    android {
        buildFeatures {
            compose = true
        }
    }

Setting the `compose` flag to `true` inside the Android [`BuildFeatures`](https://developer.android.com/reference/tools/gradle-api/7.0/com/android/build/api/dsl/BuildFeatures)
block enables [Compose functionality](https://developer.android.com/develop/ui/compose/tooling) in Android Studio.

Finally, add the Compose BOM and the subset of Compose library dependencies
you need to your dependencies from the following block:

### Groovy

    dependencies {

        def composeBom = platform('androidx.compose:compose-bom:2026.03.00')
        implementation composeBom
        androidTestImplementation composeBom

        // Choose one of the following:
        // Material Design 3
        implementation 'androidx.compose.material3:material3'
        // or skip Material Design and build directly on top of foundational components
        implementation 'androidx.compose.foundation:foundation'
        // or only import the main APIs for the underlying toolkit systems,
        // such as input and measurement/layout
        implementation 'androidx.compose.ui:ui'

        // Android Studio Preview support
        implementation 'androidx.compose.ui:ui-tooling-preview'
        debugImplementation 'androidx.compose.ui:ui-tooling'

        // UI Tests
        androidTestImplementation 'androidx.compose.ui:ui-test-junit4'
        debugImplementation 'androidx.compose.ui:ui-test-manifest'

        // Optional - Add window size utils
        implementation 'androidx.compose.material3.adaptive:adaptive'

        // Optional - Integration with activities
        implementation 'androidx.activity:activity-compose:1.13.0'
        // Optional - Integration with ViewModels
        implementation 'androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0'
        // Optional - Integration with LiveData
        implementation 'androidx.compose.runtime:runtime-livedata'
        // Optional - Integration with RxJava
        implementation 'androidx.compose.runtime:runtime-rxjava2'

    }

### Kotlin

    dependencies {

        val composeBom = platform("androidx.compose:compose-bom:2026.03.00")
        implementation(composeBom)
        androidTestImplementation(composeBom)

        // Choose one of the following:
        // Material Design 3
        implementation("androidx.compose.material3:material3")
        // or skip Material Design and build directly on top of foundational components
        implementation("androidx.compose.foundation:foundation")
        // or only import the main APIs for the underlying toolkit systems,
        // such as input and measurement/layout
        implementation("androidx.compose.ui:ui")

        // Android Studio Preview support
        implementation("androidx.compose.ui:ui-tooling-preview")
        debugImplementation("androidx.compose.ui:ui-tooling")

        // UI Tests
        androidTestImplementation("androidx.compose.ui:ui-test-junit4")
        debugImplementation("androidx.compose.ui:ui-test-manifest")

        // Optional - Add window size utils
        implementation("androidx.compose.material3.adaptive:adaptive")

        // Optional - Integration with activities
        implementation("androidx.activity:activity-compose:1.13.0")
        // Optional - Integration with ViewModels
        implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
        // Optional - Integration with LiveData
        implementation("androidx.compose.runtime:runtime-livedata")
        // Optional - Integration with RxJava
        implementation("androidx.compose.runtime:runtime-rxjava2")

    }

> [!NOTE]
> **Note:** Jetpack Compose is shipped using a Bill of Materials (BOM), to keep the versions of all library groups in sync. Read more about it in the [Bill of
> Materials page](https://developer.android.com/develop/ui/compose/bom/bom).