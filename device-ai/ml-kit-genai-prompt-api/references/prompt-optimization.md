This skill provides guidance on how to optimize prompts for use with the ML Kit
Prompt API.

## Guidelines \& Rules

The best practices for prompt design for Prompt API are at
[Prompt Design](https://developer.android.com/agents/skills/device-ai/prompt-api/references/prompt-design). Apply these core rules:

- Include examples for in-context learning with examples of the output. This is an example prompt that includes in-context learning because it has examples of the desired model output: Analyze the message and return whether the customer sentiment is positive, negative, or neutral. Example 1: Input - "This product doesn't work"; Output - "Negative" Example 2: Input - "I liked this product"; Output - "Positive"
- Make prompts concise. Remove duplicate or repeated instructions. Do not include conversational filler such as: hello, please, do this, help me with, etc. For example, a verbose prompt such as: "Hello! I want you to act as an expert translator for me today. I am going to give you a short paragraph of text in English, and I would really like it if you could translate it into formal, high-level Business French. Please make sure that you do not include any intro like 'Here is your translation' or any closing remarks at all. Just output the translation itself." can be improved with this concise prompt: "Translate to formal Business French. Return ONLY the translation"
- Use paired XML/HTML delimiters to denote dynamic inputs (for example `<email>[content]</email>`) to clearly isolate user data from prompt instructions. Mention that the delimiters represent placeholder text that should be replaced by code in the actual implementation.
- Use the [Structured Output API](https://developer.android.com/agents/skills/device-ai/prompt-api/references/structured-output) if the output requires parsing responses into certain formats. Don't include the Structured Output API implementation in the prompt itself, but mention it after presenting the optimized prompt to the user so the user knows how to parse the model output. Prompts that use Structured Output API must return the `@Generable` typed object from the function signature instead of a string or JSON string.
- Keep output short by adding output constraints such as word count, character count, or number of bullets or sentences. For example, add constraints such as "The summarization must be 10 words or fewer."
- Use [system instructions](https://developer.android.com/agents/skills/device-ai/prompt-api/references/system-instructions) for short instructions that define how a model should behave, and use [prefix caching](https://developer.android.com/agents/skills/device-ai/prompt-api/references/prefix-caching) for prompts that are over 200 words.

## Examples

Here are examples of prompts that the user might ask you to optimize, and the
improved, optimized versions.

### Example 1

Unoptimized prompt:

    Process this customer email and return the order ID, what was bought, the phone
    number, and what they want. Format it as JSON so my app can read it.

Optimized prompt:

    ## Task

    Extract key details from the customer email below.

    ## Customer Email

    <email>
    [email_content]
    </email>

Explanation: Because the information that the user wants to extract using the
prompt is well suited for the Structured Output API, the optimized prompt
implicitly uses the Structured Output API and doesn't need to explicitly define
the schema for the extracted data. In your response to the user that explains
the optimizations made to the prompt, mention that the optimized prompt is
designed for use with the Structured Output API. Also, mention that the
"" delimiters signify placeholder text that should be replaced with code in the user's actual implementation.

### Example 2

Unoptimized prompt:

    Given this itinerary for a trip: [Flight to Paris (CDG), Check-in: Hotel Le
    Meurice, Visit to the Louvre, Dinner at Le Jules Verne, Eiffel Tower Visit,
    Versailles Palace Tour, Montmartre Walk, Seine River Cruise, Pastry and Macaron
    Tasting, Flight Out], generate the following: overall vibe, tips on how to
    prepare for this trip, and common short phrases to learn for the trip.

Optimized prompt:

    ## Task

    Analyze the trip itinerary below and extract details to build a concise travel
    guide.

    ## Rules
    - Overall vibe: Limit to 1 short sentence (under 15 words).
    - Preparation tips: Provide exactly 3 short tips (maximum 10 words per tip).
    - Useful phrases: Provide exactly 3 phrases (under 15 words each).

    ## Itinerary

    <itinerary>
    [itinerary_content]
    </itinerary>