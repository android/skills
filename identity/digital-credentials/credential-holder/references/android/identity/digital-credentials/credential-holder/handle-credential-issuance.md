To receive and store credentials from issuers, your holder app needs to handle
issuance flows. Credential issuance can be triggered in two ways:

- **Issuer-initiated flow** : the user triggers a request to issue a VDC from an issuer application or website. For example, a website may offer its users an option to "Add your passport to your wallet". The issuer calls the [Credential
  Manager issuance API](https://developer.android.com/identity/digital-credentials/credential-issuer/issue-credentials) to make an OpenID4VCI Credential Offer request. To handle such requests, a holder must first integrate with Credential Manager to register its metadata. Credential Manager will display relevant options for a user to select. After the user selects a holder option, the holder application will be invoked and can proceed with the steps needed to complete the issuance.
- **Holder-initiated flow**: the user requests to add a VDC from their holder application. For example, a holder may offer a button to "Add your passport". In this case, the holder maintains its supported issuer list and metadata. It doesn't need to integrate with the Android Credential Manager to complete this function.

For more information about how credentials work with the Holder API, see
[About storing digital credentials](https://developer.android.com/identity/digital-credentials/credential-holder/about-storing-digital-credentials).

### Android version compatibility

The Holder API is supported on Android 6 (API level 23) and higher.

## Implementation

To use the Credential Manager Holder API, add the following dependencies to your
app module's build script:

### Groovy

```groovy
dependencies {
    // Use to implement credentials registrys

    implementation "androidx.credentials.registry:registry-digitalcredentials-mdoc:1.0.0-alpha05"
    implementation "androidx.credentials.registry:registry-digitalcredentials-openid:1.0.0-alpha05"
    implementation "androidx.credentials.registry:registry-digitalcredentials-sdjwtvc:1.0.0-alpha05"
    implementation "androidx.credentials.registry:registry-provider:1.0.0-alpha05"
    implementation "androidx.credentials.registry:registry-provider-play-services:1.0.0-alpha05"

}
```

### Kotlin

```kotlin
dependencies {
    // Use to implement credentials registrys

    implementation("androidx.credentials.registry:registry-digitalcredentials-mdoc:1.0.0-alpha05")
    implementation("androidx.credentials.registry:registry-digitalcredentials-openid:1.0.0-alpha05")
    implementation("androidx.credentials.registry:registry-digitalcredentials-sdjwtvc:1.0.0-alpha05")
    implementation("androidx.credentials.registry:registry-provider:1.0.0-alpha05")
    implementation("androidx.credentials.registry:registry-provider-play-services:1.0.0-alpha05")

}
```

### Create the RegistryManager

Create a `RegistryManager` instance and register a
`RegisterCreationOptionsRequest` request with it.

```kotlin
val registryManager = RegistryManager.create(context)

try {
    registryManager.registerCreationOptions(object :
        RegisterCreationOptionsRequest(
            creationOptions = buildIssuanceData(),
            matcher = loadIssuanceMatcher(),
            type = DigitalCredential.TYPE_DIGITAL_CREDENTIAL,
            id = "openid4vci",
        ) {}
    )
} catch (e: Exception) {
    Log.e(TAG, "Issuance registration failed.", e)
}
```

The matcher is a WebAssembly (Wasm) binary file that receives the
`creationOptions` set during registration and the credential offer sent by the
issuer to determine the entries displayed on the Credential Manager UI. Refer to
the open source example [wallet app](https://github.com/digitalcredentialsdev/CMWallet/blob/71bf9086d196b05f4f85ff6badb975e5cbd7068d/app/src/main/java/com/credman/cmwallet/CmWalletApplication.kt#L97) for an example of a matcher.

### Handle an issuance request

Next, the wallet needs to handle when a credential creation option is selected
by the user. Define an activity that listens to the
`androidx.credentials.registry.provider.action.CREATE_CREDENTIAL` intent filter,
as demonstrated in the [sample wallet](https://github.com/digitalcredentialsdev/CMWallet/blob/9a0e1a713dc7559aaec6bf2c9583ab809b678cc9/app/src/main/AndroidManifest.xml#L36-L44).

The intent that launches the activity contains the creation request and calling
origin, which you can extract with the
`PendingIntentHandler.retrieveProviderCreateCredentialRequest` function. The API
returns a `ProviderCreateCredentialRequest` containing all information
associated with the creation request. There are two key components:

- The app that made the request. You can retrieve this with `getCallingAppInfo`.
- The request from the calling app. You can retrieve this with `getCallingRequest`, which returns a `CreateCredentialRequest`. If the request is for digital credentials, it's an instance of `CreateDigitalCredentialRequest`, which contains the issuance request JSON in the `requestJson` property. You can process that with the following sample code:

```kotlin
val pendingIntentRequest =
    PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)
val request = pendingIntentRequest!!.callingRequest
if (request is CreateDigitalCredentialRequest) {
    Log.i(TAG, "Got DC creation request: ${request.requestJson}")
    processCreationRequest(request.requestJson)
}
```

### Key attestation

Note the following about key attestation:

- When generating a key attestation, you might be using `BigInteger.toByteArray` to convert the key materials to, for example, a JSON web key. This method can sometimes add a sign byte and cause a validation error. If this happens, remove these bytes before sending the key attestation to the issuer.
- We recommend using the [`android_key_attestation`](https://developer.android.com/identity/digital-credentials/credential-issuer/keystore-attestation) as the key proof type.

### Return the creation response

Once the wallet finishes the steps needed to save the credential, finish the
activity with the credential response:

```kotlin
val resultData = Intent()
PendingIntentHandler.setCreateCredentialResponse(
    resultData,
    CreateDigitalCredentialResponse(response.responseJson)
)
setResult(RESULT_OK, resultData)
finish()
```

If there is an exception, you can similarly send the credential exception:

```kotlin
val resultData = Intent()
PendingIntentHandler.setCreateCredentialException(
    resultData,
    CreateCredentialUnknownException() // Configure the proper exception
)
setResult(RESULT_OK, resultData)
finish()
```

To see a full example of returning the credential response in context, see the
[sample app](https://github.com/digitalcredentialsdev/CMWallet/blob/7f6fe62d208f2a88ea531925c22f9e4eccc403f6/app/src/main/java/com/credman/cmwallet/createcred/CreateCredentialActivity.kt#L212).