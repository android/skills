---
name: restore-credentials
description: Provides knowledge and workflows to implement Android's Restore Credentials
  feature using the androidx.credentials library. Use this skill to create, sign in
  with, and delete restore keys, enabling silent user sign-in on new devices after
  a restore. It covers version compatibility, dependencies, server-side prerequisites,
  and the complete client-side implementation for creating, retrieving, and clearing
  restore keys.
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-11-08'
  keywords:
  - Credential Manager
  - Restore Credentials
  - backup & restore
  - backup
  - restore
  - implementation
---

## Fundamentals

The objective is to implement the **Restore Credentials** feature through the
Android Credential Manager API (`androidx.credentials`). This allows apps that
use or are integrating Credential Manager to silently log users back in when
they restore their app on a new device. Restore Credentials operates
independently of the app's primary authentication method (passwords, passkeys,
federated sign-in) and requires no UI changes to existing sign-in flows.

### Scope

**Crucial:** This skill focuses exclusively on the Android client-side
integration. It does **not** implement the server-side cryptographic
validation logic. The developer must be reminded of this and the
[Points to inform the developer about](#backend-guidelines) after implementation is done.

## Implementation Guidelines

When instructed to implement Restore Credentials on a developer's application,
remember the following:

1. Before the implementation, you **MUST** read and understand the [Two-Tier
   Restoration Architecture](#two-tier-restoration-architecture) and review the [DOs and DON'Ts](#dos-and-donts).
2. After the implementation, you **MUST** present the developer with the [Backend Guidelines](#backend-guidelines) as a reminder for their backend setup. It is important that you remind the developer that they still have to implement the backend.

## Two-Tier Restoration Architecture

To enable a resilient sign-in experience, retrieve credentials through a
**two-tier architecture**:

1. **Tier 1 (Primary - Background):** Executes automatically during device setup using the app's `BackupAgent.onRestoreFinished()` callback. This provides an invisible restoration before the user opens the app for the first time, allowing background sync and notification delivery.
2. **Tier 2 (Secondary - Foreground):** Runs in the Launcher `Activity.onCreate()` to catch failovers if background restoration didn't complete (example: dropped network, delayed restoration) or if `allowBackup` is disabled.

If `allowBackup` in the manifest is set to true, implement both. Otherwise, only
implement tier 2 (Foreground Restoration). Do **NOT** change the value of
`allowBackup` in the manifest.

If you added a `BackupAgent` to the app, you **MUST** also set
`android:fullBackupOnly="true"` in the manifest. Do **NOT** do this if there
already existed a `BackupAgent` in the app before your implementation.

## DOs and DON'Ts

**DO:**

- Do check `AndroidManifest.xml` for the value of allowBackup to determine what you have to implement.
- Do implement a fallback for createCredential: always try calling it first with `isCloudBackupEnabled` set to true. If an `E2eeUnavailableException` is thrown, catch it and retry the call with `isCloudBackupEnabled` set to `false`.
- Do implement a `BackupAgent` (subclass of `android.app.backup.BackupAgent`) if `allowBackup` is true in the manifest.
- Do set `android:fullBackupOnly="true"` in the manifest if you added a `BackupAgent` to the app.
- Call `clearCredentialState()` when the user signs out. This is a mandatory security measure to log the user out fully.
- Do attempt to get the restore key on the first launch of the app on a new device and also within the `BackupAgent.onRestoreFinished()` callback if your app uses it.
- Do ensure that a restore credential is created even if the user is already logged in.
- Do ensure that the credential retrieval and login in `onRestoreFinished()` is performed synchronously (for example using `runBlocking`).
- Do restore notifications in the `BackupAgent` if your app uses them. (For example capture and send FCM token to backend)
- Do ensure that if you implement mock network requests or stubs, you replace all placeholders with valid, properly formatted JSON payloads for the credential requests.
- Do encapsulate credential creation and retrieval into their own dedicated functions. Because credential creation must be called in multiple places (sign-up, sign-in) and retrieval across multiple tiers (`BackupAgent` and Launcher `Activity`), this prevents code duplication.
- Do remind the developer of the [critical guidelines](#implementation-guide) for implementing the backend once you're done with the implementation.
- Do generate a separate restore key for each application if the organization has multiple apps with different package names, as a restore key is tied to a unique application package name.

**DON'T:**

- DON'T change the value of `allowBackup` in `AndroidManifest.xml`. Restore Credentials functionality is not affected by the allowBackup setting, meaning the user will still be automatically logged in when a Restore Credential exists, even if `allowBackup` is false.
- DON'T implement a `BackupAgent` if `allowBackup` is `false` in `AndroidManifest.xml`.
- DON'T assume the credential stored in the `GetCredentialResponse` to be of type `PublicKeyCredential`. It has type `RestoreCredential`.
- DON'T chain `GetRestoreCredentialOption` with any other `CredentialOption` in the construction of a `GetCredentialRequest`.
- DON'T assume `CredentialManager` or the Android system will automatically delete a restore key when a user signs out of the app. You must explicitly call `clearCredentialState` with a `ClearCredentialStateRequest` of `TYPE_CLEAR_RESTORE_CREDENTIAL`.
- DON'T remove any existing calls to `clearCredentialState()`. A `ClearCredentialStateRequest` without a type specified only clears all NON-restore credentials.

## Implementation Guide

Implement the Android client-side code by using the following guide. Follow it
**step-by-step** and don't implement any backend functionality, only remind
the user of the [Backend Guidelines](#backend-guidelines) once you're done.

## Version compatibility

Credential Manager's Restore Credentials works on devices running Android 9 (API
level 28) and higher, Google Play services (GMS) core version `242200000`
(v24.22) or higher, and version `1.5.0` or higher of the `androidx.credentials`
library. On devices without Google Play services or with an older GMS version,
catch `CreateCredentialProviderConfigurationException` and
`GetCredentialProviderConfigurationException` so your app falls back gracefully
to standard sign-in.

## Prerequisites

Configure a WebAuthn [relying party server](#backend-guidelines) similar to the server for
[passkeys](#dos-and-donts). If you already have a [server](#two-tier-restoration-architecture) set up to handle authentication
with passkeys, you can reuse the same WebAuthn registration and assertion
endpoints for restore keys.

> [!NOTE]
> **Note:** While the server-side cryptographic validation is the same for passkeys and restore keys, your client-side app can support restore keys without supporting user-facing passkeys. Store restore keys with a distinct credential flag in your backend database so they don't appear in user-facing passkey management screens.

## Dependencies

Add the following dependencies to your app module's `build.gradle` file:

### Kotlin

```kotlin
dependencies {
    implementation("androidx.credentials:credentials:1.7.0-alpha03")
    implementation("androidx.credentials:credentials-play-services-auth:1.7.0-alpha03")
}
```

### Groovy

```groovy
dependencies {
    implementation "androidx.credentials:credentials:1.7.0-alpha03"
    implementation "androidx.credentials:credentials-play-services-auth:1.7.0-alpha03"
}
```

Restore Credentials is available from version `1.5.0` and higher of the
`androidx.credentials` library. However, it's recommended to use the latest
stable versions of the dependencies where possible.

### How `android:allowBackup` affects Restore Credentials

The [`android:allowBackup`](references/android/guide/topics/manifest/application-element.md) manifest attribute controls app data backup,
which interacts with Restore Credentials in three distinct ways:

- **Restore key transfer (`allowBackup` independent)** : Google Play services transfers the `RestoreCredential` key pair using end-to-end encrypted cloud backup or local device-to-device (D2D) transfer regardless of whether `android:allowBackup` is `true` or `false`. For D2D transfers, the restore key always transfers; the only condition under which it isn't transferred is during a cloud backup when the user has opted out of cloud backup in Android Settings.
- **Background restoration in `BackupAgent` (requires
  `android:allowBackup="true"`)** : Silent background sign-in before the user opens the app on the new device (`BackupAgent.onRestoreFinished()`) only runs when `android:allowBackup="true"` and `android:backupAgent` are configured in your manifest. If `android:allowBackup="false"`, restore the session when the user first launches your main `Activity`. You can also implement both approaches to make restoration more robust.
- **Android Studio Cloud Backup testing (requires
  `android:allowBackup="true"`)** : Testing the **Cloud** backup type in Android Studio's Backup and Restore tool requires `android:allowBackup="true"`, whereas **Device to Device** testing works regardless of `allowBackup`.

## Overview

Restore Credentials follows a three-stage lifecycle across the user's previous
and new devices:

1. [**Create a restore key on the previous device**](#create-restore-key): After the user signs in (or on app startup if the user is already signed in and no restore key is recorded in local storage), fetch creation options from your relying party server and call `credentialManager.createCredential()`.
2. [**Sign in with the restore key on the new device**](#sign-restore): Retrieve the transferred `RestoreCredential` using a two-tier strategy: first silently in `BackupAgent.onRestoreFinished()` (when `android:allowBackup="true"`) so background sync and push notifications resume immediately, and second as a fallback in your launcher `Activity.onCreate()` on first app launch.
3. [**Delete the restore key on sign-out**](#delete-restore): When the user signs out or their session is invalidated by the server, call `credentialManager.clearCredentialState()` with `ClearCredentialStateRequest(ClearCredentialStateRequest.TYPE_CLEAR_RESTORE_CREDENTIAL)` and clear your local sync flag.

## Create a restore key

Your app should cover all cases of a user signing in to ensure active users have
a restore key created. Create the restore key in the following scenarios:

- If the user is signed in and a restore key isn't already created (such as in the `onCreate()` method for the main `Activity`).
- When the user is signing in or completing a new account registration flow.

To avoid calling `createCredential()` on every app launch, store a local sync
flag such as `has_synced_restore_credential` inside local storage. Don't store
this flag in default backed-up `SharedPreferences` or `DataStore` unless that
file is excluded in `data_extraction_rules.xml`; otherwise, Auto Backup copies
`has_synced_restore_credential = true` onto the new device and prevents the new
device from generating or rotating its own restore key.

> [!NOTE]
> **Note:** A restore key is tied to an application's unique package name. If your organization's main app and sub-apps have different package names, create a separate restore key for each app.

### Instantiate Credential Manager

Use your app's activity context to instantiate a `CredentialManager` object.

    // Use your app or activity context to instantiate a client instance of
    // CredentialManager.
    private val credentialManager = CredentialManager.create(context)

### Get credential creation options from your app server

Use a FIDO-compliant library in your app server to send your client app the
`PublicKeyCredentialCreationOptions` JSON required to create the restore
credential. For more information about the server-side implementation, see
[Server-side guidance](https://developers.google.com/identity/passkeys/developer-guides/server-registration).

The creation options JSON follows the same structure as passkey registration:

    {
      "challenge": "<base64url-encoded challenge>",
      "rp": {
        "name": "<relying party name>",
        "id": "<relying party host name>"
      },
      "user": {
        "id": "<base64url-encoded user ID>",
        "name": "<user name>",
        "displayName": "<user display name>"
      },
      "pubKeyCredParams": [
        {
          "type": "public-key",
          "alg": -7
        }
      ],
      "attestation": "none",
      "excludeCredentials": [
        {
            "id": "<base64url-encoded credential ID to exclude>", 
            "type": "public-key"
        }
      ],
      "authenticatorSelection": {
        "requireResidentKey": true,
        "residentKey": "required",
        "userVerification": "required"
      }
    }

Ensure that your creation options JSON meets these requirements:

- `challenge` and `user.id`: Encode both values as unpadded Base64URL strings (replacing `+` with `-`, `/` with `_`, and stripping trailing `=`). Standard Base64 characters or raw integer IDs cause `CreateRestoreCredentialDomException` or `IllegalArgumentException`.
- `rp.id`: Set to your relying party's domain (for example, `example.com`).
- `pubKeyCredParams`: Include `{"type": "public-key", "alg": -7}` (ES256) and `{"type": "public-key", "alg": -257}` (RS256).

### Create the restore key

After receiving the public key creation options JSON from your server, wrap the
JSON in a [`CreateRestoreCredentialRequest`](https://developer.android.com/reference/androidx/credentials/CreateRestoreCredentialRequest) object and call
[`createCredential()`](https://developer.android.com/reference/androidx/credentials/CredentialManager#createCredential(android.content.Context,androidx.credentials.CreateCredentialRequest)) on the `CredentialManager` object.

    // requestJson contains the credential creation options sent by the server
    val createRestoreRequest = CreateRestoreCredentialRequest(requestJson)
    val response = try {
        credentialManager.createCredential(context, createRestoreRequest)
    } catch (e: E2eeUnavailableException) {
        // Fall back to local device-to-device (D2D) restore if cloud E2EE is unavailable
        credentialManager.createCredential(
            context,
            CreateRestoreCredentialRequest(requestJson, isCloudBackupEnabled = false),
        )
    }

#### Key points about the code

- The `CreateRestoreCredentialRequest` object contains the following fields:

  - `requestJson`: The credential creation options sent by the app server in the [Web Authentication API](https://developer.mozilla.org/en-US/docs/Web/API/Web_Authentication_API) format for [`PublicKeyCredentialCreationOptionsJSON`](https://w3c.github.io/webauthn/#dictdef-publickeycredentialcreationoptionsjson).
  - `isCloudBackupEnabled`: `Boolean` field to determine if the restore key
    should be backed up to the cloud. By default, this flag is `true`. This
    field has these values:

    - `true`: (**Recommended**) This value enables the backup of restore keys to the cloud if the user has Google Backup and end-to-end encryption, such as a screen lock, enabled.
    - `false`: This value saves the key locally and not in the cloud. The key is not available on the new device if the user chooses to restore from the cloud, but it still transfers during a local cable or Wi-Fi device-to-device (D2D) setup.

  > [!CAUTION]
  > **Caution:** Set `isCloudBackupEnabled` to `true` on your initial call, and fall back to `isCloudBackupEnabled = false` only if `E2eeUnavailableException` is thrown. If the user disabled cloud backup (or `isCloudBackupEnabled` is `false`) and later restores from a cloud backup, the call to retrieve the restore key fails. Users who restore your app with a cloud backup don't receive the restore key and aren't automatically signed in.

### Handle the credential creation response

The Credential Manager API returns a response of type
[`CreateRestoreCredentialResponse`](https://developer.android.com/reference/androidx/credentials/CreateRestoreCredentialResponse). This response holds the public key
credential registration response in [JSON format](https://w3c.github.io/webauthn/#authenticatorattestationresponse).

Send `response.registrationResponseJson` from your app to your relying party
server to verify and store the public key alongside the user's account, and then
record your local sync marker in local storage. For more information about
server-side verification, see [the guidance for passkeys](references/android/identity/passkeys/create-passkeys.md).

During the restore key creation process, handle these exceptions:

- [`CreateRestoreCredentialDomException`](https://developer.android.com/reference/androidx/credentials/exceptions/restorecredential/CreateRestoreCredentialDomException): This exception occurs if `requestJson` is invalid and doesn't follow the WebAuthn format for [`PublicKeyCredentialCreationOptionsJSON`](https://w3c.github.io/webauthn/#dictdef-publickeycredentialcreationoptionsjson).
- [`E2eeUnavailableException`](https://developer.android.com/reference/androidx/credentials/exceptions/restorecredential/E2eeUnavailableException): This exception occurs if `isCloudBackupEnabled` is `true`, but the user's device doesn't have data backup or end-to-end encryption, such as a screen lock.  
  To ensure that Restore Credentials are created for local device-to-device (D2D) transfers even when cloud E2EE is unavailable, catch `E2eeUnavailableException` and immediately retry `createCredential()` with `CreateRestoreCredentialRequest(requestJson, isCloudBackupEnabled = false)`.
- `IllegalArgumentException`: This exception occurs if `requestJson` is empty, isn't valid JSON, or doesn't have a valid Base64URL-encoded `user.id` that conforms to the WebAuthn [specifications](https://w3c.github.io/webauthn/#dictdef-publickeycredentialcreationoptionsjson).

## Sign in with a restore key

Use Restore Credentials to silently sign in the user during the device setup
process.

### Get credential retrieval options from the app server

Before calling `getCredential()` on the new device, request a WebAuthn
`PublicKeyCredentialRequestOptions` JSON challenge from your app server. Because
the app on the new device doesn't yet know which user is signing in, this
endpoint must be **unauthenticated** (userless discovery). For related server
guidance, see [Sign in with a passkey](references/android/identity/passkeys/sign-in-with-passkeys.md) and the [server-side authentication
guide](https://developers.google.com/identity/passkeys/developer-guides/server-authentication#create_credential_request_options).

The retrieval options JSON has the following structure:

    {
      "challenge": "<your app challenge>",
      "allowCredentials": [],
      "rpId": "<your app server domain>"
    }

Configure the retrieval options JSON on your server as follows:

- `allowCredentials`: Omit this field or pass an empty array (`[]`) because the user's account isn't known before restoration. After `getCredential()` returns, your server identifies the user from the `response.userHandle` field (which matches the `user.id` registered during creation) and looks up the public key by `id` (`credentialId`).
- `userVerification`: Set to `"discouraged"` (or `"preferred"`). Because Restore Credentials runs silently without biometric or screen-lock UI, Android overrides `"required"` to `"discouraged"`, and the authenticator data returned to your server has the User Verified (`uv`) flag set to `false`. Configure your WebAuthn server verifier to accept `uv = false` for restore key assertions.

### Get the restore key

To get the restore key on the new device, wrap the server's `requestJson` in a
`GetRestoreCredentialOption` inside a `GetCredentialRequest` and call
`getCredential()` on the `CredentialManager` object.

> [!CAUTION]
> **Caution:** Don't combine `GetRestoreCredentialOption` with any other `CredentialOption` (such as `GetPublicKeyCredentialOption` or `GetPasswordOption`) in the same `GetCredentialRequest`. Passing multiple options with `GetRestoreCredentialOption` throws an `IllegalArgumentException`.

Implement a **two-tier restoration strategy** so users are signed in whether or
not background app data restore runs:

- **Tier 1 (Background restore in `BackupAgent.onRestoreFinished()`)** : If app data backup is enabled (`android:allowBackup="true"`), subclass [`BackupAgent`](https://developer.android.com/reference/android/app/backup/BackupAgent) and override [`onRestoreFinished()`](https://developer.android.com/reference/android/app/backup/BackupAgent#onRestoreFinished()) (don't use `onRestore()`, which is only called for legacy key-value backups). Because `onRestoreFinished()` is synchronous and `getCredential()` is a Kotlin `suspend` function, bridge the call using `runBlocking(Dispatchers.IO)`. Catch `NoCredentialException` quietly when no restore key is present.
- **Tier 2 (Foreground fallback in launcher `Activity.onCreate()`)** : On the first app launch when no local session exists, call `getCredential()` from `lifecycleScope` before showing your login screen. This handles apps with `android:allowBackup="false"` and recovers if the device was offline when `onRestoreFinished()` ran.

If you add a new `BackupAgent` to an app that already uses Auto Backup with
`android:allowBackup="true"`, also set `android:fullBackupOnly="true"` on the
`<application>` element in `AndroidManifest.xml` so Android continues using
Auto Backup instead of switching to legacy key-value backup.

> [!IMPORTANT]
> **Important:** Push notifications aren't automatically restored when credentials are retrieved. Inside `BackupAgent.onRestoreFinished()` (or immediately after Tier-2 sign-in), fetch a fresh Firebase Cloud Messaging (FCM) registration token using `FirebaseMessaging.getInstance().token.await()` and register it with your backend so background notifications resume before the user opens the app.

        suspend fun getRestoreKey(
            fetchAuthenticationJson: () -> String,
        ) {
            // Fetch the options required to get the restore key
            val authenticationJson = fetchAuthenticationJson()

            // Create the GetRestoreCredentialOption object
            val options = GetRestoreCredentialOption(authenticationJson)
            val getRequest = GetCredentialRequest(listOf(options))

            try {
                val response = credentialManager.getCredential(context, getRequest)

                // Type-check and extract the restore credential
                val credential = response.credential
                if (credential is RestoreCredential) {
                    val responseJson = credential.authenticationResponseJson
                }
            } catch (e: NoCredentialException) {
                // No restore key is present on this device
            }
        }

    class MyBackupAgent : BackupAgent() {

        override fun onRestoreFinished() {
            super.onRestoreFinished()
            val credentialManager = CredentialManager.create(this)
            val restoreFunctions = RestoreCredentialsFunctions(this, credentialManager)

            runBlocking(Dispatchers.IO) {
                restoreFunctions.getRestoreKey(
                    fetchAuthenticationJson = {
                        /* Fetch challenge JSON from server */ ""
                    }
                )
            }
        }
    }

The Credential Manager API returns a [`GetCredentialResponse`](https://developer.android.com/reference/androidx/credentials/GetCredentialResponse) whose
`credential` property is a [`RestoreCredential`](https://developer.android.com/reference/androidx/credentials/RestoreCredential) (not a
`PublicKeyCredential`) containing `authenticationResponseJson`.

### Handle the sign-in response

Send `restoreCredential.authenticationResponseJson` from the app to your relying
party server to verify the cryptographic assertion and issue a new session token
for the user. For server-side verification details, see [Sign in with a
passkey](references/android/identity/passkeys/sign-in-with-passkeys.md).

When designing your backend storage and lifecycle policies for restore keys,
follow these guidelines:

- **Differentiate restore keys from user passkeys** : Tag restore keys in your database so they are hidden from user-facing passkey management screens and exempt from user verification (`uv = true`) checks.
- **Support multiple active devices per user**: A user can have multiple devices (such as a phone and a tablet). Allow multiple active restore keys per user account (for example, keyed by device or installation ID) rather than overwriting a single row.
- **Set an appropriate TTL and clean up orphaned keys**: Uninstalling an app or clearing app data deletes the local restore key without notifying your server. Retain restore keys long enough to survive device upgrade windows (even if the user signs out on the old device after transferring), and prune stale keys based on inactivity TTL or per-user key limits.

## Delete the restore key

Credential Manager is stateless and unaware of user activity, so it doesn't
automatically delete restore keys after use. To delete a restore key, call the
[`clearCredentialState()`](https://developer.android.com/reference/androidx/credentials/CredentialManager#clearCredentialState(androidx.credentials.ClearCredentialStateRequest)) method. For security, delete the key whenever a
user signs out so they aren't silently signed back in on the next app launch.

> [!NOTE]
> **Note:** A user sign-out can also happen remotely, for example when a user changes their password on the web or when they're signed out remotely by the server (such as when your server returns an HTTP `401 Unauthorized` error). If your app detects a **server-side session invalidation**, delete the restore key on the device as well.

Restore keys are removed only in the following situations:

- **System-level actions**: Users uninstall the app or clear its data.
- **App-level calls** : Programmatically delete the key by calling [`clearCredentialState()`](https://developer.android.com/reference/androidx/credentials/CredentialManager#clearCredentialState(androidx.credentials.ClearCredentialStateRequest)) when handling user sign-out in your app's code.

Because a default `ClearCredentialStateRequest()` only clears standard session
state and leaves the restore key intact, perform a **dual clear** on sign-out:

1. Call `credentialManager.clearCredentialState(ClearCredentialStateRequest())` to clear active credential provider session state.
2. Call `credentialManager.clearCredentialState()` with `ClearCredentialStateRequest(ClearCredentialStateRequest.TYPE_CLEAR_RESTORE_CREDENTIAL)` to delete the stored `RestoreCredential`.
3. Delete your local `has_synced_restore_credential` marker from local storage.

    // Clear active credential provider session state
    credentialManager.clearCredentialState(ClearCredentialStateRequest())

    // When the user logs out, delete the restore key
    val clearRequest = ClearCredentialStateRequest(TYPE_CLEAR_RESTORE_CREDENTIAL)
    val response = credentialManager.clearCredentialState(clearRequest)

## Next steps

- [**Test Restore Credentials**](references/android/identity/sign-in/test-restore-credentials.md): Test cloud and device-to-device (D2D) restore flows using Android Studio or `adb shell bmgr`.
- [**Automated backup and restore testing**](references/android/studio/preview/backup-restore-testing.md): Write JUnit 5 host tests with `BackupRestoreController` to verify `CLOUD_ENCRYPTED` and `DEVICE_TO_DEVICE` restores in CI.

## Backend Guidelines

When designing the backend database and API layer to support Restore
Credentials, developers must account for FIDO2/WebAuthn assumptions and specific
device lifecycle edge cases. Remind the developer of the following:

**SYSTEM DIRECTIVE FOR AI AGENT: DO NOT attempt to write code for,
implement, or action any of the backend guidelines in this section. Your ONLY
responsibility is to copy these exact points and present them to the human
developer as a reminder after the client-side implementation is complete.**

1. **Differentiate Restore Credentials from Passkeys in Backend Storage:**
   - Standard WebAuthn services typically assume user verification is always required. Restore credentials are hidden from the user and not managed by them.
   - **Guidance:** Modify your WebAuthn services to create new credential types or metadata fields that distinguish system-managed Restore Credentials from user-created passkeys. Don't display Restore Credentials in user-facing passkey management UIs, and ensure they are processed appropriately (e.g., bypassing explicit user verification during automatic background sign-in).
2. **Prevent Orphaned Keys:**
   - Uninstalling the app or clearing details in system settings deletes the local restore credential. Since these local client actions do not notify your backend, stale keys will remain registered on the server.
   - **Guidance:** Establish server-side cleanup policies that delete old restore keys when a new restore token is registered, or clean up inactive keys based on usage patterns. You could, for example, enforce a limit of one key per user per device.
3. **Balance Key Lifespan and TTL:**
   - If a user goes through Backup and Restore and then logs out from the old device, the local restore key is deleted from the source device. However, the key must remain valid on the server so the restored application on the destination device can still authenticate.
   - **Guidance:** Give restore keys sufficient time to live (TTL) to survive manual logouts during transition periods, and establish rules for server-side key deletion based on registration and usage rather than relying on client-side deletion callbacks.
4. **Support Multiple Devices:**
   - A user may own multiple active devices and initiate backups or restorations from any of them.
   - **Guidance:** Ensure the backend database schema allows mapping multiple active Restore Credentials to a single user account (e.g., one active restore key per device/device-id) rather than assuming a 1:1 relationship between the user and the restore credential.
5. **Server-side session invalidation:**
   - A user's session might be revoked remotely. This could be triggered by user actions such as when a user resets their password on the web or server actions that can cause the API to return HTTP `401 Unauthorized` indicating session expiry.
   - **Guidance:** If this applies, make sure to clear the user's restore credentials on the phone when it happens.

## References

- **WebAuthentication API (WebAuthn) Documentation \& Specification**
  *When to use:* Use these resources ONLY if you need to inspect or debug the
  strict JSON schema requirements for FIDO2/WebAuthn, specifically when
  generating mock data or formatting the `requestJson`
  (`PublicKeyCredentialCreationOptionsJSON`) and `authenticationJson` payloads.

- [MDN Web Authentication API Documentation](https://developer.mozilla.org/en-US/docs/Web/API/Web_Authentication_API): Mozilla Developer Network
  guide and reference for WebAuthn APIs

- [W3C `PublicKeyCredentialCreationOptionsJSON`](https://w3c.github.io/webauthn/#dictdef-publickeycredentialcreationoptionsjson): Data structure definition
  for WebAuthn credential creation requests in JSON format.

- [W3C `PublicKeyCredentialRequestOptionsJSON`](https://w3c.github.io/webauthn/#dictdef-publickeycredentialrequestoptionsjson): Data structure definition
  for WebAuthn authentication or assertion requests in JSON format.
