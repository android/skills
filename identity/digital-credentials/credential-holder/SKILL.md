---
name: credential-holder
description: Implement digital credential holder (wallet) registration, presentation,
  and issuance handling in an Android app using the Jetpack Credential Manager Registry
  API. Use this skill when building a digital wallet or holder app in an Android project,
  registering SD-JWT or mdoc credential metadata with RegistryManager and OpenId4VpRegistry,
  configuring inline issuance entries, registering OpenID4VCI creation options with
  RegisterCreationOptionsRequest, or handling GET_CREDENTIAL and CREATE_CREDENTIAL
  intents with PendingIntentHandler. Don't use for requesting credentials as a verifier,
  issuing credentials from an issuer app, passkeys, or carrier TS.43 phone number
  verification.
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-11-20'
  keywords:
  - Android
  - identity
  - digital credentials
  - Credential Manager
  - credential holder
  - wallet app
  - RegistryManager
  - OpenId4VpRegistry
  - PendingIntentHandler
  - RegisterCreationOptionsRequest
  - mdoc
  - SD-JWT
  - OpenID4VP
  - OpenID4VCI
---

Build an Android digital wallet (holder) app that registers credential metadata
(`SdJwtEntry` and `MdocEntry`) with Credential Manager using `RegistryManager`
and `OpenId4VpRegistry`, registers OpenID4VCI creation options with
`RegisterCreationOptionsRequest`, and handles incoming `GET_CREDENTIAL`
presentation and `CREATE_CREDENTIAL` issuance requests with
`PendingIntentHandler`. When reviewing holder credential storage concepts,
presentation flows, or issuance handling, consult
[About storing digital credentials](references/android/identity/digital-credentials/credential-holder/about-storing-digital-credentials.md), [Present credentials](references/android/identity/digital-credentials/credential-holder/present-credentials.md), and
[Handle credential issuance](references/android/identity/digital-credentials/credential-holder/handle-credential-issuance.md). When requesting credentials as a relying
party, suggest the `credential-verifier` skill; when provisioning credentials
from an issuer app, suggest the `credential-issuer` skill.

## Prerequisites and setup

- **Android version compatibility:** The Holder API is supported on Android 6 (API level 23) and higher.
- **Build config permission:** Ask for user confirmation before modifying `settings.gradle.kts`, root `build.gradle.kts`, or `gradle/libs.versions.toml`, unless already requested by the user or running non-interactively.
- **Dependencies and package mapping:** Add the Credential Manager and Registry artifacts to the app module build script (`app/build.gradle.kts` or `app/build.gradle`; when checking full dependency declarations, consult [Present credentials](references/android/identity/digital-credentials/credential-holder/present-credentials.md) and [Handle credential issuance](references/android/identity/digital-credentials/credential-holder/handle-credential-issuance.md)):

| Artifact | Key classes, constructor signatures, and package |
|---|---|
| `androidx.credentials:credentials:1.7.0-alpha03` (or `1.6.0`+) | `GetCredentialResponse`, `GetDigitalCredentialOption`, `DigitalCredential(credentialJson: String)`, `CreateDigitalCredentialRequest`, `CreateDigitalCredentialResponse(responseJson: String)`, and `ExperimentalDigitalCredentialApi` in `androidx.credentials`; `PendingIntentHandler`, `CallingAppInfo`, `ProviderGetCredentialRequest`, and `ProviderCreateCredentialRequest` in `androidx.credentials.provider`; `GetCredentialException` and `CreateCredentialException` subtypes in `androidx.credentials.exceptions` |
| `androidx.credentials.registry:registry-digitalcredentials-mdoc:1.0.0-alpha05` | `MdocEntry(docType: String, fields: List<MdocField>, entryDisplayPropertySet: Set<EntryDisplayProperties>, id: String)`, `MdocField(namespace: String, identifier: String, fieldValue: Any?, fieldDisplayPropertySet: Set<FieldDisplayProperties>)`, and `MdocInlineIssuanceEntry` in `androidx.credentials.registry.digitalcredentials.mdoc` |
| `androidx.credentials.registry:registry-digitalcredentials-openid:1.0.0-alpha05` | `OpenId4VpRegistry(credentialEntries: List<DigitalCredentialEntry>, id: String, intentAction: String = RegistryManager.ACTION_GET_CREDENTIAL, inlineIssuanceEntries: List<InlineIssuanceEntry> = emptyList(), supportedProtocols: List<String> = ...)` in `androidx.credentials.registry.digitalcredentials.openid4vp` |
| `androidx.credentials.registry:registry-digitalcredentials-sdjwtvc:1.0.0-alpha05` | `SdJwtEntry(verifiableCredentialType: String, claims: List<SdJwtClaim>, entryDisplayPropertySet: Set<EntryDisplayProperties>, id: String)`, `SdJwtClaim(path: List<String>, value: Any?, fieldDisplayPropertySet: Set<FieldDisplayProperties>, isSelectivelyDisclosable: Boolean = true)`, and `SdJwtInlineIssuanceEntry` in `androidx.credentials.registry.digitalcredentials.sdjwt` |
| `androidx.credentials.registry:registry-provider:1.0.0-alpha05` | `RegistryManager`, `RegisterCreationOptionsRequest(type: String, id: String, creationOptions: ByteArray, matcher: ByteArray, intentAction: String = RegistryManager.ACTION_CREATE_CREDENTIAL)`, `ProviderGetCredentialRequest.selectedCredentialSet`, `ProviderGetCredentialRequest.selectedEntryId`, and `ProviderCreateCredentialRequest.selectedEntryId` in `androidx.credentials.registry.provider`; `DigitalCredentialEntry`, `EntryDisplayProperties`, `FieldDisplayProperties`, `InlineIssuanceEntry`, `InlineIssuanceEntry.InlineIssuanceDisplayProperties`, `VerificationEntryDisplayProperties(title: CharSequence, subtitle: CharSequence?, icon: Bitmap, ...)` (`icon` is non-null `android.graphics.Bitmap`), and `VerificationFieldDisplayProperties(displayName: CharSequence, displayValue: CharSequence?)` in `androidx.credentials.registry.provider.digitalcredentials` |
| `androidx.credentials.registry:registry-provider-play-services:1.0.0-alpha05` | Google Play services provider integration for Credential Manager Registry |

### Expected inputs and file scope

- **Required inputs:** The credential formats stored by the wallet (`SdJwtEntry` or `MdocEntry`), whether the wallet supports presentation (`GET_CREDENTIAL`), issuer-initiated issuance (`CREATE_CREDENTIAL`), or inline issuance, and the privileged browser allowlist JSON for caller verification.
- **Files to modify:** `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml` (for exported `GET_CREDENTIAL` and `CREATE_CREDENTIAL` activities), wallet registry managers, presentation and issuance `Activity` classes, and UI screens (such as `MainActivity.kt`).

## Codebase exploration

Before modifying files, inspect the Android project:

1. **Validate baseline build and inspect configuration:** Run `./gradlew assembleDebug` to confirm the project compiles before making changes, and check `app/build.gradle.kts`, `gradle/libs.versions.toml`, and `app/src/main/AndroidManifest.xml` for existing `androidx.credentials` or `androidx.credentials.registry` dependencies and exported holder activities.
2. **Locate wallet storage and UI entry points:** Identify where credentials are stored and locate the primary `Activity` or Compose screen (such as `MainActivity.kt` or `TasksScreen.kt`) to wire credential registration and wallet UI state.

## Critical implementation mandates

Follow the implementation steps and code snippets in
[About storing digital credentials](references/android/identity/digital-credentials/credential-holder/about-storing-digital-credentials.md), [Present credentials](references/android/identity/digital-credentials/credential-holder/present-credentials.md), and
[Handle credential issuance](references/android/identity/digital-credentials/credential-holder/handle-credential-issuance.md), and apply these non-obvious rules:

- **Opt in to experimental APIs:** Annotate classes or functions referencing `GetDigitalCredentialOption`, `DigitalCredential`, `CreateDigitalCredentialRequest`, or `CreateDigitalCredentialResponse` with `@OptIn(androidx.credentials.ExperimentalDigitalCredentialApi::class)`.
- **Use exact package names for registry artifacts:**
  - `registry-digitalcredentials-openid` exposes `OpenId4VpRegistry` in package `androidx.credentials.registry.digitalcredentials.openid4vp` (not `.openid`).
  - `registry-digitalcredentials-sdjwtvc` exposes `SdJwtEntry`, `SdJwtClaim`, and `SdJwtInlineIssuanceEntry` in package `androidx.credentials.registry.digitalcredentials.sdjwt` (not `.sdjwtvc`).
  - `registry-digitalcredentials-mdoc` exposes `MdocEntry`, `MdocField`, and `MdocInlineIssuanceEntry` in package `androidx.credentials.registry.digitalcredentials.mdoc`.
  - `registry-provider` exposes `DigitalCredentialEntry`, `EntryDisplayProperties`, `FieldDisplayProperties`, `InlineIssuanceEntry`, `VerificationEntryDisplayProperties`, and `VerificationFieldDisplayProperties` in package `androidx.credentials.registry.provider.digitalcredentials`.
- **Keep registry and entry `id` strings within 64 characters:** The `id` parameter on `SdJwtEntry`, `MdocEntry`, `OpenId4VpRegistry`, and `RegisterCreationOptionsRequest` must be at most 64 characters (longer IDs throw `IllegalArgumentException`), and `VerificationEntryDisplayProperties` requires a non-null `android.graphics.Bitmap` `icon`.
- **Declare exported manifest activities for holder intents:** Declare exported (`android:exported="true"`) presentation and issuance activities in `app/src/main/AndroidManifest.xml` with `<intent-filter>` blocks containing category `android.intent.category.DEFAULT` and action `androidx.credentials.registry.provider.action.GET_CREDENTIAL` (for presentation and inline issuance) or `androidx.credentials.registry.provider.action.CREATE_CREDENTIAL` (for issuer-initiated issuance).
- **Handle presentation and issuance intents safely:**
  - In the `GET_CREDENTIAL` activity, inspect `request.selectedCredentialSet` and `request.selectedEntryId` (including checking whether the selected ID matches a registered `MdocInlineIssuanceEntry` or `SdJwtInlineIssuanceEntry`), verify caller origin using `request.callingAppInfo.getOrigin(privilegedAppsJson)` for browsers or `"android:apk-key-hash:$certHash"` (with a null or empty check on `signingCertificateHistory`) for Android apps, and initialize `val resultData = Intent()` before calling `PendingIntentHandler.setGetCredentialResponse(resultData, ...)` or `setGetCredentialException(resultData, ...)`.
  - In the `CREATE_CREDENTIAL` activity (used for issuer-initiated offers using `RegisterCreationOptionsRequest` with the wallet's `creationOptions` bytes and issuance Wasm `matcher` bytes), null-check the nullable result of `PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)` before reading `callingRequest` and `selectedEntryId`. Once a newly issued credential is stored, re-register the wallet's updated credential list with `OpenId4VpRegistry` (`registryManager.registerCredentials`) under the same registry `id` so the credential can be presented.
- **Strip the leading sign byte from `BigInteger.toByteArray()` in JWK
  coordinates:** `BigInteger.toByteArray()` returns a two's-complement signed array that prepends a leading `0x00` sign byte when the most significant bit is set (producing 33 bytes instead of 32 bytes for P-256). Strip any leading `0x00` sign byte before Base64url-encoding JWK `x` and `y` coordinates so issuer key attestation verification doesn't fail.

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| Experimental API compilation error | `GetDigitalCredentialOption`, `DigitalCredential`, `CreateDigitalCredentialRequest`, and `CreateDigitalCredentialResponse` require opt-in | Add `@OptIn(androidx.credentials.ExperimentalDigitalCredentialApi::class)` to the calling class or function. |
| Unresolved reference `OpenId4VpRegistry` or `SdJwtEntry` | Importing `.openid` or `.sdjwtvc` instead of the actual package names | Import `OpenId4VpRegistry` from `androidx.credentials.registry.digitalcredentials.openid4vp` and `SdJwtEntry` from `androidx.credentials.registry.digitalcredentials.sdjwt`. |
| Holder app does not appear in Credential Manager selector | Metadata not registered or `GET_CREDENTIAL` / `CREATE_CREDENTIAL` activity missing from `AndroidManifest.xml` | Call `registryManager.registerCredentials` or `registerCreationOptions` and declare exported activities with the `androidx.credentials.registry.provider.action.*` intent filters. |
| Key attestation JWK validation fails on issuer | `BigInteger.toByteArray()` prepended a `0x00` sign byte to the EC coordinate | Strip any leading `0x00` byte when the byte array exceeds the curve coordinate length before Base64url encoding. |

## Verification and reporting guidelines

- \[ \] Run `./gradlew assembleDebug` to verify the Android project compiles without errors.
- \[ \] Verify `app/build.gradle.kts` includes `androidx.credentials:credentials` and the `androidx.credentials.registry:*` artifacts.
- \[ \] Verify `AndroidManifest.xml` declares exported activities for `androidx.credentials.registry.provider.action.GET_CREDENTIAL` or `androidx.credentials.registry.provider.action.CREATE_CREDENTIAL`.

### Post-implementation warnings for the human developer

Present these reminders after completing holder code changes:

1. **Privileged caller allowlist:** Supply a production allowlist JSON for `CallingAppInfo.getOrigin(...)` to verify web browsers making requests on behalf of web origins.
2. **Localization and matcher assets:** Localize `VerificationEntryDisplayProperties` titles and subtitles, and supply an issuance Wasm matcher binary (`matcher.wasm`) for `RegisterCreationOptionsRequest`.

## Antipatterns

- **NEVER** implement a digital wallet holder app using only a local database, simulated QR scanner, or `Intent.ACTION_SEND` without integrating the Jetpack `RegistryManager` and `PendingIntentHandler` APIs.
- **NEVER** use predictable, sequential, or unencrypted plain-text IDs (or IDs exceeding 64 characters) in `SdJwtEntry` or `MdocEntry`.
- **NEVER** pass raw `BigInteger.toByteArray()` output to JWK coordinates without stripping any leading `0x00` sign byte.

## Best practices

- **MUST** annotate classes or functions using `GetDigitalCredentialOption`, `DigitalCredential`, `CreateDigitalCredentialRequest`, and `CreateDigitalCredentialResponse` with `@OptIn(androidx.credentials.ExperimentalDigitalCredentialApi::class)`.
- **MUST** verify caller identity in holder presentation activities using `CallingAppInfo.getOrigin(privilegedAppsJson)` for browsers and `android:apk-key-hash:` SHA-256 signing certificate hashes (with a null check on `signingCertificateHistory`) for Android apps.
- **MUST** re-register updated credential metadata with `OpenId4VpRegistry` using the same registry `id` whenever credentials are added, updated, or deleted in the wallet.
