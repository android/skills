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
  last-updated: '2026-09-01'
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
level 28) and higher, Google Play services (GMS) core version 24220000 or
higher, and version 1.5.0 or higher of the `androidx.credentials` library.

## Prerequisites

Set up a [relying party server](#backend-guidelines) similar to the server for [passkeys](#dos-and-donts). If
you already have a [server](#two-tier-restoration-architecture) set up to handle authentication with passkeys,
use the same server-side implementation for restore keys.

> [!NOTE]
> **Note:** While the server-side implementation is the same for passkeys and restore keys, your client-side app can support restore keys without supporting passkeys. Because restore keys work independently of the authentication method in your app (for example, passwords or Sign in with Google), you don't need to make any additional changes to the existing authentication methods in your app's code.

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

Restore Credentials is available from version 1.5.0 and higher of the
androidx.credentials library. However, it's recommended to use the latest stable
versions of the dependencies where possible.

> [!NOTE]
> **Note:** The Restore Credentials feature works regardless of whether [`allowBackup`](references/android/guide/topics/manifest/application-element.md) is set in the manifest.

## Overview

1. [**Create a restore key**](#create-restore-key): To create a restore key, complete the following steps:
   1. [**Instantiate Credential Manager**](#implementation-guide): Create a `CredentialManager` object.
   2. [**Get credential creation options from the app server**](https://developer.mozilla.org/en-US/docs/Web/API/Web_Authentication_API): Send the client app the details required to create the restore key from your app server.
   3. [**Create the restore key**](https://w3c.github.io/webauthn/#dictdef-publickeycredentialcreationoptionsjson): Create a restore key for the user's account if the user is signed in to your app.
   4. [**Handle the credential creation response**](https://w3c.github.io/webauthn/#dictdef-publickeycredentialrequestoptionsjson): Send the credentials from your client app to your app server for processing, and handle any exceptions.
2. [**Sign in with a restore key**](#sign-restore): To sign in with a restore key, complete the following steps:
   1. [**Get credential retrieval options from the app server**](#get-credential-retrieval): Send the client app the details required to retrieve the restore key from your app server.
   2. [**Get the restore key**](#get-restore): Request the restore key from Credential Manager when the user sets up a new device. This lets the user sign in without additional input.
   3. [**Handle the credential retrieval response**](#handle-sign-in): Send the restore key from the client app to the app server to sign in the user.
3. [**Delete a restore key**](#delete-restore).

## Create a restore key

Your app should cover all cases of a user signing in to ensure active users have
a restore key created. Create the restore key in the following scenarios:

- If the user is signed in and a restore key isn't already created (such as in the `onCreate` method for the main `Activity`).
- When the user is signing in or completing a new account registration flow.

To optimize performance and avoid the overhead of creating or checking for a
restore credential on every single login, set a `boolean` flag or a credential
creation timestamp in local storage, such as `has_synced_restore_credential`, to
track whether the key has already been created.

> [!NOTE]
> **Note:** A restore key is tied to an application's unique package name. If your organization's main app and sub-apps have different package names, create a separate restore key for each app.

### Instantiate Credential Manager

Use your app's activity context to instantiate a `CredentialManager` object.

    // Use your app or activity context to instantiate a client instance of
    // CredentialManager.
    private val credentialManager = CredentialManager.create(context)

### Get credential creation options from your app server

Use a FIDO-compliant library in your app server to send your client app the
information required to create the restore credential, such as information about
the user, the app, and additional configuration properties. For more information
about the server-side implementation, see [Server-side
guidance](https://developers.google.com/identity/passkeys/developer-guides/server-registration).

### Create the restore key

After parsing the public key creation options sent by the server, create a
restore key by wrapping these options in a
[`CreateRestoreCredentialRequest`](https://developer.android.com/reference/androidx/credentials/CreateRestoreCredentialRequest) object and calling the
[`createCredential()`](https://developer.android.com/reference/androidx/credentials/CredentialManager#createCredential(android.content.Context,androidx.credentials.CreateCredentialRequest)) method with the `CredentialManager` object.

    // createRestoreRequest contains the details sent by the server 
    val response = credentialManager.createCredential(context, createRestoreRequest)

#### Key points about the code

- The `CreateRestoreCredentialRequest` object contains the following fields:

  - `requestJson`: The credential creation options sent by the app server in the [Web Authentication API](https://developer.mozilla.org/en-US/docs/Web/API/Web_Authentication_API) format for [`PublicKeyCredentialCreationOptionsJSON`](https://w3c.github.io/webauthn/#dictdef-publickeycredentialcreationoptionsjson).
  - `isCloudBackupEnabled`: `Boolean` field to determine if the restore key
    should be backed up to the cloud. By default, this flag is `true`. This
    field has these values:

    - `true`: (**Recommended**) This value enables the backup of restore keys to the cloud if the user has Google Backup and end-to-end encryption, such as a screen lock, enabled.
    - `false`: This value saves the key locally and not in the cloud. The key is not available on the new device if the user chooses to restore from the cloud.

  > [!CAUTION]
  > **Caution:** It's recommended to set `isCloudBackupEnabled` to `true`. If cloud backup is disabled and the user restores from a cloud backup, the call to retrieve the restore key fails. Users who restore your app with a cloud backup don't receive the restore key and are not automatically signed in.

### Handle the credential creation response

The Credential Manager API returns a response of type
[`CreateRestoreCredentialResponse`](https://developer.android.com/reference/androidx/credentials/CreateRestoreCredentialResponse). This response holds the public key
credential registration response in [JSON format](https://w3c.github.io/webauthn/#authenticatorattestationresponse).

Send the public key from your app to the relying party server. This public key
is similar to the public key generated when you create a passkey. The same code
that handles passkey creation on the server can also handle restore key
creation. For more information about the server-side implementation, see [the
guidance for passkeys](references/android/identity/passkeys/create-passkeys.md).

During the restore key creation process, handle these exceptions:

- [`CreateRestoreCredentialDomException`](https://developer.android.com/reference/androidx/credentials/exceptions/restorecredential/CreateRestoreCredentialDomException): This exception occurs if `requestJson` is invalid and does not follow the WebAuthn format for [`PublicKeyCredentialCreationOptionsJSON`](https://w3c.github.io/webauthn/#dictdef-publickeycredentialcreationoptionsjson).
- [`E2eeUnavailableException`](https://developer.android.com/reference/androidx/credentials/exceptions/restorecredential/E2eeUnavailableException): This exception occurs if `isCloudBackupEnabled` is `true`, but the user's device doesn't have data backup or end-to-end encryption, such as a screen lock.  
  To ensure that Restore Credentials are created in all cases, you must handle the `E2eeUnavailableException` explicitly by calling `createCredential` with `isCloudBackupEnabled` set to `true`. If `E2eeUnavailableException` is thrown, catch and call `createCredential` again with `isCloudBackupEnabled` set to `false`.
- `IllegalArgumentException`: This exception occurs if `createRestoreRequest` is empty or not valid JSON, or if it doesn't have a valid `user.id` that conforms to the WebAuthn [specifications](https://w3c.github.io/webauthn/#dictdef-publickeycredentialcreationoptionsjson).

## Sign in with a restore key

Use Restore Credentials to silently sign in the user during the device setup
process.

### Get credential retrieval options from the app server

Send the client app the options required to get the restore key from the server.
For similar passkey guidance for this step, see [Sign in with a passkey](references/android/identity/passkeys/sign-in-with-passkeys.md).
For more information about the server-side implementation, see the [server-side
authentication guide](https://developers.google.com/identity/passkeys/developer-guides/server-authentication#create_credential_request_options).

### Get the restore key

To get the restore key on the new device, call the `getCredential()` method on
the `CredentialManager` object.

It's recommended to fetch the restore key in both of the following scenarios:

- On the first launch of the app on the device. Credential restoration in this scenario is independent of restoration of the app data.
- If app data backup and restore is enabled, get the restore key immediately after the app data is restored. Use [`BackupAgent`](https://developer.android.com/reference/android/app/backup/BackupAgent) to configure your app's backup and ensure you complete the `getCredential` functionality within the [`onRestoreFinished`](https://developer.android.com/reference/android/app/backup/BackupAgent#onRestoreFinished()) callback. Don't use the `onRestore` method, as it is only called for key-value backups, whereas `onRestoreFinished` is reliably called for any kind of backup restore. This avoids potential delays when users open their new device for the first time and lets users interact with the app without waiting for them to open your app. For example, this lets your app send the user notifications before they open the app for the first time on the new device, which is particularly relevant for messaging or communications apps.

If you newly create a `BackupAgent` and previously had backup enabled with
`allowBackup="true"`, set the boolean value `android:fullBackupOnly="true"`in
your app's manifest. This ensures that your app's backup and restore behavior is
maintained.

> [!IMPORTANT]
> **Important:** Notifications aren't automatically restored after the restore credentials are retrieved. If you use Firebase to handle notifications, you must fetch and send the Firebase Cloud Messaging (FCM) token to the backend to successfully resume background messaging and notifications.

    // Fetch the options required to get the restore key
    val authenticationJson = fetchAuthenticationJson()

    // Create the GetRestoreCredentialRequest object
    val options = GetRestoreCredentialOption(authenticationJson)
    val getRequest = GetCredentialRequest(listOf(options))

    val response = credentialManager.getCredential(context, getRequest)

    // Type-check and extract the restore credential
    val credential = response.credential as RestoreCredential

The credential manager APIs return a response of type
[`GetCredentialResponse`](https://developer.android.com/reference/android/credentials/GetCredentialResponse). The credential contained in this response is
explicitly of type `RestoreCredential`, which holds the public key.

### Handle the sign-in response

Send the public key from the app to the relying party server, which can then be
used to sign in the user. On the server side, this action is similar to signing
in using a passkey. The same code that handles sign-in with passkeys on the
server can also handle sign-ins with restore keys. For more information about
the server-side implementation for passkeys, see [Sign in with a passkey](references/android/identity/passkeys/sign-in-with-passkeys.md).

> [!NOTE]
> **Note:** Even though restore keys and passkeys use the same underlying server implementation, differentiate between them when saving them in your app server's database. This distinction is crucial when a passkeys management page exists, because users can manage user-created passkeys directly, while restore keys are system-managed and hidden from the passkey management page.

## Delete the restore key

Credential Manager is stateless and unaware of user activity, so it doesn't
automatically delete restore keys after use. To delete a restore key, call the
`clearCredentialState()` method. For security, delete the key whenever a user
signs out. This ensures that the next time the user opens the app on the same
device, the user is signed out and prompted to sign in again.

Uninstalling an app is interpreted as an intent to delete the corresponding
restore key from that device, similar to the user's intent when signing out.

Restore keys are removed only in the following situations:

- **System-level actions**: Users uninstall the app or clear its data.
- **App-level calls** : Programmatically delete the key by calling [`clearCredentialState()`](https://developer.android.com/reference/androidx/credentials/CredentialManager#clearCredentialState(androidx.credentials.ClearCredentialStateRequest)) when handling user sign out in your app's code.

When the user signs out of your app, call the `clearCredentialState()` method on
the `CredentialManager` object.

    // Create a ClearCredentialStateRequest object
    val clearRequest = ClearCredentialStateRequest(TYPE_CLEAR_RESTORE_CREDENTIAL)

    // When the user logs out, delete the restore key
    val response = credentialManager.clearCredentialState(clearRequest)

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
