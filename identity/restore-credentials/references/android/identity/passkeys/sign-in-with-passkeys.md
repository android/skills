This guide continues on the implementation of using passkeys for authentication.
Before your users can sign in with passkeys, you must also complete the
instructions in [Create passkeys](https://developer.android.com/identity/passkeys/create-passkeys).

To authenticate with a passkey, you must first retrieve the options required to
retrieve the public key from your [app server](https://developer.android.com/identity/credential-manager#authentication-terminology), and then call the Credential
Manager API to retrieve the public key. Then, handle the sign-in response
appropriately.

> [!TIP]
> **Tip:** While designing authentication flows with passkeys, make sure you follow the recommendations in the [UX guidelines for passkeys](https://developer.android.com/design/ui/mobile/guides/patterns/passkeys).

## Overview

This guide focuses on the changes required in your client app to sign in your
user with a passkey, and gives a brief overview of the app server-side
implementation. To learn more about server-side integration, see [Server-side
passkey authentication](https://developers.google.com/identity/passkeys/developer-guides/server-authentication).

To retrieve all the passkey and password options that are associated with the
user's account, complete these steps:

1. [**Get credential request options from the server**](https://developer.android.com/identity/passkeys/sign-in-with-passkeys#get-options): Make a request from your app to your authentication server to start the passkey sign-in process. From the server, send the options required to get the public key credential, as well as a unique challenge.
2. [**Create the object required to get the public key credential**](https://developer.android.com/identity/passkeys/sign-in-with-passkeys#create-object): Wrap the options sent by the server in a `GetPublicKeyCredentialOption` object
3. ([**optional) Prepare getCredential**](https://developer.android.com/identity/passkeys/sign-in-with-passkeys#reduce-latency): In Android 14 and higher, you can reduce latency by showing the account selector by using the `prepareGetCredential()` method before calling `getCredential()`.
4. [**Launch the sign in flow**](https://developer.android.com/identity/passkeys/sign-in-with-passkeys#launch-sign-in): Call `getCredential()` method to sign in the user
5. [**Handle the response**](https://developer.android.com/identity/passkeys/sign-in-with-passkeys#handle-response): Handle each of the possible credential responses.
6. [**Handle exceptions**](https://developer.android.com/identity/passkeys/sign-in-with-passkeys#handle-exceptions): Make sure that you handle exceptions appropriately.

> [!TIP]
> **Tip:** Enhance the user experience during sign in by also adding functionality to [restore credentials](https://developer.android.com/identity/sign-in/restore-credentials) on a new device to let users seamlessly set up their existing accounts on new Android devices.

## Get credential request options from the server

Request the server for the options required to get the public key credentials,
as well as the `challenge`, which is unique for each sign-in attempt. To learn
more about the server-side implementation, see [Create the
challenge](https://developers.google.com/identity/passkeys/developer-guides/server-authentication#create_the_challenge) and [Create credential request
options](https://developers.google.com/identity/passkeys/developer-guides/server-authentication#create_credential_request_options).

The options look similar to the following:

    {
      "challenge": "<your app challenge>",
      "allowCredentials": [],
      "rpId": "<your app server domain>"
    }

To learn more about the fields, see the blogpost about [signing in with a
passkey](https://web.dev/articles/passkey-form-autofill#fetch_information_from_the_backend).

> [!TIP]
> **Tip:** To reduce wait times in the UI because of as network calls, fetch the details required from the app server at the beginning of the user's app session.

## Create the object required to get the public key credential

In your app, use the options to create a `GetPublicKeyCredentialOption` object.
In the following example, `requestJson` represents the options sent by the
server.

    // Get password logins from the credential provider on the user's device.
    val getPasswordOption = GetPasswordOption()

    // Get passkeys from the credential provider on the user's device.
    val getPublicKeyCredentialOption = GetPublicKeyCredentialOption(
        requestJson = requestJson
    )

Then, wrap `GetPublicKeyCredentialOption` in a `GetCredentialRequest` object.

    val credentialRequest = GetCredentialRequest(
        // Include all the sign-in options that your app supports.
        listOf(getPasswordOption, getPublicKeyCredentialOption),
        // Defines whether you prefer to use only immediately available
        // credentials or hybrid credentials.
        preferImmediatelyAvailableCredentials = preferImmediatelyAvailableCredentials
    )

> [!NOTE]
> **Note:** [`GetCredentialRequest`](https://developer.android.com/reference/androidx/credentials/GetCredentialRequest#GetCredentialRequest) can include an `origin` field. However, `origin` is automatically set for Android apps, and hence should be left as null. For browsers and similarly privileged apps that need to set `origin`, see [Make Credential Manager calls on behalf of other parties for privileged
> apps](https://developer.android.com/training/sign-in/privileged-apps).

## Optional: Reduce sign-in latency

On Android 14 or higher, you can reduce latency when showing the account
selector by using the [`prepareGetCredential()`](https://developer.android.com/reference/androidx/credentials/CredentialManager#prepareGetCredential) method before calling
`getCredential()`.

The `prepareGetCredential()` method returns a
[`PrepareGetCredentialResponse`](https://developer.android.com/reference/androidx/credentials/PrepareGetCredentialResponse) object which is cached. This lets the
`getCredential()` method in the following step bring up the account selector
with the cached data.

    coroutineScope {
        val response = credentialManager.prepareGetCredential(
            GetCredentialRequest(
                listOf(
                    // Include all the sign-in options that your app supports
                    getPublicKeyCredentialOption, 
                    getPasswordOption
                )
            )
        )
    }

## Launch the sign-in flow

Call the `getCredential()` method to show the user the account selector. Use the
following code snippet as a reference for how to launch the sign-in flow:

> [!NOTE]
> **Note:** To avoid a potential memory leak, pass in a `MutableContextWrapper` of your foreground `Activity` to the `getCredential()` call. Credential Manager handles updating the context during `Activity` reconstruction.

    // Use an activity-based context to avoid undefined system UI
    // launching behavior.
    val context = MutableContextWrapper(activityContext)
    coroutineScope {
        try {
            result = credentialManager.getCredential(
                // Use MutableContextWrapper to avoid memory leak during configuration changes
                context = context,
                request = credentialRequest
            )
            handleSignIn(result)
        } catch (e: GetCredentialException) {
            // Handle failure
        }
    }

> [!NOTE]
> **Note:** If you call `getCredential()` but the user does not have any credentials available, or if they don't grant consent to using their credentials, a `NoCredentialException` is returned. To learn more about this exception, see the [troubleshooting guide](https://developer.android.com/identity/sign-in/credential-manager-troubleshooting-guide).

## Handle the response

Handle the response, which can contain one of various types of credential
objects.

    fun handleSignIn(result: GetCredentialResponse) {
        // Handle the successfully returned credential.
        val credential = result.credential

        when (credential) {
            is PublicKeyCredential -> {
                val responseJson = credential.authenticationResponseJson
                // Share responseJson i.e. a GetCredentialResponse on your server to
                // validate and  authenticate
            }

            is PasswordCredential -> {
                val username = credential.id
                val password = credential.password
                // Use id and password to send to your server to validate
                // and authenticate
            }

            is CustomCredential -> {
                // If you are also using any external sign-in libraries, parse them
                // here with the utility functions provided.
                if (credential.type == ExampleCustomCredential.TYPE) {
                    try {
                        val ExampleCustomCredential =
                            ExampleCustomCredential.createFrom(credential.data)
                        // Extract the required credentials and complete the authentication as per
                        // the federated sign in or any external sign in library flow
                    } catch (e: ExampleCustomCredential.ExampleCustomCredentialParsingException) {
                        // Unlikely to happen. If it does, you likely need to update the dependency
                        // version of your external sign-in library.
                        Log.e(TAG, "Failed to parse an ExampleCustomCredential", e)
                    }
                } else {
                    // Catch any unrecognized custom credential type here.
                    Log.e(TAG, "Unexpected type of credential")
                }
            }
            else -> {
                // Catch any unrecognized credential type here.
                Log.e(TAG, "Unexpected type of credential")
            }
        }
    }

The `PublicKeyCredential` returned from authentication is essentially a signed
assertion, structured as follows:

    {
      "id": "<credential ID>",
      "type": "public-key",
      "rawId": "<raw credential ID>",
      "response": {
        "clientDataJSON": "<signed client data containing challenge>",
        "authenticatorData": "<authenticator metadata>",
        "signature": "<digital signature to be verified>",
        "userHandle": "<user ID from credential registration>"
      }
    }

On the server, you must verify the credential. To learn more, see [Verify and
sign in the user](https://developers.google.com/identity/passkeys/developer-guides/server-authentication#verify_and_sign_in_the_user).

## Handle exceptions

You should handle all the subclass exceptions of [`GetCredentialException`](https://developer.android.com/reference/androidx/credentials/exceptions/GetCredentialException).
To learn how to handle each exception, see the [troubleshooting guide](https://developer.android.com/identity/sign-in/credential-manager-troubleshooting-guide).

    coroutineScope {
        try {
            result = credentialManager.getCredential(
                context = activityContext,
                request = credentialRequest
            )
        } catch (e: GetCredentialException) {
            Log.e("CredentialManager", "No credential available", e)
        }
    }

## Next steps

- [Manage passkeys](https://developer.android.com/identity/passkeys/manage-passkeys)
- [Understand passkey user experience flows](https://developer.android.com/design/ui/mobile/guides/patterns/passkeys)