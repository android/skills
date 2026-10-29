Follow this systematic procedure to migrate Android applications that use the
AppFunctions API in version 1.0.0-alpha09 and lower to the compile-time
`@AppFunctionServiceEntryPoint` architecture introduced in version
`1.0.0-alpha10`. Then follow the Upgrade to 1.0.0-beta01 section. Apps that
already use `@AppFunctionServiceEntryPoint` only need that section.

*** ** * ** ***

## Architectural shift overview

In lower versions of the AppFunctions API, for example version `1.0.0-alpha09`:

- Applications require separate dependencies for core functionality, specifically `androidx.appfunctions:appfunctions`, and service components, specifically `androidx.appfunctions:appfunctions-service`.
- The application implements `AppFunctionConfiguration.Provider` on its `Application` class.
- You manually register enclosing class instantiation using `AppFunctionConfiguration.Builder().addEnclosingClassFactory(...)`.
- You place metadata property tags directly under `<application>`.

In version `1.0.0-alpha10` featuring `@AppFunctionServiceEntryPoint`:

- The core and service dependencies are consolidated into a single runtime artifact, `androidx.appfunctions:appfunctions`, which eliminates the need for the standalone `appfunctions-service` library.
- A dedicated wrapper class extending `AppFunctionService` is annotated with `@AppFunctionServiceEntryPoint` and Hilt's `@AndroidEntryPoint` or an alternative dependency injection framework.
- The KSP compiler generates a concrete service subclass and an XML metadata schema file in `assets/`.
- The OS discovers and routes executions using a consolidated `<service>` and `app_metadata` declaration in `AndroidManifest.xml`.

### Required changes from 1.0.0-alpha09 to the entry point API

When focusing solely on the mandatory API changes required by the new
`@AppFunctionServiceEntryPoint` architecture, the migration consists of four
strict requirements that you must complete:

1. **Build dependency consolidation** : Remove the merged `appfunctions-service` dependency while retaining core `appfunctions` and the KSP compiler.
2. **Service wrapper creation** : Replace the legacy `AppFunctionConfiguration.Provider` on the `Application` class with an abstract class extending `AppFunctionService`, annotated with `@AppFunctionServiceEntryPoint`.
3. **Annotation and context decoupling** : Move AppFunction annotations to the new wrapper methods as `@AppFunctionDeclaration`. Drop `AppFunctionContext` parameters, because `AppFunctionService` inherits directly from `Context`.
4. **Manifest registration** : Register the KSP-generated concrete service in `AndroidManifest.xml` with `BIND_APP_FUNCTION_SERVICE`, the `AppFunctionService` intent filter, and metadata property tags.

*** ** * ** ***

## Systematic migration steps

### Consolidate AppFunctions build dependencies

Remove the standalone `appfunctions-service` library from your module build
files like `build.gradle.kts` and version catalog like `libs.versions.toml`. In
version `1.0.0-alpha10`, all core service capabilities are consolidated directly
within the main `appfunctions` artifact.

    // build.gradle.kts
    dependencies {
        implementation(libs.androidx.appfunctions)
    -   implementation(libs.androidx.appfunctions.service)
        ksp(libs.androidx.appfunctions.compiler)
    }

    # libs.versions.toml
    [versions]
    appfunctions = "1.0.0-beta01"

    [libraries]
    androidx-appfunctions = { module = "androidx.appfunctions:appfunctions", version.ref = "appfunctions" }
    -   androidx-appfunctions-service = { module = "androidx.appfunctions:appfunctions-service", version.ref = "appfunctions" }
    androidx-appfunctions-compiler = { module = "androidx.appfunctions:appfunctions-compiler", version.ref = "appfunctions" }

> [!NOTE]
> **Note:** If another dependency in your project uses snapshot builds like `1.0.0-SNAPSHOT` or custom snapshot repositories from `https://androidx.dev/snapshots/...`, preserve your custom snapshot repository configuration in `settings.gradle.kts`. Otherwise, standard Google Maven repositories resolve `1.0.0-beta01` directly.

*** ** * ** ***

### Create a dedicated wrapper service extending `AppFunctionService`

Instead of annotating standalone business logic classes or implementing manual
configuration providers, create an abstract service wrapper across your project,
for example `BaseAppFunctionService`, extending `AppFunctionService` and
annotated with `@AppFunctionServiceEntryPoint`.

#### Recommended approach using Hilt

Annotate your service with `@AndroidEntryPoint` and inject your data
repositories or use cases using standard `@Inject internal lateinit var`:


```kotlin
@RequiresApi(36)
@AndroidEntryPoint
@AppFunctionServiceEntryPoint(
    serviceName = "MyAppFunctionService",
    appFunctionXmlFileName = "my_app_function_service", // Do NOT include .xml extension
)
abstract class BaseAppFunctionService : AppFunctionService() {
    @Inject internal lateinit var messageRepository: MessageRepository

    @AppFunction(isDescribedByKDoc = true)
    internal suspend fun send(
        name: String,
        endpointValue: String,
        messageBody: String,
    ): MessageResult {
        return messageRepository.send(name, endpointValue, messageBody)
    }
}
```

<br />

#### Framework-agnostic approach using alternative dependency injection or a service locator

While Hilt is recommended, many Android applications implement AppFunctions with
alternative dependency injection frameworks like Koin, Anvil, or manual Service
Locators. Because `AppFunctionService` inherits from Android
`android.app.Service` and therefore `Context`, you are able access your
application's DI container directly through `applicationContext` in property
getters or during service lifecycle execution:


```kotlin
@RequiresApi(36)
@AppFunctionServiceEntryPoint(
    serviceName = "ServiceLocatorMyAppFunctionService",
    appFunctionXmlFileName = "service_locator_my_app_function_service",
)
abstract class ServiceLocatorBaseAppFunctionService : AppFunctionService() {
    // Example using a manual Service Locator or Application container
    private val messageRepository by lazy {
        (applicationContext as AppFunctionApplication).appContainer.messageRepository
    }
    // Or with Koin / alternative DI locators:
    // private val messageRepository: MessageRepository by inject()

    @AppFunction(isDescribedByKDoc = true)
    internal suspend fun send(
        name: String,
        endpointValue: String,
        messageBody: String,
    ): MessageResult {
        return messageRepository.send(name, endpointValue, messageBody)
    }
}
```

<br />

> [!IMPORTANT]
> **Important:** The `appFunctionXmlFileName` parameter, for example `"my_app_function_service"`, mustn't include the `.xml` extension, as the KSP compiler automatically appends `.xml`. Passing `"my_app_function_service.xml"` results in the asset being named `"my_app_function_service.xml.xml"`.

*** ** * ** ***

### Simplify method signatures and decouple context

Remove legacy `AppFunctionContext` parameters from your core methods. When a
method requires an Android `Context`, for example when constructing a
`PendingIntent`, access `this` directly from your `AppFunctionService` wrapper
because the wrapper inherently extends `android.content.Context`.

    -   suspend fun makeCall(appFunctionContext: AppFunctionContext, contactName: String?): PendingIntent
    +   suspend fun makeCall(contactName: String?): PendingIntent

*** ** * ** ***

### Remove legacy configuration provider

Update your `Application` class by removing
`AppFunctionConfiguration.Provider` and its associated builder entry points:

    -   abstract class BaseChatApplication : Application(), AppFunctionConfiguration.Provider { ... }
    +   abstract class BaseChatApplication : Application()

*** ** * ** ***

### Avoid redundant abstraction layers

Don't attempt to make an AppFunction class or method OS-agnostic---AppFunctions
are inherently part of the Android platform integration through the
`androidx.appfunctions` package. For architectural cleanliness, use existing
application functionality, such as existing repositories, use cases, or domain
orchestrators, to execute the behavior within the `@AppFunctionDeclaration`
methods rather than creating redundant abstraction layers around the OS service.

*** ** * ** ***

### Consolidate service and metadata manifest declarations

Register the KSP-generated service declaration and `app_metadata` property
inside your module manifest, for example in `src/main/AndroidManifest.xml`
within the `<application>` tag:


```xml
<service
    android:name="com.example.snippets.ai.MyAppFunctionService"
    android:permission="android.permission.BIND_APP_FUNCTION_SERVICE"
    android:exported="true"
    tools:targetApi="36">
    <property
        android:name="android.app.appfunctions.schema"
        android:value="app_functions_schema.xsd" />
    <property
        android:name="android.app.appfunctions.v2"
        android:value="my_app_function_service.xml" />
    <intent-filter>
        <action android:name="android.app.appfunctions.AppFunctionService" />
    </intent-filter>
</service>
<property
    android:name="android.app.appfunctions.app_metadata"
    android:resource="@xml/app_metadata" />
```

<br />

Set `android:name` to the class that KSP generates from `serviceName`, and
the `android.app.appfunctions.v2` value to `appFunctionXmlFileName` plus
`.xml`.

*** ** * ** ***

## Upgrade to 1.0.0-beta01

Apply these changes to every app that uses `@AppFunctionServiceEntryPoint`,
including apps migrated with the preceding steps:

1. **Annotation rename** : Replace each `@AppFunction` annotation with `@AppFunctionDeclaration`, and update its import. Match the whole name, so annotations such as `@AppFunctionSerializable` stay unchanged. Version 1.0.0-alpha13 renamed this annotation.
2. **Explicit KDoc flag** : Set `isDescribedByKDoc = true` on every `@AppFunctionDeclaration`, `@AppFunctionSerializable`, and `@AppFunctionSignature`. Since 1.0.0-beta01, it has no default value.
3. **App description attribute** : In `app_metadata.xml`, rename `appfn:description` to `appfn:instructions`.
4. **Imports** : Import `AppFunctionName` from `androidx.appfunctions`, not from `androidx.appfunctions.metadata`.
5. **Removed APIs** : Remove remaining uses of `AppFunctionConfiguration` and `AppFunctionContext`. Version 1.0.0-alpha12 removed them.
6. **Other API changes** : If the app uses these APIs, update them:
   - `@AppFunctionStringValueConstraint`: Replace regular expression patterns with `patternMatchers` (1.0.0-beta01).
   - `AppFunctionResourceContainer` and `AppFunctionTextResource`: Opt in with `@OptIn(ExperimentalAppFunctionsApi::class)` (1.0.0-beta01).
   - `AppFunctionAppMetadata.description`: Read `instructions` instead (1.0.0-beta01).
   - `AppFunctionMetadata.isEnabled`: Call `AppFunctionManager.getAppFunctionStates` instead (1.0.0-alpha11).
   - `ObserveAppFunctionsEvent`: Use `AppFunctionsChangeEvent` instead (1.0.0-alpha13).

*** ** * ** ***

## Verification and troubleshooting

1. **Clean rebuild and deploy** : `bash
   ./gradlew clean installDebug`
2. **Verify AppSearch discovery / indexing** : Run the following ADB command to
   confirm the OS successfully discovered and indexed your functions:
   `bash
   adb shell cmd app_function list-app-functions`
   *If your package doesn't appear, confirm that `android.app.appfunctions.v2`
   matches the exact asset name generated in `assets/`.*

3. **Verify execution using ADB** :
   `bash
   adb shell "cmd app_function execute-app-function \
   --package com.example.chatapp \
   --function 'com.example.chatapp.appfunctions.BaseAppFunctionService#send' \
   --parameters '{\"name\": \"Alice\", \"endpointValue\": \"1\", \"messageBody\": \"Hello Alice!\"}'"`