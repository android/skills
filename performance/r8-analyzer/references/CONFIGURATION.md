To achieve maximum utilization of R8, the codebase must be configured correctly
depending on the build script language (Kotlin DSL versus Groovy DSL).

## 1. App modules (`com.android.application`)

The app's `build.gradle` or `build.gradle.kts` file must enable minification
and resource shrinking within the `release` build type or the app's custom build
type for release and performance testing. It must use the optimized default file
(`proguard-android-optimize.txt`).

On **AGP 9.3 and higher** , the `optimization { enable = true }` DSL block turns
on code shrinking (R8), resource shrinking, and `proguard-android-optimize.txt`
in a single setting. When `optimization { enable = true }` is present in the
release build type, all three checks are satisfied---do **not** report
minification, resource shrinking, or `proguard-android-optimize.txt` as
missing, and do **not** recommend switching back to `isMinifyEnabled` or
`isShrinkResources`.

**Kotlin DSL --- AGP 9.3 and higher (`build.gradle.kts`)**:

    buildTypes {
       release {
           optimization {
               enable = true
           }
           proguardFiles("proguard-rules.pro")
       }
    }

**Kotlin DSL --- Legacy or Pre-AGP 9.3 (`build.gradle.kts`)**:

    buildTypes {
       release {
           isMinifyEnabled = true
           isShrinkResources = true
           proguardFiles(
               getDefaultProguardFile("proguard-android-optimize.txt"),
               "proguard-rules.pro"
           )
       }
    }

**Groovy DSL (`build.gradle`)**:

    buildTypes {
       release {
           minifyEnabled = true
           shrinkResources = true
           proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
       }
    }

## 2. `gradle.properties` flags

**Full mode**: R8 full mode enables all optimizations.

- **AGP 8.0 and higher** : Enabled by default. Verify that `android.enableR8.fullMode=false` is **not** present in `gradle.properties`.
- **Pre-AGP 8.0** : Verify that `android.enableR8.fullMode=true` is set in `gradle.properties`.

**Optimized resource shrinking** : If AGP is 8.12 or 8.13, verify that
`android.r8.optimizedResourceShrinking=true` is set in `gradle.properties`.
From AGP 9.0 it's on by default.

    android.r8.optimizedResourceShrinking=true