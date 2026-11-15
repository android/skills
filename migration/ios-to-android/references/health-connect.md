> [!IMPORTANT]
> **Important:** **ORCHESTRATION GATEWAY PREREQUISITE** : This document is a supplemental reference guide for [`../SKILL.md`](https://developer.android.com/agents/skills/migration/ios-to-android/skill). You **MUST** read and follow the primary orchestration protocol in `../SKILL.md` before applying any patterns from this guide. `SKILL.md` enforces mandatory incremental compilation gates (`./gradlew compileDebugKotlin`), vertical feature slicing, and strict anti-premature-exit execution discipline.

This skill maps iOS `HealthKit` constructs and workflows to their Android
equivalents using the Health Connect library.

**Official Reference Links:**

- [Health Connect overview \| Android Developers](https://developer.android.com/health-and-fitness/health-connect)
- [Get started with Health Connect \| Android Developers](https://developer.android.com/health-and-fitness/health-connect/get-started)
- [Read data \| Health Connect \| Android Developers](https://developer.android.com/health-and-fitness/health-connect/read-data)

## Prerequisites

Before using this skill, you **Must** ensure the following prerequisites are
met:

- **Source Context:** Access to the existing iOS implementation using `HealthKit` is **Mandatory**.
- **API Requirements:** Health Connect requires Android 14 (API 34) integrated, or Android 8.0+ via the Health Connect app.

## Limitations

This skill has the following limitations:

- **Health Connect Focus:** Examples are tailored exclusively for the Health Connect API, which is the modern replacement for Google Fit. It does not cover legacy Google Fit APIs or custom low-level implementations.
- **Data Types:** This skill covers only `StepsRecord`, `DistanceRecord`, `HeartRateRecord`, and `SleepSessionRecord`. All other Health Connect data types (e.g., nutrition) are not covered.

## Glossary

| iOS Term (`HealthKit`) | Android Equivalent (`Health Connect` / `Health Services`) | Definition |
|---|---|---|
| `HKHealthStore` | `HealthConnectClient` | The entry point for interacting with historical health data repository. |
| `HKWorkoutSession` / Live Sensor | `ExerciseClient` / `MeasureClient` (Health Services) | Real-time workout tracking and live sensor streaming on Wear OS / wearable devices. |
| `HKQuery` | `ReadRecordsRequest` | A request to read specific health records from Health Connect. |
| `HKObjectType` | `Record` | The base class for all health data types in Health Connect. |
| `HKQuantityType` | Specific `Record` subclasses | Types like `StepsRecord`, `DistanceRecord`. |
| `HKSampleQuery` | `healthConnectClient.readRecords` | Querying for specific sample data. |

## Architecture: Health Connect vs. Health Services

When translating an iOS `HealthKit` implementation to Android, you **Must**
distinguish between historical repository data and real-time sensor streaming:

1. **Health Connect (`androidx.health.connect:connect-client`)** :
   - **Purpose:** Central on-device repository for historical health and wellness records, aggregation, and cross-app sharing (equivalent to iOS `HKHealthStore`).
   - **Use Cases:** Reading/writing steps, daily sleep summaries, historical heart rate logs, distance records.
2. **Health Services (`androidx.health.services:services-client`)** :
   - **Purpose:** Real-time sensor streaming, active workout sessions, and background health monitoring on Wear OS and wearable devices (equivalent to iOS `HKWorkoutSession` and `HKLiveWorkoutBuilder`).
   - **Use Cases:**
     - `ExerciseClient`: Tracking live workouts (reporting live heart rate, steps, GPS distance during exercise).
     - `MeasureClient`: Taking point-in-time sensor measurements on demand.
     - `PassiveMonitoringClient`: Background tracking for daily goals.

## Setup \& Dependencies

You **Must** add the following dependencies to your `build.gradle.kts` file to
enable Health Connect:

```kotlin
dependencies {
    val healthConnectVersion = "1.1.0"
    implementation("androidx.health.connect:connect-client:$healthConnectVersion")
}
```

You **Must** also declare the permissions and package visibility queries you
need in your `AndroidManifest.xml`. For example:

```xml
<uses-permission android:name="android.permission.health.READ_STEPS" />
<uses-permission android:name="android.permission.health.WRITE_STEPS" />
<uses-permission android:name="android.permission.health.READ_DISTANCE" />
<uses-permission android:name="android.permission.health.WRITE_DISTANCE" />
<uses-permission android:name="android.permission.health.READ_HEART_RATE" />
<uses-permission android:name="android.permission.health.WRITE_HEART_RATE" />

<!-- Mandatory for Health Connect provider visibility on Android 14+ (API 34+) -->
<queries>
    <package android:name="com.google.android.apps.healthdata" />
    <package android:name="com.google.android.healthconnect.controller" />
    <intent>
        <action android:name="androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE" />
    </intent>
</queries>
```

You **Must** also declare an `<activity-alias>` for permissions rationale in
`AndroidManifest.xml` so Health Connect recognizes your app as a valid health
provider and lists it under Health Apps:

```xml
<activity
    android:name=".MainActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
    <intent-filter>
        <action android:name="androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE" />
    </intent-filter>
    <intent-filter>
        <action android:name="android.intent.action.VIEW_PERMISSION_USAGE" />
        <category android:name="android.intent.category.HEALTH_PERMISSIONS" />
    </intent-filter>
</activity>

<!-- Mandatory activity-alias for Health Connect app recognition -->
<activity-alias
    android:name="ViewPermissionUsageActivity"
    android:exported="true"
    android:targetActivity=".MainActivity"
    android:permission="android.permission.START_VIEW_PERMISSION_USAGE">
    <intent-filter>
        <action android:name="android.intent.action.VIEW_PERMISSION_USAGE" />
        <category android:name="android.intent.category.HEALTH_PERMISSIONS" />
    </intent-filter>
    <intent-filter>
        <action android:name="androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE" />
    </intent-filter>
</activity-alias>
```

> [!CAUTION]
> **Caution:** **Health Connect Permissions**: Health Connect permissions are sensitive and must be declared in the manifest AND requested at runtime. Failure to do so will result in security exceptions.

### Requesting Health Connect Permissions at the Point of Use

Declaring a launcher is not enough --- it **MUST** be invoked by the action that
reads health data. Check granted permissions first, request if anything is
missing, and only fall back to sample data after denial or when the SDK is
unavailable:

```kotlin
@Composable
fun HealthSyncButton(viewModel: HealthViewModel, client: HealthConnectClient?) {
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(HealthManager.PERMISSIONS)) viewModel.syncHealthData()
        else viewModel.useSampleData() // user denied
    }
    Button(onClick = {
        if (client == null) { viewModel.useSampleData(); return@Button } // SDK unavailable
        scope.launch {
            val granted = client.permissionController.getGrantedPermissions()
            if (granted.containsAll(HealthManager.PERMISSIONS)) viewModel.syncHealthData()
            else launcher.launch(HealthManager.PERMISSIONS)
        }
    }) { Text(stringResource(R.string.sync_health)) }
}
```

## Instructions: Mapping Concepts

### 1. Initializing the Client \& Checking SDK Status

Map iOS `HKHealthStore` initialization to Android `HealthConnectClient`
retrieval. You **Must** check SDK availability via
`HealthConnectClient.getSdkStatus(context)` before initializing or requesting
permissions.

- **Input:** Application context.
- **Output:** `HealthConnectClient` instance (if available) or SDK status error handling.

**iOS**
`swift
let healthStore = HKHealthStore()`

**Android**
\`\`\`kotlin
val context = LocalContext.current
val sdkStatus = HealthConnectClient.getSdkStatus(context)

val healthConnectClient = if (sdkStatus == HealthConnectClient.SDK_AVAILABLE) {
HealthConnectClient.getOrCreate(context)
} else {
// Handle SDK_UNAVAILABLE (e.g., prompt user to install Health Connect from Play Store)
null
}
\`\`\`

### 2. Requesting Permissions (User-Initiated \& BAL Compliant)

Map iOS HealthKit authorization to Android Health Connect permission requests.

> [!IMPORTANT]
> **Important:** **Android 14+ Background Activity Launch (BAL) Rule:** When launching the Health Connect permission dialog (`permissionLauncher.launch(permissions)`), you **Must** invoke the launcher synchronously inside the UI gesture handler (e.g., `Button(onClick = { permissionLauncher.launch(permissions) })`). **Never** call `.launch()` after an asynchronous suspending call (`getGrantedPermissions()`) inside a coroutine, or Android 14+ BAL restrictions will silently reject the dialog launch!

> [!TIP]
> **Tip:** **Physical Device \& Sideloaded APK Rule:** For non-Play Store debug builds, users must either enable **Developer mode** in Health Connect settings (`Settings > Security & privacy > Health Connect > Developer mode`) or use an explicit **"Open Health Connect Settings"** button (`androidx.health.ACTION_HEALTH_CONNECT_SETTINGS`) so they can grant permissions directly in system settings.

- **Input:** Set of permissions to request.
- **Output:** Permissions granted or denied.

**Android Code Example:**
\`\`\`kotlin
@Composable
fun HealthPermissionScreen(modifier: Modifier = Modifier) {
val context = LocalContext.current
// Status holds a string resource ID; the UI resolves it with stringResource().
var statusRes by remember { mutableIntStateOf(R.string.hc_status_ready) }

    val sdkStatus = remember { HealthConnectClient.getSdkStatus(context) }
    val healthConnectClient = remember {
        if (sdkStatus == HealthConnectClient.SDK_AVAILABLE) HealthConnectClient.getOrCreate(context) else null
    }

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class)
    )

    val requestPermissionContract = PermissionController.createRequestPermissionResultContract()
    val permissionLauncher = rememberLauncherForActivityResult(requestPermissionContract) { grantedPermissions ->
        if (grantedPermissions.containsAll(permissions)) {
            statusRes = R.string.hc_status_granted
        } else {
            statusRes = R.string.hc_status_use_settings
        }
    }

    Column {
        Button(onClick = {
            if (healthConnectClient != null) {
                // CRITICAL: Launch synchronously on UI thread with direct user gesture token!
                // Do NOT wrap in coroutineScope.launch after a suspending call to comply with Android 14+ BAL rules.
                permissionLauncher.launch(permissions)
            } else {
                statusRes = R.string.hc_status_unavailable
            }
        }) {
            Text(stringResource(R.string.hc_request_permissions))
        }

        Button(onClick = {
            try {
                val intent = Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS")
                context.startActivity(intent)
            } catch (e: Exception) {
                statusRes = R.string.hc_status_settings_unavailable
            }
        }) {
            Text(stringResource(R.string.hc_open_settings))
        }
    }

}
\`\`\`

### 3. Reading Steps Data

Map iOS `HKSampleQuery` for steps to Android `readRecords`.

- **Input:** Time range and data type.
- **Output:** List of `StepsRecord`.

**iOS**
\`\`\`swift
let stepsType = HKQuantityType.quantityType(forIdentifier: .stepCount)!
let predicate = HKQuery.predicateForSamples(withStart: startDate, end: endDate, options: .strictStartDate)

let query = HKSampleQuery(sampleType: stepsType, predicate: predicate, limit: HKObjectQueryNoLimit, sortDescriptors: nil) { (query, samples, error) in
// Process samples
}
healthStore.execute(query)
\`\`\`

**Android**
\`\`\`kotlin
val request = ReadRecordsRequest(
recordType = StepsRecord::class,
timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
)

val response = healthConnectClient.readRecords(request)
for (stepRecord in response.records) {
val steps = stepRecord.count
// Process steps
}
\`\`\`

### 4. Reading Distance and Heart Rate

Map iOS queries for distance and heart rate to Android equivalents.

- **Input:** Time range and data type.
- **Output:** List of records.

**iOS (Distance)**
`swift
let distanceType = HKQuantityType.quantityType(forIdentifier: .distanceWalkingRunning)!
let predicate = HKQuery.predicateForSamples(withStart: startDate, end: endDate, options: .strictStartDate)
let query = HKSampleQuery(sampleType: distanceType, predicate: predicate, limit: HKObjectQueryNoLimit, sortDescriptors: nil) { (query, samples, error) in
// Process samples
}
healthStore.execute(query)`

**Android Code Example (Distance):**
\`\`\`kotlin
val request = ReadRecordsRequest(
recordType = DistanceRecord::class,
timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
)

val response = healthConnectClient.readRecords(request)
for (distanceRecord in response.records) {
val distanceMeters = distanceRecord.distance.inMeters
// Process distance
}
\`\`\`

**iOS (Heart Rate)**
`swift
let heartRateType = HKQuantityType.quantityType(forIdentifier: .heartRate)!
let predicate = HKQuery.predicateForSamples(withStart: startDate, end: endDate, options: .strictStartDate)
let query = HKSampleQuery(sampleType: heartRateType, predicate: predicate, limit: HKObjectQueryNoLimit, sortDescriptors: nil) { (query, samples, error) in
// Process samples
}
healthStore.execute(query)`

**Android Code Example (Heart Rate):**
\`\`\`kotlin
val request = ReadRecordsRequest(
recordType = HeartRateRecord::class,
timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
)

val response = healthConnectClient.readRecords(request)
for (heartRateRecord in response.records) {
val samples = heartRateRecord.samples
for (sample in samples) {
val bpm = sample.beatsPerMinute
// Process heart rate
}
}
\`\`\`

### 5. Aggregating Data

Map iOS statistics queries to Android `aggregate` requests. This is the
recommended way to get sums or averages.

- **Input:** Time range and metrics to aggregate.
- **Output:** Aggregated values.

**iOS**
`swift
let stepsType = HKQuantityType.quantityType(forIdentifier: .stepCount)!
let predicate = HKQuery.predicateForSamples(withStart: startDate, end: endDate, options: .strictStartDate)
let query = HKStatisticsQuery(quantityType: stepsType, quantitySamplePredicate: predicate, options: .cumulativeSum) { (query, statistics, error) in
if let sum = statistics?.sumQuantity() {
let totalSteps = sum.doubleValue(for: HKUnit.count())
// Process total steps
}
}
healthStore.execute(query)`

**Android Code Example:**
\`\`\`kotlin
val response = healthConnectClient.aggregate(
AggregateRequest(
metrics = setOf(StepsRecord.COUNT_TOTAL, DistanceRecord.DISTANCE_TOTAL),
timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
)
)

val totalSteps = response\[StepsRecord.COUNT_TOTAL\]
val totalDistance = response\[DistanceRecord.DISTANCE_TOTAL\]
\`\`\`

### 6. Writing Distance and Heart Rate Data

Map iOS writing samples to Android `insertRecords` for Distance and Heart Rate.

- **Input:** Records with values and time ranges.
- **Output:** Confirmation of insertion.

**Android Code Example (Distance):**
\`\`\`kotlin
val distanceRecord = DistanceRecord(
distance = 1000.meters,
startTime = startTime,
endTime = endTime,
startZoneOffset = ZoneOffset.UTC,
endZoneOffset = ZoneOffset.UTC
)

val response = healthConnectClient.insertRecords(listOf(distanceRecord))
// Handle response.recordIds
\`\`\`

**Android Code Example (Heart Rate):**
\`\`\`kotlin
val heartRateRecord = HeartRateRecord(
startTime = startTime,
endTime = endTime,
startZoneOffset = ZoneOffset.UTC,
endZoneOffset = ZoneOffset.UTC,
samples = listOf(
HeartRateRecord.HeartRateSample(
beatsPerMinute = 72,
time = startTime
),
HeartRateRecord.HeartRateSample(
beatsPerMinute = 80,
time = endTime
)
)
)

val response = healthConnectClient.insertRecords(listOf(heartRateRecord))
// Handle response.recordIds
\`\`\`

### 7. Reading and Writing Sleep Data

Map iOS `categoryType` for sleep to Android `SleepSessionRecord`.

- **Input:** Time range.
- **Output:** Sleep records.

**iOS Reading**
`swift
let sleepType = HKObjectType.categoryType(forIdentifier: .sleepAnalysis)!
let predicate = HKQuery.predicateForSamples(withStart: startDate, end: endDate, options: .strictStartDate)
let query = HKSampleQuery(sampleType: sleepType, predicate: predicate, limit: HKObjectQueryNoLimit, sortDescriptors: nil) { (query, samples, error) in
// Process samples
}
healthStore.execute(query)`

**Android Reading**
\`\`\`kotlin
val request = ReadRecordsRequest(
recordType = SleepSessionRecord::class,
timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
)

val response = healthConnectClient.readRecords(request)
for (sleepRecord in response.records) {
val startTime = sleepRecord.startTime
val endTime = sleepRecord.endTime
// Process sleep session
}
\`\`\`

**Android Writing**
\`\`\`kotlin
val sleepRecord = SleepSessionRecord(
startTime = startTime,
endTime = endTime,
startZoneOffset = ZoneOffset.UTC,
endZoneOffset = ZoneOffset.UTC
)

val response = healthConnectClient.insertRecords(listOf(sleepRecord))
// Handle response.recordIds
\`\`\`

### 8. Error Handling

Health Connect operations can fail due to various reasons. You **Must** handle
these errors gracefully.

| Error Scenario | Description | Recovery Strategy |
|---|---|---|
| `SDK_UNAVAILABLE` | Health Connect is not installed or available. | Prompt the user to install Health Connect from the Play Store. |
| `SDK_DISABLED` | Health Connect is disabled by the user. | Guide the user to enable Health Connect in system settings. |
| `SecurityException` | Permission not granted. | Request permissions using the contract or check status. |
| `RemoteException` | IPC communication failed. | Retry the operation or log the error. |

## Antipatterns

### DO

- **DO** check if Health Connect is available on the device before calling any APIs using `HealthConnectClient.getSdkStatus(context)`.
- **DO** handle the case where the user revokes permissions in the Health Connect app settings.
- **DO** use `AggregateRequest` for querying sums or averages instead of reading all records and summing manually.

### DON'T

- **DON'T** assume Health Connect is installed on pre-Android 14 devices. Prompt the user to install it if missing.
- **DON'T** write records with overlapping time ranges for the same app unless intended (e.g., duplicate entries).

### NEVER

- **NEVER** block the main thread with health data queries. Always run them in a coroutine or background thread.
- **NEVER** request permissions repeatedly if the user has denied them without providing clear rationale first.