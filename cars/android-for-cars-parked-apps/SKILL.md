---
name: android-for-cars-parked-apps
description: Adapt existing Android video streaming apps, games, and web browsers
  to run as parked apps on Android Auto and Android Automotive OS. Covers manifest
  configuration, Google Play hardware feature declarations, compatibility mode, back
  navigation affordances, orientation and display continuity, controllable system
  bar insets, irregular display cutouts, driver distraction lifecycle handling, MediaSession
  playback restrictions with CarUxRestrictionsManager, background audio while driving,
  and browser sensitive data protection. Use this skill when migrating, building,
  or auditing parked apps for cars to meet Car App Quality guidelines. Don't use for
  media audio apps, templated Car App Library navigation, point of interest, internet
  of things, weather, or communication apps designed for use while driving.
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-10-09'
  keywords:
  - Android Auto
  - Android Automotive OS
  - parked apps
  - video
  - games
  - browsers
  - adaptive
---

Adapt existing Android video apps, games, and web browsers to run as parked
apps on Android Auto and Android Automotive OS. Follow these steps to meet the
car app quality guidelines for Google Play distribution.

## Prerequisites

- Begin with an existing adaptive Android app in one of the supported parked app categories: video streaming apps, games, or web browsers.

## Glossary

- **Parked apps**: Parked apps run on Android Auto and Android Automotive OS only while the vehicle is parked. The three parked categories are video, games, and browsers; other categories can't be published as parked apps.
- **Android Auto**: Android Auto is a phone-powered projection system. Apps execute on the user's phone and are projected to the in-car display over a wired or wireless connection. Android Auto is a separate platform from Android Automotive OS.
- **Android Automotive OS**: Android Automotive OS, or AAOS, is a variant of Android that runs directly on the vehicle itself. Android Automotive OS is a separate platform from Android Auto.
- **Video category**: Video apps display streaming videos while parked. Generally available on Android Automotive OS; unsupported on Android Auto.
- **Games category**: Game apps provide gameplay while parked. Generally available on Android Automotive OS and Android Auto.
- **Browsers category**: Web browsers let users browse the web while parked. Available in beta on Android Automotive OS; unsupported on Android Auto.
- **Media category**: Not a parked category. Media apps play audio content (music, radio, audiobooks, excluding video) while driving or parked.
- **Android for Cars platform**: Android for Cars is an umbrella term that consists of Android Auto and Android Automotive OS.
- **Generally available categories**: Permitted on internal testing, closed testing, open testing, and production Google Play tracks for a supported platform.
- **Beta categories**: Permitted on internal or closed testing tracks; open testing and production tracks require acceptance into an early access program.
- **Audio while driving**: A beta feature on Android Automotive OS that lets video apps play audio while the vehicle is in motion.
- **Car app quality guidelines** : When auditing an app against the Tier 3 (*Car ready* ), Tier 2 (*Car optimized* ), or Tier 1 (*Car differentiated* ) criteria, **MUST** check [car app quality guidelines](references/android/docs/quality-guidelines/car-app-quality/index.md).
- **Android Automotive OS compatibility mode** : Compatibility mode (`android.software.car.display_compatibility`) is a system feature on select vehicles providing a system back affordance, safe area rendering, density scaling, and opaque driving-mode blocking (moving activities to *Stopped*). Compatibility mode is only relevant to apps that support Android Automotive OS; it isn't relevant for apps that only support Android Auto.

## Codebase exploration

Search the codebase for `AndroidManifest.xml` (or create
`Assets/Plugins/Android/AndroidManifest.xml` in Unity projects),
`res/xml/automotive_app_desc.xml`, `build.gradle`, `build.gradle.kts`,
or `gradle/libs.versions.toml` files. Locate existing video players,
`MediaSession` classes, `requestedOrientation` calls, and `WindowInsets`
handlers. Unless instructed otherwise by the user, ask permission before adding
dependencies.

## Step 1: Configure the app manifest

Declare the app category, target platforms, hardware feature requirements, and
activity configuration in `AndroidManifest.xml`.

### 1.1: Identify the app category

- **Video apps** : Video apps **MUST** set `android:appCategory="video"` on `<application>` and **MUST NOT** include `app/src/main/res/xml/automotive_app_desc.xml` with `uses name="video"`.
- **Games** : Games **MUST** set `android:appCategory="game"` on `<application>`. Declare `android.hardware.gamepad` with `android:required="false"` if controller input is supported by the app.
- **Browsers** : Browsers **MUST** include `<category
  android:name="android.intent.category.APP_BROWSER"/>` in the `MAIN` `<intent-filter>` of an exported `<activity>` (`android:exported="true"`).

### 1.2: Identify target platforms and hardware features

- **Android Auto** : Apps targeting Android Auto (Android 15+) **MUST** add `<category android:name="android.intent.category.CAR_LAUNCHER"/>` to an `<intent-filter>` in the `AndroidManifest.xml` file. Add it to the `MAIN` `<intent-filter>` unless the user specifies a different intent filter. **NEVER** add this category if the app is solely targeting Android Automotive OS.
- **Android Automotive OS** : Apps targeting Android Automotive OS **MUST** declare `<uses-feature android:name="android.hardware.type.automotive"
  android:required="false"/>` (`"false"` for mobile or multi-form-factor tracks; `"true"` only if the app will be published on the dedicated Automotive OS track) in the `AndroidManifest.xml` file. **NEVER** add this feature tag if the app is solely targeting Android Auto.
- **Unrequired hardware features (`DO-1`)** : To satisfy Google Play feature requirements and `DO-1`, declare unsupported features (`android.hardware.wifi`, `android.hardware.screen.portrait`, `android.hardware.screen.landscape`, `android.hardware.camera`) with `android:required="false"`, and remove `android:required="true"` on features absent in cars.
- **Display continuity (`LS-C1`)** : Configure `android:configChanges` for `screenSize`, `smallestScreenSize`, `orientation`, `screenLayout`, and `density` on `<activity>` so moving to or from a distant display does not recreate the activity or stutter playback.
- **Compatibility mode and distraction** : Declaring compatibility mode is optional and is only relevant for apps that support Android Automotive OS. **NEVER** add compatibility mode for apps that only support Android Auto. Adding `android.hardware.type.automotive` disables compatibility mode unless `<meta-data android:name="android.software.car.display_compatibility"
  android:value="true"/>` is added to `<application>`. When an app declares `android.software.car.display_compatibility` in `AndroidManifest.xml`, the app **MUST** check `PackageManager.hasSystemFeature("android.software.car.display_compatibility")` at runtime and hide duplicate in-app back or close affordances when it returns `true`. Parked apps **MUST NOT** include `<meta-data android:name="distractionOptimized" android:value="true"/>` on any `<activity>` (`DD-2`, `DD-3`).

## Step 2: Provide back navigation and orientation support

### 2.1: Add in-app back and close affordances

Android Automotive OS vehicles aren't required to have a hardware or software
back button or gesture navigation (`AN-1`). Provide explicit UI back and close
controls on every non-root screen, including detail screens and full-screen
players:

- **If `AndroidManifest.xml` does not declare
  `android.software.car.display_compatibility`**: Display explicit in-app back and close controls unconditionally.
- **If `AndroidManifest.xml` declares
  `android.software.car.display_compatibility` with `android:value="true"`** : Show in-app back and close controls only when `PackageManager.hasSystemFeature("android.software.car.display_compatibility")` returns `false`.

### 2.2: Guard runtime orientation requests

Vehicle displays have fixed orientations. Requesting landscape on a
fixed-portrait car display causes letterboxing or flickering loops (`DO-1`).
Remove hardcoded `android:screenOrientation` manifest attributes and check
`PackageManager.FEATURE_SCREEN_LANDSCAPE` and
`PackageManager.FEATURE_SCREEN_PORTRAIT` before setting `requestedOrientation`.

## Step 3: Adapt to system bars and irregular display cutouts

### 3.1: Track controllable system bar insets

On Android Automotive OS, OEMs are permitted to lock system bars visible so
climate and vehicle controls remain accessible (`AR-1`). Track controllable
insets with `OnControllableInsetsChangedListener`, hide only controllable bars,
and apply `windowInsetsPadding` for uncontrollable `statusBars`,
`navigationBars`, and `displayCutout` insets.

### 3.2: Render into irregular display cutouts safely

To meet Tier 1 criterion `AR-2` on curved or non-rectangular car displays,
**NEVER** use `LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT` (`0`) or
`LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` (`1`). Configure
`LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS` (`3`) or
`LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER` (`2`) using the `car` resource qualifier
in `res/values-car-v30/integers.xml`:


```xml
<integer name="windowLayoutInDisplayCutoutMode">3</integer>
```

<br />

Protect interactive UI, app bars, and scrollable lists with
`WindowInsets.safeDrawing`.

## Step 4: Meet driver distraction and media playback requirements

### 4.1: Pause playback on lifecycle transitions and block driving resumption

When a vehicle begins moving, the OS covers parked activities with a blocking
overlay and triggers `onPause` (and `onStop` on compatibility mode vehicles). To
satisfy `DD-2`, `DD-3`, and `MC-1`:

1. Pause playback on `ON_PAUSE` or `ON_STOP` using `LifecycleEventEffect` from `androidx.lifecycle:lifecycle-runtime-compose`.
2. Expose a `MediaSession` (`androidx.media3:media3-session`) with play, pause, or stop commands and item metadata (`MC-1`), and release it in `onCleared`.
3. Add `useLibrary("android.car")` in `build.gradle.kts`, wrap `ExoPlayer` in `ForwardingSimpleBasePlayer`, listen to `CarUxRestrictionsManager`, pause playback when `isRequiresDistractionOptimization` becomes `true`, and remove `COMMAND_PLAY_PAUSE` in `getState` so external `MediaSession` controls can't resume playback while driving:


```kotlin
@UnstableApi
class CarAwarePlayer(context: Context) :
    ForwardingSimpleBasePlayer(ExoPlayer.Builder(context).build()) {
    private var shouldPreventPlay = false
    private var pausedByUxRestrictions = false
    private lateinit var carUxRestrictionsManager: CarUxRestrictionsManager

    init {
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            val car = Car.createCar(context)
            carUxRestrictionsManager =
                car.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
            shouldPreventPlay =
                carUxRestrictionsManager.currentCarUxRestrictions.isRequiresDistractionOptimization
            invalidateState()

            carUxRestrictionsManager.registerListener { restrictions: CarUxRestrictions ->
                shouldPreventPlay = restrictions.isRequiresDistractionOptimization
                if (!shouldPreventPlay && pausedByUxRestrictions) {
                    handleSetPlayWhenReady(true)
                    invalidateState()
                } else if (shouldPreventPlay && isPlaying) {
                    pausedByUxRestrictions = true
                    handleSetPlayWhenReady(false)
                    invalidateState()
                }
            }
        }
    }

    override fun getState(): State {
        val state = super.getState()
        return state.buildUpon()
            .setAvailableCommands(
                state.availableCommands.buildUpon()
                    .removeIf(COMMAND_PLAY_PAUSE, shouldPreventPlay)
                    .build()
            )
            .build()
    }

    override fun handleRelease(): ListenableFuture<*> {
        if (::carUxRestrictionsManager.isInitialized) {
            carUxRestrictionsManager.unregisterListener()
        }
        return super.handleRelease()
    }
}
```

<br />

### 4.2: Support background audio while driving (optional beta feature)

Because this is an optional beta feature, **NEVER** implement this feature
unless the user requests it. To implement the feature:

1. Declare `<uses-feature
   android:name="com.android.car.background_audio_while_driving"
   android:required="false" />` in `AndroidManifest.xml`, along with `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permissions.
2. Check runtime vehicle support with `CarFeatures.isFeatureEnabled(context,
   CarFeatures.FEATURE_BACKGROUND_AUDIO_WHILE_DRIVING)` from `androidx.car.app:app`. Only set `shouldPreventPlay` when `!isBackgroundAudioWhileDrivingSupported &&
   restrictions.isRequiresDistractionOptimization`.
3. Host the `MediaSession` and player inside an exported `MediaSessionService` with `android:foregroundServiceType="mediaPlayback"`, and connect from the UI using `MediaController.Builder` and `buildAsync`.

## Step 5: Protect sensitive data in browser apps

Because vehicles are shared devices, browsers **MUST** block access to
saved passwords and payment methods unless protected by a profile lock (`SD-1`).
Before syncing passwords or payment data to the vehicle, browsers **MUST**
prompt the user to authenticate and display a notice on the car screen stating
that their data will be synchronized to the car (`SD-2`).

Check whether a PIN, pattern, or password is set using `KeyguardManager`, open
`Settings.ACTION_SECURITY_SETTINGS` if unset, and authenticate with
`BiometricPrompt` using `BiometricManager.Authenticators.DEVICE_CREDENTIAL`
only (vehicles don't have biometric hardware):


```kotlin
val keyguardManager = context.getSystemService<KeyguardManager>()
val isDeviceSecure = keyguardManager?.isDeviceSecure == true
lateinit var biometricPrompt: BiometricPrompt

if (!isDeviceSecure) {
    context.startActivity(
        Intent(Settings.ACTION_SECURITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
} else {
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(context.getString(R.string.auth_title))
        .setSubtitle(context.getString(R.string.sync_data_to_car_notice))
        .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        .build()
    biometricPrompt.authenticate(promptInfo)
}
```

<br />

## Step 6: Verify implementation

Build the app using `./gradlew assembleDebug`. Find and fix any build errors.

## Post-implementation guidance

Provide this guidance to the user after implementation:

- For Android Auto apps, install the app on a phone and test using the Desktop Head Unit (DHU).
- For Android Automotive OS, install the app on the Android Automotive OS emulator in Android Studio.
- If the app is in a beta category for the target platform, acceptance into the early access program is required before submitting the app to an open testing or production track.
- If the app implements the beta audio while driving feature, acceptance into the early access program is required before submitting the app to an open testing or production track.
- Test the app against the relevant criteria in the car app quality guidelines before submitting the app.

## Antipatterns

- **NEVER** include `<meta-data android:name="distractionOptimized"
  android:value="true"/>` on any `<activity>` in a parked app, or include `res/xml/automotive_app_desc.xml` with `uses name="video"` in a video app.
- **NEVER** assume a hardware or software back button exists on Android Automotive OS.
- **NEVER** assume system bars are positioned only at the top and bottom or can always be hidden.
- **NEVER** use `LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT` or `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` on Android Automotive OS.
- **NEVER** infer UX restrictions from vehicle speed or gear state instead of reading `isRequiresDistractionOptimization` from `CarUxRestrictionsManager`.
- **NEVER** use biometric authenticators (`BIOMETRIC_STRONG`, `BIOMETRIC_WEAK`) with `BiometricPrompt` on browser apps on Android Automotive OS; use `DEVICE_CREDENTIAL` only.
- **NEVER** add the `CAR_LAUNCHER` intent category if the user only requests Android Automotive OS support.
- **NEVER** add the `android.hardware.type.automotive` feature tag if the user only requests Android Auto support.

## Best practices

- **MUST** declare `android:required="false"` on unsupported hardware features (`wifi`, `screen.portrait`, `screen.landscape`, `camera`) and guard `requestedOrientation` calls by checking `FEATURE_SCREEN_LANDSCAPE` and `FEATURE_SCREEN_PORTRAIT` (`DO-1`).
- **MUST** provide in-app back and close affordances (`AN-1`) for Android Automotive OS when not in compatibility mode and protect all interactive UI and text from system bars and display cutouts using `WindowInsets.safeDrawing` and `OnControllableInsetsChangedListener` (`AR-1`, `AR-2`).
- **MUST** pause playback when UX restrictions become active and strip `COMMAND_PLAY_PAUSE` from `MediaSession` while driving unless audio while driving is supported and enabled (`DD-2`, `DD-4`, `MC-1`).
- **MUST** require profile lock authentication and display an on-screen sync disclosure before saving, accessing, or syncing browser passwords or payment information (`SD-1`, `SD-2`).
