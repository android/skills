This page describes the security and trust architecture for Android digital
credentials, which rely on cryptographic validation from trusted issuers to
secure user data.

## Verifiable digital credentials

On Android, digital credentials refer to verifiable credentials
cryptographically validated by an issuer. These credentials contain secured
claims, making them tamper-evident and proving their origin.

A claim marked as "verified" means that the issuer is asserting they can vouch
for it, and have signed it with their digital certificate. Because of this
signature, verifying applications can be certain that the credential hasn't been
tampered with and was issued by a confirmable issuer.

> [!IMPORTANT]
> **Important:** Verifiers must determine the level of trust they place in a specific issuer.

## What it means for a claim to be verified

When a credential arrives through the Android Credential Manager API and a claim
within it is marked as "verified", it implies that the issuer is asserting that
they performed a check on that specific piece of data. However, it doesn't mean
the data is an absolute, universal truth. "Verified" is an assertion of process,
not an automatic guarantee of trust.

The core of this ecosystem is that trust is always resolved at the verifier.
When your app, as the verifier, receives the cryptographically secure data and
sees that the issuer marked it as "verified," it must determine whether it
trusts the issuer to have verified the claim to its standards.

## Security of digital credentials

The digital credentials ecosystem has built-in cryptographic security measures
at each step of the way to ensure data integrity and authenticity:

- **Issuer certificate signing:** All issued credentials are cryptographically signed using the issuer's digital certificate. This enables verifiers to check this signature against the issuer's public certificate to confirm the credential's origin and verify no data was modified.
- **Holder credential binding:** When an issuer issues a credential to a holder, the holder needs to prove that the credentials it holds are tied to secure device hardware and can't be copied or transferred. The holder does this by creating a hardware attestation certificate signed by a root key of its device's OS, proving its key's security and manufacturer origin, and sends this to the issuer. The issuer then verifies these attributes to confirm the key is bound to genuine hardware. Once verified, the issuer embeds the holder's public key in the credential payload and signs the credential with its own private key before issuing it to the holder. This permanently links that issued credential with the holder, which prevents credential theft or copying.
- **Dual-layer presentation checks:** When a verifier requests a credential, it sends a nonce to the holder app which is signed by the holder's private, hardware-bound key and verifies this with the holder's public key embedded in the credential. This proves that the holder currently controls the key, and also prevents replay attacks. The verifier must also check the issuer's digital signature to verify the credential's origin. This dual-layer cryptographic verification helps ensure verifiers receive credentials that haven't been compromised at either the holder or issuer level.