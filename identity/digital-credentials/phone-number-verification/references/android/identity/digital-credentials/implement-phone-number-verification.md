This guide details how to use the [DigitalCredential API](https://developer.android.com/reference/kotlin/androidx/credentials/DigitalCredential) to obtain verified
phone numbers for your users. The process involves two steps:

1. **Precheck**: Check if the user's carrier supports this API by querying the Android Telephony API. If the carrier is unsupported, fall back to your normal SMS flow.
2. **Request a `TS.43 token`** : Your client app (the "verifier") requests a temporary TS.43 token from the user's device. The `TS.43 token` is a carrier-issued credential that represents the user's identity.
3. **Exchange the token for a phone number** : Your app's backend exchanges the `TS.43 token` with an aggregator or a carrier for the user's verified phone number.

> [!NOTE]
> **Note:** The Phone Number Verification API can be used with a wide range of aggregators. For a Firebase solution, see [Firebase Phone Number Verification](https://firebase.google.com/docs/phone-number-verification).

### Android version compatibility

The Phone Number Verification API is supported on Android 10 (API level 29) and
higher.

## Prerequisites

To implement phone number verification with the DigitalCredential API, you need
an account with an aggregator or mobile carrier. An aggregator interacts with
carriers and provides the necessary API surface for your app, usually as a
billable cloud API endpoint. In this guide, both aggregators and carriers are
collectively referred to as **aggregators**.

You also need to add the following dependencies to your Gradle build script:

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

## Implementation

Before calling the Digital Credentials API, check if the user's carrier
supports this feature using the Android Telephony API.

Add the `READ_PHONE_STATE` permission to your `AndroidManifest.xml` file.

```kotlin
val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
val carrierId = telephonyManager.getSimCarrierId()
```

With this ID, verify TS.43 support with your aggregator backend.
If this user's carrier is not supported, then you can fall back to an
alternate method such as SMS, as calling the Digital Credentials API
returns an empty result. Otherwise, continue with the Digital Credentials flow.

The end-to-end process generally follows these steps:

1. [**Request DCQL (Digital Credential Query Language) parameters from an
   aggregator**](https://developer.android.com/identity/digital-credentials/implement-phone-number-verification#get-dcql): Call one or more aggregators and request a set of DCQL parameters. DCQL lets you specify the exact digital credentials that you need from each aggregator.
2. [**Create the OpenID4VP request**](https://developer.android.com/identity/digital-credentials/implement-phone-number-verification#create-openid4vp): From your app's backend, create the
   OpenID4VP request, while including the DCQL parameters from the aggregator.
   Then, send the OpenID4VP request to your client app.

   > [!NOTE]
   > **Note:** The OpenID4VP request is a standardized protocol used to request digital credentials. It can also include requests for other digital credentials, such as mobile driving licenses.

3. [**Call the Credential Manager API**](https://developer.android.com/identity/digital-credentials/implement-phone-number-verification#make-credentialmanager): In your client app, use the
   Credential Manager API to send the OpenID4VP request to the operating
   system. In response, you receive an OpenID4VP response object containing the
   `TS.43 Digital Credential`. This credential is encrypted and can only be
   decrypted by the associated aggregator. After receiving the carrier token,
   send the response from your client app to the app's backend.

4. [**Validate the response**](https://developer.android.com/identity/digital-credentials/implement-phone-number-verification#handle-digital): In your app's backend, validate the
   OpenID4VP response.

5. [**Exchange for phone number**](https://developer.android.com/identity/digital-credentials/implement-phone-number-verification#exchange-phone): From your app's backend, send the `TS.43
   Digital Credential` to the aggregator. The aggregator validates the
   credential and returns the verified phone number.

![Image showing the flow of a phone number verification request](https://developer.android.com/static/identity/digital-credentials/images/pnv_verifier_diagram.svg) **Figure 1.**The life of a phone number verification request, starting from the Verifier backend requesting parameters from an aggregator and ending with a returned verified phone number.

## Request DCQL parameters from an aggregator

From your app's backend, send a request to the aggregator for a Digital
Credential Query Language (DCQL) credential object. Make sure to provide a nonce
and a request ID in your request. The aggregator returns the DCQL credential
object, which has a structure similar to the following:

```json
{
  // The credential ID is mapped to the request ID that is sent in your request to the aggregator.
  "id": "aggregator1",
  "format": "dc-authorization+sd-jwt",
  "meta": {
    "vct_values": [
      "number-verification/device-phone-number/ts43"
    ],
    "credential_authorization_jwt": "..."
  },
  "claims": [
    {
      "path": ["subscription_hint"],
      "values": [1]
    },
    {
      "path": ["phone_number_hint"],
      "values": ["+14155552671"]
    }
  ]
}
```

> [!NOTE]
> **Note:** To retrieve a phone number, use `number-verification/device-phone-number/ts43`. To verify an existing phone number, use `number-verification/verify/ts43`.

## Create the OpenID4VP request

First, from your app's backend, create a `dcql_query` object by placing the DCQL
credential object in a `credentials` array nested within a `dcql_query` object
as shown in the following example:

```json
"dcql_query": {
  "credentials": [
    {
      "id": "aggregator1",
      "format": "dc-authorization+sd-jwt",
      "meta": {
        "vct_values": [
          "number-verification/device-phone-number/ts43"
        ],
        "credential_authorization_jwt": "..."
      },
      "claims": [
        {
          "path": ["subscription_hint"],
          "values": [1]
        },
        {
          "path": ["phone_number_hint"],
          "values": ["+14155552671"]
        }
      ]
    }
  ]
}
```

Then, create an OpenID4VP request with the following structure:

```json
{
  "protocol": "openid4vp-v1-unsigned",
  "data": {
    "response_type": "vp_token",
    "response_mode": "dc_api",
    "nonce": "...",
    "dcql_query": { ... }
  }
}
```

- `protocol`: Must be set to `openid4vp-v1-unsigned` for phone number verification requests.
- `response_type` and `response_mode`: Constants denoting the format of the request with fixed values `vp_token` and `dc_api`, respectively.
- `nonce`: A unique value generated by your backend for each request. The nonce in the aggregator DCQL credential object must match this nonce.
- `dcql_query`: In this case, use the `dcql_query` to specify that a `TS.43
  Digital Credential` is being requested. You can also request other digital credentials here.

Then, wrap the OpenID4VP request in a DigitalCredential API request object and
send it to the client app.

```json
{
  "requests":
    [
      {
        "protocol": "openid4vp-v1-unsigned",
        "data": {
          "response_type": "vp_token",
          "response_mode": "dc_api",
          "nonce": "...",
          "dcql_query": { ... }
        }
      }
    ]
}
```

The following snippet demonstrates how to generate the DigitalCredential API
request:

```python
def GenerateDCRequest():
    credentials = []
    aggregator1_dcql = call_aggregator_endpoint(nonce, "aggregator1", additional_params)
    credentials.append(aggregator1_dcql) # You can optionally work with multiple
    # aggregators, or request other types of credentials

    val dc_request =
    {
      "requests":
        [
          {
            "protocol": "openid4vp-v1-unsigned",
            "data": {
              "response_type": "vp_token",
              "response_mode": "dc_api",
              "nonce": "...",
              "dcql_query": {"credentials": credentials}
            }
          }
        ]
    }
    return dc_request
```

## Call the Credential Manager API

In your client app, make a call to the Credential Manager API, with the
DigitalCredential API request provided by your app's backend.

```kotlin
val requestJson = generateTs43DigitalCredentialRequestFromServer()
val digiCredOption = GetDigitalCredentialOption(requestJson = requestJson)
val getCredRequest = GetCredentialRequest(
    listOf(digiCredOption)
)

coroutineScope.launch {
  try {
    val response = credentialManager.getCredential(
      context = activityContext,
      request = getCredRequest
    )
    val credential = response.credential
    when (credential) {
      is DigitalCredential -> {
        val responseJson = credential.credentialJson
        validateResponseOnServer(responseJson)
      }
      else -> {
        // Catch any unrecognized credential type here.
        Log.e(TAG, "Unexpected type of credential ${credential.type}")
      }
    }
  } catch (e : GetCredentialException) {
      // If user cancels the operation, the feature isn't available, or the
      // SIM doesn't support the feature, a GetCredentialCancellationException
      // will be returned. Otherwise, a GetCredentialUnsupportedException will
      // be returned with details in the exception message.
      handleFailure(e)
  }
}
```

The DigitalCredential API response contains the OpenID4VP response. A typical
credential json from the [`DigitalCredential`](https://developer.android.com/identity/digital-credentials/implement-phone-number-verification#create-openid4vp) result is as follows:

```json
{
  "protocol": "openid4vp-v1-unsigned",

  "data": {
    "vp_token": {
      "aggregator1": ["eyJhbGciOiAiRVMy..."] # The encrypted TS.43 Digital
                                             # Credential in an array structure.
    }
  }
}
```

From your client app, send the DigitalCredential API response back to the
backend server where it can be validated and used to exchange for the verified
phone number with an aggregator.

In some cases, the response may contain a TS.43 error. The error response is a
json object following the OpenID4VP error response format:

```json
{
  "protocol": "openid4vp-v1-unsigned",

  "data": {
    "error": "<error_code>",
    "error_description": "<Human-readable description of the error>",
  }
}
```

There are 2 possible `error_code` values:

- `invalid_request`: This indicates that the request was malformed.
- `server_error`: This indicates that there were failures when processing the request. This could be either local errors or TS.43 issues.

The `error_description` field provides additional details about the issue.

## Validate the Digital Credential response

The following is an example of how to parse the response and perform the
validation step in your app's backend:

```python
def processDigitalCredentialsResponse(response):
  # Step 1: Parse out the TS.43 Digital Credential from the response
  openId4VpResponse = response['data']

  ts43_digital_credential = openId4VpResponse['vp_token']["aggregator1"][0]

  # Step 2: Perform response validation
  verifyResponse(ts43_digital_credential)

def verifyResponse(ts43_digital_credential):
  # The returned ts43_digital_credential is an SD-JWT-based Verifiable Credentials
  # (SD-JWT VC) as defined in this IETF spec. The section 3.4 of the specification
  # outlines how to validate the credential. At a high level, the steps involves
  # validating (1) the nonce in the response credential matches the one in the
  # request, (2) the integrity of the credential by checking the credential is
  # signed by the trusted issuer Android Telephony, and (3) other validity
  # properties associated with this credential, such as issue time and expiration
  # time

  # In most cases, you can use an SD-JWT VC library to perform these validations.

  # Some aggregators may also perform the validation logic for you. Check with your
  # aggregator to decide the exact scope of the validation required.
```

## Exchange for phone number

From your app's backend, send the validated `TS.43 Digital Credential` to the
aggregator's endpoint, to validates the credential and receive the verified
phone number.

```python
def processDigitalCredentialsResponse(response):
  # ... prior steps

  # Step 3: Call aggregator endpoint to exchange the verified phone number
  callAggregatorPnvEndpoint(ts43_digital_credential)
```

## Optional: Add user verification

You can require the user to authenticate with a biometric (such as a fingerprint
or face) or their screen lock (PIN, pattern, or password) to share their phone
number. If you'd like to add this additional check, you can include the
following in your DCQL request:

```json
"claims": [
  //... other claims
  {
    "path": [
      "user_verification_hint" // Use this to enable biometric authentication
    ]
  }
]
```

The OpenID4VP response will then contain a `user_verification_hint` property in
the payload body. There are 3 possible values:

- `success`: If user verification succeeded.
- `absent`: If user verification cannot be performed, such as when the user has no lock screen knowledge factor set up.
- `error`: If user verification failed.

If the verification was cancelled by the user, then a
`GetCredentialCancellationException` will be thrown.