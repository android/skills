<br />

**OWASP category:** [MASVS-CRYPTO: Cryptography](https://mas.owasp.org/MASVS/06-MASVS-CRYPTO)

## Overview

> [!NOTE]
> **Note:** This article isn't focused on how to protect API keys.

Developers use cryptography to protect confidentiality and integrity of data using robust algorithms. However, the key storage is often underused, and it's common to find them hardcoded into the application as a string or byte array in the code or in an asset file such as `strings.xml`. If secrets are exposed in any files of the app, this goes against [Kerchoff's principle](https://en.wikipedia.org/wiki/Kerckhoffs%27s_principle) and the security model can be considered as broken.

## Impact

An attacker with access to reverse engineering tools can retrieve a hard-coded secret very easily. Depending on conditions the impact might vary, but in many cases it leads to major security issues, such as access to sensitive data.

## Mitigations

To mitigate this issue, consider using the [KeyChain](https://developer.android.com/reference/android/security/KeyChain) API when you want system-wide credentials, or the [Android Keystore](https://developer.android.com/training/articles/keystore) provider to let an individual app store its own credentials that only the app itself can access.

The following code snippet shows how to store and use a symmetric key using
`KeyStore`:

### Kotlin

```kotlin
private val ANDROID_KEY_STORE_PROVIDER = "AndroidKeyStore"
private val ANDROID_KEY_STORE_ALIAS = "AES_KEY_DEMO"

@Throws(
    KeyStoreException::class,
    NoSuchAlgorithmException::class,
    NoSuchProviderException::class,
    InvalidAlgorithmParameterException::class
)
private fun createAndStoreSecretKey() {
    val builder: KeyGenParameterSpec.Builder = KeyGenParameterSpec.Builder(
        ANDROID_KEY_STORE_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    )
    val keySpec: KeyGenParameterSpec = builder
        .setKeySize(256)
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setRandomizedEncryptionRequired(true)
        .build()
    val aesKeyGenerator: KeyGenerator =
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE_PROVIDER)
    aesKeyGenerator.init(keySpec)
    val key: SecretKey = aesKeyGenerator.generateKey()
}

@Throws(
    KeyStoreException::class,
    UnrecoverableEntryException::class,
    NoSuchAlgorithmException::class,
    CertificateException::class,
    IOException::class,
    NoSuchPaddingException::class,
    InvalidKeyException::class,
    IllegalBlockSizeException::class,
    BadPaddingException::class
)
private fun encryptWithKeyStore(plainText: String): ByteArray? {
    // Initialize KeyStore
    val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE_PROVIDER)
    keyStore.load(null)
    // Retrieve the key with alias androidKeyStoreAlias created before
    val keyEntry: KeyStore.SecretKeyEntry =
        keyStore.getEntry(ANDROID_KEY_STORE_ALIAS, null) as KeyStore.SecretKeyEntry
    val key: SecretKey = keyEntry.secretKey
    // Use the secret key at your convenience
    val cipher: Cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, key)
    return cipher.doFinal(plainText.toByteArray())
}
```

### Java

```java
static private final String ANDROID_KEY_STORE_PROVIDER = "AndroidKeyStore";
static private final String ANDROID_KEY_STORE_ALIAS = "AES_KEY_DEMO";

private void createAndStoreSecretKey() throws KeyStoreException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
    KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
        ANDROID_KEY_STORE_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT);
    KeyGenParameterSpec keySpec = builder
        .setKeySize(256)
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setRandomizedEncryptionRequired(true)
        .build();
    KeyGenerator aesKeyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE_PROVIDER);
    aesKeyGenerator.init(keySpec);
    SecretKey key = aesKeyGenerator.generateKey();
}

private byte[] encryptWithKeyStore(final String plainText) throws KeyStoreException, UnrecoverableEntryException, NoSuchAlgorithmException, CertificateException, IOException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
    // Initialize KeyStore
    KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE_PROVIDER);
    keyStore.load(null);
    // Retrieve the key with alias ANDROID_KEY_STORE_ALIAS created before
    KeyStore.SecretKeyEntry keyEntry = (KeyStore.SecretKeyEntry) keyStore.getEntry(ANDROID_KEY_STORE_ALIAS, null);
    SecretKey key = keyEntry.getSecretKey();
    // Use the secret key at your convenience
    final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, key);
    return cipher.doFinal(plainText.getBytes());
}
```

## Resources

- [Android KeyStore system](https://developer.android.com/training/articles/keystore)

- [KeyStore documentation](https://developer.android.com/reference/java/security/KeyStore)

- [KeyChain documentation](https://developer.android.com/reference/android/security/KeyChain)

- [CWE-321: Use of Hard-coded Cryptographic Key](https://cwe.mitre.org/data/definitions/321.html)