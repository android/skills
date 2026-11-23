---
name: android-jni-security
description: Audits, detects, and remediates Android JNI and native C/C++ boundary
  security vulnerabilities. Use this skill when reviewing, auditing, hardening, or
  securing Android JNI boundaries, Kotlin external or Java native methods, and C/C++
  shared libraries (System.loadLibrary) against buffer overflow, memory pinning, Time-of-Check
  to Time-of-Use (TOCTOU) file path races, hardcoded C++ cryptographic keys, cross-thread
  JNI reference and session callback lifecycles, or unhandled pending JVM exceptions
  and local reference leaks. Don't use for pure managed Kotlin or Java code without
  native JNI bindings or general reverse-engineering of stripped binaries.
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-11-19'
  keywords:
  - recipe
  - Android
  - Security
  - JNI
  - NDK
  - Native
  - C++
  - System.loadLibrary
  - GetByteArrayRegion
  - GetByteArrayElements
  - ReleaseByteArrayElements
  - ParcelFileDescriptor
  - TOCTOU
  - AndroidKeyStore
  - memset_explicit
  - NewGlobalRef
  - DeleteGlobalRef
  - AttachCurrentThread
  - ExceptionCheck
---

This skill provides rules and code patterns to secure Android JNI boundaries.
Follow these patterns to prevent buffer overflows, pinned memory leaks, and
Time-of-Check to Time-of-Use (TOCTOU) file races. They also prevent hardcoded
native secrets, cross-thread reference crashes, and unhandled pending JVM
exceptions.

## Glossary

- **JNI (Java Native Interface):** The platform bridge connecting ART bytecode in Kotlin or Java with C/C++ `.so` libraries.
- **JNIEnv:** A thread-local pointer to the JNI function table passed as the first argument to every native method. A `JNIEnv*` pointer must NEVER be shared or cached across threads.
- **JavaVM:** The process-wide virtual machine pointer used to attach and detach native threads (`AttachCurrentThread`, `DetachCurrentThread`).
- **Local reference:** A thread-scoped reference (`jobject`, `jclass`, `jstring`, `jarray`) valid only during the active native call.
- **Global reference:** A cross-thread reference (`NewGlobalRef`) that keeps a Java object reachable across threads and calls until freed with `DeleteGlobalRef`.
- **TOCTOU (Time-of-Check to Time-of-Use):** A race condition when managed code checks a file path before C/C++ re-opens that path string. An attacker can swap the file or symlink before `fopen` or `open` runs.
- **Pending JVM exception state:** The state after a JNI call throws a managed exception. Native code must not call most JNI functions while an exception is pending. Check `ExceptionCheck()`, then free resources and return, or clear and handle the failure.

## Limitations

- This skill covers JNI boundary validation, reference and thread lifecycles, and native secret handling.
- This skill **doesn't** cover binary obfuscation or decompilation of stripped third-party `.so` libraries.
- For `.so` binary hardening, suggest NDK visibility flags (`-fvisibility=hidden`, `-Wl,--exclude-libs,ALL`), symbol stripping, and the **Play Integrity API**.

## Setup and dependencies

- **Android NDK and CMake:** Configure `externalNativeBuild` with `cmake` in `build.gradle` or `build.gradle.kts`, and include `<jni.h>` in C/C++. Ask for user confirmation before modifying Gradle build files or rebuilding the project interactively.
- **Android SDK:** Use `android.os.ParcelFileDescriptor` and `android.security.keystore.*` (API 23+ for AES-GCM in `AndroidKeyStore`).

*** ** * ** ***

## JNI boundary security logic and decisions

### 1. JNI parameter mapping and release requirements

When managing JNI reference lifetimes, UTF-8 strings, or thread attachment,
follow [JNI tips](references/android/ndk/guides/jni-tips.md). Evaluate every parameter crossing the JNI boundary against
its C/C++ release contract:

| JNI Type | C/C++ Acquisition API | Risk if Unchecked or Unreleased | Required Cleanup Action |
|---|---|---|---|
| `jbyteArray` (copy) | `GetByteArrayRegion()` | Stack or heap buffer overflow if length exceeds destination | Validate `GetArrayLength()` and check `ExceptionCheck()` (no release call needed) |
| `jbyteArray` (in-place) | `GetByteArrayElements()` | Heap pinning leak or out-of-memory (OOM) `nullptr` dereference | Validate `GetArrayLength()`, check for `nullptr`, and call `ReleaseByteArrayElements()` on all paths |
| `jstring` (input) | `GetStringUTFChars()` | Native UTF-8 buffer leak or OOM `nullptr` dereference | Check for `nullptr` and call `ReleaseStringUTFChars()` before `DeleteLocalRef()` |
| `jstring` (output) | `NewStringUTF()` | Runtime abort when raw native bytes are not valid Modified UTF-8 | Sanitize text to valid Modified UTF-8 first, or return `jbyteArray` for binary data |
| `jobject` / `jclass` (local) | `GetObjectClass()`, `FindClass()`, `GetObjectField()` | Local reference exhaustion in loops and on attached threads | Call `DeleteLocalRef()` when finished, especially in loops or attached threads |
| `jobject` / `jclass` (cached) | `NewGlobalRef()` | Dangling local pointer crash across calls or threads if not promoted | Promote with `NewGlobalRef()` and free with `DeleteGlobalRef()` on teardown |

### 2. Boundary input validation and buffer bounds logic

When passing untrusted inputs across JNI into C or C++ code, follow
[Use of native code](references/android/privacy-and-security/risks/use-of-native-code.md). Validate array sizes in both managed wrappers and
native C/C++ entry points before copying into fixed buffers:

IF (a Kotlin or Java class exposes a native method accepting arrays or
lengths) {
MUST declare the `external` (or `native`) method `private`.
MUST expose a public wrapper that validates non-empty inputs and enforces
maximum byte length bounds before calling JNI.
IN C/C++:
MUST verify `array != nullptr` and validate `length > 0`,
`length <= kMaxBufferSize`, and `length <= env->GetArrayLength(array)`.
IF (bounds validation fails) {
MUST throw `java/lang/IllegalArgumentException` using
`env->ThrowNew` and return immediately before copying memory.
}
IF (copying bytes into a native buffer) {
MUST copy with `env->GetByteArrayRegion(array, 0, length, buf)` and
check `env->ExceptionCheck()`.
}
IF (accessing or mutating elements in place) {
MUST check that `env->GetByteArrayElements(array, nullptr)` returns
a non-null pointer.
MUST call `env->ReleaseByteArrayElements(array, ptr, mode)` (`0` to
copy back changes, or `JNI_ABORT` for read-only access) on every
return path.
}
}

IF (native code accesses a direct `ByteBuffer` or constructs `jstring` output) {
MUST verify `env->GetDirectBufferAddress(buf) != nullptr` and validate byte
offsets against `env->GetDirectBufferCapacity(buf)`.
NEVER pass raw file or network bytes directly to `env->NewStringUTF()`.
MUST sanitize text bytes to valid Modified UTF-8 before `NewStringUTF()`, or
return a `jbyteArray` for arbitrary binary data.
}

### 3. TOCTOU-safe file access across JNI

Never pass raw file path strings across JNI after checking file properties in
Kotlin or Java:

IF (native C/C++ code reads or writes an application or shared file) {
NEVER pass a raw file path `String` (`jstring`) across JNI to `fopen()` or
`open()`.
MUST canonicalize app-internal paths inside `context.filesDir` and open a
`ParcelFileDescriptor` with the narrowest required mode (for example
`MODE_READ_ONLY`). For `content://` URIs, open the descriptor through
`context.contentResolver.openFileDescriptor(uri, "r")`.
MUST pass only the integer descriptor (`pfd.fd`) across JNI (`jint fd`)
inside `.use { ... }`, and perform I/O on `fd` in C/C++ (`pread` or `read`).
NEVER call `close(fd)`, pass `fd` directly to `fdopen()`, or retain `fd`
after `.use { ... }` returns because `ParcelFileDescriptor` owns it. Call
`dup(fd)` if native code needs an owned descriptor copy.
}

### 4. Cryptographic keys and native memory scrubbing

When storing or deriving cryptographic keys in Android apps, follow
[Hardcoded cryptographic secrets](references/android/privacy-and-security/risks/hardcoded-cryptographic-secrets.md). Never embed static cryptographic keys or
salts in C/C++ source files:

IF (an application encrypts, decrypts, or authenticates payloads across JNI) {
NEVER hardcode secret keys, passwords, or salts in C/C++ arrays or string
literals.
MUST generate and store master keys in `AndroidKeyStore`
(`KeyStore.getInstance("AndroidKeyStore")`) and run `Cipher` operations in
Kotlin or Java.
IF (native C/C++ cryptographic processing is required) {
MUST pass only a short-lived, unwrapped per-session data encryption key
for that single call rather than embedding a static key.
}
MUST overwrite transient sensitive native buffers before freeing memory
using a volatile byte loop (`volatile uint8_t*`), or `memset_explicit` when
`minSdk` is 34 or higher.
MUST copy sensitive JNI byte arrays with `GetByteArrayRegion` into an owned
buffer that you zero, or unpin `GetByteArrayElements` pointers with
`ReleaseByteArrayElements(..., JNI_ABORT)`.
}

### 5. Cross-thread reference lifecycles and pending JVM exceptions

Never share `JNIEnv*` or local references across threads. Check for pending JVM
exceptions after every JNI call:

IF (native code caches a callback object or invokes JNI from a background
thread) {
NEVER store a raw local `jobject`, `jclass`, or `JNIEnv*` in a static or
global C/C++ variable.
MUST cache `JavaVM*` (via `JNI_OnLoad` or `env->GetJavaVM`) and promote
cached objects with `env->NewGlobalRef(obj)`.
MUST guard shared global references with a mutex and acquire a
`NewLocalRef` under the lock before calling out, or stop worker threads
before calling `env->DeleteGlobalRef(globalRef)`.
ON A NATIVE BACKGROUND THREAD:
MUST check `gJavaVm->GetEnv(..., JNI_VERSION_1_6)` if the helper may run
on an already-attached thread; call `AttachCurrentThread` only on
`JNI_EDETACHED`, and call `DetachCurrentThread` only if this code
attached the thread.
MUST resolve app classes via `GetObjectClass` on a cached instance or a
global `jclass` cached in `JNI_OnLoad`, because `FindClass` on a
natively attached thread uses the system class loader.
MUST delete local references (`DeleteLocalRef`), log and clear pending
exceptions (`ExceptionDescribe()` then `ExceptionClear()`), and detach
if attached by this call.
}

IF (native code calls `FindClass`, `GetMethodID`, `GetFieldID`, `Call*Method`,
`GetObjectField`, or `GetStringUTFChars`) {
NEVER let C++ exceptions cross a `JNIEXPORT` boundary; catch them in C++ and
translate them with `env->ThrowNew()`.
MUST check `env->ExceptionCheck()` and null returns after EACH call before
invoking the next JNI function.
IF (`env->ExceptionCheck()` is true in a Kotlin or Java JNI entry point) {
MUST release acquired native buffers and local references, then return
immediately so the exception propagates; call `ExceptionClear()` only
when native code handles the failure itself.
}
MUST annotate JNI-referenced Kotlin or Java members with `@Keep` (or add
`-keepclassmembers` R8 rules). This prevents release shrinking from causing
`NoSuchMethodError` or `NoSuchFieldError`.
MUST call `env->DeleteLocalRef` on temporary local object, class, and
string references before returning.
}

*** ** * ** ***

## Code and configuration patterns

### Pattern 1: Kotlin wrapper validation and C++ bounds-checked region copy

Declare `external` methods `private` and validate sizes in a public Kotlin
wrapper. Re-verify `GetArrayLength` in C++ and copy read-only bytes with
`GetByteArrayRegion` so the managed array is never pinned. Keep C++ JNI symbol
names aligned with the Kotlin `external fun` names, or the first call throws
`UnsatisfiedLinkError`.

- **Expected Inputs:** A non-empty `ByteArray` within the maximum frame size.
- **Expected Outputs:** Validates bounds, copies into a native buffer with `GetByteArrayRegion`, and throws `IllegalArgumentException` on invalid sizes.


```kotlin
object SecureNativeDataProcessor {

    private const val MAX_PACKET_SIZE = 2048

    init {
        System.loadLibrary("secure_bridge")
    }

    // Keep raw external declarations private so callers cannot bypass validation
    @JvmStatic
    private external fun processPacketNative(packet: ByteArray, length: Int): Int

    @JvmStatic
    fun processPacketSafely(packet: ByteArray): Int {
        require(packet.isNotEmpty()) {
            "Packet must not be empty"
        }
        require(packet.size <= MAX_PACKET_SIZE) {
            "Packet size (${packet.size}) exceeds maximum ($MAX_PACKET_SIZE)"
        }
        return processPacketNative(packet, packet.size)
    }
}
```

<br />


```c++
extern "C"
JNIEXPORT jint JNICALL
Java_com_example_snippets_security_jni_SecureNativeDataProcessor_processPacketNative(
        JNIEnv* env,
        jclass /*clazz*/,
        jbyteArray packet,
        jint length) {
    constexpr jsize kMaxFrameSize = 2048;
    if (packet == nullptr) {
        jclass exClass = env->FindClass("java/lang/IllegalArgumentException");
        if (exClass != nullptr) {
            env->ThrowNew(exClass, "Packet array must not be null");
            env->DeleteLocalRef(exClass);
        }
        return -1;
    }

    jsize arrayLength = env->GetArrayLength(packet);
    if (length <= 0 || length > kMaxFrameSize || length > arrayLength) {
        jclass exClass = env->FindClass("java/lang/IllegalArgumentException");
        if (exClass != nullptr) {
            env->ThrowNew(exClass, "Packet length out of valid bounds");
            env->DeleteLocalRef(exClass);
        }
        return -1;
    }

    // Copy into a bounds-checked native buffer without pinning the managed array
    uint8_t frameBuffer[kMaxFrameSize];
    env->GetByteArrayRegion(
        packet, 0, length, reinterpret_cast<jbyte*>(frameBuffer));
    if (env->ExceptionCheck()) {
        return -1;
    }

    uint32_t checksum = 0;
    for (jint i = 0; i < length; ++i) {
        checksum = (checksum * 31u) + static_cast<uint32_t>(frameBuffer[i]);
    }
    return static_cast<jint>(checksum);
}
```

<br />

### Pattern 2: Descriptor-bound file reading with ParcelFileDescriptor

Open files in Kotlin with `ParcelFileDescriptor.open` and pass only the integer
descriptor (`pfd.fd`) to C++. Borrow `fd` with `pread` without closing it so
`ParcelFileDescriptor` retains ownership.

- **Expected Inputs:** `context` (`Context`) and `fileName` (`String`).
- **Expected Outputs:** Reads the header from `fd` in C++ and sanitizes bytes to valid Modified UTF-8 before `NewStringUTF`.


```kotlin
class SecureNativeFileLoader(private val context: Context) {

    companion object {
        init {
            System.loadLibrary("secure_bridge")
        }
    }

    fun readVerifiedHeader(fileName: String): String {
        val baseDir = context.filesDir.canonicalFile
        val targetFile = File(baseDir, fileName).canonicalFile
        require(targetFile.path.startsWith("${baseDir.path}${File.separator}")) {
            "Invalid file path outside app files directory"
        }

        // Open the file in managed code and pass only the integer FD to C++
        // to remove the managed-to-native path re-open race window.
        return ParcelFileDescriptor.open(
            targetFile,
            ParcelFileDescriptor.MODE_READ_ONLY,
        ).use { pfd ->
            readHeaderFromFdNative(pfd.fd)
        }
    }

    private external fun readHeaderFromFdNative(fd: Int): String
}
```

<br />


```c++
extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_snippets_security_jni_SecureNativeFileLoader_readHeaderFromFdNative(
        JNIEnv* env,
        jobject /*thiz*/,
        jint fd) {
    if (fd < 0) {
        jclass exClass = env->FindClass("java/lang/IllegalArgumentException");
        if (exClass != nullptr) {
            env->ThrowNew(exClass, "Invalid file descriptor");
            env->DeleteLocalRef(exClass);
        }
        return nullptr;
    }

    char header[256];
    std::memset(header, 0, sizeof(header));

    // Borrow fd with pread; never close(fd) because ParcelFileDescriptor owns it
    ssize_t bytesRead = pread(fd, header, sizeof(header) - 1, 0);
    if (bytesRead < 0) {
        jclass ioExClass = env->FindClass("java/io/IOException");
        if (ioExClass != nullptr) {
            env->ThrowNew(ioExClass, "Failed to read header from descriptor");
            env->DeleteLocalRef(ioExClass);
        }
        return nullptr;
    }

    // Sanitize to printable 7-bit ASCII so NewStringUTF never receives invalid Modified UTF-8
    for (ssize_t i = 0; i < bytesRead; ++i) {
        unsigned char c = static_cast<unsigned char>(header[i]);
        if (c == 0 || c > 0x7f) {
            header[i] = '?';
        }
    }
    header[bytesRead] = '\0';
    return env->NewStringUTF(header);
}
```

<br />

### Pattern 3: AndroidKeyStore encryption and native buffer zeroing

Manage master keys in `AndroidKeyStore` instead of hardcoding secrets in C++.
Copy transient sensitive bytes into an owned C++ buffer with
`GetByteArrayRegion` and wipe that buffer through a `volatile uint8_t*` loop
(or `memset_explicit` on API 34+) before returning.

- **Expected Inputs:** Plaintext `payload` (`ByteArray`).
- **Expected Outputs:** Frames plaintext in C++, zeroes the sensitive native and managed buffers, and encrypts with an `AndroidKeyStore` AES-GCM key.


```kotlin
object SecureNativeCryptoBridge {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "app_master_aes_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    init {
        System.loadLibrary("secure_bridge")
    }

    @JvmStatic
    private external fun framePlaintextNative(plaintext: ByteArray): ByteArray

    @JvmStatic
    fun encryptAndProcess(payload: ByteArray): ByteArray {
        val framedPlaintext = framePlaintextNative(payload)
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            val iv = cipher.iv
            val encrypted = cipher.doFinal(framedPlaintext)

            // Prepend IV length (1 byte) + IV + authenticated ciphertext
            val output = ByteArray(1 + iv.size + encrypted.size)
            output[0] = iv.size.toByte()
            System.arraycopy(iv, 0, output, 1, iv.size)
            System.arraycopy(encrypted, 0, output, 1 + iv.size, encrypted.size)
            return output
        } finally {
            framedPlaintext.fill(0)
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) {
            return existing.secretKey
        }
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE,
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}
```

<br />


```c++
namespace {
void secureZeroMemory(void* ptr, size_t len) {
    volatile uint8_t* p = reinterpret_cast<volatile uint8_t*>(ptr);
    while (len-- > 0) {
        *p++ = 0;
    }
}
}  // namespace

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_example_snippets_security_jni_SecureNativeCryptoBridge_framePlaintextNative(
        JNIEnv* env,
        jclass /*clazz*/,
        jbyteArray plaintext) {
    constexpr jsize kMaxPayloadSize = 65536;
    if (plaintext == nullptr) {
        jclass exClass = env->FindClass("java/lang/IllegalArgumentException");
        if (exClass != nullptr) {
            env->ThrowNew(exClass, "Plaintext array must not be null");
            env->DeleteLocalRef(exClass);
        }
        return nullptr;
    }

    jsize length = env->GetArrayLength(plaintext);
    if (length <= 0 || length > kMaxPayloadSize) {
        jclass exClass = env->FindClass("java/lang/IllegalArgumentException");
        if (exClass != nullptr) {
            env->ThrowNew(exClass, "Plaintext length out of valid bounds");
            env->DeleteLocalRef(exClass);
        }
        return nullptr;
    }

    // Copy sensitive bytes into an owned buffer via GetByteArrayRegion without pinning
    std::vector<uint8_t> sensitiveBuffer(static_cast<size_t>(length) + 1);
    sensitiveBuffer[0] = 0x01;  // Protocol frame version
    env->GetByteArrayRegion(
        plaintext, 0, length, reinterpret_cast<jbyte*>(sensitiveBuffer.data() + 1));
    if (env->ExceptionCheck()) {
        secureZeroMemory(sensitiveBuffer.data(), sensitiveBuffer.size());
        return nullptr;
    }

    jbyteArray framed = env->NewByteArray(length + 1);
    if (framed != nullptr && !env->ExceptionCheck()) {
        env->SetByteArrayRegion(
            framed, 0, length + 1, reinterpret_cast<const jbyte*>(sensitiveBuffer.data()));
    }

    // Wipe transient sensitive native memory before returning to managed code
    secureZeroMemory(sensitiveBuffer.data(), sensitiveBuffer.size());
    return framed;
}
```

<br />

### Pattern 4: Thread-safe callbacks with NewGlobalRef and AttachCurrentThread

Promote cross-thread callbacks with `NewGlobalRef`, guard them with a mutex, and
free them with `DeleteGlobalRef`. Attach native worker threads to `JavaVM*`
using `AttachCurrentThread` and `DetachCurrentThread`.

- **Expected Inputs:** A callback listener instance registered from Kotlin.
- **Expected Outputs:** Safely invokes the callback from a native C++ worker thread using a thread-local `JNIEnv*`, logs and clears pending exceptions, and frees global and local references before detaching.


```c++
namespace {
JavaVM* gJavaVm = nullptr;
std::mutex gListenerMutex;
jobject gListenerGlobalRef = nullptr;
}  // namespace

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* /*reserved*/) {
    gJavaVm = vm;
    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_snippets_security_jni_SecureSessionMonitor_registerListener(
        JNIEnv* env,
        jobject /*thiz*/,
        jobject listener) {
    if (gJavaVm == nullptr) {
        env->GetJavaVM(&gJavaVm);
    }
    std::lock_guard<std::mutex> lock(gListenerMutex);
    if (gListenerGlobalRef != nullptr) {
        env->DeleteGlobalRef(gListenerGlobalRef);
        gListenerGlobalRef = nullptr;
    }
    if (listener != nullptr) {
        // Promote local jobject to a Global Reference so it stays reachable across threads
        gListenerGlobalRef = env->NewGlobalRef(listener);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_snippets_security_jni_SecureSessionMonitor_dispatchBackgroundCheck(
        JNIEnv* /*env*/,
        jobject /*thiz*/) {
    std::thread worker([]() {
        if (gJavaVm == nullptr) {
            return;
        }
        JNIEnv* threadEnv = nullptr;
        if (gJavaVm->AttachCurrentThread(&threadEnv, nullptr) != JNI_OK ||
            threadEnv == nullptr) {
            return;
        }

        jobject localListener = nullptr;
        {
            std::lock_guard<std::mutex> lock(gListenerMutex);
            if (gListenerGlobalRef != nullptr) {
                localListener = threadEnv->NewLocalRef(gListenerGlobalRef);
            }
        }

        if (localListener != nullptr) {
            jclass listenerClass = threadEnv->GetObjectClass(localListener);
            if (listenerClass != nullptr && !threadEnv->ExceptionCheck()) {
                jmethodID methodId = threadEnv->GetMethodID(
                    listenerClass, "onStatusUpdate", "(I)V");
                if (methodId != nullptr && !threadEnv->ExceptionCheck()) {
                    threadEnv->CallVoidMethod(localListener, methodId, 1);
                }
                threadEnv->DeleteLocalRef(listenerClass);
            }
            threadEnv->DeleteLocalRef(localListener);
        }

        // Log and clear any pending JVM exception before detaching the native thread
        if (threadEnv->ExceptionCheck()) {
            threadEnv->ExceptionDescribe();
            threadEnv->ExceptionClear();
        }
        gJavaVm->DetachCurrentThread();
    });
    worker.detach();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_snippets_security_jni_SecureSessionMonitor_unregisterListener(
        JNIEnv* env,
        jobject /*thiz*/) {
    std::lock_guard<std::mutex> lock(gListenerMutex);
    if (gListenerGlobalRef != nullptr) {
        env->DeleteGlobalRef(gListenerGlobalRef);
        gListenerGlobalRef = nullptr;
    }
}
```

<br />

*** ** * ** ***

## Error handling

Check `env->ExceptionCheck()` and null returns after every JNI lookup, method
call, field read, or string acquisition. Free local references with
`DeleteLocalRef` on every return path.

- **Expected Inputs:** An options `jobject` passed across JNI.
- **Expected Outputs:** Frees acquired strings and local references on failure, returning early so pending JVM exceptions propagate to the caller.


```c++
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_snippets_security_jni_SecureConfigParser_validateOptions(
        JNIEnv* env,
        jclass /*clazz*/,
        jobject options) {
    if (options == nullptr) {
        return JNI_FALSE;
    }

    jclass optionsClass = env->GetObjectClass(options);
    if (optionsClass == nullptr || env->ExceptionCheck()) {
        return JNI_FALSE;
    }

    jmethodID buildHeaderMethod = env->GetMethodID(
        optionsClass, "buildAuthHeader", "()Ljava/lang/String;");
    if (buildHeaderMethod == nullptr || env->ExceptionCheck()) {
        env->DeleteLocalRef(optionsClass);
        return JNI_FALSE;
    }

    // Check ExceptionCheck() immediately after CallObjectMethod before any other JNI call
    jstring headerStr = static_cast<jstring>(
        env->CallObjectMethod(options, buildHeaderMethod));
    if (env->ExceptionCheck() || headerStr == nullptr) {
        env->DeleteLocalRef(optionsClass);
        return JNI_FALSE;
    }

    const char* headerChars = env->GetStringUTFChars(headerStr, nullptr);
    if (headerChars == nullptr || env->ExceptionCheck()) {
        env->DeleteLocalRef(headerStr);
        env->DeleteLocalRef(optionsClass);
        return JNI_FALSE;
    }
    bool validHeader = headerChars[0] != '\0';
    env->ReleaseStringUTFChars(headerStr, headerChars);
    env->DeleteLocalRef(headerStr);

    jfieldID hostField = env->GetFieldID(
        optionsClass, "host", "Ljava/lang/String;");
    if (hostField == nullptr || env->ExceptionCheck()) {
        env->DeleteLocalRef(optionsClass);
        return JNI_FALSE;
    }

    jfieldID accountField = env->GetFieldID(
        optionsClass, "accountId", "Ljava/lang/String;");
    if (accountField == nullptr || env->ExceptionCheck()) {
        env->DeleteLocalRef(optionsClass);
        return JNI_FALSE;
    }

    jstring hostStr = static_cast<jstring>(
        env->GetObjectField(options, hostField));
    if (env->ExceptionCheck() || hostStr == nullptr) {
        env->DeleteLocalRef(optionsClass);
        return JNI_FALSE;
    }

    jstring accountStr = static_cast<jstring>(
        env->GetObjectField(options, accountField));
    if (env->ExceptionCheck() || accountStr == nullptr) {
        env->DeleteLocalRef(hostStr);
        env->DeleteLocalRef(optionsClass);
        return JNI_FALSE;
    }

    const char* hostChars = env->GetStringUTFChars(hostStr, nullptr);
    if (hostChars == nullptr || env->ExceptionCheck()) {
        env->DeleteLocalRef(accountStr);
        env->DeleteLocalRef(hostStr);
        env->DeleteLocalRef(optionsClass);
        return JNI_FALSE;
    }

    const char* accountChars = env->GetStringUTFChars(accountStr, nullptr);
    if (accountChars == nullptr || env->ExceptionCheck()) {
        env->ReleaseStringUTFChars(hostStr, hostChars);
        env->DeleteLocalRef(accountStr);
        env->DeleteLocalRef(hostStr);
        env->DeleteLocalRef(optionsClass);
        return JNI_FALSE;
    }

    bool nonEmpty = validHeader && hostChars[0] != '\0' && accountChars[0] != '\0';

    env->ReleaseStringUTFChars(hostStr, hostChars);
    env->ReleaseStringUTFChars(accountStr, accountChars);
    env->DeleteLocalRef(accountStr);
    env->DeleteLocalRef(hostStr);
    env->DeleteLocalRef(optionsClass);
    return nonEmpty ? JNI_TRUE : JNI_FALSE;
}
```

<br />

*** ** * ** ***

## Quality checklist

### Antipatterns

- **NEVER** hardcode cryptographic keys, secrets, or salts in C/C++ source code or `.so` binaries.
- **NEVER** pass unvalidated lengths to `memcpy`, `strcpy`, or `sprintf` without checking `env->GetArrayLength(array)` or `GetDirectBufferCapacity(buf)`.
- **NEVER** pass raw file path strings (`jstring`) across JNI to `fopen` or `open` after managed checks, or `close()` a borrowed `ParcelFileDescriptor`.
- **NEVER** pass unsanitized file or network bytes to `NewStringUTF()`, or let C++ exceptions escape across a `JNIEXPORT` boundary.
- **NEVER** cache a raw local `jobject`, `jclass`, or thread-local `JNIEnv*` in static or global C/C++ variables.
- **NEVER** invoke another JNI function while a JVM exception may be pending; check `env->ExceptionCheck()` first.

### Best practices

- **MUST** make raw `external` or `native` buffer methods `private` and validate bounds in a public wrapper. Verify `GetArrayLength` in C++ and throw `IllegalArgumentException` via `env->ThrowNew` on invalid lengths.
- **MUST** copy read-only byte arrays with `GetByteArrayRegion`, or check `GetByteArrayElements` and `GetStringUTFChars` for `nullptr` and release them on all paths.
- **MUST** open local files with `ParcelFileDescriptor.open` (or `content://` URIs with `ContentResolver.openFileDescriptor`) and pass `pfd.fd` inside `.use { }` without closing `fd` in C++.
- **MUST** store keys in `AndroidKeyStore` and zero transient native buffers (`volatile uint8_t*` or API 34+ `memset_explicit`) before freeing memory.
- **MUST** promote cross-thread references with `NewGlobalRef` under a lock, free them with `DeleteGlobalRef`, and attach threads safely via `JavaVM*`.
- **MUST** check `env->ExceptionCheck()` after each JNI call, release local references and return on pending exceptions, and mark JNI targets `@Keep`.
