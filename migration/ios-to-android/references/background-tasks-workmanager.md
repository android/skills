> [!IMPORTANT]
> **Important:** **ORCHESTRATION GATEWAY PREREQUISITE** : This document is a supplemental reference guide for [`../SKILL.md`](https://developer.android.com/agents/skills/migration/ios-to-android/skill). You **MUST** read and follow the primary orchestration protocol in `../SKILL.md` before applying any patterns from this guide. `SKILL.md` enforces mandatory incremental compilation gates (`./gradlew compileDebugKotlin`), vertical feature slicing, and strict anti-premature-exit execution discipline.

This skill maps iOS `BackgroundTasks` (using `BGTaskScheduler`) to Android
`WorkManager`, focusing on modern Jetpack practices and respecting Android's
strict background execution limits.

**Official Reference Links:**

- [Schedule tasks with WorkManager \| Android Developers](https://developer.android.com/develop/background-work/background-tasks/persistent)
- [Define work requests \| Android Developers](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work)
- [Test your WorkManager implementation \| Android Developers](https://developer.android.com/develop/background-work/background-tasks/testing/persistent/integration-testing)

## Prerequisites

Before using this skill, you **Must** ensure the following prerequisites are
met:

- **Source Context:** Access to the existing iOS implementation using `BGTaskScheduler` or `AppDelegate` background fetch is **Mandatory**.
- **Software Requirements:** Android SDK API Level 34+ (`compileSdk = 34`, required for Android 14 mandatory foreground service type declarations), Android Gradle Plugin (AGP) 8.0+, Kotlin 1.8+, and Jetpack WorkManager (`androidx.work:work-runtime-ktx:2.9.0`) are **Mandatory**.
- **Dependency:** You **Must** have the `androidx.work:work-runtime-ktx` dependency added to your `build.gradle.kts`.

## Limitations

This skill has the following limitations:

- **Exact Timing:** `WorkManager` does not guarantee exact execution timing. It is subject to OS-level optimizations, including Doze mode and App Standby Buckets. You **Must Not** use it for tasks requiring immediate or precise timing (use Foreground Services or AlarmManager instead).
- **Foreground Services:** This skill does not cover continuous foreground processing (e.g., active navigation or music playback).

## Glossary

| iOS Term | Android Equivalent | Definition |
|---|---|---|
| `BGTaskScheduler` | `WorkManager` | The system service that manages background work. |
| `BGAppRefreshTask` | `PeriodicWorkRequest` | A request for work that repeats periodically. |
| `BGProcessingTask` | `OneTimeWorkRequest` | A request for work that runs exactly once. |
| `Info.plist` identifiers | `Worker` classes | In Android, tasks are defined by creating specific `Worker` classes rather than registering IDs in a plist. |

## Setup \& Dependencies

You **Must** add the following dependency to your `build.gradle.kts` file:

```kotlin
dependencies {
   val workVersion = "2.8.1"
   implementation("androidx.work:work-runtime-ktx:$workVersion")

   // For testing WorkManager
   androidTestImplementation("androidx.work:work-testing:$workVersion")
}
```

## Instructions: Mapping Concepts

### 1. Configuration \& Submission

#### Registering Tasks

- **iOS** : Register task identifiers in `Info.plist` and call `BGTaskScheduler.shared.register`.
- **Android** : Inherently handled by declaring standard `Worker` classes and enqueuing them. No manifest registration for specific tasks is required.

#### Submitting Tasks

- **iOS** : `BGTaskScheduler.shared.submit(request)`
- **Android** : `WorkManager.getInstance(context).enqueue(workRequest)`

#### Setting Criteria/Constraints

- **iOS** : `request.requiresNetworkConnectivity = true`
- **Android** : `Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()`

### 2. Code Examples

#### iOS (Reference)

```swift
let request = BGAppRefreshTaskRequest(identifier: "com.example.refresh")
request.earliestBeginDate = Date(timeIntervalSinceNow: 15 * 60)
try? BGTaskScheduler.shared.submit(request)
```

#### Android (Target)

You **Must** use `CoroutineWorker` for Kotlin-first background work.

```kotlin
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit


// 1. Define the Worker
class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
   CoroutineWorker(appContext, workerParams) {

   override suspend fun doWork(): Result {
       // Perform the background sync here
       val success = performSync()

       return if (success) {
           Result.success()
       } else {
           Result.retry() // Triggers exponential backoff
       }
   }

   private suspend fun performSync(): Boolean {
       // Implementation
       return true
   }
}


// 2. Enqueue the Work
fun scheduleSync(context: Context) {
   // You Must evaluate the task's impact on battery.
   // If the task is non-critical and can be deferred until the device is charging,
   // you Must apply the constraint: .setRequiresCharging(true).
   val constraints = Constraints.Builder()
       .setRequiredNetworkType(NetworkType.CONNECTED)
       .setRequiresCharging(true)
       .build()

   val syncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
       .setConstraints(constraints)
       .setInitialDelay(15, TimeUnit.MINUTES)
       .build()

   WorkManager.getInstance(context).enqueue(syncWorkRequest)
}
```

### 3. Production Insight: Centralizing Background Logic

In large-scale production applications dealing with offline sync:

- **iOS Real-World Implementation:** Employs `BGTaskScheduler` natively, registered within `AppDelegate`, invoking single manager classes (like a `BackgroundSyncManager.swift`) to orchestrate downloads.
- **Android Real-World Equivalent:** Android completely offloads this to Jetpack `WorkManager`. Instead of a single manager handling multiple sync flows, the architecture uses discrete `Worker` classes tailored for specialized tasks (e.g., `RefreshDataWorker`, `DownloadContentWorker`, `SyncWorker`).

**Key Takeaway:** Do not try to replicate a monolithic `BackgroundSyncManager`
inside an Android `Service`. Break down the iOS background operations into
discrete, chainable `Worker` classes for reliable execution under Android's
strict background execution limits.

### 4. Chaining Work

WorkManager allows for a sequence of dependent tasks (e.g., A must finish before
B starts) using `.beginWith(...).then(...)`. This declarative chaining is not a
built-in feature of the base iOS `BGTaskScheduler` API.

```kotlin
val cleanupRequest = OneTimeWorkRequestBuilder<CleanupWorker>().build()
val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()


WorkManager.getInstance(context)
   .beginWith(cleanupRequest)
   .then(syncRequest)
   .enqueue()
```

### 5. Expedited \& Foreground Work on Android 14+ (API 34+)

When long-running work requires foreground execution (e.g. active sync, media
processing), you **MUST** configure `ForegroundInfo` and declare the mandatory
`android:foregroundServiceType` attribute in `AndroidManifest.xml`:

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
<!-- or FOREGROUND_SERVICE_HEALTH, FOREGROUND_SERVICE_LOCATION based on capability -->

<application ...>
    <service
        android:name="androidx.work.impl.foreground.SystemForegroundService"
        android:foregroundServiceType="dataSync"
        android:exported="false" />
</application>
```

In your `CoroutineWorker`, set foreground info with the matching notification
and type:

```kotlin
override suspend fun doWork(): Result {
    setForeground(getForegroundInfo())
    return performSync()
}

private fun getForegroundInfo(): ForegroundInfo {
    val notification = createNotification()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    } else {
        ForegroundInfo(NOTIFICATION_ID, notification)
    }
}
```

### 5. Logic: Choosing the Right WorkRequest

To determine the correct Android `WorkRequest`, you **Must** follow this logic:

- **IF** the iOS task is a one-time, deferrable operation (e.g., `BGProcessingTask` for database cleanup), **THEN** you **Must** use a `OneTimeWorkRequest`.
- **ELSE IF** the iOS task is for fetching fresh content on a recurring schedule (e.g., `BGAppRefreshTask`), **THEN** you **Must** use a `PeriodicWorkRequest`.

### 6. Unique Work (Avoiding Duplicates)

To prevent multiple instances of the same task from running concurrently or
queuing up unnecessarily, you **Must** use Unique Work.

```kotlin
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy


// For OneTime Work
WorkManager.getInstance(context).enqueueUniqueWork(
   "uniqueSyncTask",
   ExistingWorkPolicy.KEEP, // Keep existing, ignore new
   syncWorkRequest
)


// For Periodic Work
WorkManager.getInstance(context).enqueueUniquePeriodicWork(
   "uniquePeriodicSyncTask",
   ExistingPeriodicWorkPolicy.KEEP,
   periodicSyncWorkRequest
)
```

### 7. Observing Work State

You can observe the state of work to update the UI or trigger other actions.

```kotlin
WorkManager.getInstance(context)
   .getWorkInfoByIdLiveData(syncWorkRequest.id)
   .observe(lifecycleOwner) { workInfo ->
       if (workInfo != null && workInfo.state == WorkInfo.State.SUCCEEDED) {
           // Show success message
       }
   }
```

### 8. Periodic Work with Flex Interval

If you need to run work periodically but want to restrict it to a specific
window within the period (e.g., last 15 minutes of every hour), use flex
intervals.

```kotlin
val periodicWork = PeriodicWorkRequestBuilder<SyncWorker>(
   1, TimeUnit.HOURS, // Repeat interval
   15, TimeUnit.MINUTES // Flex interval
).build()
```

### 9. Expedited Work

For tasks that need to run immediately but are still subject to background
constraints, use Expedited Work. This is useful for tasks like sending a message
or uploading a file that the user just initiated.

```kotlin
import androidx.work.OutOfQuotaPolicy


val request = OneTimeWorkRequestBuilder<SyncWorker>()
   .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
   .build()

WorkManager.getInstance(context).enqueue(request)
```

### 10. Testing Workers

You **Must** use `work-testing` to verify your workers behave correctly without
waiting for system triggers.

```kotlin
@Test
fun testSyncWorker() {
   val context = ApplicationProvider.getApplicationContext<Context>()
   val worker = TestListenableWorkerBuilder<SyncWorker>(context).build()

   runBlocking {
       val result = worker.doWork()
       assertThat(result, `is`(ListenableWorker.Result.success()))
   }
}


@Test
fun testSyncWorkerWithInput() {
   val context = ApplicationProvider.getApplicationContext<Context>()
   val inputData = Data.Builder()
       .putString("KEY_URL", "https://example.com/api")
       .build()

   val worker = TestListenableWorkerBuilder<SyncWorker>(context)
       .setInputData(inputData)
       .build()

   runBlocking {
       val result = worker.doWork()
       assertThat(result, `is`(ListenableWorker.Result.success()))
   }
}
```

### 11. Decision Table: WorkManager vs AlarmManager

You **Must** use this table to evaluate whether to use `WorkManager` or
`AlarmManager` for background tasks:

| Requirement | Recommended API | Rationale |
|---|---|---|
| Exact timing required (e.g., calendar alarm). | **AlarmManager** | Guarantees execution at the specified time, bypassing Doze mode if necessary. |
| Deferrable work with constraints (e.g., sync when charging). | **WorkManager** | Optimizes battery by batching work and respecting OS constraints. |
| High-priority task needing immediate execution. | **WorkManager (Expedited)** | Runs immediately while still respecting system quotas. |

### 12. Data Handling: Input \& Output

You **Must** use `Data` objects to pass parameters to and return results from
Workers.

```kotlin
// Passing Input Data
val inputData = Data.Builder()
   .putString("KEY_ACTION", "SYNC")
   .build()


val request = OneTimeWorkRequestBuilder<SyncWorker>()
   .setInputData(inputData)
   .build()


// Returning Output Data in Worker
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
   override suspend fun doWork(): Result {
       val action = inputData.getString("KEY_ACTION")
       // ...
       val outputData = Data.Builder()
           .putInt("KEY_ITEMS_SYNCED", 42)
           .build()
       return Result.success(outputData)
   }
}
```

### 13. Backoff \& Retry Policies

You **Must** evaluate the retry strategy for failing tasks. Use
`.setBackoffCriteria()` to control retry behavior.

```kotlin
val request = OneTimeWorkRequestBuilder<SyncWorker>()
   .setBackoffCriteria(
       BackoffPolicy.EXPONENTIAL,
       OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
       TimeUnit.MILLISECONDS
   )
   .build()
```

### 14. Handling Long-Running Tasks (Foreground Workers)

If a task exceeds the 10-minute execution limit (e.g., downloading a large
file), you **Must** promote the Worker to a Foreground Service using
`setForeground()`.

```kotlin
class DownloadWorker(appContext: Context, workerParams: WorkerParameters) :
   CoroutineWorker(appContext, workerParams) {

   override suspend fun doWork(): Result {
       // Promote to foreground service
       setForeground(createForegroundInfo())

       // Perform long-running download
       return Result.success()
   }

   private fun createForegroundInfo(): ForegroundInfo {
       val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
           .setContentTitle("Downloading...")
           .setSmallIcon(R.drawable.ic_download)
           .build()
       return ForegroundInfo(NOTIFICATION_ID, notification)
   }
}
```

## Antipatterns

### DO

- **DO** use `CoroutineWorker` for modern Kotlin apps.
- **DO** use `Result.retry()` to leverage WorkManager's automatic exponential backoff.
- **DO** chain Worker classes together using `WorkManager.beginWith(...).then(...).enqueue()` to manage dependencies.
- **DO** specify constraints (like requiring network) to prevent waste of battery.
- **DO** use Unique Work to avoid duplicate task execution.
- **DO** use Expedited Work for high-priority tasks that need to run immediately.
- **DO** evaluate task impact on battery and use constraints appropriately.

### DON'T

- **DON'T** use `WorkManager` for tasks that must run at an exact time. Use `AlarmManager`.
- **DON'T** put large amounts of code directly in the `Activity` to trigger background work; delegate to a repository or UseCase.
- **DON'T** assume background work will run immediately unless marked as expedited.
- **DON'T** pass large amounts of data in the `Data` object (limit is 10KB). Use a database instead.

### NEVER

- **NEVER** attempt to replicate a monolithic `BackgroundSyncManager` from iOS inside a single Android `Service` or `Worker`.
- **NEVER** block the thread in `doWork()` of a `CoroutineWorker`. Use suspending functions.
- **NEVER** use `WorkManager` for tasks that require foreground execution guarantees (like active location tracking). Use a Foreground Service.