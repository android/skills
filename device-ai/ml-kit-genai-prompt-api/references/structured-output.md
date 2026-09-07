If you need to parse the responses from the Prompt API into certain formats,
such as JSON, for further processing, use the Structured Output API.

With the Structured Output API, you define the target output structure using
Kotlin classes and annotations. The Prompt API then returns a response in the
form of your Kotlin object.

Generating structured output is particularly useful for tasks like the
following:

- **Entity extraction**: Extracting structured fields (for example, event name, date, location) from unstructured text.
- **Classification**: Categorizing input text into predefined categories.
- **Data serialization**: Converting unstructured user input into a format suitable for database storage or API calls.

## Prerequisites

To verify that the Structured Output API is available on the device, use the
`isStructuredOutputFeatureAvailable()` API. The API returns `true` if the
Structured Output API is available on the device, and `false` otherwise.

    suspend fun isStructuredOutputFeatureAvailable(): Boolean

The Structured Output API also has the following requirements:

- Android API level 26 or higher (`minSdk` 26)
- KSP plugin version 2.3.6 or higher

## Limitations

The Structured Output API has the following limitations:

- Works in Kotlin only.
- ProGuard might interfere with the parsing of your annotated class. Add your annotated class to your [keep rules](https://developer.android.com/topic/performance/app-optimization/keep-rules-overview) to exclude them from ProGuard if you get errors parsing, for example:

    # Keep classes used by structured output for deserialization for release builds.
    -keep class com.google.mlkit.genai.demo.kotlin.Plant { *; }

## Configure project

To get started with the Structured Output API, follow these steps:

1. [Add the ML Kit Prompt API as a dependency](https://developer.android.com/agents/skills/device-ai/prompt-api/references/get-started#configure-project) in your
   app-level `build.gradle.kts` (or `build.gradle`) file, if you haven't
   already.

2. Add the KSP plugin to your project-level `build.gradle.kts` file. Use a
   KSP plugin version that is compatible with your Kotlin version; we
   recommend KSP version 2.3.6 or higher.

       dependencies {
           ...
           classpath "com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.3.6"
       }

3. Add the structured compiler dependencies to your app-level
   `build.gradle.kts` file:

       dependencies {
           ...
           ksp("com.google.mlkit:genai-schema-compiler:1.0.0-alpha1")
       }

## Define the output structure

Define the structure of the data you want the model to return using Kotlin
data classes. There are two main annotations for defining the output
structure:

- Use the `@Generable` annotation to define the class as a target for structured output.
- Use the `@Guide` annotations on the class properties to provide descriptions and constraints that guide the model's output.

The following example defines a structure for extracting plant information:

    import com.google.mlkit.genai.schema.annotations.Generable
    import com.google.mlkit.genai.schema.annotations.Guide

    @Generable
    data class PlantList(
        @Guide(description = "The list of plants found", minItems = 1, maxItems = 5)
        val plants: List<Plant>
    )

    @Generable("Information about a plant species")
    data class Plant(
        @Guide(description = "The common name of the plant")
        val commonName: String,
        
        @Guide(description = "The full latin scientific name of the plant")
        val scientificName: String,
        
        @Guide(
            description = "The maximum height of the plant in centimeters.",
            minimum = 1.0,
            maximum = 10000.0
        )
        val maxHeightCm: Int,
        
        @Guide(description = "Whether the plant is poisonous or not")
        val isPoisonous: Boolean?,
        
        @Guide(
            description = "The primary continent where this plant is native to",
            enumValues = ["Africa", "Antarctica", "Asia", "Australia", "Europe", "North America", "South America"]
        )
        val nativeContinent: String
    )

### Supported types and constraints

The following types are supported within a `@Generable` annotated class,
along with their respective `@Guide` constraints:

| Type | Description | Supported `@Guide` constraints |
|---|---|---|
| `String` | For text. | `description`, `enumValues` |
| `Double` / `Float` | For floating-point numbers. | `description`, `minimum`, `maximum` |
| `Int` / `Long` | For whole numbers. | `description`, `minimum`, `maximum` |
| `Boolean` | For true/false values. | `description` |
| `List<T>` | For lists of supported types or nested `@Generable` classes. | `description`, `minItems`, `maxItems` |
| `List<String>` | For lists of `String` values. | `description`, `enumValues`, `minItems`, `maxItems` > [!NOTE] > **Note:** Setting the `enumValues` parameter defines the values allowed for the individual list items. |
| `@Generable` class | For nested structured objects. | `description` |

> [!NOTE]
> **Note:** Circular dependencies between nested `@Generable` classes are not supported (for example, a class referencing itself, or Class A referencing Class B which in turn references Class A).

## Generate structured content

To request structured output, use the `generateTypedContentRequest` helper
function to wrap your standard prompt and specify the target output class.

    // 1. Initialize your GenerativeModel as usual
    val generativeModel = Generation.getClient()

    // 2. Prepare the prompt text
    val promptText = "List some common plants found in California."
    val baseRequest = GenerateContentRequest.Builder(TextPart(promptText)).build()

    // 3. Create the typed request, specifying the target class (e.g., PlantList)
    val typedRequest = generateTypedContentRequest(
        generateContentRequest = baseRequest,
        outputClass = PlantList::class
    )

    // 4. Run the inference
    try {
        val typedResponse = generativeModel.generateContent(typedRequest)
        
        // 5. Access the parsed object
        // The response candidates contain the parsed object of type T (PlantList in this case)
        val plantList: PlantList? = typedResponse.candidates.firstOrNull()?.response
        
        if (plantList != null) {
            // Process the structured data
            for (plant in plantList.plants) {
                Log.d("StructuredOutput", "Found plant: ${plant.commonName} (${plant.scientificName})")
            }
        } else {
            Log.e("StructuredOutput", "Failed to parse response into the desired structure.")
            
            // Inspect finish reason for details
            val finishReason = typedResponse.candidates.firstOrNull()?.finishReason
            Log.d("StructuredOutput", "Finish reason: $finishReason")
        }
    } catch (e: GenAiException) {
        // Handle API errors
        when (e.errorCode) {
            GenAiException.STRUCTURED_OUTPUT_INVALID_CLASS -> {
                Log.e("StructuredOutput", "The class structure is not supported.")
            }
            GenAiException.STRUCTURED_OUTPUT_INVALID_VALUE -> {
                Log.e("StructuredOutput", "The model generated values that violate the schema constraints.")
            }
            else -> {
                Log.e("StructuredOutput", "API error: ${e.message}")
            }
        }
    }

## Handle finish reasons and errors

When using the Structured Output API, you should handle potential exceptions
thrown by the API and inspect the `finishReason` property in the response
candidates if the parsed response is null.

### finishReason values

The `finishReason` property can take one of the following values:

- `TypedFinishReason.STOP`: The model finished generating successfully and the output matches the schema.
- `TypedFinishReason.MAX_TOKENS`: The model stopped because it reached the token limit. The output might be incomplete.
- `TypedFinishReason.PARSE_CLASS_ERROR`: The model completed generation, but the resulting JSON couldn't be parsed into the target Kotlin class.
- `TypedFinishReason.STRUCTURE_NOT_ANNOTATED`: The target class or its nested classes are missing the required `@Generable` annotation.
- `TypedFinishReason.STRUCTURE_VALUES_INVALID`: The generated values violated the constraints defined in the `@Guide` annotations (for example value out of range, list size out of bounds).
- `TypedFinishReason.OTHER`: Generation stopped due to other reasons.

### Exceptions

The Structured Output API might throw `GenAiException` with the following
error codes:

- `GenAiException.STRUCTURED_OUTPUT_INVALID_CLASS` (-104): The structure of the annotated class is invalid or contains unsupported types. This is typically a development-time configuration error. Review your `@Generable` data class definition to check that all property types are supported and that there aren't any circular dependencies.
- `GenAiException.STRUCTURED_OUTPUT_INVALID_VALUE` (-105): The values generated by the model are invalid or fail constraints verification. This is a runtime error. If you encounter this error frequently, consider the following solutions:
  - Refining your prompt instructions to guide the model more strictly.
  - Relaxing the constraints (like minimum, maximum, or list size limits) in your `@Guide` annotations if they are too restrictive for the model's capabilities.
  - Implementing a fallback strategy in your app, such as retrying the request or displaying a default state.

## Count tokens

To check if your structured prompt is within the input token limit, calculate
the token count using the [`countTokens()`](https://developer.android.com/android/reference/com/google/mlkit/genai/prompt/GenerativeModel#countTokens(com.google.mlkit.genai.prompt.GenerateContentRequest)) method.

Because structured output requests need to instruct the model on the schema
structure, counting tokens on just the raw prompt text (using a
`GenerateContentRequest` instance) isn't accurate. To get an accurate token
count, you must pass the complete `GenerateTypedContentRequest` instance, which
includes your target class and schema configurations, to the `countTokens()`
method:

    suspend fun <T : Any> countTokens(request: GenerateTypedContentRequest<T>): CountTokensResponse