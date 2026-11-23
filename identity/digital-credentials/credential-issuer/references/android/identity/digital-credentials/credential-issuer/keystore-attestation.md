In a typical digital credential issuance flow using the
[OpenID for Verifiable Credential Issuance (OpenID4VCI) specification](https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html), an
issuer needs to know that the key within the credential to be signed is stored
in a secure location. The `android_keystore_attestation` proof type---a format for
use with OpenID4VCI---provides a hardware-signed report from the
[Android Keystore](https://developer.android.com/privacy-and-security/keystore), ensuring the key is locked in a Trusted Execution
Environment (TEE) or StrongBox and cannot be exported or cloned.

> [!NOTE]
> **Note:** This document does not provide a minimum recommended set of attestation checks.

## Hardware attestation overview

When a key is generated in the Android Keystore, the system can generate an
attestation certificate. This certificate is signed by a key protected by the
device's hardware, which chains back to a Google-held Root of Trust.

The `android_keystore_attestation` proof is an array of X.509 certificate
chains. Each chain represents a single authentication key and is structured by
a leaf certificate followed by intermediate certificates.

- **Leaf certificate**: Contains the key and an Android-specific attestation extension.
- **Intermediate certificates**: Connect the leaf to the Android Root.

## Verification steps

Issuers should perform several validations on the attestation.

- Find the certificate in the chain that contains the Android attestation extension (typically the leaf certificate). This certificate contains the attestation data for the key minted by the Android Keystore.
- Verify that the `attestationChallenge` field in the extension matches the `c_nonce` provided by the protocol to prevent replay attacks.

> [!NOTE]
> **Note:** `attestationChallenge` is an `OCTET_STRING` and is the UTF-8 encoded value of the `c_nonce` string. This field can support a maximum of 128 bytes from the `c_nonce` string.

- Verify the value of all assertions in the extensions that you are interested in.
- Perform a revocation check against the Android Keystore certificates.

The values in the attestation proof come from several sources:

- **Issuer:** Various values are provided by the issuer, and the most common are put in the [issuer metadata format](https://developer.android.com/identity/digital-credentials/credential-issuer/keystore-attestation#issuer-metadata-format) to allow for filtering at presentation.
- **Holder:** Values such as the package name and signature come from the holder. These are not shared through standard issuance procedures and need to be obtained independently from the holder.
- **Protocol:** Values such as the nonce with `attestationChallenge` come from the protocol.

For more complete instructions on validating the attestation data, see the
following resources:

- [Verify hardware-backed key pairs with key attestation](https://developer.android.com/privacy-and-security/security-key-attestation#verifying)
- [Key and ID Attestation Extension Format](https://source.android.com/docs/security/features/keystore/attestation#attestation-extension)
- [Key attestation library](https://github.com/android/keyattestation)

## Attestation proof format

In a credential request, the `android_keystore_attestation` proof is included
in the following example:

```json
{
  "type": "array",
  "description": "An array of certificate chains. Each chain attests a single key.",
  "items": {
    "type": "array",
    "description": "An X.509 certificate chain. Each certificate is a Base64-encoded string. The first element in the chain is the leaf certificate with the extension, the last is the Android Keystore root certificate.",
    "items": {
      "type": "string",
      "description": "A single X.509 certificate (Base64-NoWrap padded DER encoded)."
    },
    "minItems": 1
  },
  "minItems": 1
}
```

It is then stored in the `proofs` object of a credential request.

```json
{
  "credential_configuration_id": "org.iso.18013.5.1.mDL",
  "proofs": {
    "android_keystore_attestation": [
      [
        "MII...", // Leaf certificate (contains Keystore extension)
        "MII...", // Intermediate certificate
        "MII..."  // Android Root certificate
      ],
      [ "MII...", "MII...", "MII..." ] // second proof
    ]
  }
}
```

## Issuer metadata format

An issuer indicates the proof types it supports by including the
`android_keystore_attestation` object within the `proof_types_supported` object
for a given credential configuration.

This is an example of the `android_keystore_attestation` object for issuers:

```json
{
  "type": "object",
  "properties": {
    "proof_signing_alg_values_supported": {
      "type": "array",
      "description": "REQUIRED. As defined in OpenID4VCI 1.0 Section 12.2.4.",
      "items": {
        "type": "string",
        "description": "Cryptographic algorithm identifiers used in the proof_signing_alg_values_supported Credential Issuer metadata parameter for this proof type are case sensitive strings and SHOULD be one of those defined in [IANA.JOSE]."
      },
      "minItems": 1
    },
    "key_attestations_required": {
      "type": "object",
      "description": "OPTIONAL. Specifies the minimum attestation requirements.",
      "properties": {
        "key_mint_security_level": {
          "type": "string",
          "description": "OPTIONAL. Minimum accepted keyMintSecurityLevel. Values defined in https://source.android.com/docs/security/features/keystore/attestation#securitylevel-values.",
          "enum": ["Software", "TrustedEnvironment", "StrongBox"],
          "default": "TrustedEnvironment"
        },
        "user_auth_types": {
          "type": "array",
          "description": "OPTIONAL. A list of authentication types which can authorize the use of the key. If empty, no authentication is required. If multiple, any are allowed.",
          "items": {
            "type": "string",
            "description": "Allowed values are 'LSKF' and 'BIOMETRIC'. These values are meant to mimic the values used during the key generation process here.",
            "enum": ["LSKF", "BIOMETRIC"]
          },
          "default": []
        }
      }
    }
  },
  "required": ["proof_signing_alg_values_supported"]
}
```

This is an example of the outer `proof_types_supported` object:

```json
{
  "credential_configurations_supported": {
    "org.iso.18013.5.1.mDL": {
      "format": "mso_mdoc",
      "doctype": "org.iso.18013.5.1.mDL",
      "cryptographic_binding_methods_supported": [
        "cose_key"
      ],
      "credential_signing_alg_values_supported": [
        -7, -9
      ],
      "proof_types_supported": {
        "android_keystore_attestation": {
          "proof_signing_alg_values_supported": [
            "ES256" // ecdsaWithSHA256
          ],
          "key_attestations_required" : {
            // OPTIONAL String - Representing the minimum accepted value for keyMintSecurityLevel values
            // defined here ("Software"|"TrustedEnvironment"|"StrongBox"). Default value: "TrustedEnvironment"
            "key_mint_security_level": "TrustedEnvironment",
            // OPTIONAL List of Strings - Representing all allowed values for userAuthType values defined here.
            // [] value will represent noAuthRequired. Default value: [].
            "user_auth_types": ["LSKF", "BIOMETRIC"]
          }
        }
      }
    }
  }
}
```

## Mapping VCI attestation claims to Android Keystore

This table provides an informational mapping to help entities familiar with the
standard OpenID4VCI attestation proof type understand where similar concepts are
located within the Android Keystore attestation.

|---|---|---|
| **VCI attestation claim** | **`android_keystore_attestation` location** | **Expected value location** |
| **iss** | Public key of the Keystore root certificate | N/A |
| **iat** | `creationDateTime` value in the attestation extension | N/A |
| **exp** | `validUntil` field in the leaf certificate | N/A |
| **attested_keys** | Public key contained in the leaf certificate of each chain | N/A |
| **key_storage** | `keyMintSecurityLevel` value in the attestation extension | Issuer selected: `key_mint_security_level` field in the issuer metadata |
| **user_authentication** | `userAuthType` and `noAuthRequired` values in the attestation extension | Issuer selected: `user_auth_types` field in the issuer metadata |
| **nonce** | `attestationChallenge` value in the attestation extension | From protocol: `c_nonce` value from the [nonce endpoint](https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-7) described in VCI |
| **certification** | N/A | N/A |
| **status** | N/A | N/A |