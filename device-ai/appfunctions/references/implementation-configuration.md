Specialized instructions for generating Kotlin implementations of AppFunctions,
handling system-wide configuration, and managing build dependencies.

## Instructions

### Step 1: Configure Gradle dependencies and KSP

Add the following to `build.gradle.kts`. App Functions requires the KSP (Kotlin
Symbol Processing) plugin.

1. **Version check** : Use library version `1.0.0-beta01` or later from maven.google.com.
2. **Version catalog** : The following snippet uses version catalog aliases for `androidx.appfunctions:appfunctions` and `androidx.appfunctions:appfunctions-compiler`. Add both to `libs.versions.toml`. Without a version catalog, declare the coordinates and version directly.


```kotlin
implementation(libs.androidx.appfunctions)
ksp(libs.androidx.appfunctions.compiler)
```

<br />

<br />

<br />

### Step 2: Set up app metadata XML

Describe the app's capabilities to the LLM by defining
`res/xml/app_metadata.xml`. Write the app description in the
`appfn:instructions` attribute, as described in the KDoc refinement step.
Before 1.0.0-beta01, this attribute was `appfn:description`. The optional
`appfn:displayDescription` attribute references a string resource with a
short, user-visible summary of what the app's functions let agents do.
Define that resource, or omit the attribute.


```xml
<AppFunctionAppMetadata xmlns:appfn="http://schemas.android.com/apk/androidx.appfunctions"
    appfn:description="This app manages user tasks and reminders.
    Operational Patterns:
    - Use 'createTask' to add new tasks or reminders with titles and content.
    Constraints:
    - Title or content must be non-null when creating a task."
    appfn:displayDescription="@string/user_visible_description" />
```

<br />

Register the service and reference the app metadata in `AndroidManifest.xml`
within the `<application>` tag:


```xml
<service
    android:name="com.example.snippets.ai.TaskAppFunctionService"
    android:permission="android.permission.BIND_APP_FUNCTION_SERVICE"
    android:exported="true"
    tools:targetApi="36">
    <property
        android:name="android.app.appfunctions.schema"
        android:value="app_functions_schema.xsd" />
    <property
        android:name="android.app.appfunctions.v2"
        android:value="task_app_function_service.xml" />
    <intent-filter>
        <action android:name="android.app.appfunctions.AppFunctionService" />
    </intent-filter>
</service>
<property
    android:name="android.app.appfunctions.app_metadata"
    android:resource="@xml/app_metadata" />
```

<br />

Set `android:name` to the class that KSP generates from the `serviceName` of
the `@AppFunctionServiceEntryPoint` class in Step 4, in that class's package.
Set the `android.app.appfunctions.v2` value to its `appFunctionXmlFileName`
plus `.xml`.

### Step 3: Implement functions

When generating Kotlin code for AppFunctions, you MUST adhere to these rules:

1. **Annotations** :
   - Annotate the function with `@AppFunctionDeclaration(isDescribedByKDoc = true)`. Before 1.0.0-alpha13, this annotation was `@AppFunction`.
   - Annotate associated data classes with `@AppFunctionSerializable(isDescribedByKDoc = true)`.
   - Always set `isDescribedByKDoc` explicitly. Since 1.0.0-beta01, it has no default value.
2. **Naming and parameters** :
   - **Function names** : Give each function a human-readable name that identifies its purpose, such as `createTask`. Never use a name that misstates what the code does or resembles another tool or service.
   - **Specificity**: Keep parameters specific. State objects must be unambiguous.
   - **Necessity**: Define parameters only for data that the function uses.
   - **Naming** : Name parameters after the expected input, such as `taskId`, `searchQuery`, or `recipientEmail`. Never use names such as `full_context` or `conversation_history`. These ambiguous names risk agents passing unneeded user data to the function.
   - **Optionality**: If a parameter isn't essential, make it optional with a default value.
   - **Updates** : For functions that update existing items, distinguish "set to null" from "don't change". Declare each updatable parameter as a nullable `@AppFunctionSerializable` wrapper class that the app defines, such as `SetField<T>`. A `null` wrapper means "don't change", and a wrapper that holds `null` means "set to null". Never unwrap a nullable field with `?:`, which turns "set to null" into "don't change".
3. **Execution and threading** :
   - Use `suspend` functions.
   - To avoid blocking the Android UI thread, always run AppFunction implementations on a background dispatcher, such as `withContext(Dispatchers.IO)`.
4. **Supported types** :
   - **Primitives** : `Int`, `Long`, `Float`, `Double`, `Boolean`
   - **Arrays** : `IntArray`, `LongArray`, `FloatArray`, `DoubleArray`, `BooleanArray`
   - **Native types** : `String`, `PendingIntent`, `Uri`, `LocalTime`, `LocalDate`, `LocalDateTime`, `Instant`, `ZoneId`. Prefer using `LocalDateTime` or `Instant` for date and time fields.
   - **Custom objects** : Classes annotated with `@AppFunctionSerializable`.
   - **Collections** : `List` of any supported non-primitive type
   - Use these types instead of defining custom date, time, or link classes. The library represents them the same way in every app.
5. **Default values** :
   - Optional parameters MUST be nullable, primitive, list, or array types.
   - Declare defaults that match the type's empty state, such as `0` for `Int`, `null` for nullable objects, and `emptyList()` for `List`.
   - When the agent omits a parameter, the library passes this empty value, not the declared default. Resolve meaningful defaults in the function body.
6. **Error handling** :
   - Throw the `androidx.appfunctions.AppFunctionException` subclass that matches the cause, with a plain-language message. For example, throw `AppFunctionInvalidArgumentException` for an invalid argument, or `AppFunctionElementNotFoundException` for a missing item.
   - Never let other exceptions escape. The agent receives them as `AppFunctionAppUnknownException`, which doesn't identify the failure.
7. **Security** :
   - Expose only safe, non-destructive tasks that the app's UI already offers on that form factor.
   - Never give agents wide-ranging access to user data or system resources.
   - Never require or return passwords, financial details, private keys, or other highly sensitive personal information. Share other personal or confidential data only when the user explicitly consents to share it for the requested action.
   - Never perform actions that the user didn't request. Actions that the user scheduled, such as sending a message later, are allowed.
8. **Opening screens** :
   - To let the agent open a screen in the app, return a `PendingIntent`. Create it from an explicit intent with `FLAG_IMMUTABLE`, which locks where it leads.

### Step 4: Set up dependency injection and service entry points

In version 1.0.0-alpha10 and later, App Functions use the compile-time
`@AppFunctionServiceEntryPoint` architecture. Create an abstract class extending
`AppFunctionService` annotated with `@AppFunctionServiceEntryPoint`. KSP
generates the concrete service class and XML schema. Declare only one entry
point class: before `Build.VERSION_CODES.CINNAMON_BUN`, Android allows only
one service per app to handle the
`android.app.appfunctions.AppFunctionService` action.

#### Recommended approach with Hilt

Annotate your service with `@AndroidEntryPoint` and inject your data
repositories or use cases using standard `@Inject internal lateinit var`:


```kotlin
@RequiresApi(36)
@AndroidEntryPoint
@AppFunctionServiceEntryPoint(
    serviceName = "ConfigAppFunctionServiceHeader",
    appFunctionXmlFileName = "config_app_function_service_header",
)
abstract class BaseAppFunctionServiceHeader : AppFunctionService() {
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

#### Framework-agnostic approach with alternative dependency injection or service locators

While Hilt is recommended, many Android applications implement AppFunctions with
alternative dependency injection frameworks (like Koin, Anvil, or manual Service
Locators). Because `AppFunctionService` inherits from Android
`android.app.Service` (and therefore `Context`), you are able access your
application's DI container directly through `applicationContext` in property
getters or during service lifecycle execution:


```kotlin
@RequiresApi(36)
@AppFunctionServiceEntryPoint(
    serviceName = "ServiceLocatorConfigAppFunctionService",
    appFunctionXmlFileName = "service_locator_config_app_function_service",
)
abstract class BaseAppFunctionServiceLocator : AppFunctionService() {
    // Example using a manual Service Locator / Application container
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

### Step 5: Architectural cleanliness

Don't attempt to make an AppFunction class or method OS-agnostic---AppFunctions
are inherently part of the Android platform integration in
`androidx.appfunctions`. For architectural cleanliness, use existing
application functionality (such as existing repositories, use cases, or domain
orchestrators) to execute the behavior within the `@AppFunctionDeclaration`
methods rather than creating redundant abstraction layers around the OS service.

<br />

## Critical constraints

### KSP compliance for serializables

**Critical constraints** : For `@AppFunctionSerializable` data classes, KSP
only extracts documentation if it's written as inline KDoc directly for each
property definition. Don't use class-level `@param` or `@property` tags.
Declare every primary constructor parameter as a `val` property, and keep the
primary constructor public. Other constructor parameters cause a compiler
error.

### Package integrity

The `@AppFunctionDeclaration` annotation, `AppFunctionService`, and
`AppFunctionName` are located in `androidx.appfunctions`. Before 1.0.0-beta01,
`AppFunctionName` was in `androidx.appfunctions.metadata`.

## Examples

### Example: Serializable with inline KDoc


```kotlin
/** The parameter to create the task. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class CreateTaskParams(
    /** The title of the task. */
    val title: String?,
    /** The content of the task. */
    val content: String?,
)

/** The user-created task. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class Task(
    /** The ID of the task. */
    val id: String,
    /** The title of the task. */
    val title: String,
    /** The content of the task. */
    val content: String,
)
```

<br />

<br />

### Example: Implementation detail


```kotlin
@RequiresApi(36)
@AndroidEntryPoint
@AppFunctionServiceEntryPoint(
    serviceName = "TaskAppFunctionService",
    appFunctionXmlFileName = "task_app_function_service",
)
abstract class BaseTaskAppFunctionService : AppFunctionService() {
    @Inject internal lateinit var taskRepository: TaskRepository

    /**
     * Creates a task based on [createTaskParams].
     *
     * @param createTaskParams The parameter to describe how to create the task.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun createTask(
        createTaskParams: CreateTaskParams,
    ): Task = withContext(Dispatchers.IO) {
        // Developers can use predefined exceptions to let the agent know
        // why it failed.
        if (createTaskParams.title == null && createTaskParams.content == null) {
            throw AppFunctionInvalidArgumentException("Title or content should be non-null")
        }

        val id = taskRepository.createTask(
            createTaskParams.title,
            createTaskParams.content
        )

        return@withContext taskRepository
            .getTask(id)
            ?.toTask()
            ?: throw AppFunctionElementNotFoundException("Task not found for ID = $id")
    }

    // Maps internal TaskEntity
    private fun TaskEntity.toTask() = Task(id = id, title = title, content = description)
}
```

<br />

<br />

## Troubleshooting

### Error: Build fails after adding or upgrading the library

**Cause** : An annotation omits `isDescribedByKDoc`, which has no default value
since 1.0.0-beta01, or the module's `compileSdk` is lower than 37.

**Solution** : Set `isDescribedByKDoc = true` on every `@AppFunctionDeclaration`
and `@AppFunctionSerializable`, and set `compileSdk` to 37 or higher.

### Error: "AppFunction unavailable" or "Metadata missing"

**Cause**: The AppSearch indexing failed to extract the schema correctly.

**Solution**:

1. Verify `@AppFunctionSerializable` classes use inline KDoc comments, not class-level `@param` tags.
2. Check that the `assets/<appFunctionXmlFileName>.xml` file exists in the APK.
3. Confirm the `ksp("androidx.appfunctions:appfunctions-compiler")` dependency is correctly applied.