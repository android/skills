> [!IMPORTANT]
> **Important:** **ORCHESTRATION GATEWAY PREREQUISITE** : This document is a supplemental reference guide for [`../SKILL.md`](https://developer.android.com/agents/skills/migration/ios-to-android/skill). You **MUST** read and follow the primary orchestration protocol in `../SKILL.md` before applying any patterns from this guide. `SKILL.md` enforces mandatory incremental compilation gates (`./gradlew compileDebugKotlin`), vertical feature slicing, and strict anti-premature-exit execution discipline.

This skill guides the migration of local key-value storage from iOS
(`UserDefaults` and `@AppStorage`) to Android (`Preferences DataStore` and
`SharedPreferences`). You **Must** use this skill when porting apps that rely on
persistent user settings, flags, or small amounts of data storage.

**Official Reference Links:**

- [DataStore overview \| Android Developers](https://developer.android.com/topic/libraries/architecture/datastore)
- [SharedPreferences \| Android Developers](https://developer.android.com/reference/android/content/SharedPreferences)
- [Data and files security best practices \| Android Developers](https://developer.android.com/privacy-and-security/security-tips)

## Prerequisites

Before initiating the preferences migration, you **Must** ensure the following
prerequisites are met:

- **Source Context:** Access to the iOS codebase using `UserDefaults` or `@AppStorage` is **Mandatory**.
- **Software Requirements:** Android Gradle Plugin (AGP) 8.0+, Kotlin 1.8+, Kotlin Coroutines (`kotlinx-coroutines-android:1.7+`), and Jetpack DataStore (`androidx.datastore:datastore-preferences:1.1+`) are **Mandatory**.
- **Jetpack Compose:** Understanding of state management in Compose (e.g., `collectAsState`) is **Mandatory** for mapping `@AppStorage`.

## Limitations

This skill has the following limitations:

- **Complex Objects:** Storing complex custom objects in `UserDefaults` via `Codable` should be migrated to **Proto DataStore** or **Room**. This skill provides a foundational implementation for Proto DataStore; advanced topics like schema evolution or custom data migrations are outside its scope.
- **Secure Storage:** This skill focuses on non-secure preferences. For sensitive data like passwords or tokens, refer to the Keychain to encrypted DataStore (Tink) mapping section. Evaluate the need for a dedicated biometrics skill if the flow requires user authentication to access the secure data.

## Glossary

| iOS Concept | Android Equivalent | Definition |
|---|---|---|
| **UserDefaults** | **Preferences DataStore / SharedPreferences** | The standard interface for storing small key-value pairs persistently. |
| **@AppStorage** | **DataStore Flow + collectAsState** | SwiftUI property wrapper that reflects `UserDefaults` values and invalidates view. |
| **Codable (to UserDefaults)** | **Proto DataStore / Room** | Mechanism for serializing objects for storage. |
| **Keychain** | **DataStore + Tink (Android Keystore-backed key)** | Secure storage for sensitive data. |

## Data Types Mapping

You **Must** map iOS data types to the correct Android DataStore preference
keys.

| iOS Type | Android DataStore Key Type | Example |
|---|---|---|
| `Bool` | `booleanPreferencesKey` | `booleanPreferencesKey("is_logged_in")` |
| `String` | `stringPreferencesKey` | `stringPreferencesKey("username")` |
| `Int` | `intPreferencesKey` | `intPreferencesKey("visit_count")` |
| `Float` | `floatPreferencesKey` | `floatPreferencesKey("score")` |
| `Double` | `doublePreferencesKey` | `doublePreferencesKey("latitude")` |
| `Array<String>` | `stringSetPreferencesKey` | `stringSetPreferencesKey("tags")` |

## Setup \& Dependencies

You **Must** add the DataStore dependency (and Tink, if the app stores secrets)
to your `app/build.gradle.kts` file using Version Catalogs.

**Example: libs.versions.toml**
\`\`\`toml
\[versions\]
datastore = "1.0.0"
tink = "1.24.0"

\[libraries\]
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
tink-android = { group = "com.google.crypto.tink", name = "tink-android", version.ref = "tink" }
\`\`\`

**Example: app/build.gradle.kts**
`kotlin
dependencies {
implementation(libs.datastore.preferences)
implementation(libs.tink.android)
}`

## Instructions: UserDefaults to DataStore

DataStore is the modern, recommended replacement for SharedPreferences. It
handles data updates transactionally and exposes data as a `Flow`.

### Step 1: Creating the DataStore Instance

You **Must** create the DataStore instance at the top level of your file or use
dependency injection.

- **iOS:** `UserDefaults.standard`
- **Android:** `val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")`

**Example: Initialization**

- **Input:** `Context` (via extension property).
- **Output:** `DataStore<Preferences>` instance.
- **Side Effect:** Creates a file-backed data store named "user_preferences".

```kotlin
// At top level or in a singleton
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")
```

### Step 2: Defining Keys

Android DataStore requires typed keys.

- **iOS:** Uses raw strings as keys.
- **Android:** You **Must** use `booleanPreferencesKey`, `stringPreferencesKey`, etc.

**Example: Defining Keys**

- **Input:** String key names.
- **Output:** Typed `Preferences.Key` objects.
- **Side Effect:** None (pure definition).

```kotlin
object PreferencesKeys {
   val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
   val USERNAME = stringPreferencesKey("username")
}
```

### Step 3: Reading Data

DataStore exposes data via a `Flow`. You **Must** handle potential exceptions.

- **iOS:** `UserDefaults.standard.bool(forKey: "isLoggedIn")`
- **Android:** `dataStore.data.map { it[IS_LOGGED_IN] ?: false }`

**Example: Reading Data**

- **Input:** None (reads from `dataStore`).
- **Output:** `Flow<Boolean>` emitting current state.
- **Side Effect:** Reads from disk asynchronously; handles IO exceptions.

```kotlin
val isLoggedInFlow: Flow<Boolean> = context.dataStore.data
   .catch { exception ->
       if (exception is IOException) {
           emit(emptyPreferences())
       } else {
           throw exception
       }
   }
   .map { preferences ->
       preferences[PreferencesKeys.IS_LOGGED_IN] ?: false
   }
```

### Step 4: Writing Data

Writing to DataStore is asynchronous and requires a suspend function.

- **iOS:** `UserDefaults.standard.set(true, forKey: "isLoggedIn")`
- **Android:** `dataStore.edit { prefs -> prefs[IS_LOGGED_IN] = true }`

**Example: Writing Data**

- **Input:** `Context`, `loggedIn` (Boolean).
- **Output:** `Unit` (via suspend function).
- **Side Effect:** Performs a transactional write to disk.

```kotlin
suspend fun setLoggedIn(context: Context, loggedIn: Boolean) {
   context.dataStore.edit { preferences ->
       preferences[PreferencesKeys.IS_LOGGED_IN] = loggedIn
   }
}
```

## Instructions: SwiftUI @AppStorage Mapping

SwiftUI's `@AppStorage` automatically updates the view when the underlying
`UserDefaults` value changes. In Android Compose, you **Must** collect the
DataStore `Flow` as state.

- **iOS:** `@AppStorage("username") var username = "Guest"`
- **Android:** `val username by dataStore.data.map { it[USERNAME] ?: "Guest" }.collectAsState(initial = "Guest")`

**Example: Compose Usage**

- **Input:** `UserViewModel` providing `usernameFlow`.
- **Output:** Composable UI emitting `Text`.
- **Side Effect:** Recomposes when the DataStore flow emits new values.

```kotlin
@Composable
fun UserProfileScreen(viewModel: UserViewModel = viewModel()) {
   val username by viewModel.usernameFlow.collectAsState(initial = "Guest")

   // <string name="welcome_user">Welcome, %1$s</string>
   Text(text = stringResource(R.string.welcome_user, username))
}
```

## Instructions: Complex Objects (Proto DataStore)

If the iOS app stores complex objects in `UserDefaults` (e.g., via `Codable`),
you **Must** use **Proto DataStore** or **Room** on Android. Proto DataStore
allows you to store instances of a custom data type with a schema defined using
Protocol Buffers.

**Example: Proto DataStore Implementation**

1. **Define the Schema** (`app/src/main/proto/user_prefs.proto`):
2. **Input:** Proto schema definition.
3. **Output:** Generated Java/Kotlin classes. \`\`\`proto syntax = "proto3";

option java_package = "com.example.application";
option java_multiple_files = true;

message UserPreferences {
string username = 1;
bool is_logged_in = 2;
}
\`\`\`

1. **Create the Serializer**:
2. **Input:** `InputStream`.
3. **Output:** `UserPreferences` object. \`\`\`kotlin object UserPreferencesSerializer : Serializer { override val defaultValue: UserPreferences = UserPreferences.getDefaultInstance()

override suspend fun readFrom(input: InputStream): UserPreferences {
try {
return UserPreferences.parseFrom(input)
} catch (exception: InvalidProtocolBufferException) {
throw CorruptionException("Cannot read proto.", exception)
}
}

override suspend fun writeTo(t: UserPreferences, output: OutputStream) = t.writeTo(output)
}
\`\`\`

1. **Create DataStore Instance**:
2. **Input:** File name and serializer.
3. **Output:** `DataStore<UserPreferences>` instance. `kotlin
   val Context.userPreferencesStore: DataStore<UserPreferences> by dataStore(
   fileName = "user_prefs.pb",
   serializer = UserPreferencesSerializer
   )`

- **When to use:** When you have non-trivial data structures that don't require a full relational database like Room.

## Instructions: Migrating SharedPreferences to DataStore

If you are migrating a legacy Android app that was previously ported from iOS
using `SharedPreferences`, you **Must** use `SharedPreferencesMigration` to move
data to DataStore without data loss.

**Example: SharedPreferences Migration**

- **Input:** Legacy SharedPreferences name ("legacy_prefs").
- **Output:** `DataStore<Preferences>` instance with migration logic.
- **Side Effect:** Migrates data from SharedPreferences to DataStore on first run.

```kotlin
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
   name = "user_preferences",
   produceMigrations = { context ->
       listOf(SharedPreferencesMigration(context, "legacy_prefs"))
   }
)
```

## Production Insight: DataStore vs SharedPreferences

While DataStore is the modern recommendation, many legacy production
applications actively being developed still heavily rely on `SharedPreferences`.

- **iOS Real-World Implementation:** Often uses extended native wrappers over `UserDefaults` configured via a centralized `Settings.swift`.
- **Android Real-World Equivalent:** The exact same centralized configuration is observed via an interface `Settings.kt` backed by a `SettingsImpl` class injecting `SharedPreferences`.

**Logic:**

- **IF** the iOS application heavily wraps `UserDefaults` synchronously, **THEN** `SharedPreferences` remains the most direct 1:1 functional map, as DataStore requires migrating synchronous calls to reactive Coroutine `Flows` which can introduce significant architectural overhead during a 1:1 port.
- **ELSE** , you **Must** use DataStore for all new development.

## Instructions: Secure Storage (Keychain to encrypted DataStore)

For sensitive data like tokens, iOS uses Keychain. On Android, you **Must**
encrypt the value with [Tink](https://developers.google.com/tink) using a key protected by the Android Keystore,
and store the ciphertext in DataStore.

> [!NOTE]
> **Note:** Don't use `EncryptedSharedPreferences` or `MasterKey` from `androidx.security:security-crypto`. That library is deprecated.

**Logic:**

- **IF** the iOS Keychain is used for simple key-value data (e.g., API tokens), **THEN** you **Must** migrate it to DataStore, with each value encrypted by a Tink `Aead` whose keyset is protected by an Android Keystore master key.
- **IF** the Keychain holds sign-in credentials (passwords, passkeys), **THEN** you **Must** prefer Credential Manager over storing them yourself.
- **IF** Keychain access is gated by biometrics or requires complex access control, **THEN** you **Must** evaluate the need for a dedicated security skill handling BiometricPrompt.

**Example: Tink-encrypted DataStore value**

- **Input:** Context, a DataStore, the secret to store.
- **Output:** An `Aead` that encrypts and decrypts values; ciphertext stored in DataStore.
- **Side Effect:** Creates an Android Keystore master key and an encrypted keyset in app-private storage on first use.

```kotlin
AeadConfig.register()
val aead: Aead = AndroidKeysetManager.Builder()
   .withSharedPref(context, "secure_keyset", "secure_keyset_prefs")
   .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
   .withMasterKeyUri("android-keystore://secure_master_key")
   .build()
   .keysetHandle
   .getPrimitive(RegistryConfiguration.get(), Aead::class.java)

val TOKEN = stringPreferencesKey("auth_token")
val associatedData = "auth_token".encodeToByteArray()

suspend fun saveToken(token: String) {
   val cipher = aead.encrypt(token.encodeToByteArray(), associatedData)
   context.dataStore.edit {
      it[TOKEN] = Base64.encodeToString(cipher, Base64.NO_WRAP)
   }
}

val token: Flow<String?> = context.dataStore.data.map { prefs ->
   prefs[TOKEN]?.let {
      aead.decrypt(Base64.decode(it, Base64.NO_WRAP), associatedData)
         .decodeToString()
   }
}
```

## Tooling Integration: Standard Actions

To enable automated assessment and migration tracking, this skill defines the
following standard actions. Any automation tool built on this skill **Must**
implement these actions with the specified parameters to ensure benchmark
compliance.

### Verification Commands \& Agnostic Agent Instructions

To audit iOS preferences and verify DataStore migration:

1. **UserDefaults Scanner (CLI):** `bash
   grep -rnE "UserDefaults\.standard|@AppStorage" .`
2. **DataStore Migration Verification:** *Generate a Markdown verification checklist mapping each discovered iOS
   `UserDefaults` key to its Kotlin `DataStore<Preferences>` / `PreferencesKey`
   counterpart.*

## Antipatterns

### DO

- **DO** use DataStore for all new non-secure preferences.
- **DO** encrypt sensitive data like auth tokens with Tink (Android Keystore-backed key) before storing it in DataStore.
- **DO** collect DataStore flows as state in Compose.

### DON'T

- **DON'T** use the deprecated `EncryptedSharedPreferences` / `MasterKey` (`androidx.security:security-crypto`).
- **DON'T** use standard `SharedPreferences` for new development unless migrating a legacy app with heavy synchronous dependencies.
- **DON'T** store large amounts of data in preferences; use Room for complex data.

### NEVER

- **NEVER** store raw passwords or sensitive tokens in plain `UserDefaults` or `SharedPreferences`.
- **NEVER** perform blocking `first()` or `runBlocking` calls on DataStore flows on the main thread.