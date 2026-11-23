---
name: credential-verifier
description: Request and verify digital credentials (such as ISO mdoc mobile driver's
  licenses and SD-JWT credentials) from a holder wallet in an Android app or WebView
  using Jetpack Credential Manager. Use this skill when building a digital credential
  verifier app in an Android project, constructing OpenID4VP (openid4vp-v1-unsigned)
  and Digital Credential Query Language (dcql_query) requests, calling CredentialManager.getCredential
  with GetDigitalCredentialOption, requesting multiple credentials simultaneously,
  or bridging WebView navigator.credentials.get requests with WebViewCompat.WebMessageListener.
  Don't use for holder wallet registration, issuing credentials over OpenID4VCI, passkeys,
  or carrier TS.43 phone number verification.
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-11-20'
  keywords:
  - Android
  - identity
  - digital credentials
  - Credential Manager
  - credential verifier
  - GetDigitalCredentialOption
  - DigitalCredential
  - OpenID4VP
  - openid4vp-v1-unsigned
  - DCQL
  - dcql_query
  - mso_mdoc
  - dc+sd-jwt
  - WebView
  - WebMessageListener
---

Build an Android verifier (relying party) app that requests and verifies digital
credentials (`mso_mdoc` and `dc+sd-jwt`) from a user's holder wallet over
OpenID4VP (`openid4vp-v1-unsigned`) and Digital Credential Query Language
(`dcql_query`) using `GetDigitalCredentialOption` and
`credentialManager.getCredential()`, either directly in an Android app or
through a `WebView` JavaScript bridge. When reviewing digital credential
architecture, trust requirements, or verifier implementation snippets, consult
[Digital credentials overview](references/android/identity/digital-credentials/index.md), [About trust and safety](references/android/identity/digital-credentials/trust-safety.md),
[Request credentials](references/android/identity/digital-credentials/credential-verifier/request-credential.md), and [Request a credential through WebView](references/android/identity/digital-credentials/credential-verifier/webview-request.md). When
building a wallet app that stores and presents credentials, suggest the
`credential-holder` skill; when issuing credentials to a wallet, suggest the
`credential-issuer` skill; when implementing carrier TS.43 phone number
verification, suggest the `phone-number-verification` skill.

## Prerequisites and setup

- **Android version compatibility:** Runtime support for the Verifier API requires Android 9 (API level 28) and higher (`WebView` JavaScript bridge targets API level 23 or higher, and `androidx.credentials` compiles with `minSdk` 23 or higher).
- **Build config permission:** Ask for user confirmation before modifying `settings.gradle.kts`, root `build.gradle.kts`, or `gradle/libs.versions.toml`, unless already requested by the user or running non-interactively.
- **Dependencies and package mapping:** Add the Credential Manager artifacts (and `androidx.webkit` when bridging `WebView` requests) to the app module build script (`app/build.gradle.kts` or `app/build.gradle`; when checking full dependency snippets, consult [Request credentials](references/android/identity/digital-credentials/credential-verifier/request-credential.md) and [Request a credential through WebView](references/android/identity/digital-credentials/credential-verifier/webview-request.md)):

| Scenario | Artifact | Key classes, constructor signatures, and package |
|---|---|---|
| All verifier flows | `androidx.credentials:credentials:1.7.0-alpha03` (or `1.6.0`+) | `CredentialManager`, `GetCredentialRequest`, `GetCredentialResponse`, `GetDigitalCredentialOption(requestJson: String)`, `DigitalCredential`, and `ExperimentalDigitalCredentialApi` in `androidx.credentials`; `GetCredentialException`, `GetCredentialCancellationException`, `GetCredentialInterruptedException`, `NoCredentialException`, `GetCredentialUnknownException`, and `GetCredentialCustomException` in `androidx.credentials.exceptions` |
| All verifier flows | `androidx.credentials:credentials-play-services-auth:1.7.0-alpha03` (or `1.6.0`+) | Google Play services provider integration for Credential Manager |
| `WebView` bridge | `androidx.webkit:webkit:1.18.0` | `WebViewCompat`, `WebViewCompat.WebMessageListener`, `WebMessageCompat`, `WebViewFeature`, and `JavaScriptReplyProxy` in `androidx.webkit` |

### Expected inputs and file scope

- **Required inputs:** The credential formats (`mso_mdoc` with `doctype_value` or `dc+sd-jwt` with `vct_values`) and minimum selective-disclosure claims to request, and whether verification runs directly in an Android screen or inside a `WebView`.
- **Files to modify:** `app/build.gradle.kts`, verifier client or repository classes, `WebViewCompat.WebMessageListener` bridge classes (when using `WebView`), ViewModels, and UI screens (such as `MainActivity.kt`).

## Codebase exploration

Before modifying files, inspect the Android project:

1. **Validate baseline build and inspect configuration:** Run `./gradlew assembleDebug` to confirm the project compiles before making changes, and check `app/build.gradle.kts` and `gradle/libs.versions.toml` for `minSdk`, `androidx.credentials`, and `androidx.webkit` dependencies.
2. **Locate target UI entry points:** Identify the `Activity` or Compose screen (such as `MainActivity.kt` or `TasksScreen.kt`) where verification is triggered so `credentialManager.getCredential()` receives an `Activity` `Context`.

## Critical implementation mandates

- **Opt in to experimental APIs:** Annotate classes or functions referencing `GetDigitalCredentialOption` or `DigitalCredential` with `@OptIn(androidx.credentials.ExperimentalDigitalCredentialApi::class)` (omitted from the snippet in [Request credentials](references/android/identity/digital-credentials/credential-verifier/request-credential.md)).
- **Use the exact OpenID4VP and DCQL JSON schema:** Follow the single-credential (`mso_mdoc` or `dc+sd-jwt`) and multi-credential (`dcql_query.credentials`) JSON schemas in [Request credentials](references/android/identity/digital-credentials/credential-verifier/request-credential.md) (`"protocol":
  "openid4vp-v1-unsigned"`, `"response_type": "vp_token"`, `"response_mode": "dc_api"`, `"nonce"`, and `"dcql_query"`). Never wrap the Android `GetDigitalCredentialOption(requestJson)` payload in a browser-only `{"providers": [...]}` object.
- **Send `credentialJson` to the app's backend for verification:** Extract `credential.credentialJson` from `DigitalCredential` and send it with the session `nonce` to the app's backend to verify the cryptographic signature and issuer trust chain; never trust unverified claims directly on the client.
- **Complete the `WebView` listener and `AndroidView` setup:** When following [Request a credential through WebView](references/android/identity/digital-credentials/credential-verifier/webview-request.md):
  - Add `androidx.webkit:webkit:1.18.0` alongside the Credential Manager dependencies and inject the `bridge.js` script that intercepts `navigator.credentials.get({ digital })`.
  - Implement `WebViewCompat.WebMessageListener.onPostMessage(view: WebView,
    message: WebMessageCompat, sourceOrigin: Uri, isMainFrame: Boolean,
    replyProxy: JavaScriptReplyProxy)` (described in prose only in [Request a credential through WebView](references/android/identity/digital-credentials/credential-verifier/webview-request.md)): verify `isMainFrame` is `true` and `sourceOrigin.scheme == "https"`, parse `message.data`, call `credentialManager.getCredential` with `GetDigitalCredentialOption`, and return `credential.credentialJson` (or a JSON error object) through `replyProxy.postMessage(...)`.
  - Inside `WebView(context).apply { ... }` in `AndroidView`, pass `this` (not an undeclared `webView` variable) to `WebViewCompat.addWebMessageListener`, pass the declared `handler` variable to `DigitalCredentialWebListener`, and close the `if (WebViewFeature.isFeatureSupported(...))` block before calling `loadUrl("https://...")`.

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| Experimental API compilation error | `GetDigitalCredentialOption` and `DigitalCredential` require opt-in | Add `@OptIn(androidx.credentials.ExperimentalDigitalCredentialApi::class)` to the calling class or function. |
| Compilation error on `CreateCredentialUnknownException` in `when (e: GetCredentialException)` | `CreateCredential*Exception` doesn't inherit from `GetCredentialException` | Match `GetCredentialUnknownException` and `GetCredentialCustomException` instead. |
| Credential selector fails or returns invalid request | Using a browser `providers` wrapper object instead of OpenID4VP | Use `"requests"` with `"protocol": "openid4vp-v1-unsigned"`, `"response_type": "vp_token"`, `"response_mode": "dc_api"`, and `"dcql_query"`. |
| `WebView` credential request fails silently | `WebMessageListener` not registered or origin not allowed | Check `WebViewFeature.WEB_MESSAGE_LISTENER` and register `WebViewCompat.addWebMessageListener` with explicit HTTPS `allowedOriginRules` before `loadUrl`. |

## Verification and reporting guidelines

- \[ \] Run `./gradlew assembleDebug` to verify the Android project compiles without errors.
- \[ \] Verify `app/build.gradle.kts` includes `androidx.credentials:credentials` and `androidx.credentials:credentials-play-services-auth` (plus `androidx.webkit:webkit` for `WebView` flows).
- \[ \] Verify the OpenID4VP request JSON uses `"protocol":
  "openid4vp-v1-unsigned"`, `"response_type": "vp_token"`, `"response_mode": "dc_api"`, and `"dcql_query"`.
- \[ \] Verify `credentialManager.getCredential()` is called with an `Activity` `Context` and catches `GetCredentialException` subtypes.

### Post-implementation warnings for the human developer

Present these reminders after completing verifier code changes:

1. **Server-side cryptographic verification:** Generate single-use cryptographic `nonce` values on the app's backend and verify the returned `DigitalCredential.credentialJson` (`vp_token` issuer signature, trust chain, device binding, and nonce) on the backend server before trusting any claims.
2. **Minimum selective disclosure:** Request only the minimum attributes needed (such as `age_over_21` rather than full birth date or address) in `dcql_query`.

## Antipatterns

- **NEVER** implement a digital credential verifier using only a simulated QR scanner or local mock UI without integrating `CredentialManager` and `GetDigitalCredentialOption`.
- **NEVER** invent a top-level `{"providers": [...]}` JSON schema for `GetDigitalCredentialOption(requestJson)` in an Android verifier app; always use `"requests"` with `"protocol": "openid4vp-v1-unsigned"` and `"dcql_query"`.
- **NEVER** trust raw claims parsed on the client without validating `credential.credentialJson` and the session `nonce` on the app's backend.

## Best practices

- **MUST** annotate classes or functions using `GetDigitalCredentialOption` and `DigitalCredential` with `@OptIn(androidx.credentials.ExperimentalDigitalCredentialApi::class)`.
- **MUST** handle `GetCredentialCancellationException`, `GetCredentialInterruptedException`, `NoCredentialException`, `GetCredentialUnknownException`, and `GetCredentialCustomException` when calling `credentialManager.getCredential()`.
- **MUST** verify `isMainFrame` and `https` `sourceOrigin` in `WebView` `WebMessageListener.onPostMessage` before forwarding credential requests to `CredentialManager`.
