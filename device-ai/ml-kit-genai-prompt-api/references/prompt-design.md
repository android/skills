When using Prompt API, there are specific strategies you can use to tailor your
prompts and receive optimal results. This page describes best practices for
formatting prompts for Gemini Nano.

For more general prompt engineering guidance, see [Prompt Engineering
whitepaper](https://www.kaggle.com/whitepaper-prompt-engineering), [Prompt Engineering for Generative
AI](https://developer.android.com/machine-learning/resources/prompt-eng), and [Prompt design strategies](https://ai.google.dev/gemini-api/docs/prompting-strategies).

Alternatively, to automatically refine and improve prompts, you can use the
[zero-shot optimizer](https://docs.cloud.google.com/vertex-ai/generative-ai/docs/learn/prompts/zero-shot-optimizer#optimizing_for_smaller_models), which can target on-device models such as
`gemma-3n-e4b-it`.

## Prompt design best practices

When designing prompts for Prompt API, use the following techniques:

- **Provide examples for in-context learning**. Add well-distributed examples to
  your prompt to show Gemini Nano the kind of result you expect.

  Consider using the [prefix caching](https://developer.android.com/agents/skills/device-ai/prompt-api/references/prefix-caching) feature when you use in-context
  learning, as providing examples makes the prompt longer and increases
  inference time.
- **Be concise** . Verbose preambles with repeated instructions can produce
  suboptimal results. Keep your prompt focused and to-the-point. If you need to
  repeat a short directive that guides the model's behavior, consider using
  [system instructions](https://developer.android.com/agents/skills/device-ai/prompt-api/references/system-instructions).

- **Structure prompts** to generate more effective responses, such as this
  [sample prompt template](https://cloud.google.com/vertex-ai/generative-ai/docs/learn/prompts/prompt-design-strategies#sample-prompt-template) that clearly defines instructions,
  constraints, and examples.

- **Keep output short** . LLM inference speeds are heavily dependent on the
  output length. Carefully consider how you can generate the shortest possible
  output for your use case and do manual post-processing to structure the output
  in the chosen format. To help ensure that the response output is in your
  preferred format, use the [Structured Output API](https://developer.android.com/agents/skills/device-ai/prompt-api/references/structured-output).

- **Add delimiters** . Use delimiters like `<background_information>`,
  `<instruction>`, and `##` to create separation between different parts of your
  prompt. Using `##` between components is particularly critical for Gemini
  Nano, as it significantly reduces the chances of the model failing to
  correctly interpret each component.

- **Prefer simple logic and a more focused task** . If you find it challenging
  to achieve good results with a prompt requiring multi-step reasoning (for
  example, *do X first, if the result of X is A, do M; otherwise do N;
  then do Y...* ), consider breaking the task up and let each Gemini Nano call
  handle a more focused task, while using code to chain multiple calls together.
  If you do need to tackle a complex, reasoning-intensive task all at once,
  consider using [thinking mode](https://developer.android.com/ml-kit/genai/prompt/android/thinking-mode).

- **Use lower temperature values for deterministic tasks** . For tasks such as
  entity extraction or translation that don't rely on creativity, consider
  starting with a `temperature` value of `0.2`, and tune this value based on
  your testing.