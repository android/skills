This page describes how to do the following:

- Configure your project to use Prompt API
- Provide text-only input and receive a response
- Provide an image input with related text input and receive a response

For more details about the Prompt API, see the
reference documentation for Kotlin ([com.google.mlkit.genai.prompt](https://developer.android.com/android/reference/kotlin/com/google/mlkit/genai/prompt/package-summary)) and
Java ([com.google.mlkit.genai.prompt.java](https://developer.android.com/android/reference/com/google/mlkit/genai/prompt/java/package-summary),
[com.google.mlkit.genai.prompt](https://developer.android.com/android/reference/com/google/mlkit/genai/prompt/package-summary)).

## Configure project

> [!NOTE]
> **Note:** This API requires Android API level 26 or higher.

Add the ML Kit Prompt API as a dependency in your `build.gradle` configuration:

    implementation("com.google.mlkit:genai-prompt:1.0.0-beta2")

If you need your responses in a certain format using the Structured Output API,
you need to configure KSP and add additional dependencies. For details, see
[Generate structured output](https://developer.android.com/agents/skills/device-ai/prompt-api/references/structured-output).

## Implement generative model

To implement the code in your project, follow these steps:

- Create a `generativeModel` object:

  ### Kotlin

      // Get a GenerativeModel instance
      val generativeModel = Generation.getClient()

  ### Java

      // Get a GenerativeModel instance
      GenerativeModelFutures generativeModelFutures = GenerativeModelFutures
          .from(Generation.INSTANCE.getClient());

- Check if Gemini Nano is `AVAILABLE,` `DOWNLOADABLE`, or `UNAVAILABLE`. Then,
  download the feature if it is downloadable:

  ### Kotlin

      val status = generativeModel.checkStatus()
      when (status) {
          FeatureStatus.UNAVAILABLE -> {
              // Gemini Nano not supported on this device or device hasn't fetched the latest configuration to support it
          }

          FeatureStatus.DOWNLOADABLE -> {
              // Gemini Nano can be downloaded on this device, but is not currently downloaded
              generativeModel.download().collect { status ->
                  when (status) {
                      is DownloadStatus.DownloadStarted ->
                          Log.d(TAG, "starting download for Gemini Nano")

                      is DownloadStatus.DownloadProgress ->
                          Log.d(TAG, "Nano ${status.totalBytesDownloaded} bytes downloaded")

                      DownloadStatus.DownloadCompleted -> {
                          Log.d(TAG, "Gemini Nano download complete")
                          modelDownloaded = true
                      }

                      is DownloadStatus.DownloadFailed -> {
                          Log.e(TAG, "Nano download failed ${status.e.message}")
                      }
                  }
              }
          }

          FeatureStatus.DOWNLOADING -> {
              // Gemini Nano currently being downloaded
          }

          FeatureStatus.AVAILABLE -> {
              // Gemini Nano currently downloaded and available to use on this device
          }
      }

  ### Java

      ListenableFuture<Integer> status = generativeModelFutures.checkStatus();
      Futures.addCallback(generativeModelFutures.checkStatus(), new FutureCallback<>() {
          @Override
          public void onSuccess(Integer featureStatus) {
              switch (featureStatus) {
                  case FeatureStatus.AVAILABLE -> {
                      // Gemini Nano currently downloaded and available to use on this device
                  }
                  case FeatureStatus.UNAVAILABLE -> {
                      // Gemini Nano not supported on this device or device hasn't fetched the latest configuration to support it
                  }
                  case FeatureStatus.DOWNLOADING -> {
                      // Gemini Nano currently being downloaded
                  }
                  case FeatureStatus.DOWNLOADABLE -> {
                      generativeModelFutures.download(new DownloadCallback() {
                          @Override
                          public void onDownloadStarted(long l) {
                              Log.d(TAG, "starting download for Gemini Nano");
                          }
                          @Override
                          public void onDownloadProgress(long l) {
                              Log.d(TAG, "Nano " + l + " bytes downloaded");
                          }
                          @Override
                          public void onDownloadCompleted() {
                              Log.d(TAG, "Gemini Nano download complete");
                          }
                          @Override
                          public void onDownloadFailed(@NonNull GenAiException e) {
                              Log.e(TAG, "Nano download failed: " + e.getMessage());
                          }
                      });
                  }
              }
          }
          @Override
          public void onFailure(@NonNull Throwable t) {
              // Failed to check status
          }
      }, ContextCompat.getMainExecutor(context));

## Provide text-only input

### Kotlin

    val response = generativeModel.generateContent("Write a 3 sentence story about a magical dog.")

### Java

    GenerateContentResponse response = generativeModelFutures.generateContent(
      new GenerateContentRequest.Builder(
        new TextPart("Write a 3 sentence story about a magical dog."))
      .build())
      .get();

Alternatively, add optional parameters:

### Kotlin

    val response = generativeModel.generateContent(
        generateContentRequest(
            TextPart("Write a 3 sentence story about a magical dog."),
        ) {
            // Optional parameters
            temperature = 0.2f
            topK = 10
            candidateCount = 3
        },
    )

### Java

    GenerateContentRequest.Builder requestBuilder =
            new GenerateContentRequest.Builder(
                    new TextPart("Write a 3 sentence story about a magical dog."));
    requestBuilder.setTemperature(.2f);
    requestBuilder.setTopK(10);
    requestBuilder.setCandidateCount(3);

    GenerateContentResponse response =
            generativeModelFutures.generateContent(requestBuilder.build()).get();

For more information about the optional parameters, see [Optional
configurations](https://developer.android.com/agents/skills/device-ai/ml-kit-genai-prompt-api/references/get-started#optional-configurations).

## Provide multimodal (image and text) input

Bundle an image and a text input together in the `generateContentRequest()`
function, with the text prompt being a question or command related to the
image. You can bundle multiple images and text together in the same request.

### Kotlin

    val response = generativeModel.generateContent(
        generateContentRequest(ImagePart(bitmap), TextPart(textPrompt)) {
            // optional parameters
            ...
        },
    )

### Java

    GenerateContentResponse response = generativeModelFutures.generateContent(
        new GenerateContentRequest.Builder(
            new ImagePart(bitmap),
            new TextPart("textPrompt"))
        // optional parameters
        .build())
    .get();

## Process inference result

- Run the inference and retrieve the result. You can choose to either wait for
  the full result or stream the response as it's generated for both text-only
  and multimodal prompts.

  - This uses non-streaming inference, which retrieves the entire result from
    the AI model before returning the result:

  ### Kotlin

      // Call the AI model to generate content and store the complete
      // in a new variable named 'response' once it's finished
      val response = generativeModel.generateContent("Write a 3 sentence story about a magical dog")

  ### Java

      GenerateContentResponse response = generativeModelFutures.generateContent(
              new GenerateContentRequest.Builder(
                      new TextPart("Write a 3 sentence story about a magical dog."))
                      .build())
              .get();

  - The following snippets are examples of using streaming inference, which
    retrieves the result in chunks as it's being generated:

  ### Kotlin

      // Streaming inference
      var fullResponse = ""
      generativeModel.generateContentStream("Write a 3 sentence story about a magical dog").collect { chunk ->
          val newChunkReceived = chunk.candidates[0].text
          print(newChunkReceived)
          fullResponse += newChunkReceived
      }

  ### Java

      // Streaming inference
      StringBuilder fullResponse = new StringBuilder();
      generativeModelFutures.generateContent(new GenerateContentRequest.Builder(
          (new TextPart("Write a 3 sentence story about a magical dog"))).build(),
              chunk -> {
                  Log.d(TAG, chunk);
                  fullResponse.append(chunk);
              });

For more information about streaming and non-streaming inference, see [Streaming
versus non-streaming](https://developer.android.com/ml-kit/genai#streaming-vs-non).

## Latency optimization

To optimize for the first inference call, your application may optionally call
`warmup()`. This loads Gemini Nano into memory and initializes runtime
components.

## Optional configurations

As part of each `GenerateContentRequest`, you can set the following optional
parameters:

- `temperature` : Controls the degree of randomness in token selection.
- `seed` : Enables generating stable and deterministic results.
- `topK` : Controls randomness and diversity in results.
- `candidateCount` : Requests the number of unique responses returned. Note that the exact number of responses may not be the same as `candidateCount` because duplicate responses are automatically removed.
- `maxOutputTokens` : Defines the maximum number of tokens that can be generated in the response.

For more guidance on setting optional configurations, see
[`GenerateContentRequest`](https://developer.android.com/android/reference/kotlin/com/google/mlkit/genai/prompt/GenerateContentRequest).

## Supported features and limitations

- Input must be under 4000 tokens (or approximately 3000 English words). For more information, see the [`countTokens`](https://developer.android.com/android/reference/com/google/mlkit/genai/prompt/GenerativeModel#countTokens(com.google.mlkit.genai.prompt.GenerateContentRequest)) reference.
- Use cases that require long output (more than 4K tokens) should be avoided.
- AICore enforces an inference quota per app. For more information, see [Quota
  per application](https://developer.android.com/ml-kit/genai#quota-per).