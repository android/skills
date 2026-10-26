<br />

Like previous releases, Android 17 includes behavior changes that might affect
your app. The following behavior changes apply exclusively to apps that are
targeting Android 17 or higher. If your app is targeting Android 17 or higher,
you should modify your app to support these behaviors, where applicable.

Be sure to also review the list of [behavior changes that affect all apps
running on Android 17](https://developer.android.com/about/versions/17/behavior-changes-all) regardless of your app's [`targetSdkVersion`](https://developer.android.com/guide/topics/manifest/uses-sdk-element#target).

> [!NOTE]
> **Note:** This page lists some of the more important changes. For more detailed information, see the [Android 17 release notes](https://developer.android.com/about/versions/17/release-notes).

## User experience and system UI

Android 17 includes the following changes that are intended to create a
more consistent, intuitive user experience.

### Memory limit widget

Beginning with Android 17, the system enforces a strict memory limit on
`RemoteViews` updates for apps targeting Android 17 (API level 37) or
higher. When an app passes a `RemoteViews` object to
[`AppWidgetManager.updateAppWidget`](https://developer.android.com/reference/android/appwidget/AppWidgetManager#updateAppWidget(int,%20android.widget.RemoteViews)),
`system_server` calculates the total memory footprint of all image data across
every orientation and size bucket in the payload. If the total memory exceeds
the system ceiling (`mMaxWidgetBitmapMemory`), the system rejects the update and
throws an `IllegalArgumentException`:

    java.lang.IllegalArgumentException: RemoteViews for widget update exceeds maximum bitmap memory usage (used: XXXXX, max: YYYYY)

For apps targeting API level 36 or lower, the system logs this memory violation
as a warning (`Slog.w`) without throwing an exception.

#### How the system calculates the memory ceiling

The system sets the widget bitmap memory budget at `6 * width * height` bytes
(`1.5 * 4 * width * height`, equivalent to 1.5 times the size of a full-screen
`ARGB_8888` buffer) for the active display configuration. Under API level 37 and
higher, this calculation includes both of the following:

- In-memory [`Bitmap`](https://developer.android.com/reference/android/graphics/Bitmap) and bitmap-backed [`Icon`](https://developer.android.com/reference/android/graphics/drawable/Icon) objects (`Icon.TYPE_BITMAP` and `Icon.TYPE_ADAPTIVE_BITMAP`) stored in `RemoteViews.mBitmapCache` (such as calls to `setImageViewBitmap`, `Icon.createWithBitmap`, `Icon.createWithAdaptiveBitmap`, or Jetpack Glance `ImageProvider(bitmap)`). Each unique `Bitmap` or bitmap-backed `Icon` instance in `mBitmapCache` is counted once per `RemoteViews` hierarchy.
- URI-backed images (`Icon.createWithContentUri`, `setImageViewUri`, or Glance `ImageProvider(uri)`) don't count toward the `RemoteViews` IPC byte cap in `system_server`, but the widget host process decodes the referenced `content://` file at full resolution into its own heap. Always write a downsampled image into `context.cacheDir` before exposing it through a `FileProvider` URI.

#### Mitigation steps

To keep your widget updates within the memory ceiling across phones, tablets,
and foldables, do the following:

1. **Compute your runtime memory budget from `WindowMetrics`:** On API level 30
   and higher, read the active window bounds from
   `WindowManager.currentWindowMetrics.bounds` rather than
   `resources.displayMetrics`. On multi-resolution devices and foldables,
   `displayMetrics` can report inactive display dimensions. Estimate your total
   widget image budget at `6L * bounds.width() * bounds.height()`
   as shown in the following example:

       private const val FALLBACK_MAX_BYTES = 15_000_000L

       fun calculateWidgetMemoryCapBytes(context: Context): Long {
           if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
               val wm = context.getSystemService(WindowManager::class.java)
                   ?: return FALLBACK_MAX_BYTES
               val bounds = wm.currentWindowMetrics.bounds
               return 6L * bounds.width().toLong() * bounds.height().toLong()
           }
           val dm = context.resources.displayMetrics
           return 6L * dm.widthPixels.toLong() * dm.heightPixels.toLong()
       }

2. **Downsample images to the widget's viewable dimensions and use `RGB_565`:**
   Decode bitmaps with power-of-two `BitmapFactory.Options.inSampleSize` values
   sized to the widget's maximum display dimensions (`maxWidthDp` by
   `maxHeightDp`), and use `Bitmap.Config.RGB_565` (`2` bytes per pixel)
   instead of `ARGB_8888` (`4` bytes per pixel) for opaque photographs and
   thumbnails.

3. **Hoist bitmap decoding outside `provideContent` in Jetpack Glance:** When
   using `SizeMode.Responsive` or `SizeMode.Exact`, Glance invokes your
   `provideContent` lambda once per size bucket. Decode each shared `Bitmap`
   once inside `provideGlance` *before* calling `provideContent` so all size
   buckets reference the same `Bitmap` instance in `RemoteViews.mBitmapCache`.

4. **Use full `updateAppWidget` calls when replacing images:** In classic
   `AppWidgetProvider` implementations, avoid calling
   `AppWidgetManager.partiallyUpdateAppWidget` to swap `Bitmap` or `Icon`
   views. Partial updates merge new actions into the existing `RemoteViews`
   tree in `system_server` without evicting previously cached image actions.
   Build a complete `RemoteViews` layout and call `updateAppWidget` instead.

5. **Provide an error fallback layout:** In Jetpack Glance, override
   [`onCompositionError`](https://developer.android.com/develop/ui/compose/glance/error-handling)
   to render a zero-bitmap `RemoteViews` fallback if an update fails.

You can inspect your widget's current serialized bitmap memory usage on a
connected device by running the following:

    adb shell dumpsys appwidget

## Core functionality

Android 17 includes the following changes that modify or
expand various core capabilities of the Android system.

### New lock-free implementation of MessageQueue

Beginning with Android 17, apps targeting Android 17 (API level 37)
or higher receive a new lock-free implementation of
[`android.os.MessageQueue`](https://developer.android.com/reference/android/os/MessageQueue). The new implementation improves performance and
reduces missed frames, but may break clients that reflect on `MessageQueue`
private fields and methods.

For more information, including mitigation strategies, see [MessageQueue
behavior change guidance](https://developer.android.com/about/versions/17/changes/messagequeue).

### Static final fields are now unmodifiable

Apps running on Android 17 or higher that target
Android 17 (API level 37) or higher cannot change `static final` fields. If
an app attempts to change a `static final` field by using reflection, it will
cause an `IllegalAccessException`. Attempting to modify one of these fields
through JNI APIs (such as `SetStaticLongField()`) will cause the app to crash.

## Accessibility

Android 17 makes the following changes to improve accessibility.

### Accessibility support of complex IME physical keyboard typing

This feature introduces new [`AccessibilityEvent`](https://developer.android.com/reference/android/view/accessibility/AccessibilityEvent) and [`TextAttribute`](https://developer.android.com/reference/android/view/inputmethod/TextAttribute)
APIs to enhance screen reader spoken feedback for CJKV language input. CJKV IME
apps can now signal whether a text conversion candidate has been selected during
text composition. Apps with edit fields can specify *text change types* when
sending text changed accessibility events.
For example, apps can specify that a text change occurred during text
composition, or that a text change resulted from a commit.
Doing this enables accessibility
services such as screen readers to deliver more precise feedback based on the
nature of the text modification.

#### App adoption

- **IME Apps:** When setting composing text in edit fields, IMEs can use
  `TextAttribute.Builder.setTextSuggestionSelected()` to indicate whether a
  specific conversion candidate was selected.

- **Apps with Edit Fields:** Apps that maintain a custom `InputConnection` can
  retrieve candidate selection data by calling
  `TextAttribute.isTextSuggestionSelected()`. These apps should then call
  `AccessibilityEvent.setTextChangeTypes()` when dispatching
  `TYPE_VIEW_TEXT_CHANGED` events. Apps targeting
  Android 17 (API level 37) that use the standard `TextView` will have
  this feature enabled by default. (That is, `TextView` will handle retrieving
  data from the IME and setting text change types when sending events to
  accessibility services).

- **Accessibility Services:** Accessibility services that process
  `TYPE_VIEW_TEXT_CHANGED` events can call
  `AccessibilityEvent.getTextChangeTypes()` to identify the nature of the
  modification and adjust their feedback strategies accordingly.

## Privacy

Android 17 includes the following changes to improve user privacy.

### ECH (Encrypted Client Hello) enabled

Android 17 introduces platform support for Encrypted Client Hello (ECH), a TLS
extension that enhances user privacy by encrypting the Server Name Indication
(SNI) in the TLS handshake. This encryption helps prevent network observers from
easily identifying the specific domain your app is connecting to.

For apps targeting Android 17 (API level 37) or higher, ECH is used for TLS
connections. ECH is active only if the networking library used by the app (for
example, HttpEngine, WebView, or OkHttp) has integrated ECH support and the
remote server also supports the ECH protocol. If ECH cannot be negotiated, the
client sends an ECH extension with randomized contents (a mechanism called ECH
GREASE). See [RFC 9849](https://www.rfc-editor.org/rfc/rfc9849.html#name-grease-ech) for more details on how ECH GREASE works.

To allow apps to customize this behavior, Android 17 adds a new
[`<domainEncryption>`](https://developer.android.com/privacy-and-security/security-config#domainEncryption) element to the Network Security Configuration file.
Developers can use `<domainEncryption>` within `<base-config>` or
`<domain-config>` tags to select an ECH mode (for example,
`"enabled"` or `"disabled"`) on a global or per-domain basis.

For more information, see the [Encrypted Client Hello](https://developer.android.com/privacy-and-security/security-config#EncryptedClientHelloSummary) documentation.

### Local network permission required for apps targeting Android 17

Android 17 introduces the [`ACCESS_LOCAL_NETWORK`](https://developer.android.com/reference/android/Manifest.permission#ACCESS_LOCAL_NETWORK) runtime permission
to protect users from unauthorized local network access. Because this falls
under the existing `NEARBY_DEVICES` permission group, users who have already
granted other `NEARBY_DEVICES` permissions aren't prompted again. This new
requirement prevents malicious apps from exploiting unrestricted local network
access for covert user tracking and fingerprinting. By declaring and requesting
this permission, your app can discover and connect to devices on the local area
network (LAN), such as smart home devices or casting receivers.

Apps targeting Android 17 (API level 37) or higher now have two paths to
maintain communication with LAN devices: Adopt system-mediated,
privacy-preserving device pickers to skip the permission prompt, or explicitly
request this new permission at runtime to maintain local network communication.

For more information, see the [Local network permission](https://developer.android.com/privacy-and-security/local-network-permission) documentation.

> [!NOTE]
> **Note:** In Android 16, apps could opt in to local network permissions. Beginning with Android 17, enforcement is mandatory for apps that target Android 17 (API level 37) or higher.

### Hiding passwords from physical devices

If an app targets Android 17 (API level 37) or higher and the user is using
a physical input device (for example, an external keyboard), the Android
operating system applies the new `show_passwords_physical` setting to all
characters in the password field. By default, that setting hides all password
characters.

The Android system shows the last-typed password character to help the user see
if they mistyped the password. However, this is much less necessary with larger
external keyboards. In addition, devices with external keyboards often have
larger displays, which increases the danger of someone seeing the typed
password.

If the user is using the device's touchscreen, the system applies the new
`show_passwords_touch` setting.

### OTP protection for standard SMS messages

Beginning with Android 17, Android is extending its SMS OTP protection
to apply to standard SMS messages (SMS messages containing an OTP that do not
use the WebOTP or SMS Retriever formats). For most apps targeting
Android 17 (API level 37) or higher, these SMS messages do not become
available until three hours after receipt. This delay is intended to help
prevent OTP hijacking. During this three hour delay, the
[`SMS_RECEIVED_ACTION`](https://developer.android.com/reference/android/provider/Telephony.Sms.Intents#SMS_RECEIVED_ACTION) broadcast is withheld and
[SMS provider](https://developer.android.com/reference/android/provider/Telephony.Sms) database queries are filtered. The SMS message is
available to these apps after the delay.

Certain apps such as the default SMS assistant app, connected device companion
apps, etc., are exempted from this delay. All apps that rely on reading SMS
messages for OTP extraction should transition to using [SMS Retriever](https://developer.android.com/identity/sms-retriever) or [SMS
User Consent](https://developers.google.com/identity/sms-retriever/user-consent/overview) APIs to ensure continued functionality.

> [!NOTE]
> **Note:** Additional SMS OTP protections for WebOTP and SMS Retriever format messages apply to all apps, regardless of their target API level. For more information, see [SMS OTP protection](https://developer.android.com/about/versions/17/behavior-changes-all#sms-otp-all-apps) in the [Behavior changes for all
> apps](https://developer.android.com/about/versions/17/behavior-changes-all) documentation.

## Security

Android 17 makes the following improvements to device and app security.

### Activity Security

In Android 17, the platform continues its shift toward a
"secure-by-default" architecture, introducing a suite of enhancements designed
to mitigate high-severity exploits such as phishing, interaction hijacking, and
confused deputy attacks. This update requires developers to explicitly opt in to
new security standards to maintain app compatibility and user protection.

Key impacts for developers include:

- **BAL hardening \& improved opt-in:** We are refining Background Activity Launch (BAL) restrictions by extending protections to [`IntentSender`](https://developer.android.com/reference/android/content/IntentSender). Developers must migrate away from the legacy [`MODE_BACKGROUND_ACTIVITY_START_ALLOWED`](https://developer.android.com/reference/android/app/ActivityOptions#MODE_BACKGROUND_ACTIVITY_START_ALLOWED) constant. Instead, you should adopt granular controls like [`MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE`](https://developer.android.com/reference/android/app/ActivityOptions#MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE), which restricts activity starts to scenarios where the calling app is visible, significantly reducing the attack surface.
- **Adoption tools:** Developers should utilize strict mode and updated lint checks to identify legacy patterns and ensure readiness for future target SDK requirements.

### Enable CT by default

If an app targets Android 17 (API level 37) or higher,
[certificate transparency (CT)](https://developer.android.com/privacy-and-security/security-config#CertificateTransparencySummary) is enabled by default. (On Android 16, CT is
available but apps had to [opt in](https://developer.android.com/privacy-and-security/security-config#certificateTransparency).)

### Safer Native DCL---C

If your app targets Android 17 (API level 37) or higher, the Safer Dynamic
Code Loading (DCL) protection introduced in Android 14 for DEX and JAR files now
extends to native libraries.

All native files loaded using `System.load()` must be marked as read-only.
Otherwise, the system throws `UnsatisfiedLinkError`.

We recommend that apps avoid dynamically loading code whenever possible, as
doing so greatly increases the risk that an app can be compromised by code
injection or code tampering.

### Restrict PII fields in CP2 data view

For apps targeting Android 17 (API level 37) and
higher, Contacts Provider 2 (CP2) restricts certain columns containing
Personally Identifiable Information (PII) from the data view. When this change
is enabled, these columns are removed from the data view to enhance user
privacy.
The restricted columns include:

- [`ACCOUNT_NAME`](https://developer.android.com/reference/android/provider/ContactsContract.SyncColumns#ACCOUNT_NAME)
- [`ACCOUNT_TYPE`](https://developer.android.com/reference/android/provider/ContactsContract.SyncColumns#ACCOUNT_TYPE)
- [`ACCOUNT_TYPE_AND_DATA_SET`](https://developer.android.com/reference/android/provider/ContactsContract.RawContactsColumns#ACCOUNT_TYPE_AND_DATA_SET)

Apps that are using these columns from [`ContactsContract.Data`](https://developer.android.com/reference/android/provider/ContactsContract.Data)
can extract them from [`ContactsContract.RawContacts`](https://developer.android.com/reference/android/provider/ContactsContract.RawContacts)
instead, by joining with [`RAW_CONTACT_ID`](https://developer.android.com/reference/android/provider/ContactsContract.DataColumns#RAW_CONTACT_ID).

### Enforce strict SQL checks in CP2

For apps targeting Android 17 (API level 37) and
higher, Contacts Provider 2 (CP2) enforces strict SQL query validation when
the [`ContactsContract.Data`](https://developer.android.com/reference/android/provider/ContactsContract.Data) table is accessed without
[`READ_CONTACTS`](https://developer.android.com/reference/android/Manifest.permission#READ_CONTACTS) permission.

With this change, if an app doesn't have [`READ_CONTACTS`](https://developer.android.com/reference/android/Manifest.permission#READ_CONTACTS)
permission, [`StrictColumns`](https://developer.android.com/reference/android/database/sqlite/SQLiteQueryBuilder#setStrictColumns(boolean)) and
[`StrictGrammar`](https://developer.android.com/reference/android/database/sqlite/SQLiteQueryBuilder#setStrictGrammar(boolean)) options are set when querying
the [`ContactsContract.Data`](https://developer.android.com/reference/android/provider/ContactsContract.Data) table. If a query
uses a pattern that isn't compatible with these, it will be
rejected and cause an exception to be thrown.

## Intelligence

Android 17 includes the following changes to system intelligence.

### Deprecation of setContentCaptureEnabled

Content Capture is enabled by default on certain devices to allow
on-device AI features to analyze screen contents for intelligent experiences.

Starting in Android 17, the
[`ContentCaptureManager.setContentCaptureEnabled(boolean)`](https://developer.android.com/reference/android/view/contentcapture/ContentCaptureManager#setContentCaptureEnabled(boolean))
API method is deprecated. For apps that target Android 17 (API level 37) or
higher, calling `setContentCaptureEnabled(false)` no longer disables Content
Capture.

If your app needs to continue disabling Content Capture or restrict screen
contents from being captured by the system, you must transition to using the
[`FLAG_SECURE`](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#FLAG_SECURE) window layout parameter.

To disable Content Capture, set the `FLAG_SECURE` flag on your window as shown
in the following example:

### Kotlin

    window.setFlags(
        WindowManager.LayoutParams.FLAG_SECURE,
        WindowManager.LayoutParams.FLAG_SECURE
    )

### Java

    getWindow().setFlags(
        WindowManager.LayoutParams.FLAG_SECURE,
        WindowManager.LayoutParams.FLAG_SECURE
    );

For more details, see the
[`WindowManager.LayoutParams.FLAG_SECURE`](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#FLAG_SECURE) reference
documentation.

## Media

Android 17 includes the following changes to media behavior.

### Background audio hardening

Beginning with Android 17, the audio framework enforces restrictions on
background audio interactions including audio playback, audio focus requests,
and volume change APIs to ensure that these changes are started intentionally by
the user.

Some audio restrictions apply to all apps. However, the restrictions are more
stringent if an app targets Android 17 (API level 37). If one of these apps
interacts with audio while it is in the background, it must have a foreground
service running. In addition, the app must meet one or both of these
requirements:

- The foreground service must have while-in-use (WIU) capabilities.
- The app must have the [exact alarm](https://developer.android.com/develop/background-work/services/alarms#exact) permission and be interacting with [`USAGE_ALARM`](https://developer.android.com/reference/android/media/AudioAttributes#USAGE_ALARM) audio streams.

For more information, including mitigation strategies, see [Background audio
hardening](https://developer.android.com/about/versions/17/changes/bg-audio).

## Device form factors

Android 17 includes the following changes to improve user
experience across a range of device sizes and form factors.

### Platform API changes to ignore orientation, resizability, and aspect ratio constraints on large screens (sw\>=600dp)

We introduced Platform API changes in Android 16 to [ignore orientation,
resizability, and aspect ratio restrictions on large screens (sw \>=
600dp)](https://developer.android.com/about/versions/16/behavior-changes-16#ignore-orientation) for apps targeting API level
36 or higher. Developers have the option to opt out of these
changes with SDK 36, but this opt-out will no longer be
available for apps that target Android 17 (API level 37) or higher.

For more information, see [Restrictions on orientation and resizability are
ignored](https://developer.android.com/about/versions/17/changes/ff-restrictions-ignored).

## Connectivity

Android 17 introduces the following change to improve consistency and
align with standard Java `InputStream` behavior for Bluetooth RFCOMM sockets.

### Consistent BluetoothSocket read() behavior for RFCOMM

For apps targeting Android 17 (API level 37), the
[`read()`](https://developer.android.com/reference/java/io/InputStream#read(byte%5B%5D)) method of the `InputStream` obtained from an
RFCOMM-based [`BluetoothSocket`](https://developer.android.com/reference/android/bluetooth/BluetoothSocket) now returns `-1` when the
socket is closed or the connection is dropped.

This change makes RFCOMM socket behavior consistent with LE CoC sockets and
aligns with the standard [`InputStream.read()`](https://developer.android.com/reference/java/io/InputStream#read(byte%5B%5D))
documentation, which states that `-1` is returned when the end of the stream is
reached.

Apps that rely solely on catching an IOException to break out of a read loop may
be impacted by this change and should update the BluetoothSocket read loops to
explicitly check for a return value of `-1`. This ensures the loop terminates
correctly when the remote device disconnects or the socket is closed. For an
example of the recommended implementation, see the
[code snippet](https://developer.android.com/develop/connectivity/bluetooth/transfer-data#example) in the [Transfer Bluetooth data](https://developer.android.com/develop/connectivity/bluetooth/transfer-data)
guide.