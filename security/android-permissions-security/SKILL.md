---
name: android-permissions-security
description: Audits, detects gaps, and remediates Android permissions and IPC component
  security vulnerabilities. Use when reviewing AndroidManifest.xml, defining custom
  permissions, securing Services, Receivers, and Providers, verifying caller signatures
  and UID using Binder, or implementing runtime permission flows.
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-09-25'
  keywords:
  - recipe
  - Android
  - Security
  - Permissions
  - IPC
  - Binder
  - UID
  - Caller Verification
  - knownSigner
  - signature
  - ContentProvider
  - Service
  - Receiver
  - Broadcast
  - UriPermission
  - runtime permissions
  - SecurityException
  - Photo Picker
  - Vulnerability
  - Remediation
---

Guidelines for auditing permission vulnerabilities, enforcing least privilege,
securing IPC boundaries, and handling runtime permissions across the Android
ecosystem.

## Glossary

- **Runtime Permission:** Dangerous permission (e.g., Camera, Location) that must be granted by the user at runtime on Android 6.0+ (API 23+).
- **Custom Permission:** A permission defined by an app (`<permission>`) to restrict access to its exported components.
- **Protection Level:** Attribute defining how the OS grants the permission (`normal`, `dangerous`, `signature`, `knownSigner`).
- **knownSigner:** Protection flag (API 31+) granting signature-level access to partner apps matching trusted certificate digests.
- **SigningInfo:** Object (API 28+) providing signing history and multiple signer certificates for an application.
- **Binder Calling Identity:** Reliable UID representing the calling process obtained using `Binder.getCallingUid()`.
- **URI Permission:** Temporary, granular access granted to a content URI using `grantUriPermissions` and intent flags.

## Prerequisites

- Add `androidx.core:core-ktx` and `androidx.activity:activity-ktx` (or `androidx.fragment:fragment-ktx`) dependencies to the app-level `build.gradle` file for `ContextCompat` and `ActivityResultContracts`.
- For `knownSigner` partner permissions (`android:knownCerts`), ensure the project `compileSdk` and `targetSdk` are at least 31 (Android 12).
- For broadcast identity sharing (`BroadcastOptions.setShareIdentityEnabled`), ensure `compileSdk` and `targetSdk` are at least 34 (Android 14).

*** ** * ** ***

## 1. Security audit and permission gap detection playbook

When auditing an Android project for permission and IPC security
vulnerabilities, systematically inspect each area:

| Vulnerability Area | Search In / Pattern | Risk | Remediation Action |
|---|---|---|---|
| **Weak Custom Permission Protection** | `AndroidManifest.xml` with `android:protectionLevel="normal"`, `"dangerous"`, or `"signatureOrSystem"` | Unauthorized apps request or claim permission without restriction | Upgrade to `signature` or `signature|knownSigner` with `android:knownCerts`. |
| **Overly Broad URI Permission Grants** | `<grant-uri-permission android:pathPrefix="/" />` or `pathPrefix=""` in `<provider>` | Leaks all provider data to third parties | Replace with explicit scoped subpath (e.g. `android:pathPrefix="/shared/"`). Retain `android:grantUriPermissions="true"`. Remove any wildcard `pathPrefix="/"`. |
| **Missing ContentProvider Split Permissions** | `<provider>` with only generic `android:permission` or missing `android:readPermission` and `android:writePermission` | Read-only callers execute write operations or write-only callers access read operations | Configure granular `android:readPermission="...READ_DATA"` and `android:writePermission="...WRITE_DATA"`, AND declare the custom `<permission>` tags with `android:protectionLevel="signature"` (or `signature|knownSigner`) at the `<manifest>` root. |
| **Spoofable Caller Identity Checks** | `intent.getStringExtra("calling_package")`, `intent.getStringExtra("sender")`, `callingPackage == "..."` in Services | Malicious callers forge intent extras or strings | Remove string checks; authenticate caller UID cryptographically using `Binder.getCallingUid()` and `pm.hasSigningCertificate()`. |
| **Bound Service Permission Bypass** | `checkCallingOrSelfPermission()` or `enforceCallingOrSelfPermission()` in Binder AIDL stubs | Falls back to host app UID outside IPC or when identity is cleared, causing Confused Deputy bypass | Replace with `enforceCallingPermission("...WRITE_DATA", ...)` or `checkCallingPermission("...WRITE_DATA")`. |
| **Missing Method-Level Checks in Bound Services** | Bound Service AIDL methods mutating state without enforcing write permissions | Callers with read-only binding access invoke mutating write operations | Add `enforceCallingPermission("...WRITE_DATA", "Caller lacks WRITE_DATA permission")` inside mutating AIDL methods. |
| **Flawed Runtime Permission Flow** | Missing `shouldShowRequestPermissionRationale()`, missing permanent denial handling, or re-requesting in loops | Poor user experience, infinite loops, or inability to recover permissions | Implement 3-state flow: check grant -\> check rationale dialog -\> launch contract. On permanent denial, redirect to `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`. |
| **Simultaneous Location Requests** | `arrayOf(ACCESS_FINE_LOCATION, ACCESS_BACKGROUND_LOCATION)` in one request | Rejected by Android 11+ (API 30+) | Request foreground location (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`) first. Request `ACCESS_BACKGROUND_LOCATION` only after foreground is granted in a separate user step. |
| **Broad Storage Permissions for Media** | Requesting `READ_EXTERNAL_STORAGE` or `READ_MEDIA_IMAGES` for user image selection | Over-privilege, user privacy violation | Migrate to zero-permission Photo Picker (`ActivityResultContracts.PickVisualMedia`). |
| **Cached Permission State** | Storing permission status in `var cachedLocationPermissionGranted = ...` | Fails when permission is revoked or expires | Query `ContextCompat.checkSelfPermission()` dynamically at invocation time and wrap protected calls in try-catch blocks catching `SecurityException`. |
| **Insecure Broadcasts** | `sendBroadcast(intent)` without package target or receiver permission; receiver without sender authentication | Data interception or command spoofing | Set explicit package (`intent.setPackage(...)`), pass receiver permission, and enable `BroadcastOptions.setShareIdentityEnabled(true)` (API 34+) to verify `sentFromPackage`. |

*** ** * ** ***

## 2. Custom permissions and protection levels

When declaring custom permissions or partner certificates in the manifest, you
must follow the instructions in the [`<permission>` element guide](references/android/guide/topics/manifest/permission-element.md).

- **`signature`**: Restricts access to apps signed with the same developer key.
- **`signature|knownSigner`** (API 31+): Restricts access to partner apps whose signing certificate digests match declared hashes in `android:knownCerts`.
- **`normal` or `dangerous`** : Do **not** use for private inter-app IPC.
- **`signatureOrSystem`** : **Deprecated** (API 23). Upgrade deprecated `signatureOrSystem` permissions to `signature`. Avoid in non-system apps.

### Manifest snippets

#### Signature-level custom permission

Use `signature` permissions when restricting component access exclusively to
apps signed with the same developer certificate by declaring the `<permission>`
in the manifest and applying `android:permission` to the target component.


```xml
<permission
    android:name="com.example.snippets.permission.ACCESS_SECURE_API"
    android:protectionLevel="signature" />
```

<br />

#### KnownSigner partner permission (API 31+)

Use `signature|knownSigner` permissions when granting access to specific
external partner apps by declaring the `<permission>` in the manifest with
`android:knownCerts` referencing a resource array of trusted SHA-256
certificate digests.


```xml
<resources xmlns:tools="http://schemas.android.com/tools">
    <!-- SHA-256 fingerprints of trusted partner certificates (lowercase, no colons) -->
    <string-array name="trusted_partner_certs" tools:ignore="Typos">
        <item>103938ee4537e59e8ee792f654504fb8346fc6b346d0bbc4415fc339fcfc8ec1</item>
    </string-array>
</resources>
```

<br />


```xml
<permission
    android:name="com.example.snippets.permission.PARTNER_API"
    android:protectionLevel="signature|knownSigner"
    android:knownCerts="@array/trusted_partner_certs" />
```

<br />

*** ** * ** ***

## 3. Exported component protection and URI permissions

- **Private components** : Always set `android:exported="false"`.
- **Exported components** : Require permissions (`android:permission`, `android:readPermission`, `android:writePermission`).
- **Granular URI Grants** : Retain `android:grantUriPermissions="true"` for secure sharing, but scope grants to specific sub-paths. **Never** use wildcard `pathPrefix="/"`.
- **Custom Permission Declarations** : When splitting read and write permissions on a provider, you **MUST** declare the `<permission>` tags with `android:protectionLevel="signature"` (or `signature|knownSigner`) at the `<manifest>` root level.
- **Remediating Existing Wildcard Grants** : You **MUST** find and remove any existing `<grant-uri-permission android:pathPrefix="/" />` or `pathPrefix=""` and replace it with an explicit scoped subpath (e.g. `android:pathPrefix="/shared/"`).
- **Granular `UriPermission` Management** : Instead of broad manifest grants, use runtime `android.content.UriPermission` tokens for least-privilege per-URI sharing:
  - *Grant temporary access* : `context.grantUriPermission(targetPackage,
    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)` or attach `Intent.FLAG_GRANT_READ_URI_PERMISSION` to outgoing intents.
  - *Persist access* : The receiving app calls `contentResolver.takePersistableUriPermission(uri, modeFlags)` for durable access across reboots.
  - *Audit active grants* : Inspect `contentResolver.persistedUriPermissions` (`List<UriPermission>`) to verify granted URIs (`uriPermission.uri`, `uriPermission.isReadPermission()`, `uriPermission.isWritePermission()`).
  - *Revoke access* : Call `context.revokeUriPermission(uri, modeFlags)` or `contentResolver.releasePersistableUriPermission(uri, modeFlags)` as soon as operations conclude.

### Manifest snippet: Complete secure ContentProvider declaration

Declare the split read and write permissions at the `<manifest>` root:


```xml
<permission
    android:name="com.example.snippets.permission.READ_DATA"
    android:protectionLevel="signature" />
<permission
    android:name="com.example.snippets.permission.WRITE_DATA"
    android:protectionLevel="signature" />
```

<br />

Then configure the provider with those permissions and a scoped URI grant:


```xml
<provider
    android:name=".permissions.SecureDataProvider"
    android:authorities="com.example.snippets.partnerprovider"
    android:exported="true"
    android:readPermission="com.example.snippets.permission.READ_DATA"
    android:writePermission="com.example.snippets.permission.WRITE_DATA"
    android:grantUriPermissions="true">
    <grant-uri-permission android:pathPrefix="/shared/" />
</provider>
```

<br />

### Runtime granular UriPermission management snippet


```kotlin
object UriPermissionManager {

    /** Grants temporary read access to a specific URI for a partner package. */
    fun grantScopedUriAccess(context: Context, targetPackage: String, uri: Uri) {
        context.grantUriPermission(
            targetPackage,
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }

    /** Audits active persisted URI permissions held by the application. */
    fun getActivePersistedPermissions(context: Context): List<UriPermission> {
        return context.contentResolver.persistedUriPermissions
    }

    /** Revokes temporary URI access when data transfer completes. */
    fun revokeScopedUriAccess(context: Context, uri: Uri) {
        context.revokeUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }
}
```

<br />

*** ** * ** ***

## 4. Cryptographic caller identity verification

Never trust caller package names provided in Intents or string arguments
(`getCallingPackage()`, `intent.getStringExtra("calling_package")`,
`intent.getStringExtra("sender")`, `intent.getStringExtra("package")`). Intent
extras and string package names are forged by malicious applications.

### Critical vulnerability: Package name spoofing using intent extras


```kotlin
// VULNERABLE PATTERN: DO NOT DO THIS
val callingPackage = intent?.getStringExtra("calling_package")
if (callingPackage != null && SignatureUtils.verifyPartnerPackage(this, callingPackage)) {
    // A malicious app passes "com.example.partner" in the extra.
    // The signature check verifies the installed partner on disk, but the CALLER was malicious!
}
```

<br />

### Secure pattern: Authenticate calling UID cryptographically

1. **Completely remove** all `intent.getStringExtra("calling_package")`, `intent.getStringExtra("sender")`, and `callingPackage == "..."` checks.
2. Obtain caller identity using `Binder.getCallingUid()`.
3. Verify caller certificate using `PackageManager.hasSigningCertificate()` (API 28+) or `PackageManager.getPackagesForUid()`.

### Complete CallerVerifier implementation

Apply this to the service that currently performs the spoofable check.

- **Placement** : declare `CallerVerifier` in the **same file** as the service you are fixing (for example inside `SecureDataService.kt`). Do not move the verification into a new file and leave the original service untouched.
- **Nullability** : `packageInfo.signatures` is `Array<Signature>?`. Copy the `?: return false` guard exactly as written below. Iterating `packageInfo.signatures` directly fails to compile with "Non-nullable value required to call an 'iterator()' method in a for-loop."


```kotlin
object CallerVerifier {
    private const val TRUSTED_PARTNER_SHA256 =
        "A1B2C3D4E5F60708090A0B0C0D0E0F1011121314151617181920212223242526"

    fun isCallerAuthorized(context: Context): Boolean {
        val callingUid = Binder.getCallingUid()
        if (callingUid == android.os.Process.myUid()) return true

        val pm = context.packageManager
        // Modern API 28+ check by UID:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val certBytes = hexStringToByteArray(TRUSTED_PARTNER_SHA256)
            if (pm.hasSigningCertificate(callingUid, certBytes, PackageManager.CERT_INPUT_SHA256)) {
                return true
            }
        }

        // Fallback for legacy APIs:
        val callingPackages = pm.getPackagesForUid(callingUid) ?: return false
        for (pkg in callingPackages) {
            if (verifyPackageSignature(pm, pkg)) {
                return true
            }
        }
        return false
    }

    @Suppress("DEPRECATION")
    private fun verifyPackageSignature(pm: PackageManager, packageName: String): Boolean {
        return try {
            val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            val signatures = packageInfo.signatures ?: return false
            for (sig in signatures) {
                val digest = java.security.MessageDigest.getInstance("SHA-256").digest(sig.toByteArray())
                val hex = digest.joinToString("") { "%02X".format(it) }
                if (hex.equals(TRUSTED_PARTNER_SHA256, ignoreCase = true)) return true
            }
            false
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
        }
        return data
    }
}
```

<br />

*** ** * ** ***

## 5. Method-level Binder enforcement in bound services (SecureBoundService.kt)

In bound services exposing AIDL Binder stubs, protect state-modifying methods
using `enforceCallingPermission()`.

### Critical security rule: Never use CallingOrSelfPermission in services

- **NEVER** use `checkCallingOrSelfPermission()` or `enforceCallingOrSelfPermission()`.
- **Why:** During an active Binder transaction, this method checks the caller's permission. However, if the method is invoked locally (outside of an active IPC) or if the IPC calling identity is dropped, it falls back to checking the host app's permissions. Because the host app holds the permission, this fallback grants access and bypasses the security boundary, causing a Confused Deputy vulnerability.
- **MUST USE:** `enforceCallingPermission("com.example.snippets.permission.WRITE_DATA",
  "Caller lacks WRITE_DATA permission")` or `checkCallingPermission("com.example.snippets.permission.WRITE_DATA")`.

### Complete SecureBoundService implementation


```kotlin
class SecureBoundService : Service() {

    private val binder = object : IMyService.Stub() {
        override fun getData(): String {
            // Read-only operation guarded by manifest-level permission
            return "Confidential Data"
        }

        override fun modifyData(newData: String) {
            // MUST use enforceCallingPermission or checkCallingPermission.
            // NEVER use checkCallingOrSelfPermission or enforceCallingOrSelfPermission.
            this@SecureBoundService.enforceCallingPermission(
                "com.example.snippets.permission.WRITE_DATA",
                "Caller lacks WRITE_DATA permission"
            )
            updateInternalState(newData)
            Log.d("SecureBoundService", "Data modified to: $newData with proper WRITE_DATA permission check")
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun updateInternalState(data: String) {
        // Internal state update logic
    }
}
```

<br />

*** ** * ** ***

## 6. Safe runtime permission workflow (RuntimePermissionsActivity.kt)

When implementing runtime permission requests or handling permission denial
states, you must follow the instructions in the [Request runtime permissions
guide](references/android/training/permissions/requesting.md) and [Permissions overview](references/android/guide/topics/permissions/overview.md). Implement a 3-state flow without
caching states or creating infinite retry loops:

1. **Check Granted State** : `ContextCompat.checkSelfPermission(this,
   permission) == PackageManager.PERMISSION_GRANTED`.
2. **Check Rationale** : `shouldShowRequestPermissionRationale(permission)` to present educational context before requesting.
3. **Launch Contract** : `ActivityResultContracts.RequestPermission()`.
4. **Handle Permanent Denial** : In the callback, if permission was denied and `!shouldShowRequestPermissionRationale(permission)`, guide the user to `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`.

### Complete RuntimePermissionsActivity.kt implementation


```kotlin
class RuntimePermissionsActivity : ComponentActivity() {

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startCameraPreview()
            } else {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    // User selected 'Don't ask again' or permanently denied.
                    // Direct user to Application Details Settings.
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                    startActivity(intent)
                } else {
                    showSnackbar("Camera permission is required to preview camera feed.")
                }
            }
        }

    fun requestCameraPermissionSafely() {
        val permission = Manifest.permission.CAMERA
        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                startCameraPreview()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                showSnackbar("Camera permission is needed to preview camera feed.")
                cameraLauncher.launch(permission)
            }
            else -> {
                cameraLauncher.launch(permission)
            }
        }
    }

    private fun startCameraPreview() {
        Log.d("RuntimePermissionsActivity", "Camera preview started")
    }

    private fun showSnackbar(msg: String) {
        Log.i("RuntimePermissionsActivity", msg)
    }
}
```

<br />

*** ** * ** ***

## 7. Zero-permission alternatives and sequential flows

When implementing photo or media selection, you must follow the instructions in
the [Photo Picker API guide](https://developer.android.com/training/data-storage/shared/photopicker) to avoid requesting broad storage permissions.

- **Photo Picker** : Prefer `ActivityResultContracts.PickVisualMedia()` over requesting wide storage permissions (`READ_MEDIA_IMAGES`, `READ_EXTERNAL_STORAGE`).
- **Sequential Location Flow** : Request `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` first. Never request `ACCESS_BACKGROUND_LOCATION` simultaneously. Only request background location in a separate step after foreground is granted.

### Photo picker implementation


```kotlin
private val photoPickerLauncher =
    registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            handleImageUri(uri)
        }
    }

fun selectPhoto() {
    photoPickerLauncher.launch(
        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    )
}
```

<br />

### Sequential location request implementation


```kotlin
private val foregroundLocationLauncher =
    registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            startForegroundLocationUpdates()
        }
    }

fun requestForegroundLocation() {
    foregroundLocationLauncher.launch(
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
}

// Background location requested only AFTER foreground is granted and user explicitly opts in:
private val backgroundLocationLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startBackgroundTracking()
        }
    }
```

<br />

*** ** * ** ***

## 8. Stateless checks and error boundaries

- **Never cache permission states** in in-memory variables (e.g. `cachedLocationPermissionGranted`). Check `ContextCompat.checkSelfPermission()` dynamically before each operation.
- **Wrap API calls in try-catch** : Catch `SecurityException` around permission-protected framework calls (e.g., location queries) to handle one-time permission revocation gracefully.


```kotlin
fun performLocationAccess() {
    val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
    if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
        requestForegroundLocation()
        return
    }

    val provider = if (fine == PackageManager.PERMISSION_GRANTED) {
        LocationManager.GPS_PROVIDER
    } else {
        LocationManager.NETWORK_PROVIDER
    }

    try {
        val location = locationManager.getLastKnownLocation(provider)
        processLocation(location)
    } catch (e: SecurityException) {
        Log.e("LocationAccess", "Permission revoked at runtime", e)
    }
}
```

<br />

*** ** * ** ***

## 9. Secure broadcasts (API 34+ share identity)

Use explicit intents, required receiver permissions, or Android 14+ broadcast
options:


```kotlin
// Sender: Enforce permission and share identity
val intent = Intent("com.example.snippets.permission.ACTION_SECRET_UPDATE").apply {
    setPackage("com.example.partner") // Explicit target
}
val isUdc = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
val options = if (isUdc) {
    BroadcastOptions.makeBasic().apply {
        setShareIdentityEnabled(true)
    }.toBundle()
} else null

sendBroadcast(intent, "com.example.snippets.permission.RECEIVE_SECRET_UPDATE", options)
```

<br />


```kotlin
// Receiver: Validate sender on Android 14+
class ProtectedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.snippets.permission.ACTION_SECRET_UPDATE") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val sender = sentFromPackage
                if (sender != "com.example.trusted_sender") {
                    return // Reject unauthorized sender
                }
            }
            processUpdate(intent)
        }
    }

    private fun processUpdate(intent: Intent) {}
}
```

<br />

*Legacy compatibility (Pre-API 34)* : For apps targeting API 33 or lower,
attach an immutable `PendingIntent` extra (`FLAG_IMMUTABLE`) to the broadcast
and authenticate the sender using `pendingIntent.creatorPackage`.

*** ** * ** ***

## Quality checklist

### DO

- **MUST** declare `<permission android:name="..."
  android:protectionLevel="signature" />` (or `signature|knownSigner`) at the `<manifest>` root for custom permissions and ContentProvider split permissions.
- **MUST** explicitly set `android:exported="false"` for all private components to remove the need for custom permission management.
- **MUST** authenticate callers cryptographically using `Binder.getCallingUid()` and certificate hashes (`PackageManager.hasSigningCertificate` or `getPackagesForUid`) rather than checking spoofable intent extras.
- **MUST** check `shouldShowRequestPermissionRationale()` before runtime prompts and during denial handling.
- **MUST** direct user to `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` when permission is permanently denied.
- **MUST** enforce method-level checks with `enforceCallingPermission()` or `checkCallingPermission()` on modifying AIDL methods.
- **MUST** scope ContentProvider URI permissions to explicit sub-paths (e.g. `android:pathPrefix="/shared/"`) while retaining `android:grantUriPermissions="true"`.
- **MUST** catch `SecurityException` around protected API calls.
- **MUST** request foreground and background location permissions sequentially, never simultaneously.

### NEVER

- **NEVER** extract or verify package names from Intent extras (`intent.getStringExtra("calling_package")`, `intent.getStringExtra("sender")`, `intent.getStringExtra("package")`). Always authenticate using `Binder.getCallingUid()` and certificates.
- **NEVER** remediate caller spoofing by merely adding manifest permissions while keeping spoofable intent extra checks.
- **NEVER** use `checkCallingOrSelfPermission()` or `enforceCallingOrSelfPermission()` to guard external caller methods. Always use `enforceCallingPermission()` or `checkCallingPermission()`.
- **NEVER** use deprecated `signatureOrSystem`.
- **NEVER** declare or retain `<grant-uri-permission android:pathPrefix="/"
  />` or `pathPrefix=""`. Always replace with a scoped sub-path.
- **NEVER** create infinite permission request loops on denial.
- **NEVER** request foreground and background location permissions in the same request.
- **NEVER** cache permission grant states in in-memory fields.
