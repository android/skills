This guide describes how to request digital credentials using Credential Manager
in an Android app that uses WebView. Use a JavaScript Bridge to facilitate
communication between the web page and the app.

## Overview

The following steps describe the life of a digital credential request from a
WebView:

1. **Request generation**: The web page in the WebView makes a request to the app server, which generates an OpenID4VP request JSON and sends it back.
2. **Request initiation** : The web page uses `navigator.credentials.get({
   digital: ...` }) to request digital credentials using the request JSON.
3. **Interception** : The injected `bridge.js` intercepts this call, extracts the specific OpenID4VP request JSON, and passes it to the Android app.
4. **Android Handling** : The Android app receives the message and calls the `CredentialManager.getCredential` API.
5. **User selection**: Credential Manager displays a UI prompting the user to share their credential. The credential is returned to the Android app.
6. **Bridge response**: The Android app sends the response back to the web page using the bridge.
7. **Completion**: The web page receives and handles the response.

## Prerequisites

Before you implement the JavaScript bridge for digital credentials in WebView,
ensure that your project meets the following requirements:

- **Android version compatibility**: Your app must target Android 6.0 (API level 23) or higher.
- **Host app control** : You must control the Android app hosting the `WebView` so that you can attach a `WebMessageListener` before loading the web page.
- **HTTPS and top-level frame** : The web page requesting the credential must be served over `https://` and run in the main frame (`isMainFrame` must be `true`). Subframes and embedded `<iframe>` elements aren't supported.
- **Experimental API opt-in** : Because the Digital Credentials API is experimental, you must annotate classes or methods that use `GetDigitalCredentialOption` with `@OptIn(ExperimentalDigitalCredentialApi::class)`.

### Add dependencies

Add the Credential Manager and AndroidX WebKit dependencies to your app module's
`build.gradle.kts` file:

```kotlin
dependencies {
    // Credential Manager dependencies
    implementation("androidx.credentials:credentials:1.7.0-alpha03")
    implementation("androidx.credentials:credentials-play-services-auth:1.7.0-alpha03")

    // AndroidX WebKit (required for WebMessageListener bridge)
    implementation("androidx.webkit:webkit:1.14.0")
}
```

## Implement digital credentials in the WebView

You'll need to add JavaScript to your WebView to make the credential request
and intercept it with `bridge.js`.

### Create the JavaScript request

To communicate between JavaScript running in a WebView and Android code, inject
JavaScript code into your WebView:

```javascript
// Example digital credential request to request verified email
const request = {
  "providers": [
      {
        "protocol": "openid4vp",
        "request": JSON.stringify({
          "requests": [
            {
              "protocol": "openid4vp-v1-unsigned",
              "data": {
                "response_type": "vp_token",
                "response_mode": "dc_api",
                "nonce": "n-0S6_W_2F", // Generate dynamically
                "dcql_query": {
                  "credentials": [
                    {
                      "id": "user_info_query",
                      "format": "dc+sd-jwt",
                      "meta": {
                        "vct_values": ["UserInfoCredential"]
                      },
                      "claims": [
                        {"path": ["email"]},
                        {"path": ["email_verified"]},
                        {"path": ["name"]}
                      ]
                    }
                  ]
                }
              }
            }
          ]
      })
    }
  ]
};

try {
  const credential = await navigator.credentials.get({
    digital: request
  });

  // Process response
} catch (err) {
  // Handle error
}
```

### Create the JavaScript bridge

Create a file named `bridge.js` or inject this code using `evaluateJavascript`.
This script overrides `navigator.credentials.get` to intercept the request and
send the inner OpenID4VP JSON string to the Android app.

```javascript
(function() {
  if (!navigator.credentials) {
    navigator.credentials = {};
  }

  const originalGet = navigator.credentials.get;

  // Overrides navigator.credentials.get() to intercept W3C Digital Credential requests.
  navigator.credentials.get = async function(options) {

    if (options && options.digital) {
      if (window.DigitalCredentialBridge) {
        return new Promise((resolve, reject) => {
          const digital = options.digital;

          if (digital.providers && digital.providers.length > 0) {
            const provider = digital.providers[0];

            if (provider.protocol === "openid4vp") {
              const requestJson = provider.request;

              const message = {
                type: "get",
                request: requestJson
              };

              console.log("Sending message to native bridge:", JSON.stringify(message));
              // Send the request string to native Android code
              window.DigitalCredentialBridge.postMessage(JSON.stringify(message));

              // Receive the response sent from native code using replyProxy.postMessage()
              window.DigitalCredentialBridge.onmessage = function(event) {
                console.log("Received response from native bridge:", event.data);
                try {
                  const response = JSON.parse(event.data);
                  if (response.error) {
                    reject(new Error(response.error));
                  } else {
                    resolve(response);
                  }
                } catch (e) {
                  // If it's not JSON, just resolve with the raw data
                  resolve(event.data);
                }
              };
            } else {
              reject(new Error("Unsupported protocol: " + provider.protocol));
            }
          } else {
            reject(new Error("No providers specified in digital credential request"));
          }
        });
      } else {
        console.warn("DigitalCredentialBridge not found, falling back to original");
      }
    }

    if (originalGet) {
      return originalGet.call(navigator.credentials, options);
    } else {
      throw new Error("navigator.credentials.get is not supported and no bridge available");
    }
  };
})();
```

## Implement digital credentials in Android

Configure your Android app to handle credential requests sent from `bridge.js`.

### Create the Android listener

Set up a `DigitalCredentialWebListener` class that implements
`WebViewCompat.WebMessageListener`. This class handles bidirectional messages
between JavaScript and native Android code, enforcing security checks on
`isMainFrame` and `sourceOrigin` and preventing overlapping requests.

### Implement the Credential Manager Handler

Call `getCredential` to request digital credentials from the device.

```kotlin
@OptIn(ExperimentalDigitalCredentialApi::class)
class CredentialManagerHandler(private val context: Context) {
  private val credentialManager = CredentialManager.create(context)

  suspend fun getDigitalCredential(requestJson: String): String {
    val digitalCredentialOption = GetDigitalCredentialOption(requestJson)
    val getCredRequest = GetCredentialRequest(listOf(digitalCredentialOption))

    return try {
      val result = credentialManager.getCredential(context, getCredRequest)
      val credential = result.credential
      if (credential is DigitalCredential) {
        credential.credentialJson
      } else {
        "{\"error\": \"Unexpected credential type\"}"
      }
    } catch (e: GetCredentialException) {
      "{\"error\": \"${e.message}\"}"
    } catch (e: Exception) {
      "{\"error\": \"Unknown exception\"}"
    }
  }

  companion object {
    private const val TAG = "CredManHandler"
  }
}
```

### Apply the framework to your Activity

In your `Activity` or `View`, register the `WebMessageListener` and associated
functions.

```kotlin
// Inside your Activity or View setup
AndroidView(
  factory = { context ->
    val handler = CredentialManagerHandler(context)
    WebView(context).apply {
      settings.javaScriptEnabled = true

      // Register the bridge before loading URL
      if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
                val listener = DigitalCredentialWebListener(coroutineScope, credentialManagerHandler)
                WebViewCompat.addWebMessageListener(
                    webView,
                    INTERFACE_NAME,
                    setOf("*"),
                    listener
                )


      loadUrl("https://your-secure-domain.com/login")
    }
  },
)
```

## Testing the flow

Test the flow to confirm communication between the WebView and the Android app.
The credential selector appears in the WebView during the process, and the
selected credential returns to the WebView upon completion.