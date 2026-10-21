This file splits model work between the agent and the user. The agent writes
app code only. The user picks, downloads, and converts model files.

## Choose a file from litert-community

Hugging Face `litert-community` holds one repository per model, such as
`litert-community/gemma-4-E2B-it-litert-lm` and `litert-community/Qwen3-0.6B`.
Use `gemma-4-E2B-it.litertlm` from the first or `Qwen3-0.6B.litertlm` from the
second; both run on the CPU and the GPU.

The other files in a repository are other builds of the same model:

- Other quantizations and context lengths. The Qwen3 card lists each file with its quantization, its context length in tokens, and its size.
- Web builds, named with the `-web` suffix.
- Builds for one chip, named after it, such as `_Google_Tensor_G5` or `.mediatek.mt6993`. These are NPU builds, which this skill doesn't cover.

When the user hasn't named a file, use one of the two files in this section.
Don't pick a web build or an NPU build.

## Write the in-app download code

The download URL is `https://huggingface.co/<repo>/resolve/main/<file>`. When
writing the download code in the app, follow these rules:

- Write the file into `filesDir`, or into `getExternalFilesDir(null)` when the app uses `DownloadManager`.
- Check the downloaded file size against the size on the model card before passing the path to the engine.
- Never place the file in `assets/` or in the APK.
- Never download the model file in the agent's own session. Files are hundreds of MB to a few GB.

## When no .litertlm file exists

This skill doesn't convert models. Tell the user that conversion is a separate
step on a workstation, never in the app. The user exports the model with
`litert-torch`, quantizes it with `ai-edge-quantizer`, and bundles the tokenizer
and metadata into a `.litertlm` file with `litert_lm_builder` from the
`litert-lm` package.