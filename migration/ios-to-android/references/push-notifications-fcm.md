> [!IMPORTANT]
> **Important:** **ORCHESTRATION GATEWAY PREREQUISITE** : This document is a supplemental reference guide for [`../SKILL.md`](https://developer.android.com/agents/skills/migration/ios-to-android/skill). You **MUST** read and follow the primary orchestration protocol in `../SKILL.md` before applying any patterns from this guide. `SKILL.md` enforces mandatory incremental compilation gates (`./gradlew compileDebugKotlin`), vertical feature slicing, and strict anti-premature-exit execution discipline.

This skill guides the migration of notification systems from iOS
(UserNotifications framework) to Android (NotificationManager and Firebase Cloud
Messaging). You **Must** use this skill when porting apps that rely on local
alerts, remote push notifications, or background content fetches triggered by
notifications.

**Official Reference Links:**

- [Notifications overview \| Android Developers](https://developer.android.com/develop/ui/views/notifications)
- [Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging)
- [Create and manage notification channels \| Android Developers](https://developer.android.com/develop/ui/compose/notifications/channels)

## Prerequisites

Before initiating the notification migration, you **Must** ensure the following
prerequisites are met:

- **Source Context:** Access to the iOS codebase handling `UNUserNotificationCenter` and `UIApplicationDelegate` remote notification callbacks is **Mandatory**.
- **Firebase Project:** A configured Firebase project with FCM enabled for the Android app is **Mandatory** for remote push notifications.
- **Software Requirements:** Android SDK API Level 33+ (`compileSdk = 33`, required for runtime `POST_NOTIFICATIONS` permission), Android Gradle Plugin (AGP) 8.0+, Kotlin 1.8+, and AndroidX Core (`androidx.core:core-ktx`) are **Mandatory** for local notifications. Firebase Cloud Messaging (`com.google.firebase:firebase-messaging-ktx:23.0+`) is required **only** when porting remote push notifications (APNs equivalents).
- **Open-Source / AOSP Push Alternatives:** For open-source, AOSP, or de-Googled Android environments where Google Play Services is unavailable, FCM cannot be used. Adopt open-source push standards like **UnifiedPush** or WebSocket/foreground-service connections for remote push delivery. Local notifications continue to rely universally on `NotificationManager`.

> [!NOTE]
> **Note:** **Open-Source \& AOSP Push Alternatives** Firebase Cloud Messaging (FCM) requires Google Play Services (GMS). For open-source, AOSP, or de-Googled Android distributions where GMS is unavailable, FCM cannot be used. Developers should adopt **UnifiedPush** (an open-source, decentralized push notification standard for Android) or maintain persistent WebSockets via Foreground Services for remote push delivery. Local notifications (`NotificationManager` / `NotificationCompat`) require zero proprietary cloud dependencies and work universally across 100% of Android distributions.

## Limitations

This skill has the following limitations:

- **Custom Layouts:** Migrating Notification Content Extensions for complex, interactive layouts is outside this skill's scope. Basic custom layouts are covered. Refer to skill `android-custom-ui-remoteviews` for complex interactions.
- **Third-Party Wrappers:** Apps using OneSignal or Braze should refer to those vendors' specific Android SDK documentation, though the core concepts here still apply.

## Glossary

| iOS Concept | Android Equivalent | Definition |
|---|---|---|
| **APNs (Apple Push Notification service)** | **FCM (Firebase Cloud Messaging)** | The remote service that delivers push notifications to devices. |
| **UNUserNotificationCenter** | **NotificationManager** | The system service that manages local and remote notifications. |
| **UNMutableNotificationContent** | **NotificationCompat.Builder** | The builder class used to construct notification content. |
| **Notification Category** | **Notification Channel** | The mechanism for grouping notifications for user control. |
| **Silent Push (content-available: 1)** | **Data Message / WorkManager** | A background trigger that does not show a visual alert immediately. |

## Authorization Options Mapping

You **Must** map iOS `UNAuthorizationOptions` to Android `NotificationChannel`
importance levels to ensure consistent user experience.

| iOS Option | Android Importance | Behavior |
|---|---|---|
| `.alert` | `IMPORTANCE_HIGH` | Shows on screen and makes sound. |
| `.sound` | `IMPORTANCE_DEFAULT` | Makes sound but doesn't pop up. |
| `.badge` | `IMPORTANCE_LOW` | Shows in status bar and updates badge. |
| `.provisional` | `IMPORTANCE_MIN` | Silent, only appears in shade. |

## Setup \& Dependencies

You **Must** add the Firebase Messaging dependency to your
`app/build.gradle.kts` file using Version Catalogs.

**Example: libs.versions.toml**
\`\`\`toml
\[versions\]
firebaseMessaging = "23.4.0"

\[libraries\]
firebase-messaging = { group = "com.google.firebase", name = "firebase-messaging-ktx", version.ref = "firebaseMessaging" }
\`\`\`

**Example: app/build.gradle.kts**
`kotlin
dependencies {
implementation(libs.firebase.messaging)
implementation("androidx.core:core-ktx:1.12.0") // For NotificationCompat
}`

You **Must** also declare the `FirebaseMessagingService` in your
`AndroidManifest.xml`:

```xml
<service
   android:name=".MyFirebaseMessagingService"
   android:exported="false">
   <intent-filter>
       <action android:name="com.google.firebase.MESSAGING_EVENT" />
   </intent-filter>
</service>
```

## Instructions: Local Notifications

### Step 1: Requesting Permissions (Android 13+)

Unlike older Android versions, Android 13 (API 33) and higher requires a runtime
permission for notifications.

- **iOS:** `UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge])`
- **Android:** You **Must** request the `POST_NOTIFICATIONS` permission.

**Example: Requesting Permission in Compose**
\`\`\`kotlin
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermissionScreen() {
val permissionState = rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)

LaunchedEffect(Unit) {
permissionState.launchPermissionRequest()
}
}
\`\`\`

### Step 2: Creating Notification Channels

Android **Requires** notification channels for all notifications on API 26+. iOS
does not have a direct equivalent, though categories provide similar grouping.

You **Must** create channels before posting notifications.

**Example: Creating a Channel**
\`\`\`kotlin
fun createNotificationChannel(context: Context) {
if (Build.VERSION.SDK_INT \>= Build.VERSION_CODES.O) {
val name = "Default Channel"
val descriptionText = "Used for general notifications"
val importance = NotificationManager.IMPORTANCE_DEFAULT
val channel = NotificationChannel("default_channel_id", name, importance).apply {
description = descriptionText
}

       val notificationManager: NotificationManager =
           context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
       notificationManager.createNotificationChannel(channel)

}
}
\`\`\`

### Step 3: Building and Posting Notifications

You **Must** use `NotificationCompat.Builder` to create the notification.

- **iOS:** `UNMutableNotificationContent()`
- **Android:** `NotificationCompat.Builder(context, channelId)`

**Example: Posting a Notification with BigTextStyle**
\`\`\`kotlin
val builder = NotificationCompat.Builder(context, "default_channel_id")
.setSmallIcon(R.drawable.ic_notification)
.setContentTitle(context.getString(R.string.notif_new_message_title))
.setContentText(context.getString(R.string.notif_new_message_text))
.setStyle(NotificationCompat.BigTextStyle()
.bigText(context.getString(R.string.notif_new_message_expanded)))
.setPriority(NotificationCompat.PRIORITY_DEFAULT)

with(NotificationManagerCompat.from(context)) {
notify(notificationId, builder.build())
}
\`\`\`

## Instructions: Notification Actions

You **Must** map iOS `UNNotificationAction` to Android
`NotificationCompat.Action` to enable interactive notifications.

**Example: Adding Action Buttons**
\`\`\`kotlin
val replyIntent = Intent(context, ReplyReceiver::class.java)
val replyPendingIntent = PendingIntent.getBroadcast(
context, 0, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
)

val remoteInput = RemoteInput.Builder("key_text_reply").run {
setLabel("Your reply...")
build()
}

val action = NotificationCompat.Action.Builder(
R.drawable.ic_reply, context.getString(R.string.notif_action_reply), replyPendingIntent
).addRemoteInput(remoteInput).build()

val builder = NotificationCompat.Builder(context, "default_channel_id")
.setSmallIcon(R.drawable.ic_notification)
.setContentTitle(context.getString(R.string.notif_new_message_title))
.setContentText(context.getString(R.string.notif_tap_to_reply))
.addAction(action)
\`\`\`

## Instructions: Badge Handling

Handling app badges (notification dots) differs significantly between iOS and
Android.

- **iOS:** `UIApplication.shared.applicationIconBadgeNumber = 5` directly sets the number.
- **Android:** Android uses "Notification Dots" on the launcher icon. The number is derived from the active notifications.
- **Logic:**
  - **IF** the app requires explicit control over the badge number (e.g., unread count), **THEN** you **Must** use a third-party library like `ShortcutBadger` or rely on the launcher's implementation, as stock Android does not provide a standard API for setting the number directly.

## Instructions: Deep Linking from Notifications

To enable deep linking when a user taps a notification, you **Must** attach a
`PendingIntent` to the notification builder.

- **iOS:** Handled via `UNNotificationDefaultActionIdentifier` in `userNotificationCenter(_:didReceive:withCompletionHandler:)`.
- **Android:** Handled via `setContentIntent(pendingIntent)`.

**Example: Adding Deep Link Intent**
\`\`\`kotlin
val intent = Intent(context, MainActivity::class.java).apply {
flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
putExtra("target_screen", "messages")
}

val pendingIntent: PendingIntent = PendingIntent.getActivity(
context, 0, intent, PendingIntent.FLAG_IMMUTABLE
)

val builder = NotificationCompat.Builder(context, "default_channel_id")
.setSmallIcon(R.drawable.ic_notification)
.setContentTitle(context.getString(R.string.notif_new_message_title))
.setContentText(context.getString(R.string.notif_tap_to_view))
.setContentIntent(pendingIntent)
.setAutoCancel(true)
\`\`\`

## Instructions: Push Notifications

### Step 1: Handling Incoming Messages

You **Must** extend `FirebaseMessagingService` to handle incoming messages.

- **iOS:** `application(_:didReceiveRemoteNotification:...)`
- **Android:** `onMessageReceived(remoteMessage: RemoteMessage)`

**Example: Handling Data Payload**
`kotlin
class MyFirebaseMessagingService : FirebaseMessagingService() {
override fun onMessageReceived(remoteMessage: RemoteMessage) {
// Handle data payload
if (remoteMessage.data.isNotEmpty()) {
val targetScreen = remoteMessage.data["target_screen"]
// Schedule a job or show a notification based on data
sendNotification("Data Received", "Target: $targetScreen")
}
}
}`

### Step 2: Silent Push to WorkManager

When a silent push (data message) is received, you **Must** use `WorkManager` to
perform the background fetch, rather than doing it in `onMessageReceived`.

**Example: Silent Push Workflow**
`kotlin
override fun onMessageReceived(remoteMessage: RemoteMessage) {
if (remoteMessage.data["type"] == "silent_sync") {
val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
.build()
WorkManager.getInstance(applicationContext).enqueue(syncRequest)
}
}`

## Instructions: Basic Custom Notification Layouts

While complex custom layouts are out of scope, you **Must** follow this basic
workflow for simple custom notification layouts using `RemoteViews`.

- **Action:** Create a custom XML layout in `res/layout/custom_notification.xml`.
- **Action:** Use `RemoteViews` to inflate and populate the layout.

**Example: Custom Layout Usage**
\`\`\`kotlin
val customView = RemoteViews(context.packageName, R.layout.custom_notification).apply {
setTextViewText(R.id.title, context.getString(R.string.notif_custom_title))
setTextViewText(R.id.text, context.getString(R.string.notif_custom_text))
}

val builder = NotificationCompat.Builder(context, "default_channel_id")
.setSmallIcon(R.drawable.ic_notification)
.setStyle(NotificationCompat.DecoratedCustomViewStyle())
.setCustomContentView(customView)
\`\`\`

**Logic:**

- **IF** the iOS app uses a Notification Content Extension for simple data display, **THEN** you **Must** use `RemoteViews` with `DecoratedCustomViewStyle`.
- **ELSE** (complex interaction), evaluate if a standard big text style can suffice to reduce complexity.

## Instructions: Migrating Time-Based Triggers

You **Must** map iOS time-based triggers to Android `WorkManager` or
`AlarmManager`.

- **iOS:** `UNTimeIntervalNotificationTrigger(timeInterval: 60, repeats: false)`
- **Android:** Use `WorkManager` with `setInitialDelay`.

**Example: Time Interval Trigger**
`kotlin
val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
.setInitialDelay(60, TimeUnit.SECONDS)
.build()
WorkManager.getInstance(context).enqueue(syncRequest)`

- **iOS:** `UNCalendarNotificationTrigger(dateMatching: dateComponents, repeats: true)`
- **Android:** Use `PeriodicWorkRequestBuilder` or `AlarmManager` for precise timing.

**Logic:**

- **IF** the trigger requires precise execution at a specific wall-clock time, **THEN** you **Must** use `AlarmManager` with `setExactAndAllowWhileIdle`.
- **ELSE** , use `WorkManager` to respect system battery optimizations.

## Tooling Integration: Standard Actions

To enable automated assessment and migration tracking, this skill defines the
following standard actions. Any automation tool built on this skill **Must**
implement these actions with the specified parameters to ensure benchmark
compliance.

### Verification Commands \& Agnostic Agent Instructions

To verify notification channel registration and runtime permissions:

1. **Notification Channel Audit (CLI):** `bash
   grep -rn "createNotificationChannel" app/src/main/`
2. **Manifest Permission Check:** *Inspect `app/src/main/AndroidManifest.xml` and verify that
   `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` is
   declared.*

## Antipatterns

### DO

- **DO** create notification channels before attempting to post notifications.
- **DO** use `NotificationCompat` to ensure backward compatibility.
- **DO** check for `POST_NOTIFICATIONS` permission on Android 13+.

### DON'T

- **DON'T** force FCM usage for background data sync if `WorkManager` can fulfill the requirement natively.
- **DON'T** use hardcoded strings for channel IDs; use constants.

### NEVER

- **NEVER** post a notification without a channel on API 26+ (it will be ignored).
- **NEVER** perform blocking network calls directly inside `onMessageReceived`. Use `WorkManager` to schedule heavy tasks.