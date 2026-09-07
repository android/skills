System instructions let you give the model a persona, set the tone of its
responses, or provide specific rules it must follow. These instructions are
separate from the user's prompt and are treated with higher priority by the
model to ensure it behaves as expected.

Common use cases include:

- **Setting a persona:** For example, "You are a helpful math tutor."
- **Enforcing output format:** For example, "Always respond in bullet points."
- **Setting constraints:** For example, "Do not answer questions about politics."

## Prerequisites

System instructions work on devices running Gemini Nano V3 and higher. For a
list of supported devices, see [Prompt API device support](https://developer.android.com/ml-kit/genai#prompt-device).

## Limitations

We don't recommend using system instructions with
[prefix caching](https://developer.android.com/agents/skills/device-ai/prompt-api/references/prefix-caching). In general, use system instructions for
short instructions that define how the model should behave; use prefix
caching if you need to repeat a large part of your prompt across queries and
need to optimize performance.

## How to use system instructions

To provide system instructions, create a `SystemInstruction` object and pass it
to the `GenerateContentRequest` builder:

    import com.google.mlkit.genai.prompt.SystemInstruction
    import com.google.mlkit.genai.prompt.TextPart
    import com.google.mlkit.genai.prompt.generateContentRequest

    // 1. Define the system instruction
    val systemInstruction =
        SystemInstruction("You are a concise assistant. Answer in 2 sentences or less.")

    // 2. Create the request
    val request = generateContentRequest(TextPart("How does photosynthesis work?")) {
        this.systemInstruction = systemInstruction
    }

    // 3. Run inference
    try {
        val response = generativeModel.generateContent(request)
        println(response.candidates.firstOrNull()?.text)
    } catch (e: GenAiException) {
        // Handle SDK-specific exceptions
    }

You can further simplify the code by passing the system instructions directly
into the `generateContentRequest` request builder:

    val request = generateContentRequest(
        SystemInstruction("You are a pirate. Speak like one."),
        TextPart("What is the weather like today?")
    ) {
        // Optional configurations like temperature
        temperature = 0.7f
    }

## Best practices

Here are some best practices when using system instructions:

- **Be clear and direct:** The model follows clear, direct instructions better than ambiguous ones. Here are some examples:
  - Vague (avoid): "Don't write too much. Try to be helpful and friendly, and format the output nicely."
  - Clear (preferred): "You are a friendly customer support assistant. Limit your responses to a maximum of 3 sentences. Format any lists using bullet points."
- **Be concise:** While system instructions are powerful, very long instructions can consume the model's limited context window.
- **Factor in token counts:** Make sure that your token counting logic includes the system instructions to avoid underestimating request size. We recommend keeping your system instructions to under 150 words (100-200 tokens).
- **Test and iterate:** Model behavior can vary based on phrasing. Test with various user inputs to ensure the model maintains its persona consistently.