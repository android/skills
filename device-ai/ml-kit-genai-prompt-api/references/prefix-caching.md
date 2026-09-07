> [!NOTE]
> **Note:** Prefix caching is experimental and may change in the future. This feature is only available on a subset of the supported devices for Prompt API, with support for more devices coming soon. We encourage you to experiment with this API on a Pixel device to understand how it can improve inference latency speeds for your specific use case.

*Prefix caching* is a feature that reduces inference time by storing and reusing
the intermediate LLM state of processing a shared and recurring prompt prefix
part. To enable prefix caching, you only have to separate the static prefix from
the dynamic suffix in your API request.

Prefix caching currently only supports text-only input, so you shouldn't use
this feature if you're providing an image in your prompt.

There are two approaches to implement prefix caching: implicit or explicit:

- [Implicit (automatic) prefix caching](https://developer.android.com/agents/skills/device-ai/ml-kit-genai-prompt-api/references/prefix-caching#implicit) is a lightweight approach where the application only needs to define a shared portion of the prompt.
- [Explicit (manual) prefix caching](https://developer.android.com/agents/skills/device-ai/ml-kit-genai-prompt-api/references/prefix-caching#explicit) allows applications to have more control over caches, including cache creation, querying, and deletion.

## Use prefix caching implicitly

To enable prefix caching, add the shared portion of the prompt to the
`promptPrefix` field, as shown in the following code snippets:

### Kotlin

    val promptPrefix = "Reverse the given sentence: "
    val dynamicSuffix = "Hello World"

    val result = generativeModel.generateContent(
      generateContentRequest(TextPart(dynamicSuffix)) {
        promptPrefix = PromptPrefix(promptPrefix)
      }
    )

### Java

    String promptPrefix = "Reverse the given sentence: ";
    String dynamicSuffix = "Hello World";

    GenerateContentResponse response = generativeModelFutures.generateContent(
        new GenerateContentRequest.Builder(new TextPart(dynamicSuffix))
        .setPromptPrefix(new PromptPrefix(promptPrefix))
        .build())
        .get();

In the preceding snippet, the `dynamicSuffix` is passed as the main content, and
the `promptPrefix` is provided separately.

### Estimated performance gains

|---|---|---|
|   | **Without prefix caching** | **With prefix cache-hit** (Prefix cache-miss may occur when prefix is used for the first time) |
| Pixel 9 with 300-token fixed prefix and a 50-token dynamic suffix prompt | 0.82 seconds | 0.45 seconds |
| Pixel 9 with a 1,000-token fixed prefix and a 100-token dynamic suffix prompt | 2.11 seconds | 0.5 seconds |

### Storage considerations

With implicit prefix caching, cache files are saved on the client application's
private storage, which increases your app's storage usage. Encrypted cache files
and their associated metadata, including original prefix text, are stored. Keep
the following storage considerations in mind:

- The number of caches is managed by an LRU (Least Recently Used) mechanism. Least used caches are deleted automatically when exceeding the max total cache amount.
- Prompt cache sizes are dependent on the length of the prefix.
- To clear all caches created from prefix caching, use the
  [`generativeMode.clearImplicitCaches()`](https://developer.android.com/android/reference/kotlin/com/google/mlkit/genai/prompt/GenerativeModel#clearCaches%28%29) method.

  > [!NOTE]
  > **Note:** The `clearImplicitCaches()` method is experimental and may change in the future.

## Use explicit cache management

The Prompt API includes explicit cache management methods to give developers
more precise control over how caches are created, searched, used, and removed.
These manual operations run independently of the system's automated cache
handling.

This example illustrates how to initialize explicit cache management and
perform inference:

### Kotlin

    val cacheName = "my_cache"
    val promptPrefix = "Reverse the given sentence: "
    val dynamicSuffix = "Hello World"

    // Create a cache
    val cacheRequest = createCachedContextRequest(cacheName, PromptPrefix(promptPrefix))
    val cache = generativeModel.caches.create(cacheRequest)

    // Run inference with the cache
    val response = generativeModel.generateContent(
      generateContentRequest(TextPart(dynamicSuffix)) {
        cachedContextName = cache.name
      }
    )

### Java

    String cacheName = "my_cache";
    String promptPrefix = "Reverse the given sentence: ";
    String dynamicSuffix = "Hello World";

    // Create a cache
    CachedContext cache = cachesFutures.create(
      new CreateCachedContextRequest.Builder(cacheName, new PromptPrefix(promptPrefix))
      .build())
      .get();

    // Run inference with the cache
    GenerateContentResponse response = generativeModelFutures.generateContent(
      new GenerateContentRequest.Builder(new TextPart(dynamicSuffix))
      .setCachedContextName(cache.getName())
      .build())
      .get();

This example demonstrates how to query, retrieve, and delete explicitly managed
caches using `generativeModel.caches`:

### Kotlin

    val cacheName = "my_cache"

    // Query pre-created caches
    for (cache in generativeModel.caches.list()) {
      // Do something with cache
    }

    // Get specific cache
    val cache = generativeModel.caches.get(cacheName)

    // Delete a pre-created cache
    generativeModel.caches.delete(cacheName)

### Java

    String cacheName = "my_cache";

    // Query pre-created caches
    for (PrefixCache cache : cachesFutures.list().get()) {
      // Do something with cache
    }

    // Get specific cache
    PrefixCache cache = cachesFutures.get(cacheName).get();

    // Delete a pre-created cache
    cachesFutures.delete(cacheName);