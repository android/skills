> [!IMPORTANT]
> **Important:** **ORCHESTRATION GATEWAY PREREQUISITE** : This document is a supplemental reference guide for [`../SKILL.md`](https://developer.android.com/agents/skills/migration/ios-to-android/skill). You **MUST** read and follow the primary orchestration protocol in `../SKILL.md` before applying any patterns from this guide. `SKILL.md` enforces mandatory incremental compilation gates (`./gradlew compileDebugKotlin`), vertical feature slicing, and strict anti-premature-exit execution discipline.

This skill establishes structured patterns and workflows for migrating iOS
location functionality to Android across core permission access abiding by
principle of least privilege, UX best practices, and geofencing scenarios.

**Official Reference Links:**

- [Location APIs overview \| Android Developers](https://developer.android.com/develop/sensors-and-location/location)
- [Request location permissions \| Android Developers](https://developer.android.com/develop/sensors-and-location/location/permissions)
- [Location button \| Android Developers](https://developer.android.com/guide/topics/permissions/private-alternatives/location-button)
- [Create and monitor geofences \| Android Developers](https://developer.android.com/develop/sensors-and-location/location/geofencing)

*** ** * ** ***

## Prerequisites

Before using this skill, you **Must** ensure the following prerequisites are
met:

- **Source Context:** Access to the existing iOS implementation is **Mandatory**.
- **Knowledge Requirements:** Full understanding of the Android runtime permission model, Jetpack Compose, and Coroutines is **Mandatory**.

*** ** * ** ***

## Setup/Dependencies

You **Must** add the following dependency to your `build.gradle.kts` file:

```kotlin
dependencies {
    implementation("androidx.core.locationbutton:locationbutton-compose:1.0.0-alpha01")
}
```

You **Must** declare every permission the migrated code actually uses in
`AndroidManifest.xml` --- and **only** those. Declaring a permission the app never
exercises (especially `ACCESS_BACKGROUND_LOCATION`) is a Play policy violation
and a best-practice failure.

```xml
<!-- In AndroidManifest.xml -->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<!-- Required if higher precision location data is necessary-->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<!-- ONLY if you render the LocationButton composable -->
<uses-permission android:name="android.permission.USE_LOCATION_BUTTON" />
<!-- ONLY if location is read while NO foreground service is running (e.g. geofencing).
     See "Migrating iOS Background Location" below before adding this. -->
<!-- <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" /> -->
```

> [!CAUTION]
> **Caution:** **Android Manifest Permissions** : The permissions request code will fail or crash if you do not explicitly declare the permissions it requests in `AndroidManifest.xml`. This is **Mandatory**. Conversely, never declare a permission that no code path requests or uses.

### Migrating iOS Background Location

iOS signals continuous tracking with `UIBackgroundModes: location`,
`allowsBackgroundLocationUpdates = true`, and/or `requestAlwaysAuthorization()`.
Decide what the migrated app actually does and pick **one** path:

| Migrated behavior | Android implementation | Manifest |
|---|---|---|
| Location only while a screen is visible | Request updates in a lifecycle-aware scope and remove them on dispose/stop. | `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION` only. **No** `ACCESS_BACKGROUND_LOCATION`. |
| User-initiated continuous tracking (e.g. a run or navigation session) that must survive the app leaving the screen | A `Service` started from the visible UI that calls `startForeground(...)` with an ongoing notification and owns the `requestLocationUpdates` / `removeLocationUpdates` lifecycle. | `<service android:foregroundServiceType="location" />`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION` (API 34+), `POST_NOTIFICATIONS` (API 33+). A foreground service started while the app is visible does **not** need `ACCESS_BACKGROUND_LOCATION`. |
| Location with no visible UI and no foreground service (geofencing, passive background updates) | Implement the feature, then request background access as a **separate second step** after foreground is granted (see `permissions-runtime.md` §5). | Add `ACCESS_BACKGROUND_LOCATION`. |

Never declare `ACCESS_BACKGROUND_LOCATION` or a location foreground service "for
parity" without also implementing the code path that uses it.

*** ** * ** ***

## Limitations

This skill has the following limitations:

1. This skill does not cover the `SettingsClient` API for checking if device location services are enabled globally.
2. Advanced background location optimization and battery throttling are not covered in depth.
3. The deprecated `android.location.LocationManager` framework is not covered.

*** ** * ** ***

## Glossary

| Term | Definition |
|---|---|
| `FusedLocationProviderClient` | The main entry point for interacting with the fused location provider in Google Play Services. |
| `Principle of Least Privilege` | The security principle of only granting the minimum level of permission required for a feature to function. |
| `LocationButton` | A specialized system UI/access pattern used to retrieve location at the exact point of use without requesting permanent fine location permissions. |
| `GeofencingClient` | The client used to add and remove geofences in Android. |

*** ** * ** ***

## Core Use Cases Addressed

1. **Location Services on iOS vs Location Permission on Android**
   - Translating iOS `CoreLocation` authorization workflows (`CLLocationManager` status checks, `requestWhenInUseAuthorization`) to Android runtime permission handling (`ContextCompat.checkSelfPermission` and `rememberLauncherForActivityResult`).
   - Understanding differences in permission granularity (iOS `When In Use` / `Always` vs Android Coarse (`ACCESS_COARSE_LOCATION`) / Fine (`ACCESS_FINE_LOCATION`) and Foreground / Background).
2. **Android location button \& Point-of-Use Access**
   - Implementing user-initiated location access via the Android [Location
     button](https://developer.android.com/guide/topics/permissions/private-alternatives/location-button) pattern as a specific mechanism to access location at the point of use matching iOS user interaction patterns.
3. **Progressive Permissions \& Principle of Least Privilege**
   - Accessing the lowest version of permission necessary to preserve user privacy.
   - Enforcing progressive permission disclosure: Requesting lowest sensitivity permissions first (Coarse before Fine, Foreground before Background).
4. **Geofencing \& Location-Based Monitoring**
   - Translating iOS region monitoring (`CLCircularRegion`) to Android `GeofencingClient`.

*** ** * ** ***

## 1. Location Services on iOS vs Location Permission on Android

### Architectural Mapping \& Authorization Models

| iOS (`CoreLocation`) | Android (`Fused Location Provider` \& Permissions) | Purpose |
|---|---|---|
| `CLLocationManager` | `FusedLocationProviderClient` | Main client entry point for location services. |
| `CLLocationManager.authorizationStatus()` | `ContextCompat.checkSelfPermission(...)` | Verifies current authorization/permission state. |
| `requestWhenInUseAuthorization()` | `rememberLauncherForActivityResult(...)` | Requests runtime permissions from the user. |
| `NSLocationWhenInUseUsageDescription` | `ACCESS_COARSE_LOCATION` / `ACCESS_FINE_LOCATION` | Required manifest permission declarations. |

### Implementation Example: Checking Permissions

- **Input:** Context and permission string (e.g., `Manifest.permission.ACCESS_COARSE_LOCATION`).
- **Output:** Boolean indicating whether permission is granted (`PackageManager.PERMISSION_GRANTED`).

**iOS (`CLLocationManager`)**
`swift
let status = CLLocationManager.authorizationStatus()
if status == .authorizedWhenInUse {
// Permission granted
}`

**Android (`ContextCompat.checkSelfPermission`)**
\`\`\`kotlin
// Check coarse location permission
val hasCoarsePermission = ContextCompat.checkSelfPermission(
context,
Manifest.permission.ACCESS_COARSE_LOCATION
) == PackageManager.PERMISSION_GRANTED

// Check fine location permission
val hasFinePermission = ContextCompat.checkSelfPermission(
context,
Manifest.permission.ACCESS_FINE_LOCATION
) == PackageManager.PERMISSION_GRANTED
\`\`\`

*** ** * ** ***

## 2. Android location button \& Point-of-Use Access

### Key Concepts \& Use Case Guidance

- **Specific Use Cases for the Location Button**: The location button pattern is best suited for specific use cases (such as one-time location sharing, "find me" map centering, or occasional user-initiated queries) where permanent runtime permission prompts are unnecessary or overly intrusive.
- **When Standard Runtime Permissions are Preferred** : Otherwise, standard runtime permission access (`ACCESS_COARSE_LOCATION` / `ACCESS_FINE_LOCATION`) via `rememberLauncherForActivityResult` is fully appropriate and expected when the app requires ongoing, persistent, or core location awareness.
- **Point-of-Use Principle** : You **Must Not** request location permissions at app startup. You **Must** always prompt or trigger location access at the point of use when the user taps a dedicated location button or control.

### Implementation Example: LocationButton Implementation

- **Input:** User interaction on a Compose `LocationButton` widget.
- **Output:** Location retrieval via `FusedLocationProviderClient.lastLocation` upon successful location retrieval callback.

```kotlin
// Example imports:
// import androidx.core.locationbutton.compose.LocationButton
// import androidx.core.locationbutton.compose.LocationButtonTextType

@Composable
fun LocationButtonScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var locationStatus by remember { mutableStateOf("Ready") }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = locationStatus)
        Spacer(modifier = Modifier.height(16.dp))

        // Android LocationButton Pattern (User-Initiated Point of Use with null fallback)
        LocationButton(
            textType = LocationButtonTextType.USE_PRECISE_LOCATION,
            onPermissionResult = { granted ->
                if (granted) {
                    try {
                        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                            if (loc != null) {
                                locationStatus = "Location retrieved: ${loc.latitude}, ${loc.longitude}"
                            } else {
                                // Fallback to fresh location request when lastLocation is null (e.g. fresh emulators)
                                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
                                fusedLocationClient.requestLocationUpdates(
                                    locationRequest,
                                    object : LocationCallback() {
                                        override fun onLocationResult(result: LocationResult) {
                                            val freshLoc = result.lastLocation
                                            if (freshLoc != null) {
                                                locationStatus = "Fresh Lat: ${freshLoc.latitude}, Lng: ${freshLoc.longitude}"
                                                fusedLocationClient.removeLocationUpdates(this)
                                            }
                                        }
                                    },
                                    Looper.getMainLooper()
                                )
                            }
                        }
                    } catch (e: SecurityException) {
                        locationStatus = "Security exception: ${e.message}"
                    }
                } else {
                    locationStatus = "Location permission denied."
                }
            }
        )
    }
}
```

*** ** * ** ***

## 3. Progressive Permissions \& Principle of Least Privilege

### Key Principles

1. **Coarse Location as Primary Path** : You **Must** consider coarse location (`ACCESS_COARSE_LOCATION`) as the primary implementation path if it satisfies the app's requirements. You **Must Not** request fine location (`ACCESS_FINE_LOCATION`) unless core feature functionality strictly dictates higher precision.
2. **Progressive Disclosure** :
   - You **Must** request **Coarse Location** (`ACCESS_COARSE_LOCATION`) before requesting **Fine Location** (`ACCESS_FINE_LOCATION`).
   - You **Must** request **Foreground Location** access before attempting to request **Background Location** access (`ACCESS_BACKGROUND_LOCATION`).

### Implementation Example: Progressive Permission Request

- **Input:** User click triggering `ActivityResultContracts.RequestPermission()`.
- **Output:** Boolean result indicating user grant status for the requested permission level.

```kotlin
@Composable
fun ProgressivePermissionScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var permissionStatus by remember { mutableStateOf("Permission not requested") }

    // Progressive permission launcher: request coarse location first (least privilege)
    val coarsePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            permissionStatus = "Coarse location granted (Least privilege preserved)."
        } else {
            permissionStatus = "Coarse permission denied."
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = permissionStatus)
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            // Request lowest sensitivity permission first
            coarsePermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }) {
            Text(text = stringResource(R.string.request_coarse_location))
        }
    }
}
```

*** ** * ** ***

## 4. Geofencing \& Location-Based Systems

### Architectural Mapping

| iOS (`CoreLocation`) | Android (`GeofencingClient`) | Purpose |
|---|---|---|
| `CLCircularRegion` | `Geofence.Builder` | Defines latitude, longitude, radius, and transition types. |
| `CLLocationManager.startMonitoring(for:)` | `GeofencingClient.addGeofences()` | Registers geofences with Google Play Services. |
| Delegate callbacks (`didEnterRegion`) | `BroadcastReceiver` / `IntentService` | Triggered when user enters or exits a geofence. |

### Implementation Example: Adding a Geofence

- **Input:** Context, unique `requestId` string, latitude/longitude/radius for the geofence.
- **Output:** A `PendingIntent` is registered with `GeofencingClient`. The operation result is handled via `addOnSuccessListener` and `addOnFailureListener`.

```kotlin
val geofencingClient = LocationServices.getGeofencingClient(context)

val geofence = Geofence.Builder()
    .setRequestId("target_location_id")
    .setCircularRegion(37.4219999, -122.084, 100f) // Lat, Lng, Radius in meters
    .setExpirationDuration(Geofence.NEVER_EXPIRE)
    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
    .build()

val geofencingRequest = GeofencingRequest.Builder()
    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
    .addGeofence(geofence)
    .build()

val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
val pendingIntent = PendingIntent.getBroadcast(
    context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.MUTABLE
)

// Add geofences with permission check
try {
    geofencingClient.addGeofences(geofencingRequest, pendingIntent).run {
        addOnSuccessListener {
            // Geofence added successfully
        }
        addOnFailureListener { e ->
            // Failed to add geofence
        }
    }
} catch (e: SecurityException) {
    // Handle missing permissions
}
```

*** ** * ** ***

## Antipatterns

### DO

- **DO** request location permissions strictly at the point of use via the Android [Location button](https://developer.android.com/guide/topics/permissions/private-alternatives/location-button) pattern when the user initiates a location action.
- **DO** request coarse location (`ACCESS_COARSE_LOCATION`) before fine location (`ACCESS_FINE_LOCATION`) to honor progressive disclosure.
- **DO** handle null locations gracefully from `lastLocation` by triggering a fresh location request when needed.

### DON'T

- **DON'T** request location permissions automatically upon app startup or splash screen display.
- **DON'T** request background location access (`ACCESS_BACKGROUND_LOCATION`) upfront without first obtaining foreground location permission.
- **DON'T** declare `ACCESS_BACKGROUND_LOCATION` just because the iOS app used `UIBackgroundModes: location` / `requestAlwaysAuthorization()`. If location updates stop when the UI goes away, the permission is unused; if tracking must continue, implement a `foregroundServiceType="location"` service instead (see "Migrating iOS Background Location").

### NEVER

- **NEVER** ignore `SecurityException` when calling location APIs without verifying permission status beforehand.
- **NEVER** request high-precision fine location when coarse location satisfies the feature's requirements.