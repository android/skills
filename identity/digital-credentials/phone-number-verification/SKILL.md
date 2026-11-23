---
name: phone-number-verification
description: Verify user phone numbers without SMS in an Android app using the Credential
  Manager DigitalCredential API, OpenID4VP, and carrier-issued TS.43 tokens. Use this
  skill whenever the user asks to verify phone numbers without SMS, implement carrier-based
  phone number verification in an Android project, perform a TelephonyManager.getSimCarrierId
  carrier precheck, or request a TS.43 Digital Credential using CredentialManager,
  GetDigitalCredentialOption, GetCredentialRequest, DigitalCredential, and dc-authorization+sd-jwt
  DCQL queries. Produces Android client-side carrier precheck and Credential Manager
  TS.43 token request code alongside backend OpenID4VP and DCQL request construction
  and token exchange logic. Don't use for SMS OTP, TelephonyManager.getLine1Number
  reads, digital wallet holder registration, or issuing credentials to wallets.
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-11-20'
  keywords:
  - Android
  - identity
  - digital credentials
  - phone number verification
  - DigitalCredential API
  - TS.43 token
  - aggregator
  - OpenID4VP
  - Credential Manager API
  - DCQL
---

Implement phishing-resistant, SMS-free phone number verification in an Android
app using Credential Manager's `DigitalCredential` API to request an encrypted,
carrier-issued `TS.43 token` over OpenID4VP and exchange it with an aggregator
or mobile carrier backend for the verified phone number. When reviewing TS.43
architecture and security standards, consult
[About verifying phone numbers](references/android/identity/digital-credentials/credential-verifier/about-verifying-phone-numbers.md). When implementing client and server code
snippets, consult [Implement phone number verification](references/android/identity/digital-credentials/implement-phone-number-verification.md). When verifying
general `mso_mdoc` or `dc+sd-jwt` wallet credentials, suggest the
`credential-verifier` skill.

## Prerequisites and setup

- **Android version compatibility:** Runtime support for the Phone Number Verification API requires Android 10 (API level 29) or higher (while the `androidx.credentials` library compiles with `minSdk` 23 or higher).
- **Aggregator account:** Requires an account with a mobile carrier or cellular aggregator endpoint that supplies DCQL parameters and exchanges `TS.43` tokens.
- **Manifest permission for carrier precheck:** Declare `<uses-permission android:name="android.permission.READ_PHONE_STATE" />` in `app/src/main/AndroidManifest.xml` to query `TelephonyManager.getSimCarrierId()` before invoking Credential Manager.
- **Build config permission:** Ask for user confirmation before modifying `settings.gradle.kts`, root `build.gradle.kts`, or `gradle/libs.versions.toml`, unless already requested by the user or running non-interactively.
- **Dependencies and package mapping:** Add the Credential Manager artifacts to the app module build script (`app/build.gradle.kts` or `app/build.gradle`; when checking the latest version declarations, consult [Implement phone number verification](references/android/identity/digital-credentials/implement-phone-number-verification.md)):

| Artifact | Key classes, constructor signatures, and package |
|---|---|
| `androidx.credentials:credentials:1.7.0-alpha03` (or `1.6.0`+) | `CredentialManager`, `GetCredentialRequest`, `GetCredentialResponse`, `GetDigitalCredentialOption(requestJson: String)`, `DigitalCredential`, and `ExperimentalDigitalCredentialApi` in `androidx.credentials`; `GetCredentialException`, `GetCredentialCancellationException`, `GetCredentialInterruptedException`, `GetCredentialUnsupportedException`, and `NoCredentialException` in `androidx.credentials.exceptions` |
| `androidx.credentials:credentials-play-services-auth:1.7.0-alpha03` (or `1.6.0`+) | Play services provider integration for Credential Manager |

### Expected inputs and file scope

- **Required inputs:** Whether the app is retrieving the device phone number (`number-verification/device-phone-number/ts43`) or verifying an existing user-entered phone number (`number-verification/verify/ts43`), whether dual SIM (`subscription_hint`) or biometric/screen-lock verification (`user_verification_hint`) is needed, and the fallback verification flow when the carrier or device is unsupported.
- **Files to modify:** `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml` (for `READ_PHONE_STATE`), phone number verification client or repository classes, ViewModels, and UI screens or `Activity` classes (such as `MainActivity.kt`).
- **Forbidden substitutions:** Don't use `TelephonyManager.getLine1Number()`, `READ_PHONE_NUMBERS`, `SmsRetriever`, or SMS OTP APIs as a substitute for the Credential Manager `DigitalCredential` TS.43 verification flow (only use `TelephonyManager.getSimCarrierId()` for the carrier support precheck, and fall back to SMS only when the carrier or device is unsupported).

## Codebase exploration

Search the Android project before modifying files:

1. **Validate baseline build and inspect configuration:** Run `./gradlew assembleDebug` to confirm the project compiles before making changes, check `app/build.gradle.kts` and `gradle/libs.versions.toml` for `minSdk` and `androidx.credentials` dependencies, and check `app/src/main/AndroidManifest.xml` for `READ_PHONE_STATE`.
2. **Locate UI entry points:** Identify the `Activity` or Compose screen (such as `MainActivity.kt` or `TasksScreen.kt`) where phone number verification is triggered so you pass an `Activity` `Context` to `credentialManager.getCredential()`.

## Critical implementation mandates

Follow the end-to-end steps and code snippets in
[Implement phone number verification](references/android/identity/digital-credentials/implement-phone-number-verification.md) and apply these rules:

- **Opt in to experimental APIs:** Annotate classes or functions referencing `GetDigitalCredentialOption` and `DigitalCredential` with `@OptIn(androidx.credentials.ExperimentalDigitalCredentialApi::class)` (omitted from the Kotlin snippet in [Implement phone number verification](references/android/identity/digital-credentials/implement-phone-number-verification.md)).
- **Perform the carrier TS.43 precheck and handle unsupported fallbacks:** Declare `READ_PHONE_STATE` in `AndroidManifest.xml`, query `telephonyManager.getSimCarrierId()`, and check whether the user's carrier supports TS.43 with the app's aggregator backend before calling `credentialManager.getCredential()`. If the carrier is unsupported, or if the device runs below Android 10 (API level 29) or throws `GetCredentialUnsupportedException` or `GetCredentialCancellationException`, trigger a fallback verification flow (such as SMS).
- **Configure DCQL parameters and optional hints:** In each `{ ... }` credential entry inside `requests[0].data.dcql_query.credentials` (with `"protocol": "openid4vp-v1-unsigned"`, `"response_type": "vp_token"`, `"response_mode": "dc_api"`, and a `"nonce"` matching the aggregator DCQL request):
  - Set `"format": "dc-authorization+sd-jwt"` and include `meta.credential_authorization_jwt` from the aggregator.
  - Set `meta.vct_values` to `["number-verification/device-phone-number/ts43"]` to retrieve the device phone number, or `["number-verification/verify/ts43"]` (with `{"path":
    ["phone_number_hint"], "values": [...]}` in `claims`) to verify an existing phone number.
  - Include `{"path": ["subscription_hint"], "values": [...]}` in `claims` when targeting a specific SIM subscription slot, and `{"path":
    ["user_verification_hint"]}` when requiring biometric or screen-lock authentication (which returns `"user_verification_hint"` as `"success"`, `"absent"`, or `"error"` in the response payload, or throws `GetCredentialCancellationException` if the user dismisses the prompt).
- **Avoid syntax pitfalls in the reference server snippets:** When referencing the backend examples in [Implement phone number verification](references/android/identity/digital-credentials/implement-phone-number-verification.md), note that the Python `GenerateDCRequest` snippet contains a Kotlin-style `val
  dc_request =` typo, the response JSON example includes `#` comments, and the error JSON example has a trailing comma; strip those invalid tokens when writing JSON or backend code, check `data.error` (`invalid_request` or `server_error`) first, and index the encrypted token from `data.vp_token[<aggregator_id>][0]`.

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| Experimental API compilation error | `GetDigitalCredentialOption` and `DigitalCredential` require `@ExperimentalDigitalCredentialApi` | Add `@OptIn(androidx.credentials.ExperimentalDigitalCredentialApi::class)` to the calling class or function. |
| Empty result or `GetCredentialCancellationException` | User canceled (including dismissing biometric `user_verification_hint`), feature unavailable, or SIM carrier does not support TS.43 | Run the `TelephonyManager.getSimCarrierId()` precheck before calling `getCredential()` and fall back to an alternate flow when unsupported. |
| `GetCredentialUnsupportedException` thrown | Device or OS version does not support the Phone Number Verification API (requires Android 10, API 29+) | Check `Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q` and handle `GetCredentialUnsupportedException` with a fallback flow. |
| Response JSON contains `"error": "invalid_request"` | Malformed OpenID4VP or DCQL request JSON or mismatched `nonce` | Verify `protocol` is `openid4vp-v1-unsigned`, `format` is `dc-authorization+sd-jwt`, `credentials` is a JSON array of `{ ... }` objects, and the outer `nonce` matches the aggregator DCQL `nonce`. |

## Verification and reporting guidelines

- \[ \] Run `./gradlew assembleDebug` to verify the project compiles without errors.
- \[ \] Verify `app/build.gradle.kts` includes `androidx.credentials:credentials` and `androidx.credentials:credentials-play-services-auth`.
- \[ \] Verify `app/src/main/AndroidManifest.xml` declares `READ_PHONE_STATE` and the client performs the `TelephonyManager.getSimCarrierId()` carrier precheck with a fallback when unsupported.
- \[ \] Verify the OpenID4VP request uses `"protocol": "openid4vp-v1-unsigned"`, `"format": "dc-authorization+sd-jwt"`, and `"number-verification/device-phone-number/ts43"` or `"number-verification/verify/ts43"`.
- \[ \] Verify `GetDigitalCredentialOption`, `GetCredentialRequest`, `credentialManager.getCredential()`, and `DigitalCredential` are used with `@OptIn(androidx.credentials.ExperimentalDigitalCredentialApi::class)`.

### Post-implementation warnings for the human developer

Present these backend reminders after completing client-side changes:

1. **Aggregator DCQL and token exchange:** Configure the app's backend service to verify carrier IDs from `getSimCarrierId()`, request live DCQL parameters (`credential_authorization_jwt`) from one or more carrier aggregators using a matching `nonce`, and exchange the returned `TS.43 Digital Credential` from `data.vp_token[<aggregator_id>][0]` for the verified phone number.
2. **Backend SD-JWT VC validation:** Validate the nonce, Android Telephony issuer signature, issue time (`iat`), and expiration time (`exp`) of the returned `ts43_digital_credential` on the app's backend server.

## Antipatterns

- **NEVER** use `TelephonyManager.getLine1Number()`, `READ_PHONE_NUMBERS`, `SmsRetriever`, or SMS OTP flows as a substitute for Credential Manager's `DigitalCredential` API (`GetDigitalCredentialOption` and `credentialManager.getCredential()`) when implementing digital credential phone number verification.
- **NEVER** skip the `TelephonyManager.getSimCarrierId()` carrier precheck before calling `credentialManager.getCredential()`.
- **NEVER** attempt to decrypt the encrypted `TS.43 Digital Credential` on the Android client; only the associated aggregator decrypts the payload.
- **NEVER** mismatch the `nonce` in the OpenID4VP request `data` object and the `nonce` sent to the aggregator when generating the DCQL credential object.

## Best practices

- **MUST** annotate classes or functions using `GetDigitalCredentialOption` and `DigitalCredential` with `@OptIn(androidx.credentials.ExperimentalDigitalCredentialApi::class)`.
- **MUST** declare `READ_PHONE_STATE` and call `telephonyManager.getSimCarrierId()` to verify carrier TS.43 support before invoking Credential Manager, falling back to an alternate verification flow when unsupported.
- **MUST** check both `GetCredentialException` (`GetCredentialCancellationException` and `GetCredentialUnsupportedException`) and OpenID4VP error JSON payloads (`invalid_request` and `server_error`).
- **MUST** use `number-verification/device-phone-number/ts43` to retrieve a device phone number and `number-verification/verify/ts43` to verify an existing phone number.
