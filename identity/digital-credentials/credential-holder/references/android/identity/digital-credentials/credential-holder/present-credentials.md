The Credential Manager Holder API enables your Android holder (also called
"wallet") app to manage and present digital credentials to verifiers. This guide
describes how to register your holder app's credentials with Credential
Manager and how to handle a presentation request when a user selects a
credential. The Holder API is supported on Android 6 (API level 23) and higher.

## Add dependencies

To use the Credential Manager Holder API, add the following dependencies to your
app module's `build.gradle` script:

```kotlin
dependencies {
    // Used to implement credential registries
    implementation "androidx.credentials.registry:registry-digitalcredentials-mdoc:1.0.0-alpha05"
    implementation "androidx.credentials.registry:registry-digitalcredentials-openid:1.0.0-alpha05"
    implementation "androidx.credentials.registry:registry-digitalcredentials-sdjwtvc:1.0.0-alpha05"
    implementation "androidx.credentials.registry:registry-provider:1.0.0-alpha05"
    implementation "androidx.credentials.registry:registry-provider-play-services:1.0.0-alpha05"
}
```

### Register your credentials

First, create a `RegistryManager` instance that you will use to register an
`OpenId4VpRegistry` request.

```kotlin
// Create the registry manager
val registryManager = RegistryManager.create(context)
```

### Build an `OpenId4VpRegistry` request

You need to register an `OpenId4VpRegistry` to handle OpenID4VP requests from
verifiers. Assuming you have local data types loaded with your wallet
credentials (e.g., `sdJwtsFromStorage` and `mdocsFromStorage`), you must
convert them into their Jetpack `DigitalCredentialEntry` equivalents:
`SdJwtEntry` and `MdocEntry`.

**Add Sd-JWTs into the registry**

Map each local SD-JWT credential to an [`SdJwtEntry`](https://developer.android.com/reference/kotlin/androidx/credentials/registry/digitalcredentials/sdjwt/SdJwtEntry) for the registry:

```kotlin
fun mapToSdJwtEntries(sdJwtsFromStorage: List<StoredSdJwtEntry>): List<SdJwtEntry> {
    val list = mutableListOf<SdJwtEntry>()

    for (sdJwt in sdJwtsFromStorage) {
        list.add(
            SdJwtEntry(
                verifiableCredentialType = sdJwt.getVCT(),
                claims = sdJwt.getClaimsList(),
                entryDisplayPropertySet = sdJwt.toDisplayProperties(),
                id = sdJwt.getId() // Make sure this cannot be readily guessed
            )
        )
    }
    return list
}
```

**Add mdocs into the Registry**

Map your local mdoc credentials into the Jetpack type [`MdocEntry`](https://developer.android.com/reference/kotlin/androidx/credentials/registry/digitalcredentials/mdoc/MdocEntry):

```kotlin
fun mapToMdocEntries(mdocsFromStorage: List<StoredMdocEntry>): List<MdocEntry> {
    val list = mutableListOf<MdocEntry>()

    for (mdoc in mdocsFromStorage) {
        list.add(
            MdocEntry(
                docType = mdoc.retrieveDocType(),
                fields = mdoc.getFields(),
                entryDisplayPropertySet = mdoc.toDisplayProperties(),
                id = mdoc.getId() // Make sure this cannot be readily guessed
            )
        )
    }
    return list
}
```

### Key points about the code

- **Encrypted IDs** : One method of configuring the `ID` field is to register an encrypted credential identifier, so only you can decrypt the value.
- **Localization**: The UI display fields for both formats should be localized.

### Combine and register your request

Combine your converted entries and register the request with the
`RegistryManager`:

```kotlin
val credentialEntries =
    mapToSdJwtEntries(sdJwtsFromStorage) + mapToMdocEntries(mdocsFromStorage)

val openidRegistryRequest = OpenId4VpRegistry(
    credentialEntries = credentialEntries,
    id = "my-wallet-openid-registry-v1" // A stable, unique ID to identify your registry record.
)
```

#### Optional: Inline issuance

If there are no existing credentials of a requested type available for a user,
you can display an option to add a credential to your holder. To do this, add an
`inlineIssuanceEntries` list containing the credentials supported to your
`OpenId4VpRegistry`:

```kotlin
val openidRegistryRequest = OpenId4VpRegistry(
    credentialEntries = credentialEntries,
    inlineIssuanceEntries = listOf(
        MdocInlineIssuanceEntry(
            id = "Issuance",
            display = InlineIssuanceEntry.InlineIssuanceDisplayProperties(
                subtitle = "Mobile Drivers License, State Id, and Others",
            ),
            supportedMdocs = setOf(
                MdocInlineIssuanceEntry.SupportedMdoc(
                    "eu.europa.ec.eudi.pid.1"
                ),
                MdocInlineIssuanceEntry.SupportedMdoc(
                    "org.iso.18013.5.1.mDL1"
                ),
            )
        ),
        SdJwtInlineIssuanceEntry(
            id = "sd-jwt-issuance",
            display = InlineIssuanceEntry.InlineIssuanceDisplayProperties(
                subtitle = "Mobile Drivers License, State Id, and Others",
            ),
            supportedSdJwts = setOf(
                SdJwtInlineIssuanceEntry.SupportedSdJwt("urn:openid:interop:id:1")
            )
        )
    ),
    id = "my-wallet-openid-registry-v1"
)
```

Register your credentials with CredentialManager.

```kotlin
try {
    val response = registryManager.registerCredentials(openidRegistryRequest)
} catch (e: Exception) {
    // Handle failure
}
```

### App metadata management

The metadata your holder app registers with `CredentialManager`has the following
properties:

- **Persistence**: The information is saved locally, persisting across reboots.
- **Siloed Storage**: Each app's registry records are stored separately, meaning one app cannot change another app's registry records.
- **Keyed Updates** : Each app's registry records are keyed by an [`id`](https://developer.android.com/reference/kotlin/androidx/credentials/registry/digitalcredentials/openid4vp/OpenId4VpRegistry#:%7E:text=protocol%20based%20request.-,The%20(type%2C%20id),-properties%20together%20act), which allows re-identifying, updating, or deleting records.
- **Updating Metadata** : It is good practice to update the persisted metadata whenever your app changes or is first loaded. If a registry is called multiple times under the same `id`, the latest call overwrites all prior records. To update, re-register without needing to clear the old record first.

## Optional: Create a matcher

A matcher is a Wasm binary that Credential Manager runs in a sandbox to filter
your registered credentials against an incoming Verifier request.

- **Default matcher** : The `OpenId4VpRegistry` class **automatically includes
  the default `OpenId4VP` matcher** (`OpenId4VpDefaults.DEFAULT_MATCHER`) when you instantiate it. For all standard OpenID4VP use cases, the library handles matching for you.
- **Custom matcher**: You would only implement a custom matcher if you are supporting a non-standard protocol that requires its own matching logic.

## Handle a selected credential

When a user selects a credential, your holder app needs to handle the request.
You will need to define an Activity that listens to the
`androidx.credentials.registry.provider.action.GET_CREDENTIAL` intent filter.
Our [sample wallet demonstrates this procedure](https://github.com/digitalcredentialsdev/CMWallet/blob/9a0e1a713dc7559aaec6bf2c9583ab809b678cc9/app/src/main/AndroidManifest.xml#L26-L35).

The intent launches your activity with the Verifier request and calling origin,
which you [extract with the](https://github.com/digitalcredentialsdev/CMWallet/blob/9a0e1a713dc7559aaec6bf2c9583ab809b678cc9/app/src/main/java/com/credman/cmwallet/getcred/GetCredentialActivity.kt#L153)
[`PendingIntentHandler.retrieveProviderGetCredentialRequest`](https://github.com/digitalcredentialsdev/CMWallet/blob/9a0e1a713dc7559aaec6bf2c9583ab809b678cc9/app/src/main/java/com/credman/cmwallet/getcred/GetCredentialActivity.kt#L153)
[function](https://github.com/digitalcredentialsdev/CMWallet/blob/9a0e1a713dc7559aaec6bf2c9583ab809b678cc9/app/src/main/java/com/credman/cmwallet/getcred/GetCredentialActivity.kt#L153). This returns a [`ProviderGetCredentialRequest`](https://developer.android.com/reference/androidx/credentials/provider/ProviderGetCredentialRequest)
containing all the information associated with the verifier request. There are
three key components:

- **The calling app** : The app that made the request, retrievable with [`getCallingAppInfo`](https://developer.android.com/reference/androidx/credentials/provider/ProviderGetCredentialRequest#getCallingAppInfo()). You can use this to [check the verifier
  identity](https://developer.android.com/identity/digital-credentials/credential-holder/present-credentials#check-verifier).
- **The selected credential** : Information about which candidate the user has chosen, retrieved through the [`selectedCredentialSet extension method`](https://developer.android.com/reference/androidx/credentials/registry/provider/ProviderGetCredentialRequest#(androidx.credentials.provider.ProviderGetCredentialRequest).getSelectedCredentialSet()); this will match the credential ID you registered.
- **Specific requests** : The specific request made by the verifier, retrieved from the [`getCredentialOptions`](https://developer.android.com/reference/androidx/credentials/provider/ProviderGetCredentialRequest#getCredentialOptions()) method. For a Digital Credentials request flow, you can expect to find a single [`GetDigitalCredentialOption`](https://developer.android.com/reference/kotlin/androidx/credentials/GetDigitalCredentialOption) in this list.

You can process the presentation request with the following code:

```kotlin
request.credentialOptions.forEach { option ->
    if (option is GetDigitalCredentialOption) {
        Log.i(TAG, "Got DC request: ${option.requestJson}")
        processRequest(option.requestJson)
    }
}
```

An example of this can be seen in the [sample wallet](https://github.com/digitalcredentialsdev/CMWallet/blob/9a0e1a713dc7559aaec6bf2c9583ab809b678cc9/app/src/main/java/com/credman/cmwallet/getcred/GetCredentialActivity.kt#L172).

### Check the verifier identity

First, extract the `ProviderGetCredentialRequest` from the intent:

```kotlin
val request = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
```

Check for privileged apps: Privileged apps (like web browsers) can make calls on
behalf of other verifiers by setting the origin parameter. To retrieve this
origin, pass a list of privileged and trusted callers (an allowlist in JSON
format) to the `CallingAppInfo` `getOrigin` API.

```kotlin
val origin = request?.callingAppInfo?.getOrigin(
    privilegedAppsJson // Your allow list JSON
)
```

If `origin` is not empty: The origin is returned if the `packageName` and
certificate fingerprints match an app in your allowlist. Consider this a
privileged call and set this origin on the OpenID4VP response instead of
computing the origin using the calling app's signature.

If `origin` is empty: The verifier request is from an Android app. The origin
mapped to the OpenID4VP response should be calculated as
`android:apk-key-hash:<encoded SHA 256 fingerprint>`.

```kotlin
val appSigningInfo =
    request?.callingAppInfo?.signingInfoCompat?.signingCertificateHistory?.firstOrNull()?.toByteArray()
val md = MessageDigest.getInstance("SHA-256")
val certHash =
    Base64.encodeToString(md.digest(requireNotNull(appSigningInfo)), Base64.NO_WRAP or Base64.NO_PADDING)
val computedOrigin = "android:apk-key-hash:$certHash"
```

### Render the Holder UI

When a credential is selected, the holder app is invoked, guiding the user
through the app's UI. There are two standard ways to handle this workflow:

- If additional user authentication is needed to release the credential, use the [BiometricPrompt API](https://developer.android.com/reference/androidx/biometric/BiometricPrompt). This is demonstrated [in the
  sample](https://github.com/digitalcredentialsdev/CMWallet/blob/9a0e1a713dc7559aaec6bf2c9583ab809b678cc9/app/src/main/java/com/credman/cmwallet/getcred/GetCredentialActivity.kt#L299).
- Otherwise, many wallets opt for a silent return by rendering an empty activity that immediately passes the data back to the calling app. This minimizes user clicks and provides a more seamless experience.

### Return the credential response

Once your holder app is ready to send the result back, finish the activity with
the credential response:

```kotlin
PendingIntentHandler.setGetCredentialResponse(
    resultData,
    GetCredentialResponse(DigitalCredential(response.responseJson))
)
setResult(RESULT_OK, resultData)
finish()
```

If there is an exception, you can similarly send the credential exception:

```kotlin
PendingIntentHandler.setGetCredentialException(
    resultData,
    GetCredentialUnknownException() // Configure the proper exception
)
setResult(RESULT_OK, resultData)
finish()
```

Refer to the [sample app](https://github.com/digitalcredentialsdev/CMWallet/blob/9a0e1a713dc7559aaec6bf2c9583ab809b678cc9/app/src/main/java/com/credman/cmwallet/getcred/GetCredentialActivity.kt#L276-L282) for a full example of returning the
credential response in context.