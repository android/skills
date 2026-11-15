> [!IMPORTANT]
> **Important:** **ORCHESTRATION GATEWAY PREREQUISITE** : This document is a supplemental reference guide for [`../SKILL.md`](https://developer.android.com/agents/skills/migration/ios-to-android/skill). You **MUST** read and follow the primary orchestration protocol in `../SKILL.md` before applying any patterns from this guide. `SKILL.md` enforces mandatory incremental compilation gates (`./gradlew compileDebugKotlin`), vertical feature slicing, and strict anti-premature-exit execution discipline.

This skill provides an exhaustive, production-grade guide for migrating,
structuring, and consuming static assets and localizations when transitioning
from an iOS Xcode project (`Assets.xcassets`, `Info.plist`,
`Localizable.strings`, `Localizable.stringsdict`, `.xcstrings`, and bundle
resources) to the Android Gradle project structure (`src/main/res/` and
`src/main/assets/`). You **Must** use this skill when porting any visual,
textual, localized, or raw binary assets to guarantee full Android Resource
Packaging Tool (AAPT2) compliance, runtime density scaling, and idiomatic
Jetpack Compose consumption.

**Official Reference Links:**

- [App resources overview \| Android Developers](https://developer.android.com/guide/topics/resources/providing-resources)
- [Vector drawables overview \| Android Developers](https://developer.android.com/studio/write/vector-asset-studio)
- [Localize your app \| Android Developers](https://developer.android.com/guide/topics/resources/localization)
- [Adaptive icons guide \| Android Developers](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive)
- [Fonts in Compose \| Android Developers](https://developer.android.com/develop/ui/compose/text/fonts)

## Prerequisites

Before initiating resource migration, you **Must** verify that the following
prerequisites are met:

- **Source Context:** Direct read access to the iOS codebase containing `Assets.xcassets`, `Localizable.strings` (or `.xcstrings`), `.stringsdict`, custom typography files (`.ttf`/`.otf`), and bundle resources is **Mandatory**.
- **Software Requirements:** Android Gradle Plugin (AGP) 8.0+, Android SDK build-tools, Kotlin 1.8+, and Jetpack Compose BOM are **Mandatory** . You can execute all workflows using any modern IDE (Android Studio, VS Code, Cursor, IntelliJ IDEA) or command-line build tools (`./gradlew`).
- **AAPT2 Strict Naming Constraint:** Android requires all resource file names inside `res/` subdirectories to strictly follow the regex `^[a-z0-9_]+$`. Uppercase characters, spaces, and hyphens (`-`) are strictly forbidden by AAPT2 and cause immediate build compilation errors.

## Limitations

This skill has the following explicit boundaries:

- **Dynamic Feature Delivery:** This skill covers static resources bundled within the APK or Android App Bundle (AAB). Dynamic on-demand asset delivery via Play Feature Delivery or asset packs is outside its scope.
- **Apple SF Symbols Licensing:** Apple's SF Symbols font and iconography are legally restricted to Apple platforms. This skill guides the translation of SF Symbols into open-source **Google Material Symbols** or custom SVG VectorDrawables.
- **Complex Animated Vectors:** Covers static vectors and multi-density raster graphics. Complex skeletal animations or animations exceeding 5 seconds (such as Adobe After Effects Lottie files or multi-layer AnimatedVectorDrawables) require dedicated animation runtimes (e.g., `com.airbnb.android:lottie-compose`).

## Glossary

| Term / Concept | iOS (`Xcode`) Context | Android (`Gradle`) Equivalent | Definition \& Architectural Scope |
|---|---|---|---|
| **Asset Catalog** | `Assets.xcassets` | `src/main/res/drawable/` \& `mipmap/` | Centralized project folder container for images, vector artwork, app icons, and named colors. |
| **Image Set** | `.imageset` | `res/drawable-<qualifier>/` | Container specifying 1x, 2x, and 3x raster bitmap variants or single-scale vector PDFs/SVGs. |
| **Color Set** | `.colorset` | `res/values/colors.xml`, `colors-night.xml`, \& Compose `Color` | Named color definition containing sRGB/Display-P3 hex/float values with Light and Dark appearance variations. |
| **App Icon** | `AppIcon.appiconset` | `res/mipmap-anydpi-v26/` (`<adaptive-icon>`) | Application launcher icon with separate foreground and background layers and legacy density fallbacks. |
| **Strings File** | `Localizable.strings` | `res/values/strings.xml` | Key-value translation dictionary for localized user-facing UI copy. |
| **Plurals Dictionary** | `Localizable.stringsdict` | `res/values/strings.xml` (`<plurals>`) | XML rules defining quantity-based string variations (zero, one, two, few, many, other). |
| **String Catalog** | `.xcstrings` | `res/values-<lang>/strings.xml` | Xcode 15+ consolidated JSON-based localization catalog. |
| **Resource Qualifier** | `.lproj` (language), scale (`@2x`) | Directory suffixes (`-es`, `-night`, `-xhdpi`, `-sw600dp`) | Suffix appended to Android resource folder names to designate target device configurations. |
| **Font Registration** | `Info.plist` (`UIAppFonts`) | `res/font/` \& `FontFamily` | Application-level registration and loading of custom TTF/OTF font typography. |
| **Raw Assets** | `Bundle.main.url(forResource:)` | `res/raw/` (ID-based) or `assets/` (Path-based) | Arbitrary non-compiled binary assets, JSON configurations, preloaded SQLite DBs, or media clips. |
| **AAPT2** | Asset Compiler / Assetutil | Android Asset Packaging Tool 2 | Official Android compiler that parses resource XML files, assigns integer IDs in `R.java`, and compresses assets. |
| **VectorDrawable** | Vector PDF / SVG in Asset Catalog | `<vector>` XML in `res/drawable/` | Resolution-independent vector graphic definition containing path data, fills, and dimensions. |

## Setup \& Dependencies

You **Must** configure your module's `build.gradle.kts` file using Gradle
Version Catalogs to enable VectorDrawable support and Jetpack Compose resource
integration:

```kotlin
// app/build.gradle.kts
android {
    defaultConfig {
        // Enable vector support library for backwards-compatible VectorDrawables
        vectorDrawables {
            useSupportLibrary = true
        }
    }
}

dependencies {
    // Jetpack Compose BOM and UI Foundation
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)

    // Google Material Icons (drop-in replacement for Apple SF Symbols)
    implementation(libs.androidx.compose.material.icons.extended)
}
```

**Version Catalog (`gradle/libs.versions.toml`):**
\`\`\`toml
\[versions\]
composeBom = "2024.06.00"

\[libraries\]
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
\`\`\`

*** ** * ** ***

## Workflow: Migrating Image Sets \& Vectors

### A. Core Density \& Format Mapping Table

You **Must** map iOS image scale factors and vector formats to their exact
Android density buckets:

| iOS Scale / Format | Android Density Bucket | Target Directory | Physical DPI Range | Scale Multiplier | Purpose / Best Practice |
|---|---|---|---|---|---|
| **Vector (SVG / PDF)** | **VectorDrawable XML** | `res/drawable/` | Scalable (All DPIs) | Resolution Independent | Standard for all icons, badges, glyphs, and line illustrations. |
| **`@1x`** | **MDPI** | `res/drawable-mdpi/` | \~160 dpi | 1.0x (Baseline) | Legacy baseline raster bitmaps. |
| **`@1.5x`** | **HDPI** | `res/drawable-hdpi/` | \~240 dpi | 1.5x | Mid-tier legacy density raster images. |
| **`@2x`** | **XHDPI** | `res/drawable-xhdpi/` | \~320 dpi | 2.0x | High-resolution raster images (e.g., iPhone Retina standard). |
| **`@3x`** | **XXHDPI** | `res/drawable-xxhdpi/` | \~480 dpi | 3.0x | Ultra high-resolution raster images (e.g., iPhone Plus/Pro standard). |
| **`@4x` (Launcher)** | **XXXHDPI** | `res/drawable-xxxhdpi/` | \~640 dpi | 4.0x | Reserved exclusively for large launcher icons (`mipmap`) and splash branding. |

### B. Step-by-Step Procedural Logic

```text
FOR each .imageset directory inside Assets.xcassets:
    IF the .imageset contains a vector file (SVG or single-scale PDF):
        1. Convert the SVG/PDF to an Android <vector> XML definition using CLI tools (e.g., svg2vectordrawable).
        2. Validate viewportWidth, viewportHeight, and pathData.
        3. Save the file to app/src/main/res/drawable/<image_name>.xml (no density qualifier).
    ELSE IF the .imageset contains raster bitmaps (PNG or JPEG):
        1. Extract image@1x.png, image@2x.png, image@3x.png.
        2. Rename the base name to snake_case (e.g., user_avatar.png).
        3. Copy image@1x.png to app/src/main/res/drawable-mdpi/user_avatar.png.
        4. Copy image@2x.png to app/src/main/res/drawable-xhdpi/user_avatar.png.
        5. Copy image@3x.png to app/src/main/res/drawable-xxhdpi/user_avatar.png.
    ELSE IF the iOS code invokes an Apple SF Symbol (Image(systemName: "...")):
        1. Replace the SF Symbol with the equivalent Google Material Icon from androidx.compose.material.icons.
```

#### Concrete Vector Example

- **Input (`Assets.xcassets/ic_settings.imageset/ic_settings.svg`):**
  `xml
  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
  <path d="M12 15C13.6569 15 15 13.6569 15 12C15 10.3431 13.6569 9 12 9C10.3431 9 9 10.3431 9 12C9 13.6569 10.3431 15 12 15Z" fill="#1976D2"/>
  <path fill-rule="evenodd" clip-rule="evenodd" d="M19.4 13C19.45 12.67 19.5 12.34 19.5 12C19.5 11.66 19.45 11.33 19.4 11L21.54 9.33C21.73 9.18 21.78 8.91 21.66 8.69L19.66 5.23C19.54 5.01 19.28 4.93 19.05 5.02L16.53 6.03C16.01 5.63 15.44 5.31 14.82 5.06L14.44 2.38C14.4 2.14 14.2 1.96 13.96 1.96H9.96C9.72 1.96 9.52 2.14 9.48 2.38L9.1 5.06C8.48 5.31 7.91 5.64 7.39 6.03L4.87 5.02C4.64 4.93 4.38 5.01 4.26 5.23L2.26 8.69C2.14 8.91 2.19 9.18 2.38 9.33L4.52 11C4.47 11.33 4.42 11.67 4.42 12C4.42 12.33 4.47 12.67 4.52 13L2.38 14.67C2.19 14.82 2.14 15.09 2.26 15.31L4.26 18.77C4.38 18.99 4.64 19.07 4.87 18.98L7.39 17.97C7.91 18.37 8.48 18.69 9.1 18.94L9.48 21.62C9.52 21.86 9.72 22.04 9.96 22.04H13.96C14.2 22.04 14.4 21.86 14.44 21.62L14.82 18.94C15.44 18.69 16.01 18.36 16.53 17.97L19.05 18.98C19.28 19.07 19.54 18.99 19.66 18.77L21.66 15.31C21.78 15.09 21.73 14.82 21.54 14.67L19.4 13Z"/>
  </svg>`

- **Output (`app/src/main/res/drawable/ic_settings.xml`):**
  `xml
  <?xml version="1.0" encoding="utf-8"?>
  <vector xmlns:android="http://schemas.android.com/apk/res/android"
  android:width="24dp"
  android:height="24dp"
  android:viewportWidth="24"
  android:viewportHeight="24">
  <path
  android:fillColor="#1976D2"
  android:pathData="M12,15C13.6569,15 15,13.6569 15,12C15,10.3431 13.6569,9 12,9C10.3431,9 9,10.3431 9,12C9,13.6569 10.3431,15 12,15Z"/>
  <path
  android:fillColor="#1976D2"
  android:pathData="M19.4,13C19.45,12.67 19.5,12.34 19.5,12C19.5,11.66 19.45,11.33 19.4,11L21.54,9.33C21.73,9.18 21.78,8.91 21.66,8.69L19.66,5.23C19.54,5.01 19.28,4.93 19.05,5.02L16.53,6.03C16.01,5.63 15.44,5.31 14.82,5.06L14.44,2.38C14.4,2.14 14.2,1.96 13.96,1.96H9.96C9.72,1.96 9.52,2.14 9.48,2.38L9.1,5.06C8.48,5.31 7.91,5.64 7.39,6.03L4.87,5.02C4.64,4.93 4.38,5.01 4.26,5.23L2.26,8.69C2.14,8.91 2.19,9.18 2.38,9.33L4.52,11C4.47,11.33 4.42,11.67 4.42,12C4.42,12.33 4.47,12.67 4.52,13L2.38,14.67C2.19,14.82 2.14,15.09 2.26,15.31L4.26,18.77C4.38,18.99 4.64,19.07 4.87,18.98L7.39,17.97C7.91,18.37 8.48,18.69 9.1,18.94L9.48,21.62C9.52,21.86 9.72,22.04 9.96,22.04H13.96C14.2,22.04 14.4,21.86 14.44,21.62L14.82,18.94C15.44,18.69 16.01,18.36 16.53,17.97L19.05,18.98C19.28,19.07 19.54,18.99 19.66,18.77L21.66,15.31C21.78,15.09 21.73,14.82 21.54,14.67L19.4,13Z"/>
  </vector>`

### C. Apple SF Symbols $\\rightarrow$ Google Material Symbols Mapping

| iOS SF Symbol Invocation | Material Symbols Equivalent (`androidx.compose.material.icons`) | Compose Vector Property |
|---|---|---|
| `Image(systemName: "magnifyingglass")` | Search Icon | `Icons.Default.Search` |
| `Image(systemName: "gearshape.fill")` | Settings Icon | `Icons.Filled.Settings` |
| `Image(systemName: "heart.fill")` | Favorite / Heart | `Icons.Filled.Favorite` |
| `Image(systemName: "trash.fill")` | Delete / Trash | `Icons.Filled.Delete` |
| `Image(systemName: "person.circle.fill")` | Account / Profile | `Icons.Filled.AccountCircle` |
| `Image(systemName: "chevron.right")` | Chevron Right | `Icons.AutoMirrored.Filled.KeyboardArrowRight` |
| `Image(systemName: "square.and.arrow.up")` | Share Sheet Icon | `Icons.Default.Share` |
| `Image(systemName: "checkmark.circle.fill")` | Checkmark / Success | `Icons.Filled.CheckCircle` |
| `Image(systemName: "exclamationmark.triangle")` | Warning / Alert | `Icons.Filled.Warning` |

#### Jetpack Compose UI Consumption

```kotlin
// Consuming VectorDrawables and Bitmaps in Compose
@Composable
fun SettingsActionRow(onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSettingsClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_settings),
            contentDescription = stringResource(id = R.string.settings_content_description),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(id = R.string.settings_title),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
```

*** ** * ** ***

## Workflow: Migrating Localized Strings \& Plurals

### A. Format Specifiers \& Escape Rules Mapping Table

You **Must** convert iOS string formatting tokens and escape sequences into
valid Android XML syntax:

| iOS Formatting / Character | Android XML Equivalent | Parsing Rule / Rationale |
|---|---|---|
| `%@` (Objective-C Object / String) | `%s` (or `%1$s`) | Android requires `%s` for string arguments. |
| `%d` / `%ld` / `%lld` (Integer) | `%d` (or `%1$d`) | Signed 32-bit or 64-bit decimal integers. |
| `%f` / `%.2f` (Float / Double) | `%f` / `%.2f` | Floating point formatting with optional precision specifiers. |
| `%1$@`, `%2$@` (Positional Strings) | `%1$s`, `%2$s` | Positional indexing is strictly required by Android lint when strings are localized into languages with differing word orders. |
| `'` (Single Quote / Apostrophe) | `\'` or `"enclosed in quotes"` | Raw unescaped apostrophes cause AAPT2 XML parsing errors. |
| `"` (Double Quote) | `\"` | Double quotes must be escaped inside XML attributes. |
| `&` (Ampersand) | `&amp;` | Standard XML character entity requirement. |
| `<` / `>` (Angle Brackets) | `&lt;` / `&gt;` or `<![CDATA[...]]>` | Prevents XML parser from interpreting text as child tags. |
| `\n` (Newline) | `\n` | Explicit newline character preserved across platforms. |

### B. Step-by-Step Procedural Logic

```text
FOR each localization directory (<lang>.lproj) in the iOS project:
    Determine target Android directory:
        IF default English (en.lproj): target is app/src/main/res/values/strings.xml
        ELSE target is app/src/main/res/values-<lang>/strings.xml (e.g., values-es, values-fr).

    FOR each entry in Localizable.strings:
        1. Convert the key from camelCase or dot.notation to snake_case (e.g., onboarding.title -> onboarding_title).
        2. Convert all %@ format specifiers to %s.
        3. Convert all %ld/%lld integer specifiers to %d.
        4. Add positional indices (%1$s, %2$d) if multiple format arguments exist.
        5. Escape raw single quotes (') with \' and double quotes (") with \".
        6. Wrap strings containing HTML formatting in <![CDATA[...]]>.
        7. Write <string name="<snake_key>"><escaped_value></string> to strings.xml.

    FOR each plural entry in Localizable.stringsdict:
        1. Extract the rule key from NSStringLocalizedFormatKey.
        2. Create a <plurals name="<plural_key>"> element in strings.xml.
        3. FOR each quantity rule (zero, one, two, few, many, other):
               Map to <item quantity="<rule>"><formatted_string></item>.
```

#### Concrete Strings \& Plurals Examples

- **Input (`ios/en.lproj/Localizable.strings`):**
  `strings
  /* User Profile & Dashboard */
  "onboarding.welcomeHeader" = "Welcome back, %@!";
  "dashboard.unreadCount" = "You have %ld unread messages from %2$@.";
  "auth.legalDisclaimer" = "Don't share your password with anyone.";
  "terms.agreement" = "I agree to the <b>Terms of Service</b> & Privacy Policy.";
  "app.brandName" = "JetEngine Pro";`

- **Input (`ios/en.lproj/Localizable.stringsdict`):**
  `xml
  <?xml version="1.0" encoding="UTF-8"?>
  <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
  <plist version="1.0">
  <dict>
  <key>flightResultsFound</key>
  <dict>
  <key>NSStringLocalizedFormatKey</key>
  <string>%#@num_flights@</string>
  <key>num_flights</key>
  <dict>
  <key>NSStringFormatSpecifierTypeKey</key>
  <string>NSStringPluralRuleType</string>
  <key>NSStringFormatValueTypeKey</key>
  <string>d</string>
  <key>zero</key>
  <string>No flights available</string>
  <key>one</key>
  <string>%d flight available</string>
  <key>other</key>
  <string>%d flights available</string>
  </dict>
  </dict>
  </dict>
  </plist>`

- **Output (`app/src/main/res/values/strings.xml`):**
  \`\`\`xml
  \<?xml version="1.0" encoding="utf-8"?\>
  Welcome back, %1$s! You have %1$d unread messages from %2$s. Don\\'t share your password with anyone. \<!\[CDATA\[I agree to the **Terms of Service** \& Privacy Policy.\]\]\> JetEngine Pro

      <!-- Plurals -->
      <plurals name="flight_results_found">
          <item quantity="zero">No flights available</item>
          <item quantity="one">%d flight available</item>
          <item quantity="other">%d flights available</item>
      </plurals>


  \`\`\`
- **Output for Spanish (`app/src/main/res/values-es/strings.xml`):**
  `xml
  <?xml version="1.0" encoding="utf-8"?>
  <resources>
  <string name="onboarding_welcome_header">¡Bienvenido de nuevo, %1$s!</string>
  <string name="dashboard_unread_count">Tienes %1$d mensajes no leídos de %2$s.</string>
  <string name="auth_legal_disclaimer">No compartas tu contraseña con nadie.</string>
  <string name="terms_agreement"><![CDATA[Acepto los <b>Términos de Servicio</b> y la Política de Privacidad.]]></string>
  <plurals name="flight_results_found">
  <item quantity="zero">No hay vuelos disponibles</item>
  <item quantity="one">%d vuelo disponible</item>
  <item quantity="other">%d vuelos disponibles</item>
  </plurals>
  </resources>`

#### Reusable Plurals Helper Composable for Jetpack Compose

```kotlin
// app/src/main/java/com/example/app/ui/util/ResourceExtensions.kt
package com.example.app.ui.util

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

@Composable
@ReadOnlyComposable
fun pluralStringResource(
    @PluralsRes id: Int,
    quantity: Int,
    vararg formatArgs: Any
): String {
    val resources: Resources = LocalContext.current.resources
    return resources.getQuantityString(id, quantity, *formatArgs)
}

// Example Compose Usage:
@Composable
fun FlightSummaryBanner(flightCount: Int) {
    Text(
        text = pluralStringResource(
            id = R.plurals.flight_results_found,
            quantity = flightCount,
            flightCount
        ),
        style = MaterialTheme.typography.titleMedium
    )
}
```

*** ** * ** ***

## Workflow: Migrating Color Sets

### A. Step-by-Step Procedural Logic

```text
FOR each .colorset directory in Assets.xcassets:
    1. Read Contents.json.
    2. Extract red, green, blue, alpha float values (0.0 ... 1.0) from the srgb/display-p3 color definition.
    3. Calculate 8-digit ARGB Hex:
           R_hex = toHex(red * 255)
           G_hex = toHex(green * 255)
           B_hex = toHex(blue * 255)
           A_hex = toHex(alpha * 255) -> Default 0xFF
           HexColor = 0xAARRGGBB
    4. IF the color definition contains a Dark appearance variant:
           Add Light Hex to ui/theme/Color.kt as <ColorName>Light and app/src/main/res/values/colors.xml.
           Add Dark Hex to ui/theme/Color.kt as <ColorName>Dark and app/src/main/res/values-night/colors.xml.
           Bind into LightColorScheme and DarkColorScheme in ui/theme/Theme.kt.
       ELSE (Universal / Single Appearance):
           Add Hex to ui/theme/Color.kt and app/src/main/res/values/colors.xml.
```

#### Concrete Color Migration Example

- **Input (`BrandAccent.colorset/Contents.json`):**
  `json
  {
  "colors" : [
  {
  "idiom" : "universal",
  "color" : {
  "color-space" : "srgb",
  "components" : {
  "alpha" : "1.000",
  "red" : "0.082",
  "green" : "0.396",
  "blue" : "0.753"
  }
  }
  },
  {
  "appearances" : [
  {
  "appearance" : "luminosity",
  "value" : "dark"
  }
  ],
  "idiom" : "universal",
  "color" : {
  "color-space" : "srgb",
  "components" : {
  "alpha" : "1.000",
  "red" : "0.400",
  "green" : "0.680",
  "blue" : "1.000"
  }
  }
  }
  ]
  }`

- **Calculations:**

  - **Light:** `R=0.082*255=21 (0x15)`, `G=0.396*255=101 (0x65)`, `B=0.753*255=192 (0xC0)` $\\rightarrow$ `0xFF1565C0`
  - **Dark:** `R=0.400*255=102 (0x66)`, `G=0.680*255=173 (0xAD)`, `B=1.000*255=255 (0xFF)` $\\rightarrow$ `0xFF66ADFF`
- **Output 1: Android XML (`app/src/main/res/values/colors.xml` \&
  `res/values-night/colors.xml`):**
  `xml
  <!-- app/src/main/res/values/colors.xml (Default / Light) -->
  <?xml version="1.0" encoding="utf-8"?>
  <resources>
  <color name="brand_accent">#FF1565C0</color>
  <color name="background">#FFF8F9FA</color>
  </resources>`

  ```xml
  <!-- app/src/main/res/values-night/colors.xml (Dark Mode) -->
  <?xml version="1.0" encoding="utf-8"?>
  <resources>
      <color name="brand_accent">#FF66ADFF</color>
      <color name="background">#FF121212</color>
  </resources>
  ```
- **Output 2: Jetpack Compose Tokens (`ui/theme/Color.kt` \& `Theme.kt`):**
  \`\`\`kotlin
  // app/src/main/java/com/example/app/ui/theme/Color.kt
  package com.example.app.ui.theme

  import androidx.compose.ui.graphics.Color

  val BrandAccentLight = Color(0xFF1565C0)
  val BrandAccentDark = Color(0xFF66ADFF)
  val BackgroundLight = Color(0xFFF8F9FA)
  val BackgroundDark = Color(0xFF121212)
  \`\`\`

  ```kotlin
  // app/src/main/java/com/example/app/ui/theme/Theme.kt
  package com.example.app.ui.theme

  import androidx.compose.foundation.isSystemInDarkTheme
  import androidx.compose.material3.MaterialTheme
  import androidx.compose.material3.darkColorScheme
  import androidx.compose.material3.lightColorScheme
  import androidx.compose.runtime.Composable

  private val LightColorScheme = lightColorScheme(
      primary = BrandAccentLight,
      background = BackgroundLight
  )

  private val DarkColorScheme = darkColorScheme(
      primary = BrandAccentDark,
      background = BackgroundDark
  )

  @Composable
  fun AppTheme(
      darkTheme: Boolean = isSystemInDarkTheme(),
      content: @Composable () -> Unit
  ) {
      val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
      MaterialTheme(
          colorScheme = colorScheme,
          typography = AppTypography,
          content = content
      )
  }
  ```

*** ** * ** ***

## Workflow: Migrating Custom Fonts

### A. Step-by-Step Procedural Logic

```text
1. Inspect Info.plist under UIAppFonts to identify registered font files (.ttf / .otf).
2. Copy font files into app/src/main/res/font/.
3. Rename all font files to strictly match regex ^[a-z0-9_]+$:
       Inter-Bold.otf    -> inter_bold.otf
       Inter-Regular.ttf -> inter_regular.ttf
       Inter-Medium.ttf  -> inter_medium.ttf
4. Construct a FontFamily in ui/theme/Type.kt binding each font weight and style to its resource ID.
5. Wire the FontFamily into Material3 Typography.
```

#### Concrete Font Migration Example

- **Input (`Info.plist` excerpt):**
  `xml
  <key>UIAppFonts</key>
  <array>
  <string>Inter-Regular.ttf</string>
  <string>Inter-Medium.ttf</string>
  <string>Inter-Bold.ttf</string>
  </array>`

- **Output (`app/src/main/java/com/example/app/ui/theme/Type.kt`):**
  \`\`\`kotlin
  package com.example.app.ui.theme

  import androidx.compose.material3.Typography
  import androidx.compose.ui.text.TextStyle
  import androidx.compose.ui.text.font.Font
  import androidx.compose.ui.text.font.FontFamily
  import androidx.compose.ui.text.font.FontWeight
  import androidx.compose.ui.unit.sp
  import com.example.app.R

  // Define custom font family mapping font weights to resource IDs
  val InterFontFamily = FontFamily(
  Font(resId = R.font.inter_regular, weight = FontWeight.Normal),
  Font(resId = R.font.inter_medium, weight = FontWeight.Medium),
  Font(resId = R.font.inter_bold, weight = FontWeight.Bold)
  )

  // Wire FontFamily into Material3 Typography
  val AppTypography = Typography(
  headlineLarge = TextStyle(
  fontFamily = InterFontFamily,
  fontWeight = FontWeight.Bold,
  fontSize = 32.sp,
  lineHeight = 40.sp
  ),
  bodyLarge = TextStyle(
  fontFamily = InterFontFamily,
  fontWeight = FontWeight.Normal,
  fontSize = 16.sp,
  lineHeight = 24.sp
  ),
  labelLarge = TextStyle(
  fontFamily = InterFontFamily,
  fontWeight = FontWeight.Medium,
  fontSize = 14.sp
  )
  )
  \`\`\`

*** ** * ** ***

## Workflow: Migrating Raw Files \& Binary Bundles

### A. Decision Matrix

You **Must** evaluate raw asset access patterns according to the following
decision table:

| Asset Characteristic / Requirement | Target Android Directory | Android Access Mechanism | Typical File Types |
|---|---|---|---|
| **Flat, single file referenced via static compile-time ID** | `app/src/main/res/raw/` | `resources.openRawResource(R.raw.<name>)` | `config.json`, `cacert.pem`, `sound_alert.mp3`, `intro_video.mp4` |
| **Nested folder structure, dynamic path resolution, or runtime file enumeration** | `app/src/main/assets/` | `context.assets.open("<path>/<file>")` or `context.assets.list("<dir>")` | `offline_maps/region1.bin`, `templates/*.html`, pre-populated SQLite `.db` |

### B. Concrete Kotlin Access Examples

#### 1. Reading from `res/raw/` (Static Resource ID)

```kotlin
// Reads content from app/src/main/res/raw/app_config.json
fun readRawConfig(context: Context): String {
    return context.resources.openRawResource(R.raw.app_config).bufferedReader().use { reader ->
        reader.readText()
    }
}
```

#### 2. Reading from `src/main/assets/` (Path \& Subdirectories)

```kotlin
// Reads content from app/src/main/assets/data/regions/us_states.json
fun readAssetRegionData(context: Context, relativePath: String): ByteArray {
    return context.assets.open(relativePath).use { inputStream ->
        inputStream.readBytes()
    }
}
```

*** ** * ** ***

## Workflow: Migrating App Launcher Icons

### A. Adaptive Icon Architecture \& Mipmap Table

Android (API 26+) uses **Adaptive Icons** composed of a separate background and
foreground layer, contained in a 108dp canvas with a 72dp visible safe area.

| Mipmap Density Bucket | Target Directory | Legacy Flattened Icon Size (px) | Adaptive Layer Size (px) | Density Multiplier |
|---|---|---|---|---|
| **MDPI** | `res/mipmap-mdpi/` | 48 x 48 px | 108 x 108 px | 1.0x (Baseline) |
| **HDPI** | `res/mipmap-hdpi/` | 72 x 72 px | 162 x 162 px | 1.5x |
| **XHDPI** | `res/mipmap-xhdpi/` | 96 x 96 px | 216 x 216 px | 2.0x |
| **XXHDPI** | `res/mipmap-xxhdpi/` | 144 x 144 px | 324 x 324 px | 3.0x |
| **XXXHDPI** | `res/mipmap-xxxhdpi/` | 192 x 192 px | 432 x 432 px | 4.0x |
| **Any-DPI (API 26+)** | `res/mipmap-anydpi-v26/` | XML Adaptive Vector | XML Adaptive Vector | Vector Scalable |

### B. Adaptive Icon XML Definition

- **File: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`:**
  `xml
  <?xml version="1.0" encoding="utf-8"?>
  <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
  <background android:drawable="@drawable/ic_launcher_background" />
  <foreground android:drawable="@drawable/ic_launcher_foreground" />
  <!-- Android 13+ Themed App Icon Support -->
  <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
  </adaptive-icon>`

- **Manifest Registration (`app/src/main/AndroidManifest.xml`):**
  `xml
  <application
  android:icon="@mipmap/ic_launcher"
  android:roundIcon="@mipmap/ic_launcher_round"
  android:label="@string/app_name"
  android:theme="@style/Theme.App">
  <!-- Activities -->
  </application>`

*** ** * ** ***

## Standard Asset Naming Conventions \& CLI Discovery

You **Must** organize all files in `res/drawable/` and `res/mipmap/` using the
following standardized prefixes:

| Asset Category | Standard Prefix | Example File Name | Usage Context |
|---|---|---|---|
| **Icons / Glyphs** | `ic_` | `ic_navigation_back.xml` | Toolbar icons, tab bar icons, action buttons. |
| **Backgrounds** | `bg_` | `bg_card_gradient.xml` | Card surfaces, modal backdrops, gradient fills. |
| **Illustrations** | `ill_` | `ill_onboarding_welcome.png` | Empty state artwork, onboarding splash scenes. |
| **Placeholders** | `ph_` | `ph_avatar_user.xml` | Image loading placeholders, user profile fallbacks. |
| **State List Drawables** | `sl_` | `sl_checkbox_toggle.xml` | Selectors handling pressed, focused, checked states. |

### iOS Asset Discovery (CLI)

```bash
# Discover all iOS asset catalogs, strings, fonts, and property lists
find . -name "*.xcassets" -o -name "*.strings" -o -name "*.stringsdict" -o -name "*.xcstrings" -o -name "*.ttf" -o -name "*.otf"
```

### Verification Checklist

- \[ \] Every `Localizable.strings` key has a corresponding `<string name="...">` in `res/values/strings.xml`.
- \[ \] All string format specifiers are translated from `%@` $\\rightarrow$ `%s` and `%ld` $\\rightarrow$ `%d`.
- \[ \] Positional arguments (`%1$s`, `%2$s`) are used for strings with multiple format parameters.
- \[ \] All filenames in `res/drawable/`, `res/font/`, and `res/raw/` match the lowercase regex `^[a-z0-9_]+$`.
- \[ \] Adaptive launcher icons (`ic_launcher.xml` and `ic_launcher_round.xml`) exist in `res/mipmap-anydpi-v26/`.
- \[ \] All custom fonts are registered in a Compose `FontFamily` with appropriate `FontWeight` bindings.

*** ** * ** ***

## Quality Control \& Antipatterns

| Action Category | DO | DON'T | NEVER |
|---|---|---|---|
| **Resource Naming** | **DO** use lowercase alphanumeric with underscores (`ic_search_active.xml`). | **DON'T** use camelCase (`icSearchActive.png`) or hyphens (`ic-search.xml`). | **NEVER** use uppercase letters or spaces in `res/` file names; AAPT2 compilation will immediately fail. |
| **Launcher Icons** | **DO** place launcher icons in `res/mipmap-<density>/` and `res/mipmap-anydpi-v26/`. | **DON'T** place standard UI assets (buttons, backgrounds) in `mipmap/`; this prevents density stripping in App Bundles. | **NEVER** hardcode square launcher icons without an adaptive icon fallback on API 26+. |
| **String Formatting** | **DO** escape single quotes (`\'`) and use positional specifiers (`%1$s`, `%2$d`). | **DON'T** leave iOS `%@` string format specifiers in XML strings. | **NEVER** hardcode user-facing string literals directly inside `@Composable` functions without `stringResource()`. |
| **Vector Graphics** | **DO** convert vector SVGs/PDFs into Android `VectorDrawable` XML definitions. | **DON'T** drop massive, unscaled multi-megabyte PNGs into `res/drawable-nodpi/`. | **NEVER** copy Apple's proprietary SF Symbols directly into an Android project (violates licensing). |
| **Color Schemes** | **DO** define colors in Compose `ui/theme/Color.kt` and assign via `MaterialTheme.colorScheme`. | **DON'T** hardcode raw hex integers (`Color(0xFF123456)`) directly inside individual UI composables. | **NEVER** use `android.graphics.Color` in Jetpack Compose UI code; always use `androidx.compose.ui.graphics.Color`. |
| **Raw Assets vs Raw Res** | **DO** use `res/raw/` for ID-accessible flat files and `assets/` for complex nested trees. | **DON'T** assume `res/raw` supports subdirectory hierarchies (it is strictly flat). | **NEVER** store dynamic, user-writable data in `res/` or `assets/` (they are strictly read-only at runtime). |