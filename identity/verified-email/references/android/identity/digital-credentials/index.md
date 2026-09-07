Digital credentials are cryptographically verifiable digital documents that your
users can use to provide information about themselves, and use to authenticate
or authorize themselves. Digital credentials are based on the [open W3C Digital
Credentials API industry standard](https://www.w3.org/TR/digital-credentials/). On Android, this is
implemented through [Credential Manager's](https://developer.android.com/identity/credential-manager) [Digital Credentials API](https://developer.android.com/reference/kotlin/androidx/credentials/DigitalCredential).
![Image showing the flow of using a digital credential](https://developer.android.com/static/identity/digital-credentials/images/digital_credentials.png) **Figure 1.** Using a digital credential in a sample app.

## Benefits of digital credentials

Digital credentials offer several advantages over physical credentials and
non-standardized digital documents:

- **Improved security and trust**: Digital credentials are encrypted, ensuring data integrity and authenticity. This asserts the surety that the credential was issued by a verifiable source and was not tampered with.
- **Enhanced privacy**: Many digital credential formats support selective disclosure, which lets users share only necessary information. For example, a user could share proof of driving qualification without revealing their birth date.
- **Consolidated storage**: Users can store various credentials from different issuers in digital holders all on their device, reducing the need to carry physical cards.
- **Interoperability**: By following open standards, digital credentials can work across different operating systems, devices, and platforms.

## Use cases

The Digital Credentials API can be used across a broad set of use cases such as
the following:

- **Accept government-issued IDs**: Apps can request and use official government document attributes, for flows including age verification, account recovery, Know-Your-Customer (KYC) process.
- **Verify phone numbers** : Apps can use the API for phone number verification, by exchanging digital credentials derived from the phone's SIM card directly with the user's carrier. This removes the need for one-time passwords (OTPs), improves security, and reduces transmission costs. For more information, see the [phone number verification guide](https://developer.android.com/identity/digital-credentials/phone-number-verification).
- **Verify email addresses** : The API lets your app retrieve verified emails directly from the user's device, removing the need for OTPs, for frictionless sign-up, sign-in, and account recovery. For more information, see the [email verification guide](https://developer.android.com/identity/digital-credentials/email-verification).
- **Custom credentials**: The extensibility of the API lets any app begin issuing its own custom digital credentials, which corresponding verifiers can request.
- **Confirm payment credentials and transactions**: The API lets you secure payment authorizations and Digital Payment Credentials (DPC) with native, cryptographically bound wallet confirmation.

## How digital credentials work

The digital credential ecosystem involves three primary categories of apps:

- **Issuers** : Issuers are apps that securely create and [issue](https://developer.android.com/identity/digital-credentials/credential-issuer/issue-credentials) credentials.
- **Holders (wallets)** : Holders are apps on a user's device that store credentials. They should be able to share these credentials with requesting apps through a [presentation](https://developer.android.com/identity/digital-credentials/credential-holder/credential-holder) process.
- **Verifiers**: Verifiers are apps that verify and use digital credentials.

Credential Manager's Digital Credentials API orchestrates the interaction
between the issuers, holders, and verifier apps.

When a verifier makes a request for a digital credential, it sends a request to
the Android system through the API. Credential Manager then displays eligible
digital credentials from various holders within a trusted system UI. Once the
user agrees to proceed, Credential Manager invokes the corresponding holder to
generate the response.

> [!NOTE]
> **Note:** Issuers and holders don't have to be separate---an app can be both an issuer and holder. For example, if you use the Digital Credentials API for email verification, Google is the issuer and holder for the Gmail email address.

## User experience

Similarly to how Credential Manager has built-in user interfaces for
authentication flows, such as with passkeys, passwords, and Sign in with Google,
there are also standardized interfaces for the various use cases of the Digital
Credentials API.

There are UI variants tailored to specific use cases. When using the API, the
Android system automatically renders context-aware bottom sheets, such as for
the following scenarios:

- **Email verification**: The interface displays an account picker featuring the verified email address and identity provider.
- **Phone number verification**: The interface includes a verification card with network carriers represented.
- **Multi-credential**: This view stacks multiple cards, and allows bundling related items into a single tap.
- **Digital payments credentials**: This checkout sheet displays payment card details, merchant info, and total transaction amount for immediate confirmation.

![Image showing the UX variants of digital credentials](https://developer.android.com/static/identity/digital-credentials/images/digital_credentials_ux_variants.png) **Figure 2.** The UX variants for digital credentials for email verification, phone verification, and multi-credential scenarios.

## Industry standards

Digital credentials rely on industry standards to ensure cross-platform
compatibility. The commonly used open standards are:

- **Sharing** : Digital credentials are requested using the [OpenID4VP
  standard](https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#name-introduction).
- **Issuance** : Digital credentials are issued using the [OpenID4VCI
  standard](https://openid.github.io/OpenID4VCI/openid-4-verifiable-credential-issuance-1_1-wg-draft.html).
- **Credential storage format** : Digital credentials are represented in standardized formats maintained by different standards bodies, primarily the following:
  - IETF [selective disclosure of JSON web tokens (sd-jwt)](https://datatracker.ietf.org/doc/draft-ietf-oauth-selective-disclosure-jwt/) VC
  - ISO [MDoc](https://www.iso.org/standard/69084.html)

These standards are used widely on multiple platforms and operating systems,
enabling seamless operation across web browsers, mobile devices, and other form
factors. This allows developers to exchange digital credentials with other apps
regardless of the platform they're on.

## Try it out

You can test the cross-platform digital credentials flow by installing the
sample holder and verifier apps on an Android-powered device.

To test the flow, complete the following steps:

1. **Install the sample apps on the Android-powered device** : Use one of the following methods to install the apps on your first device:
   - **Use the prebuilt APKs** :
     - **Download the holder app** : Sign in to your GitHub account, navigate to the [holder app GitHub Actions page](https://github.com/digitalcredentialsdev/CMWallet/actions), select the latest successful build, and download the `app-debug.apk` file from the **Artifacts** section. This app is called **CMWallet**.
     - **Download the verifier app** : For the sample verifier app, get the APK from the [Identity samples repository](https://github.com/android/identity-samples/actions). Then, install the APK on your Android-powered device. This app is called **Digital
       Credentials Demo**.
   - **Build from source**: Clone the repositories mentioned and install the app using Android Studio.
2. **Open the sample holder app**: Open the sample holder app. This registers the holder credentials with Credential Manager.
3. **Open the sample verifier app** : Open the sample verifier app. Then, select the option to request digital credentials from wallets. A bottomsheet displaying available credentials from **CMWallet** should appear.
4. **Select a credential**: Select a credential to send back to the verifier app. The verifier app should now display fields from the returned credential.

> [!NOTE]
> **Note:** You can also initiate the request from a web browser on another device by navigating to <https://digital-credentials.dev/> and selecting **Request Credentials (OpenID4VP 1.0)**.

## Resources

- For more information about issuing digital credentials, see the [issuer
  guide](https://developer.android.com/identity/digital-credentials/credential-issuer/issue-credentials).
- For more information about holder apps, see the [holder guide](https://developer.android.com/identity/digital-credentials/credential-holder/credential-holder).
- For more information about verifying users based on digital credentials, see the [verifier guide](https://developer.android.com/identity/digital-credentials/credential-verifier).
- For more information about digital credentials on the web, see [digital
  credentials on the web](https://developer.chrome.com/blog/digital-credentials-api-shipped).