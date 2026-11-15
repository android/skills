On iOS, declaring a purpose string in `Info.plist` is sufficient --- the system
presents the prompt automatically on first API use. **On Android this is not
true.** A `<uses-permission>` entry in `AndroidManifest.xml` only declares
*intent* ; for any permission in the **dangerous** class the app must
additionally request it at runtime and handle the user's answer. A manifest-only
migration produces an app that silently fails on every hardware call.

> [!CAUTION]
> **Caution:** `@SuppressLint("MissingPermission")` **does not grant a permission** . It only silences the lint check. Using it as a substitute for a real request produces code that compiles, passes lint, and then throws `SecurityException` at runtime. It is permitted **only** on a call site already guarded by an explicit `ContextCompat.checkSelfPermission(...) == PackageManager.PERMISSION_GRANTED` check.

*** ** * ** ***

## 1. Permission Mapping Table

| iOS `Info.plist` key | Android permission(s) | Class | Notes |
|---|---|---|---|
| `NSCameraUsageDescription` | `CAMERA` | Dangerous | Runtime request required. |
| `NSLocationWhenInUseUsageDescription` | `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | Dangerous | Must request **both**; user may grant only coarse. |
| `NSLocationAlwaysAndWhenInUseUsageDescription` | `ACCESS_BACKGROUND_LOCATION` **only if** the migrated app reads location with no visible UI and no foreground service | Dangerous | Continuous user-initiated tracking should instead use a `foregroundServiceType="location"` service (no background permission needed). If you do declare it, Android 11+ requires a **separate, second** request after foreground is granted. Never bundle it in the initial request, and never declare it without a code path that uses it. |
| `NSBluetoothAlwaysUsageDescription` | `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT` | Dangerous (API 31+) | Replaces legacy `BLUETOOTH`/`BLUETOOTH_ADMIN`. Add `android:usesPermissionFlags="neverForLocation"` to `BLUETOOTH_SCAN` if you do not derive location from beacons. |
| `NSHealthShareUsageDescription` | Health Connect read permissions | Special | Not a standard runtime permission --- see §4. |
| `NSHealthUpdateUsageDescription` | Health Connect write permissions | Special | See §4. |
| `NSMotionUsageDescription` | `ACTIVITY_RECOGNITION` | Dangerous | API 29+. |
| `NSPhotoLibraryUsageDescription` | `READ_MEDIA_IMAGES` (API 33+) | Dangerous | Prefer the Photo Picker, which needs no permission. |
| *(none --- implicit on iOS)* | `POST_NOTIFICATIONS` | Dangerous (API 33+) | Android-only requirement with no iOS `Info.plist` counterpart. Easy to miss. |

Declare every mapped permission in `AndroidManifest.xml`, then wire a runtime
request for each **Dangerous** row.

*** ** * ** ***

## 2. The `rememberPermissionRequest` Hook Pattern

Centralize requests in one reusable hook rather than scattering launchers across
screens. The hook returns **state plus an action** --- it deliberately does not
wrap or replace content, so a screen renders identically whether or not the
permission has been granted.

```kotlin
class PermissionRequest(
    val granted: Boolean,
    val request: () -> Unit,
)

@Composable
fun rememberPermissionRequest(permissions: List<String>): PermissionRequest {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            permissions.all {
                ContextCompat.checkSelfPermission(context, it) ==
                    PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> granted = result.values.all { it } }

    return remember(granted) {
        PermissionRequest(granted) { launcher.launch(permissions.toTypedArray()) }
    }
}
```

Usage at the screen level --- content is always composed, and the **system
dialog** is raised by the action that needs the permission:

```kotlin
@Composable
fun TrackingScreen(viewModel: TrackingViewModel) {
    val perms = rememberPermissionRequest(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    )
    TrackingContent(
        viewModel = viewModel,
        onStartClick = {
            if (perms.granted) viewModel.startTracking() else perms.request()
        },
    )
}
```

> [!WARNING]
> **Warning:** Do not write `if (granted) Content() else PermissionScreen()`, and do not render a spinner while the decision is pending. Replacing the screen makes the feature unreachable whenever the permission is absent, including for every user who declines the prompt.

If the source app has its own explanatory permission UI, port it as a
**non-blocking** part of the real screen --- a banner, inline card, or empty state
that calls `perms.request()` --- rendered *alongside* the content, never instead
of it.

*** ** * ** ***

## 3. Guarding the Call Site

Even behind a gate, manager classes must verify before touching a protected API.
This is the **only** context in which `@SuppressLint("MissingPermission")` is
acceptable.

```kotlin
class DeviceScanner(private val context: Context) {

    private fun hasScanPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // Guarded by hasScanPermission() below.
    fun startScan(onResult: (ScanResult) -> Unit) {
        if (!hasScanPermission()) {
            onResult.emitFallbackDemoDevices()
            return
        }
        try {
            scanner?.startScan(filters, settings, scanCallback)
        } catch (e: Throwable) {
            // JVM test or emulator without a BLE radio.
            onResult.emitFallbackDemoDevices()
        }
    }
}
```

> [!IMPORTANT]
> **Important:** The `try/catch` exists **only** so host-side (JVM) tests, emulators and devices without that hardware don't crash. It is **not** a substitute for the permission check. Both are required: check first, then call inside `try/catch`.

*** ** * ** ***

## 4. Health Connect (Special Permissions)

Health Connect does **not** use
`ActivityResultContracts.RequestMultiplePermissions()`. It has its own contract.

```kotlin
val healthPermissions = setOf(
    HealthPermission.getReadPermission(StepsRecord::class),
    HealthPermission.getReadPermission(HeartRateRecord::class),
    HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
    HealthPermission.getReadPermission(SleepSessionRecord::class),
)

@Composable
fun rememberHealthPermissionRequest(): PermissionRequest {
    val context = LocalContext.current
    val client = remember { runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull() }
    var granted by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { grantedSet ->
        granted = grantedSet.containsAll(healthPermissions)
    }

    LaunchedEffect(client) {
        if (client == null) return@LaunchedEffect  // Health Connect APK absent.
        granted = client.permissionController
            .getGrantedPermissions()
            .containsAll(healthPermissions)
    }

    return remember(granted) {
        PermissionRequest(granted) { launcher.launch(healthPermissions) }
    }
}
```

The health screen renders its own content regardless --- showing real records when
`granted`, and an empty state offering `request()` when not. It must not be
swapped out for a fallback screen.

Also declare the permissions in `AndroidManifest.xml` and set `minSdk = 26`:

```xml
<uses-permission android:name="android.permission.health.READ_STEPS" />
<uses-permission android:name="android.permission.health.READ_HEART_RATE" />
<uses-permission android:name="android.permission.health.READ_ACTIVE_CALORIES_BURNED" />
<uses-permission android:name="android.permission.health.READ_SLEEP" />
```

*** ** * ** ***

## 5. Background Location (Two-Step)

Android 11+ rejects a combined foreground + background location request. Request
foreground first, and only offer background after it is granted.

```kotlin
// Step 1
launcher.launch(arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION))
// Step 2 --- only after step 1 returns granted, and only on API 29+
if (foregroundGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    backgroundLauncher.launch(arrayOf(ACCESS_BACKGROUND_LOCATION))
}
```

A foreground service using location must also declare:

```xml
<service
    android:name=".service.LocationService"
    android:foregroundServiceType="location"
    android:exported="false" />
```

*** ** * ** ***

## 6. Verification Checklist

Before declaring the migration complete, confirm each item:

- \[ \] Every **Dangerous** permission (and every Health Connect permission) in `AndroidManifest.xml` has a request launcher that is **actually invoked** from the user action that needs it. A helper that is defined but never called does not count --- search for each `remember...PermissionRequest` / launcher name and confirm at least one call site outside its definition.
- \[ \] No `@SuppressLint("MissingPermission")` appears without an adjacent `checkSelfPermission` guard.
- \[ \] `POST_NOTIFICATIONS` is requested on API 33+ (it has no iOS counterpart and is commonly forgotten).
- \[ \] `CAMERA` (if declared) is checked with `checkSelfPermission` and requested via a launcher **before** the camera UI binds CameraX / opens the capture sheet --- i.e. in the screen or sheet that shows the camera (e.g. a capture sheet opened from a photo button), not just declared in the manifest. This is the most commonly missed runtime request because the camera screen often appears to work in previews without it.
- \[ \] `ACCESS_BACKGROUND_LOCATION` is requested **separately**, after foreground location is granted.
- \[ \] No permission is declared in the manifest without a code path that uses it --- in particular, `ACCESS_BACKGROUND_LOCATION` is absent unless location is read with no visible UI and no foreground service.
- \[ \] Health Connect uses `PermissionController.createRequestPermissionResultContract()`, not the standard contract.
- \[ \] Every permission denial path renders a rationale UI rather than crashing or silently no-oping.