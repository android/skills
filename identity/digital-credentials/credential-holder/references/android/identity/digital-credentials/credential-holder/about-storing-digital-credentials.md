The Credential Manager Holder API lets your Android holder (also called
"wallet") app to manage and present digital credentials to verifiers.
![Image showing the digital credentials UI in Credential Manager](https://developer.android.com/static/identity/digital-credentials/images/digital-credential-selector-ui.png) **Figure 1.** The digital credentials selector UI.

### Credential formats

Credentials can be stored in holder apps in different credential formats. These
formats are specifications for how a credential should be represented, and each
one contains the following information about the credential:

- **Type**: The category such as university degree or a driver's license.
- **Properties**: Attributes such as first and last name.
- **Encoding**: The way the credential is structured, for example SD-JWT or mdoc

- **Validity**: Method to cryptographically verify the credential's
  authenticity.

Each credential format does the encoding and validation slightly differently,
but functionally they are the same.

The registry supports two formats:

- **SD-JWT** : conforms to the [IETF SD-JWT-based Verifiable Credentials (SD-JWT
  VC) specification](https://datatracker.ietf.org/doc/draft-ietf-oauth-sd-jwt-vc/).
- **Mobile Documents or mdocs** : conforms to the [ISO/IEC 18013-5:2021
  specification](https://www.iso.org/standard/69084.html).

A verifier can make an OpenID4VP request for SD-JWT and mdocs when using
Credential Manager. The choice varies depending on the use case and the industry
choice.

### Credential metadata registration

Credential Manager doesn't store a holder's credentials directly, but rather the
credentials' **metadata** . A holder app must first register credential metadata
with Credential Manager using `RegistryManager`. This registration process
creates a registry record that serves two key purposes:

- **Matching**: Registered credential metadata is used to match with future verifier requests.
- **Display**: Customized UI elements are shown to the user on the credential selector interface.

Use the `OpenId4VpRegistry` class to register your digital credentials,
as it supports both mdoc and SD-JWT credential formats. Verifiers will send
[OpenID4VP requests](https://developer.android.com/identity/digital-credentials/credential-verifier/request-credential#add-dependencies) to request these credentials.

### Credential issuance methods

Credential issuance can be triggered in two ways:

- **Issuer-initiated flow** : the user triggers a request to issue a VDC from an issuer application or website. For example, a website may offer its users an option to "Add your passport to your wallet". The issuer calls the [Credential
  Manager issuance API](https://developer.android.com/identity/digital-credentials/credential-issuer/issue-credentials) to make an OpenID4VCI Credential Offer request. To handle such requests, a holder must first integrate with Credential Manager to register its metadata. Credential Manager will display relevant options for a user to select. After the user selects a holder option, the holder application will be invoked and can proceed with the steps needed to complete the issuance.
- **Holder-initiated flow**: the user requests to add a VDC from their holder application. For example, a holder may offer a button to "Add your passport". In this case, the holder maintains its supported issuer list and metadata. It doesn't need to integrate with the Android Credential Manager to complete this function.