---
name: android-intent-security
description: Best practices for Android Intent security. Use this skill when auditing component configurations in AndroidManifest.xml (activities, services, receivers) or source code handling incoming Intents (getIntent, getParcelableExtra) to prevent Intent Redirection and unauthorized access.
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-06-22'
  keywords:
  - recipe
  - Android
  - Security
  - Intent
  - Redirection
  - PendingIntent
  - ContentProvider
  - Service
  - Signature
  - Verification
  - Sanitizer
  - Vulnerability
  - Best Practices
---

{% import "_shared/_github.html" as github %}

# Android Intent and Component Security Best Practices

This skill provides guidelines and patterns to secure Android components (Activities, Services, Broadcast Receivers, Content Providers) and handle Intents safely, preventing privilege escalation and unauthorized access.

## Glossary
*   **Intent:** An asynchronous messaging object used to request an action from another app component.
*   **Exported Component:** A component (`android:exported="true"`) that can be launched by other apps on the device.
*   **Sticky Intent:** A broadcast intent that remains in the system cache after it is sent, allowing any app to retrieve its contents.
*   **Signature Permission:** A permission whose protection level is set to `signature`, granted only to apps signed with the same developer key.
*   **onNewIntent:** An Activity lifecycle callback invoked when an activity is launched with `FLAG_ACTIVITY_SINGLE_TOP` and is already running at the top of the history stack.
*   **PendingIntent:** A token granted to a foreign application (e.g., system services) allowing it to execute a predefined Intent with the creator's permissions.
*   **Mutable PendingIntent:** A PendingIntent whose underlying Intent parameters can be modified by the receiving application.
*   **ContentProvider:** A component that encapsulates data and provides it to other applications via standard query/insert interfaces.
*   **IntentSanitizer:** A utility class in AndroidX Core used to build a safe, sanitized copy of an incoming Intent by filtering out unauthorized components, actions, or extras.
*   **Intent Redirection (Forwarding):** A vulnerability where an application receives an intent from an untrusted source and uses it to launch a private, non-exported component.

## Prerequisites
* The agent **MUST** be able to describe the function and security implications of `onCreate`, `onNewIntent`, and the `singleTop` launch mode.
* The agent **MUST** be able to declare `<activity>`, `<service>`, `<receiver>`, and `<provider>` tags in `AndroidManifest.xml` and define their `android:exported` and `android:permission` attributes.
* The agent **MUST** be able to implement signature verification checks using `PackageManager`.

## Limitations
*   This skill focuses on local inter-component and inter-app communication security on the Android platform.
*   This skill **does not** cover network security, web integration, or host-to-server security.

## Setup and Dependencies
- **Android SDK:** Minimum API Level 23 (Android 6.0) is required for standard hardware-backed keystore operations and component validation.
- **AndroidX Core Library:** `androidx.core:core:1.9.0` or higher is **MANDATORY** to leverage `IntentSanitizer`.
- **Standard API Access:** Standard Android `PackageManager` APIs are required for runtime component verification.

---

## Intent Security Logic & Decisions

### 1. Intent Routing Comparison
Evaluate the security features of different intent delivery methods:

| Intent Delivery Method | Scope | Recommended Use Case |
| :--- | :--- | :--- |
| Explicit Intent (Internal) | App Private | Launching internal activities/services |
| Implicit Intent | System Wide | Launching system camera, dialer, or sharing |
| Local Broadcasts (LocalBroadcastManager) | App Private | Internal asynchronous event routing |
| System Broadcasts | System Wide | Receiving system events (NFC, Bluetooth) |

### 2. PendingIntent Mutability Flag Options
Evaluate the security implications of PendingIntent mutability flags:

| Flag Name | Mutability | Recommended Use Case |
| :--- | :--- | :--- |
| `PendingIntent.FLAG_IMMUTABLE` | Immutable | Default for almost all PendingIntents (alarms, notifications) |
| `PendingIntent.FLAG_MUTABLE` | Mutable | Inline notifications replies, slice actions (requires explicit target intent) |

### 3. Intent Handling & Redirection Logic

IF (the component receives a nested Intent as an extra) {
    IF (AndroidX Core 1.9.0+ is available) {
        MUST construct an `IntentSanitizer` to explicitly allowlist components, actions, data, and extras.
        MUST call `sanitizeByThrowing()` or `sanitizeByFiltering()` before launching.
    } ELSE {
        MUST verify that the nested Intent's target package matches the current application package.
        MUST verify that the target component of the nested Intent is publicly exported.
    }
    NEVER launch the nested Intent directly without validation.
} ELSE IF (the component handles system broadcasts) {
    MUST verify that the sender UID matches the system framework (UID 1000).
}

### 4. PendingIntent Security Logic

IF (a PendingIntent is created for delivery to another application) {
    MUST use `PendingIntent.FLAG_IMMUTABLE` by default.
    IF (the PendingIntent must be mutable) {
        MUST set the explicit target component or package name on the base `Intent`.
        NEVER create an implicit, mutable `PendingIntent`.
    }
}

### 5. ContentProvider Security Logic

IF (the ContentProvider is only for internal app use) {
    MUST set `android:exported="false"`.
} ELSE {
    MUST protect it with `android:readPermission` and `android:writePermission`.
    MUST set `android:grantUriPermissions="false"` unless temporary URL access is strictly required.
}

### 6. Service Caller Verification Logic

IF (an exported service communicates with trusted sister/partner apps) {
    MUST retrieve the calling package name using `Binder.getCallingUid()`.
    MUST verify that the calling package signature fingerprint matches your trusted certificate hash.
}

---

## Code and Configuration Patterns

### 1. Safe Intent Redirection (Manual Verification)
Validate the target of a nested intent before launching it when modern sanitization libraries are unavailable.

*   **Expected Inputs:**
    *   An incoming `Intent` containing a nested `Intent` extra named `EXTRA_NESTED_INTENT`.
*   **Expected Outputs:**
    *   Launches the target component if safe; throws `SecurityException` if validation fails.

{{
    github.code_snippet(
        region_tag="android_security_intent_redirect_manual",
        dir="security/src/main/java/com/example/snippets/security/intents",
        file="IntentSecurityActivity.kt"
    )
}}

### 2. Safe Intent Redirection using IntentSanitizer
Filter or reject dynamic intents using AndroidX `IntentSanitizer` (AndroidX Core 1.9.0+).

*   **Expected Inputs:**
    *   An untrusted incoming `Intent`.
*   **Expected Outputs:**
    *   `Intent`: A sanitized copy containing only allowlisted components, categories, and actions. Throws `SecurityException` on violations if using `sanitizeByThrowing()`.

{{
    github.code_snippet(
        region_tag="android_security_intent_redirect_sanitizer",
        dir="security/src/main/java/com/example/snippets/security/intents",
        file="IntentSecurityActivity.kt"
    )
}}

### 3. Custom Signature Permission Protection
Declare a custom signature-level permission in the manifest to secure family app communication.

*   **Expected Inputs:** Manifest configuration.
*   **Expected Outputs:** An activity that can only be launched by apps signed with the same developer certificate.

{{
    github.code_snippet(
        region_tag="android_security_custom_permission_manifest",
        dir="security/src/main",
        file="AndroidManifest.xml"
    )
}}

{{
    github.code_snippet(
        region_tag="android_security_custom_permission_activity_manifest",
        dir="security/src/main",
        file="AndroidManifest.xml"
    )
}}

### 4. Safe onNewIntent Lifecycle Verification (Warm Boot Protection)
Ensure that activities reusing dynamic intents (e.g., in background launch paths) apply the same strict security filters inside `onNewIntent`.

*   **Expected Inputs:**
    *   `newIntent` (`Intent`): The newly delivered intent.
*   **Expected Outputs:**
    *   Executes processing logic only if the new intent passes security validation.

{{
    github.code_snippet(
        region_tag="android_security_onnewintent_validate",
        dir="security/src/main/java/com/example/snippets/security/intents",
        file="IntentSecurityActivity.kt"
    )
}}

### 5. Secure PendingIntent Creation
Enforce immutability unless mutability is explicitly required.

*   **Expected Inputs (Immutable):** An intent target.
*   **Expected Outputs (Immutable):** A `PendingIntent` that cannot be altered by the receiver.
*   **Expected Inputs (Mutable):** An intent with an explicit component set.
*   **Expected Outputs (Mutable):** A mutable `PendingIntent` locked to a specific receiver component to prevent hijacking.

{{
    github.code_snippet(
        region_tag="android_security_pendingintent_secure",
        dir="security/src/main/java/com/example/snippets/security/intents",
        file="IntentSecurityActivity.kt"
    )
}}

### 6. Secure ContentProvider Configuration & Queries
Expose a ContentProvider securely and parameterize queries to prevent SQL Injection.

*   **Expected Inputs:**
    *   `uri` (`Uri`): The query URI.
    *   `projection` (`String[]`): Columns to retrieve.
    *   `selection` (`String`): Query criteria.
    *   `selectionArgs` (`String[]`): Values mapping to selection placeholders (`?`).
*   **Expected Outputs:**
    *   `Cursor`: Filtered query results, strictly bound to projection maps.

{{
    github.code_snippet(
        region_tag="android_security_contentprovider_manifest",
        dir="security/src/main",
        file="AndroidManifest.xml"
    )
}}

{{
    github.code_snippet(
        region_tag="android_security_contentprovider_secure",
        dir="security/src/main/java/com/example/snippets/security/intents",
        file="SecureDataProvider.kt"
    )
}}

### 7. Service Caller Signature Verification
Verify the calling application's signature before binding to a service.

*   **Expected Inputs:**
    *   `intent` (`Intent`): The binding request intent.
*   **Expected Outputs:**
    *   `IBinder`: Local binder instance if caller signature matches trusted partner; throws `SecurityException` otherwise.

{{
    github.code_snippet(
        region_tag="android_security_service_caller_verify",
        dir="security/src/main/java/com/example/snippets/security/intents",
        file="SecureBoundService.kt"
    )
}}

---

## Error Handling

Handle component binding, database queries, and intent redirection failures securely to avoid exposing internal structures.

{{
    github.code_snippet(
        region_tag="android_security_error_handling_secure",
        dir="security/src/main/java/com/example/snippets/security/intents",
        file="IntentSecurityActivity.kt"
    )
}}

```kotlin
// Secure handling of ContentProvider queries on the client side:
try {
    val cursor = contentResolver.query(providerUri, projection, selection, selectionArgs, null)
} catch (e: SQLiteException) {
    Log.e("PROVIDER_ERROR", "ContentProvider database query failed", e)
    // Secure handling: prevent raw query syntax details from leaking to UI
}
```

---

## Reporting Guidelines

When this skill is executed to apply security hardening updates to a codebase, the agent **MUST** generate a structured "Best Practices & Security Alignment Update" report for the developer. The report **MUST** be written to the session artifact folder (or printed in the final response) and include:

1.  **Security Alignment Area:** The category of improvement applied (e.g., Safe Intent Redirection, Secure PendingIntent Configuration, ContentProvider Data Guarding).
2.  **Impact & Priority:** The potential safety risk addressed by the update (e.g., Component Hijacking Prevention, Private Data Isolation).
3.  **Scope of Changes:** A list of all modified classes, XML files, and dependencies.
4.  **Implementation Summary:** Concrete details of the solution (e.g., "Updated nested intent parsing to utilize the `IntentSanitizer` API with a strict component allowlist").
5.  **Code Diff:** Standard unified diffs showing the exact modifications.

### Best Practices & Security Alignment Update Template
Use the following markdown template when reporting changes to developers:
```markdown
### Best Practices & Security Alignment Update: [Security Alignment Area]

*   **Improvement Description:** [Brief description of the hardening update and why it is recommended]
*   **Priority Level:** [High / Medium / Low]
*   **Alignment Action:** [Summary of updates, e.g., converted to FLAG_IMMUTABLE]

#### Files Modified
*   `[Relative path to File 1]`
*   `[Relative path to File 2]`

#### Implementation Diff
```diff
// Insert Unified Diff here
```

#### Testing & Verification
1. [Step 1 to verify the component behaves correctly, e.g., run component unit test]
2. [Step 2 to verify regression safety]
```

---

## Antipatterns (DON'Ts)
- **NEVER** launch a nested `Intent` received from an untrusted source without verifying its target package and exported status.
- **NEVER** use sticky broadcasts (`sendStickyBroadcast()`).
- **NEVER** assume an exported component is safe because it runs in a background thread or performs internal checks.
- **NEVER** expose sensitive functionalities (like SSO authentication or payment processors) to components without signature-level permission restrictions.
- **NEVER** process incoming intents in `onNewIntent` without applying the same security controls as `onCreate`.
- **NEVER** create a mutable `PendingIntent` without setting an explicit target component in the base `Intent`.
- **NEVER** use dynamic string concatenation to construct selection blocks inside a `ContentProvider` query.

## Best Practices (DOs)
- **MUST** explicitly set `android:exported="false"` for all components that do not need external communication.
- **MUST** protect all exported components with custom permissions utilizing `android:protectionLevel="signature"` when communicating between family apps.
- **MUST** validate all incoming intent extras and handle missing parameters gracefully to prevent crashes.
- **MUST** verify the sender's identity (system UID or package signature) when exported components receive system or framework broadcasts (e.g., NFC, Bluetooth).
- **MUST** call `setIntent(newIntent)` inside `onNewIntent()` before processing payloads to keep active references updated.
- **MUST** use `PendingIntent.FLAG_IMMUTABLE` by default when constructing `PendingIntent` instances.
- **MUST** protect exported `ContentProviders` with `readPermission` and `writePermission`.
- **MUST** enforce parameterized selection structures in `ContentProvider` query/update methods.
- **MUST** verify the package signature fingerprint of binding applications at runtime inside exported services.
- **MUST** use `androidx.core.content.IntentSanitizer` to sanitize incoming dynamic intents before redirection, if AndroidX Core 1.9.0+ is imported in the project.
