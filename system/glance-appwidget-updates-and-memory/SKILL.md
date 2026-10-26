---
name: glance-appwidget-updates-and-memory
description: Manages Android AppWidget state updates, background syncs, and RemoteViews
  bitmap memory limits with Jetpack Glance (androidx.glance:glance-appwidget) and
  AppWidgetProvider receivers on Android 17 (API 37). Use this skill when configuring
  Android 17 (API level 37) compileSdk/targetSdk and AndroidManifest.xml widget receivers,
  refreshing frozen or stale Glance widget UI after CoroutineWorker DataStore preference
  updates (updateAppWidgetState), downsampling images within the WindowMetrics memory
  limit for SizeMode.Responsive, SizeMode.Exact, or LazyColumn, sharing content://
  URIs with FileProvider, or fixing ActionCallback NoSuchMethodException and AppWidgetProvider
  ForegroundServiceStartNotAllowedException crashes with WorkManager. Don't use for
  Glance layout templates (suggest the glance-canonical-layouts skill) or in-app Compose
  screens.
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-10-23'
  keywords:
  - Android
  - AppWidget
  - Glance
  - GlanceAppWidget
  - GlanceAppWidgetReceiver
  - AppWidgetProvider
  - RemoteViews
  - widget memory limit
  - WindowMetrics
  - bitmap memory
  - SizeMode.Responsive
  - LazyColumn
  - ImageProvider
  - updateAppWidgetState
  - ActionCallback
  - NoSuchMethodException
  - R8
  - ForegroundServiceStartNotAllowedException
  - WorkManager
  - CoroutineWorker
  - FileProvider
  - Android 17
  - API 37
---

This skill provides guidance for managing state updates and keeping Android
AppWidgets that use Jetpack Glance (`androidx.glance:glance-appwidget`) or
`RemoteViews` within the Android 17 widget bitmap memory limits. For widget UI
layout templates, suggest the `glance-canonical-layouts` skill.

- When configuring a Glance widget, see [Create an app widget with Glance](references/android/develop/ui/compose/glance/create-app-widget.md).
- When targeting Android 17 (`targetSdk = 37`), see [Android 17 widget memory limits](references/android/about/versions/17/behavior-changes-17.md).
- When maintaining a classic `AppWidgetProvider`, see [Create an app widget](references/android/develop/ui/views/appwidgets/index.md).

## Prerequisites

- Kotlin Android project (`minSdk >= 26`) with `compileSdk = 37` (`targetSdk = 37` when targeting Android 17) in `build.gradle.kts`.
- Jetpack Glance (`1.2.0`, minimum `1.1.1+`) and the Compose compiler Gradle plugin enabled on the module.

## Critical mandates

- **Preserve existing package name and toolchain versions** : Inspect `build.gradle.kts`, `gradle/libs.versions.toml`, and Kotlin files first. Keep existing `namespace`, `applicationId`, `package`, `targetSdk` (unless migrating to Android 17), Android Gradle Plugin (AGP), and Kotlin versions.
- **Downsample images to widget dimensions and estimate the memory cap** : Android `system_server` computes the `RemoteViews` bitmap memory cap (`6 *
  width * height` bytes, or `1.5 * 4 * width * height`) once at boot from the real size of the default display and does not expose this cap in a public API. Because unfolding a foldable or switching display resolution to `QHD+` after boot can make active window metrics report a larger value than the boot-time cap, always downsample each image first to the widget's target dimensions (`maxWidthDp x maxHeightDp` in `RGB_565` or `ALPHA_8`), and enforce the per-image byte ceiling (`calculateWidgetMemoryCapBytes(context) /
  distinctImageCount`) estimated as `6 * width * height` from `WindowManager.currentWindowMetrics.bounds` on API 30+ (falling back to `resources.displayMetrics` on API 26-29 or if `WindowManager` is unavailable).
- **Decode shared bitmaps once outside provideContent** : Both `SizeMode.Exact` (used by `glance-canonical-layouts` to compose the current span's portrait and landscape orientations) and `SizeMode.Responsive` (used to pre-bundle a set of `DpSize` breakpoints) invoke `provideContent` multiple times per update. Decode shared bitmaps once in `provideGlance` before `provideContent` and pass them to `ImageProvider(bitmap)` so `RemoteViews` deduplicates the shared `Bitmap` across all sizes in either mode. If a widget scales distinct bitmaps per `LocalSize.current`, divide `calculateWidgetMemoryCapBytes(context)` by the number of distinct bitmaps.
- **Downsample content URIs and grant launcher read permissions** : On Android 17 (`targetSdk >= 37`), bitmap-backed `Icon`s count toward the cap alongside `Bitmap`s. `content://` URIs passed to `ImageProvider(uri)` or `RemoteViews.setImageViewUri` don't count toward the inter-process communication (IPC) cap, but the launcher decodes them at full resolution. Downsample the file into `context.cacheDir`, expose it with `FileProvider.getUriForFile`, and grant `Intent.FLAG_GRANT_READ_URI_PERMISSION` to `Intent.CATEGORY_HOME` packages.
- **Trigger explicit widget updates after state mutations** : `updateAppWidgetState` writes preferences to DataStore without updating the widget UI. After you mutate state, call `GlanceAppWidget.update(context,
  glanceId)` or `GlanceAppWidget.updateAll(context)`.
- **Use full updateAppWidget calls when you replace images** : In `AppWidgetProvider` classes, when you replace bitmaps or `Icon`s, call `AppWidgetManager.updateAppWidget` instead of `partiallyUpdateAppWidget`.

## Reference implementation files

Read the companion reference files in `references/` when implementing each step,
and update the sample `package` declaration and class names to match your app:

- **Bitmap Cap \& FileProvider Helper** : [WidgetBitmapUtils.kt](references/WidgetBitmapUtils.kt)
- **Responsive Widget, Receiver \& Error Fallback** : [SampleGlanceWidget.kt](references/SampleGlanceWidget.kt)
- **LazyColumn Collection Widget \& Revision Cache** : [FeedCollectionGlanceWidget.kt](references/FeedCollectionGlanceWidget.kt)
- **DataStore State Push, CoroutineWorker \& ActionCallback** : [WidgetStateAndActions.kt](references/WidgetStateAndActions.kt)
- **Classic AppWidgetProvider WorkManager \& Full Update** : [ClassicWidgetImageUpdater.kt](references/ClassicWidgetImageUpdater.kt)
- **Manifest, Widget Info, FileProvider Paths \& R8 Rules** : [AndroidManifest.xml](references/AndroidManifest.xml), [sample_widget_info.xml](references/res/xml/sample_widget_info.xml), [file_paths.xml](references/res/xml/file_paths.xml), [proguard-rules.pro](references/proguard-rules.pro)

## Detailed steps

### Step 1: Configure SDK levels, dependencies, and manifest declarations

1. Read `gradle/libs.versions.toml`, `app/build.gradle.kts`, and `app/src/main/AndroidManifest.xml` first. If AGP, Kotlin, or library versions are already present, keep them; otherwise, add `glance = "1.2.0"` along with `androidx.glance:glance-appwidget` and `androidx.glance:glance-material3` to `gradle/libs.versions.toml`.
2. In `app/build.gradle.kts`, keep `namespace`, `applicationId`, and any existing `minSdk >= 26`, `targetSdk`, or library versions. Set `compileSdk = 37` (and `targetSdk = 37` only when explicitly migrating the app to target Android 17), enable `buildFeatures { compose = true }`, and add `implementation(libs.androidx.glance.appwidget)` and `implementation(libs.androidx.glance.material3)`.
3. In `AndroidManifest.xml` (see [AndroidManifest.xml](references/AndroidManifest.xml)), declare `<queries>` with `<action android:name="android.intent.action.MAIN" />` and `<category
   android:name="android.intent.category.HOME" />` so `queryIntentActivities` discovers launcher packages on API 30+. Register your `GlanceAppWidgetReceiver` (pointing `android.appwidget.provider` to `@xml/sample_widget_info`, see [sample_widget_info.xml](references/res/xml/sample_widget_info.xml)) and register `androidx.core.content.FileProvider` with `android:authorities="${applicationId}.fileprovider"`, `android:exported="false"`, `android:grantUriPermissions="true"`, and `android.support.FILE_PROVIDER_PATHS` pointing to `@xml/file_paths` (which declares `<cache-path name="widget_cache" path="widget_images/" />`, see [file_paths.xml](references/res/xml/file_paths.xml)):

<br />

```xml
<receiver android:name=".glance.MyReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/my_app_widget_info" />
</receiver>
```

<br />

### Step 2: Estimate the widget memory cap and downsample images

Read [WidgetBitmapUtils.kt](references/WidgetBitmapUtils.kt) and implement the memory cap and downsampling
helpers:

1. **Estimate the cap (`calculateWidgetMemoryCapBytes`)** : On API 30+ (`Build.VERSION.SDK_INT >= Build.VERSION_CODES.R`), read `val bounds =
   context.getSystemService(WindowManager::class.java)?.currentWindowMetrics?.bounds` and, when `bounds != null && bounds.width() > 0 && bounds.height() > 0`, return `6L * bounds.width().toLong() * bounds.height().toLong()` (calling `WindowManager` from a non-UI widget or worker `Context` logs a nonfatal `StrictMode` violation on API 30+, which is expected for this estimate). On API 26-29 (or if `WindowManager` is unavailable), fall back to `context.resources.displayMetrics` (`6L * dm.widthPixels.toLong() *
   dm.heightPixels.toLong()`). Because `system_server` caches `6 * width *
   height` once at boot, always size images to the widget's `maxWidthDp x
   maxHeightDp` first (Stage 1) rather than filling the full screen budget.
2. **Downsample in two stages (`decodeDownsampledWidgetBitmap`)** :
   - First, probe dimensions with `BitmapFactory.Options().apply {
     inJustDecodeBounds = true; inPreferredConfig = config }` and double `sampleSize *= 2` while `(bounds.outWidth / (sampleSize * 2) >=
     targetWidthPx && bounds.outHeight / (sampleSize * 2) >= targetHeightPx) ||
     (bounds.outWidth / sampleSize > targetWidthPx * 2) || (bounds.outHeight /
     sampleSize > targetHeightPx * 2)` (where `targetWidthPx = max(1,
     (maxWidthDp * density).toInt())`), so wide banners or tall screenshots do not bypass downsampling when one axis already fits.
   - Second, enforce the per-image byte ceiling (`perImageCapBytes =
     calculateWidgetMemoryCapBytes(context) / max(1, distinctImageCount)`) by doubling `sampleSize *= 2` while `(max(1, bounds.outWidth /
     sampleSize).toLong() * max(1, bounds.outHeight / sampleSize).toLong() *
     bytesPerPixel) > perImageCapBytes`. Use `2L` bytes per pixel for `Bitmap.Config.RGB_565` (opaque photos), `1L` for `Bitmap.Config.ALPHA_8` masks, and `4L` when `bounds.outConfig != null && bounds.outConfig !=
     config` (because `BitmapFactory` upgrades translucent PNG or WebP files from `RGB_565` to 4-byte `ARGB_8888`). Decode with `inSampleSize =
     sampleSize` and `inPreferredConfig = config`.
3. **Choose between in-memory `Bitmap` and `FileProvider` `Uri`
   (`cacheWidgetImageUri`)** : Use `decodeDownsampledWidgetBitmap` with `androidx.glance.ImageProvider(bitmap)` when passing bitmaps directly in memory. Use `cacheWidgetImageUri` when the widget passes a `content://` `Uri` to `ImageProvider(uri)` or `RemoteViews.setImageViewUri`: it writes the downsampled bitmap into `File(context.cacheDir, "widget_images")`, obtains a `content://` URI with `FileProvider.getUriForFile(context,
   "${context.packageName}.fileprovider", outFile)`, queries `Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)`, and calls `context.grantUriPermission(pkg, uri,
   Intent.FLAG_GRANT_READ_URI_PERMISSION)`.

### Step 3: Deduplicate multi-size bitmaps and handle composition errors

Read [SampleGlanceWidget.kt](references/SampleGlanceWidget.kt) to configure multi-size bitmap deduplication and
composition error recovery:

1. **Choose `SizeMode` for your layout** : Use `SizeMode.Exact` (recommended when using `glance-canonical-layouts` to render exact `LocalSize.current` dimensions for the current grid span's portrait and landscape orientations), `SizeMode.Responsive(RESPONSIVE_BUCKETS)` (using `import
   androidx.compose.ui.unit.DpSize` and `import androidx.compose.ui.unit.dp` to pre-bundle fixed `DpSize` breakpoints such as `setOf(DpSize(120.dp, 120.dp),
   DpSize(240.dp, 120.dp), DpSize(240.dp, 240.dp))` so resize transitions don't wake the app process), or `SizeMode.Single` (for a single layout).
2. **Hoist shared bitmap decodes before `provideContent`** : Because both `SizeMode.Exact` (`2-4` orientation sizes) and `SizeMode.Responsive` (`N` buckets) run `provideContent` multiple times per update, decode shared bitmaps once in `provideGlance` **before** `provideContent` with `decodeDownsampledWidgetBitmap` and `Bitmap.Config.RGB_565` for opaque images (`distinctImageCount = 1`). Pass the resulting `Bitmap` to `androidx.glance.ImageProvider(sharedBitmap)` so `RemoteViews` deduplicates the shared `Bitmap` across all sizes.
3. **Provide a zero-bitmap error fallback** : Override `onCompositionError(context, glanceId, appWidgetId, throwable)` and call `AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId,
   fallback)` with a text-only `RemoteViews` layout (`0` bitmaps).

### Step 4: Pre-decode LazyColumn collection images and assign stable itemIds

Read [FeedCollectionGlanceWidget.kt](references/FeedCollectionGlanceWidget.kt) when rendering collection widgets with
`LazyColumn`:

1. Replace `import androidx.glance.appwidget.ImageProvider` (which accepts `Uri`) with `import androidx.glance.ImageProvider` (which accepts `Bitmap`). Remove `Uri.fromFile` and `Icon.createWithContentUri` calls.
2. While a Glance session is active (\~45 seconds after an update), `.update()` and `.updateAll()` do not restart `provideGlance`. Declare `var
   cachedRevision = -1` and `var cachedThumbnails = emptyMap<String, Bitmap>()` in `provideGlance` before `provideContent`. Inside `provideContent`, read `val revision = currentState<Preferences>()[KEY_REVISION] ?: 0`, and only when `revision != cachedRevision`, update `cachedRevision = revision` and decode the `feedItems` thumbnails with `cachedThumbnails =
   feedItems.mapNotNull { item -> decodeDownsampledWidgetBitmap(...,
   distinctImageCount = max(1, feedItems.size), config =
   Bitmap.Config.RGB_565)?.let { item.id to it } }.toMap()` (use `mapNotNull` rather than `associate` so the map stays `Map<String, Bitmap>` instead of `Map<String, Bitmap?>`). This re-decodes when DataStore state updates during an active session while sharing one decoded `Map<String, Bitmap>` across all `SizeMode.Responsive` buckets.
3. In `LazyColumn`, pass a stable `Long` `itemId` to `items(items = feedItems,
   itemId = { item -> item.id.hashCode().toLong() })` (preferring a persistent numeric ID when available) and render `Image(provider =
   ImageProvider(bitmap), contentDescription = item.title)` only when `bitmap !=
   null` (never fall back to `Uri.fromFile` in an `else` branch).

### Step 5: Push state updates, configure ActionCallbacks, and use WorkManager

Read [WidgetStateAndActions.kt](references/WidgetStateAndActions.kt), [proguard-rules.pro](references/proguard-rules.pro), and
[ClassicWidgetImageUpdater.kt](references/ClassicWidgetImageUpdater.kt) to implement these patterns:

1. **Push DataStore updates** (requires `androidx.datastore:datastore-preferences-core:1.2.1` and `import
   androidx.glance.appwidget.GlanceAppWidgetManager`, `import
   androidx.glance.appwidget.state.updateAppWidgetState`, and `import
   androidx.glance.appwidget.updateAll`): In `pushWidgetRevisionUpdate(context)` (see [WidgetStateAndActions.kt](references/WidgetStateAndActions.kt)), iterate over `for (glanceId in
   GlanceAppWidgetManager(context).getGlanceIds(Widget::class.java))`, mutate state inside `updateAppWidgetState(context, glanceId) { prefs -> ... }` (passing both `context` and `glanceId`), and immediately call `widget.update(context, glanceId)` (or `widget.updateAll(context)`).
2. **Use zero-argument `ActionCallback` classes with `ActionParameters.Key` and
   keep `<init>()` under R8** (requires `androidx.work:work-runtime-ktx:2.12.0` when scheduling background work): `RunCallbackAction` instantiates `ActionCallback` classes with `Class.getDeclaredConstructor().newInstance()`, so a constructor parameter fails in every build. Remove constructor parameters from `ActionCallback` implementations, pass values with `ActionParameters.Key` and `actionParametersOf` (see `RefreshWidgetAction` in [WidgetStateAndActions.kt](references/WidgetStateAndActions.kt)), and add `-keep class * implements
   androidx.glance.appwidget.action.ActionCallback { public <init>(); }` to `proguard-rules.pro` (see [proguard-rules.pro](references/proguard-rules.pro)). Inside `SyncWidgetWorker.doWork()`, call `pushWidgetRevisionUpdate(applicationContext)` so background work updates DataStore and refreshes the widget.
3. **Avoid unconditional one-time `WorkManager` enqueues in
   `AppWidgetProvider.onUpdate` and use full `updateAppWidget` for images** (see [ClassicWidgetImageUpdater.kt](references/ClassicWidgetImageUpdater.kt)): Never call `context.startForegroundService` from `AppWidgetProvider.onReceive` or `onUpdate` (which throws `ForegroundServiceStartNotAllowedException`). `GlanceAppWidgetReceiver` already keeps a 10-year delayed session worker queued internally so WorkManager's `RescheduleReceiver` never toggles. In a classic `AppWidgetProvider`, avoid enqueuing a `OneTimeWorkRequest` directly inside `onUpdate`: even with `ExistingWorkPolicy.KEEP`, once the one-time worker finishes (`SUCCEEDED`), `UnfinishedWorkListener` disables `RescheduleReceiver`, which broadcasts `ACTION_PACKAGE_CHANGED` and re-triggers `onUpdate` in a loop. Instead, schedule repeating syncs in `onUpdate` with `enqueueUniquePeriodicWork(...,
   ExistingPeriodicWorkPolicy.KEEP, ...)` (which stays `ENQUEUED` across runs), use `goAsync()` for short `< 10s` updates, or enqueue one-time work (`enqueueUniqueWork(..., ExistingWorkPolicy.KEEP, ...)`) only from specific action broadcasts in `onReceive`. When you update a `Bitmap` or `Icon` on a classic `RemoteViews` widget, call `appWidgetManager.updateAppWidget` rather than `partiallyUpdateAppWidget`.

## Common errors and fixes

| Symptom or exception | Root cause | Fix |
|---|---|---|
| `IllegalArgumentException: RemoteViews for widget update exceeds maximum bitmap memory usage` | `RemoteViews` bitmap or icon memory exceeded `6 * W * H` bytes. | Downsample to the widget's `maxWidthDp x maxHeightDp` with `inSampleSize` and `RGB_565` (or `ALPHA_8` for masks), enforce `calculateWidgetMemoryCapBytes(context) / distinctImageCount`, and decode shared bitmaps once before `provideContent`. |
| `SecurityException: Permission Denial: opening provider` or blank `content://` widget image | Home launcher lacks read permission for the `FileProvider` `content://` URI or app passed `file://` with `Uri.fromFile`. | Expose the file with `FileProvider`, add `<queries>` for `CATEGORY_HOME`, and call `context.grantUriPermission` with `Intent.FLAG_GRANT_READ_URI_PERMISSION`. |
| Widget UI stays frozen after `updateAppWidgetState` | `updateAppWidgetState` writes to DataStore without recomposing, or `provideGlance` was not re-triggered during an active session. | Call `.update(context, glanceId)` or `.updateAll(context)` after `updateAppWidgetState`, and observe revision state inside `provideContent`. |
| `NoSuchMethodException: <Class>.<init> []` on action tap | `ActionCallback` declared constructor parameters or R8 stripped `<init>()`. | Use a public zero-argument constructor, pass values with `ActionParameters.Key`, and keep `<init>()` in ProGuard. |
| `ForegroundServiceStartNotAllowedException` in receiver | `AppWidgetProvider` called `startForegroundService` in the background. | Schedule periodic syncs in `onUpdate` with `enqueueUniquePeriodicWork` (`ExistingPeriodicWorkPolicy.KEEP`) or enqueue one-time work (`enqueueUniqueWork`) from custom action broadcasts in `onReceive`. |
| `Unresolved reference: ImageProvider`, `DpSize`, `updateAppWidgetState`, or `updateAll` | Missing or mismatched Glance and Compose package imports, or calling `updateAppWidgetState(context)` without `glanceId`. | Import `androidx.glance.ImageProvider` (for `Bitmap`), `androidx.compose.ui.unit.DpSize` and `androidx.compose.ui.unit.dp` (for `SizeMode.Responsive`), `androidx.glance.appwidget.state.updateAppWidgetState` (passing both `context` and `glanceId`), and `androidx.glance.appwidget.updateAll`. |

## Antipatterns

- **NEVER** decode or scale a `Bitmap` inside `provideContent` per size bucket with `SizeMode.Responsive` or `SizeMode.Exact`.
- **NEVER** size widget bitmaps to fill the full screen memory cap (`6 * width *
  height`); always downsample to the widget's target `maxWidthDp x maxHeightDp` first so unfolding a foldable or switching display resolution after boot does not exceed the boot-time cap.
- **NEVER** pass an unscaled `content://` `Uri` or `Uri.fromFile` to `ImageProvider(uri)` or `RemoteViews.setImageViewUri`, or omit `context.grantUriPermission` for `Intent.CATEGORY_HOME`.
- **NEVER** call `updateAppWidgetState` without calling `GlanceAppWidget.update(context, glanceId)` or `GlanceAppWidget.updateAll(context)`.
- **NEVER** call `partiallyUpdateAppWidget` when you replace widget images.
