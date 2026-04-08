---
name: agp-9-upgrade
description: Upgrades, or migrates, an Android project to use Android Gradle Plugin
  (AGP) version 9
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  keywords:
  - Android Gradle Plugin 9
  - AGP 9
  - AGP Upgrade
  - AGP Migration
  - New AGP DSL
  - Migrate to built-in Kotlin
---

## Migration guide

See the [AGP 9 migration guide](references/android/build/releases/agp-9-0-0-release-notes.md) for the major changes, many
breaking, in AGP 9 compared to AGP 8.

## Requirements

If the user requests to update or migrate to AGP 9, first check the AGP version
used in the project. If it is lower than 9, stop and ask the user to run the AGP
Upgrade Assistant in Android Studio, and confirm when done.

Each version of AGP has its own set of compatibilities with other tools, such as
Gradle, JDK, and Kotlin. The release notes for each of these versions will
include a **Compatibility** table indicating the minimum versions for these
tools.

## Steps

If AGP is already at 9 or higher, then do the following:

### Step 1: Migrate to built-in Kotlin.

See [the guide](references/android/build/migrate-to-built-in-kotlin.md) for detailed information.

### Step 2. Migrate to the new AGP DSL.

See [the guide](references/android/build/releases/agp-9-0-0-release-notes.md) for detailed information.

See also [gradle-recipes](references/recipes.md) for examples on how to migrate old code to code
that is compatible with AGP 9 and the new DSL.

### Step 3. Migrate kapt to KSP or legacy-kapt

If KSP (`com.google.devtools.ksp`) is used in the project, ensure it is on
version 2.3.6 or higher.

See [KSP, kapt, and legacy-kapt](references/ksp-kapt.md) for detailed information.

### Step 4. BuildConfig

If any Android module contains custom BuildConfig fields, see [BuildConfig](references/buildconfig.md)
for detailed information.

### Step 5. Update gradle.properties

After the migration, check gradle.properties. Remove the following flags:

1. android.builtInKotlin
2. android.newDsl
3. android.uniquePackageNames
4. android.enableAppCompileTimeRClass

Additionally, delete all temporary files you've created.

## Guidelines

- Never write or run python scripts.
- Only search the Gradle dependency cache when inspecting external dependencies, and only as a last resort.
- Never add `android.disallowKotlinSourceSets=false` to `gradle.properties`.
- When verifying changes, don't run the `clean` task. This is a waste of time.

## Verification

After migration, verify the following:

1. Gradle IDE sync succeeds.
2. `./gradlew help` succeeds.
3. `./gradlew build --dry-run` succeeds.

## Troubleshooting

Paparazzi v2.0.0-alpha04 and lower versions have issues with AGP 9. See
[references/paparazzi-gradle-9.md](references/paparazzi-gradle-9.md) for details.