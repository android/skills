Use the Phone Number Verification API to retrieve a verified phone
number in a phishing-resistant way from a user's mobile carrier.
It's a faster, more secure, and cost-effective verification method
that doesn't rely on one-time passwords (OTPs) sent over SMS.

## Overview

Traditional phone verification relies heavily on SMS-based one-time passwords
(OTPs). SMS verification causes user friction and delivery delays. It's also
vulnerable to SIM-swapping or interception, and incurs significant
telecommunication fees.

Android's phone number verification API solves these challenges by leveraging
digital credentials. Instead of sending an SMS, the app requests a digital
token directly through Android's Credential Manager. After your user consents
to sharing this token, the app backend then exchanges this token with a
cellular aggregator or mobile carrier to retrieve the verified phone number.

## Verified phone number retrieval flow

Follow these steps to retrieve a verified phone number:

- Your app makes a request to the Digital Credentials API for a phone number.
- The Credential Manager bottom sheet appears to confirm sharing of the phone number.
- The API retrieves an encrypted TS.43 token from the user's carrier by authenticating the SIM card.
- Credential Manager shares the token with your app. Your app sends the token to your app server, which exchanges the token with an aggregator or mobile carrier endpoint. This is also when telephony charges would happen.
- The aggregator or carrier returns the verified phone number to your app.

![Image showing the phone number verification user experience.](https://developer.android.com/static/identity/digital-credentials/images/phone-number-verification1.png) **Figure 1.** The phone number verification user experience.

## Key benefits

This API provides many benefits:

- **Reduced verification costs** - You only pay for verifications that make it to your app, reducing delivery costs.
- **More secure and streamlined user experience** - Users approve the system bottom sheet prompt with one tap instead of viewing SMS OTPs outside of the app. This also eliminates the potential for SMS phishing.
- **Higher conversion rates**- SMS delivery failures and delays are major drivers of drop-off. Direct carrier verification bypasses SMS transit entirely, leading to higher completion rates.
- **Identity assurance**- Verifies the actual physical SIM/eSIM active on the Android-powered device, ensuring the identity is tied directly to an active carrier subscription.

## Security and privacy

Android's phone verification feature is built on open standards and
hardware-backed cryptographic security.

**Key security standards**:

- **GSMA TS.43 Standard**: Standardized carrier protocol for identity assertion and token generation.
- **SD-JWT VC (Selective Disclosure JWT Verifiable Credential)**: Ensures tamper-proof credential integrity and selective data disclosure.
- **OpenID4VP Protocol**: Industry-standard specification for requesting verifiable credentials.

**Cryptographic nonce and key bindings**

Each verification request includes a server-generated nonce and key-binding
signature. These elements prevent replay attacks, person-in-the-middle (PiTM)
tampering, and credential spoofing.

**End-to-end encryption**

Android Telephony encrypts the TS.43 token returned to the client app.
Only the authorized aggregator or mobile carrier endpoint can decrypt it,
which ensures the system doesn't expose raw user credentials during transit.

**Transparent user consent**

Android presents a Credential Manager bottom sheet informing the user exactly
what identity information is shared before any data is transmitted to the
app.

## Next steps

To implement this feature in your app, see the [Implementation
guide](https://developer.android.com/identity/digital-credentials/implement-phone-number-verification).