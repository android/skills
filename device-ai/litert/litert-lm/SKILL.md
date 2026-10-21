---
name: litert-lm
description: Builds an Android app that runs an open text LLM on the device, on the
  CPU or the GPU, with the LiteRT-LM Kotlin API (com.google.ai.edge.litertlm:litertlm-android).
  Use this skill when creating a chat, summarization, or extraction feature with a
  .litertlm model from Hugging Face litert-community (Gemma, Qwen, Llama, Phi), including
  the Gradle dependency, the GPU manifest entries, getting the model file onto the
  device, Engine initialization off the main thread, a streamed multi-turn Conversation
  with sendMessageAsync, and the AndroidViewModel and Compose screen. Don't use for
  running .tflite vision, audio, or embedding models with the CompiledModel API, for
  image or audio input, tool calling, or NPU backends, or for converting models.
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-10-20'
  keywords:
  - LiteRT-LM
  - litertlm
  - Gemma
  - on-device LLM
  - Android app
  - GPU
---

This skill provides step-by-step guidance for building an Android app that runs
an open text LLM through the LiteRT-LM Kotlin API
(`com.google.ai.edge.litertlm`) on the CPU or the GPU. The model is one
`.litertlm` file. Images, audio, tool calling, and NPU backends aren't covered.
For vision and audio models with `.tflite` files, suggest the `litert-runtime`
skill.

When a LiteRT-LM class or method that this skill uses doesn't compile, you must
read the [LiteRT-LM Android guide](https://ai.google.dev/edge/litert-lm/android) and the [LiteRT-LM README](https://github.com/google-ai-edge/LiteRT-LM/blob/main/README.md) before
changing the code.

## Prerequisites

- A Kotlin Android project (the Android Studio Empty Activity template is enough). The `litertlm-android` 0.18.0 AAR declares `minSdk` 24.
- The dependency in the app-level `build.gradle.kts` file from Google Maven: `implementation("com.google.ai.edge.litertlm:litertlm-android:0.18.0")`. Google Maven lists the later releases.
- For the GPU backend, two `uses-native-library` entries inside `<application>` in `AndroidManifest.xml`. Add the `INTERNET` permission if the app downloads the model.
- The model file lives on the device's file system (hundreds of MB to a few GB), never in `assets/` or in the APK.

## Critical mandates

- **Dependency first** : Before writing code that imports `com.google.ai.edge.litertlm`, add the `litertlm-android` dependency from Step 1 if the project lacks it. Verify with `./gradlew assembleDebug`.
- **Background initialization** : `Engine(config).initialize()` loads the weights and blocks for seconds. Call it on a background thread, never on the main thread.
- **Single owner** : One single-thread executor owns the `Engine` and the `Conversation`. Creation, streaming, cancellation, and closing run on it.
- **Close once** : `Engine` and `Conversation` are `AutoCloseable`. A second `close` on either throws `IllegalStateException`. So does `close` on an engine whose `initialize` threw. Store the engine only after `initialize` returns.
- **Model outside the APK** : Pass an absolute path to `EngineConfig`. Never bundle a `.litertlm` file in `assets/`.

## Detailed steps

### Step 1: Configure Gradle and the manifest

Search the project for `libs.versions.toml`, `app/build.gradle.kts`, and
`AndroidManifest.xml` before editing build files. Configure `minSdk` and add the
LiteRT-LM dependency in `app/build.gradle.kts`:

<br />

    android {
        defaultConfig {
            minSdk = 24
        }
    }

    dependencies {
        implementation("com.google.ai.edge.litertlm:litertlm-android:0.18.0")
    }

If the project uses Hilt, check the Dagger version first. `litertlm-android`
0.18.0 carries Kotlin 2.4 metadata, which Hilt 2.59.2 can't read. The build
then fails in `hiltJavaCompileDebug` with `Provided Metadata instance has
version 2.4.0, while maximum supported version is 2.3.0`. Pick one fix:

- **Path A** : Upgrade the Hilt version (`com.google.dagger`) to 2.60.1 or later. Ask the user before editing `gradle/libs.versions.toml`.
- **Path B** : Keep the Hilt version and add a later version of the metadata reader to the annotation processor path in `app/build.gradle.kts`:

<br />

    dependencies {
        ksp("org.jetbrains.kotlin:kotlin-metadata-jvm:2.4.21")
    }

For the GPU backend, add both lines inside `<application>` in
`AndroidManifest.xml`. Add the `INTERNET` permission outside `<application>`
when the app downloads the model:

<br />

    <uses-permission android:name="android.permission.INTERNET"/>

    <application>
        <uses-native-library
            android:name="libvndksupport.so"
            android:required="false"/>
        <uses-native-library
            android:name="libOpenCL.so"
            android:required="false"/>
    </application>

### Step 2: Pick a model from litert-community

Choose a `.litertlm` file from [litert-community](https://huggingface.co/litert-community) on Hugging Face. Start with
`litert-community/gemma-4-E2B-it-litert-lm` (the file `gemma-4-E2B-it.litertlm`,
2.6 GB). For a 0.6 GB model, start with `litert-community/Qwen3-0.6B` (the file
`Qwen3-0.6B.litertlm`). Both files run on the CPU and the GPU. A SoC suffix such
as `_Google_Tensor_G5` or `.mediatek.mt6993` marks an NPU build, which this
skill doesn't cover. The model card names the tested backends and the size.

You must read [get a model](references/get-a-model.md) before you choose a model file other than these
two, before you write the in-app download code, and when no `.litertlm` file
exists for the model.

### Step 3: Put the model file on the device

Pick one path:

- **Path A (local testing with `adb`)** : Run the following commands only when
  all three are true: `adb devices` lists a device, the debug build of the app
  is installed on it, and the user supplied the local `.litertlm` path. If any
  of them is false, don't run the commands. Report them to the user as a
  post-implementation step instead. Replace `<path>` with the user's local path
  and `<package>` with the app's application ID:

  1. `adb push <path> /data/local/tmp/model.litertlm`
  2. `adb shell run-as <package> cp /data/local/tmp/model.litertlm files/`
- **Path B (in-app download)** : Write the download code in the app. Use the
  app's HTTP client to write into `filesDir`, or `DownloadManager` to write
  into `getExternalFilesDir(null)`. `DownloadManager` can't write to internal
  storage. Show the download progress and check the file size against the model
  card before loading. Never download a model file in the agent's own session.

- **Path C ([Play for On-Device AI](references/android/google/play/on-device-ai.md))** : Write the code that resolves the
  path of a fast-follow or on-demand AI pack with the AI Delivery library
  (`com.google.android.play:ai-delivery`):
  `AiPackManagerFactory.getInstance(context).getPackLocation(name)?.assetsPath()`.
  The call returns `null` until the pack has finished downloading, so show a
  download state in that case. Creating the AI pack module and uploading the app
  to Play Console are post-implementation steps for the user.

On every path, pass the absolute path of the file to the engine.

### Step 4: Initialize the engine off the main thread

Create the engine with `EngineConfig(modelPath = ..., backend = ...,
cacheDir = ...)` and call `initialize` on a background thread:

- `Backend.CPU()` is the default of `EngineConfig` and needs nothing from the device.
- `Backend.GPU()` needs the two manifest lines from Step 1. Without them, the engine initializes, and the first reply ends in the error `Can not find
  OpenCL library on this device`.
- `cacheDir` speeds up the second load.

### Step 5: Implement the conversation ViewModel

Drive the engine and the conversation from a single-thread executor inside an
`AndroidViewModel`, stream the reply, and expose immutable state to the UI:

<br />

    import android.app.Application
    import androidx.lifecycle.AndroidViewModel
    import com.google.ai.edge.litertlm.Backend
    import com.google.ai.edge.litertlm.Contents
    import com.google.ai.edge.litertlm.Conversation
    import com.google.ai.edge.litertlm.ConversationConfig
    import com.google.ai.edge.litertlm.Engine
    import com.google.ai.edge.litertlm.EngineConfig
    import java.util.concurrent.Executors
    import kotlin.coroutines.cancellation.CancellationException
    import kotlinx.coroutines.CoroutineScope
    import kotlinx.coroutines.SupervisorJob
    import kotlinx.coroutines.asCoroutineDispatcher
    import kotlinx.coroutines.cancel
    import kotlinx.coroutines.delay
    import kotlinx.coroutines.flow.MutableStateFlow
    import kotlinx.coroutines.flow.StateFlow
    import kotlinx.coroutines.flow.asStateFlow
    import kotlinx.coroutines.flow.update
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.sync.Mutex
    import kotlinx.coroutines.sync.withLock

    data class ChatState(
        val ready: Boolean = false,
        val busy: Boolean = false,
        val reply: String = "",
        val error: String? = null
    )

    class ChatViewModel(app: Application) : AndroidViewModel(app) {
        private val executor = Executors.newSingleThreadExecutor()
        private val scope = CoroutineScope(
            SupervisorJob() + executor.asCoroutineDispatcher()
        )
        private val mutex = Mutex()
        private var engine: Engine? = null
        private var conversation: Conversation? = null
        private val _state = MutableStateFlow(ChatState())
        val state: StateFlow<ChatState> = _state.asStateFlow()

        fun load(modelPath: String, backend: Backend = Backend.CPU()) {
            scope.launch {
                mutex.withLock {
                    if (engine?.engineConfig?.modelPath == modelPath) return@launch
                    release()
                    try {
                        val config = EngineConfig(
                            modelPath = modelPath,
                            backend = backend,
                            cacheDir = getApplication<Application>().cacheDir.path
                        )
                        engine = Engine(config).also { it.initialize() }
                        conversation = checkNotNull(engine).createConversation(
                            ConversationConfig(
                                systemInstruction =
                                    Contents.of("You are a helpful assistant.")
                            )
                        )
                        _state.value = ChatState(ready = true)
                    } catch (e: Exception) {
                        release()
                        _state.value = ChatState(error = e.message)
                    }
                }
            }
        }

        fun send(text: String) {
            scope.launch {
                if (!_state.value.ready || _state.value.busy) return@launch
                _state.update { it.copy(busy = true, reply = "", error = null) }
                try {
                    val replies = checkNotNull(conversation).sendMessageAsync(text)
                    replies.collect { message ->
                        _state.update { it.copy(reply = it.reply + message) }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _state.update { it.copy(error = e.message) }
                } finally {
                    _state.update { it.copy(busy = false) }
                }
            }
        }

        private suspend fun release() {
            _state.update { it.copy(ready = false) }
            while (_state.value.busy) {
                conversation?.cancelProcess()
                delay(100)
            }
            conversation?.close()
            conversation = null
            engine?.close()
            engine = null
            _state.value = ChatState()
        }

        override fun onCleared() {
            scope.launch {
                mutex.withLock { release() }
                executor.shutdown()
            }.invokeOnCompletion { scope.cancel() }
        }
    }

`sendMessageAsync(text)` returns a `Flow<Message>` of chunks, and `toString`
gives a chunk's text. `sendMessage(text)` blocks and returns the whole reply.
The conversation keeps its history, so the next `send` continues the chat.
`busy` keeps the button disabled until the flow completes.

To set the sampling, pass `SamplerConfig(topK = 10, topP = 0.95,
temperature = 0.8)` in `ConversationConfig`. All three parameters are required,
and `topP` and `temperature` are `Double`.

`load` with the model path that is already loaded does nothing, whatever
backend it asks for. A second call after a rotation keeps the loaded model,
also after the app has fallen back to another backend. `send` runs only when
the state is `ready` and not `busy`, the same test as the button. A tap that
lands while `release` stops a reply does nothing. A tap queued behind a
`load` reaches the model that `load` loaded when no reply was streaming.

### Step 6: Wire the Compose screen

`viewModel` and `collectAsStateWithLifecycle` come from
`androidx.lifecycle:lifecycle-viewmodel-compose` and
`androidx.lifecycle:lifecycle-runtime-compose` (2.10.0):

<br />

    import androidx.compose.foundation.layout.Column
    import androidx.compose.foundation.rememberScrollState
    import androidx.compose.foundation.verticalScroll
    import androidx.compose.material3.Button
    import androidx.compose.material3.Text
    import androidx.compose.material3.TextField
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.LaunchedEffect
    import androidx.compose.runtime.getValue
    import androidx.compose.runtime.mutableStateOf
    import androidx.compose.runtime.remember
    import androidx.compose.runtime.setValue
    import androidx.compose.ui.Modifier
    import androidx.lifecycle.compose.collectAsStateWithLifecycle
    import androidx.lifecycle.viewmodel.compose.viewModel
    import com.google.ai.edge.litertlm.Backend

    @Composable
    fun ChatScreen(modelPath: String, viewModel: ChatViewModel = viewModel()) {
        val state by viewModel.state.collectAsStateWithLifecycle()
        var input by remember { mutableStateOf("") }
        LaunchedEffect(modelPath) { viewModel.load(modelPath, Backend.GPU()) }
        Column {
            Text(
                state.reply,
                Modifier.weight(1f).verticalScroll(rememberScrollState())
            )
            state.error?.let { Text(it) }
            TextField(value = input, onValueChange = { input = it })
            Button(
                onClick = { viewModel.send(input); input = "" },
                enabled = state.ready && !state.busy
            ) {
                Text("Send")
            }
        }
    }

### Step 7: Manage the engine lifecycle

- Keep the engine in the `ViewModel` for as long as its screen lives. When several screens share the engine, keep it in an application-scoped holder.
- The `ViewModel` keeps its own thread and scope. `viewModelScope` is cancelled before `onCleared` runs, so a close launched there wouldn't run. `onCleared` cancels `scope` once the close is done.
- `release` turns `ready` off and calls `cancelProcess` until the streaming reply's flow has ended. The flow ends at the latest when the reply ends on its own. `release` then closes the conversation and the engine, each once.
- `onCleared` and a `load` that replaces the model both call `release` under the `Mutex`, one at a time, because `release` suspends while a reply stops.
- To start a fresh chat, close the conversation and create a new one with `createConversation`. Keep the engine loaded.

## Post-implementation steps for the user

Report the steps that apply to the user after the code compiles:

- When Path A in Step 3 didn't run, copy the model to the device with the two `adb` commands from Path A.
- When the app uses Path C in Step 3, create the AI pack module that holds the `.litertlm` file and upload the app to Play Console.
- When no `.litertlm` file exists for the model, convert the model on a workstation. This skill doesn't convert models.
- When the user asks for a complete reference app, point them to [Google AI Edge Gallery](https://github.com/google-ai-edge/gallery/blob/main/README.md).

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| First reply is slow. | `initialize` ran on first use. | Load at app start and set `cacheDir`. |
| First GPU reply ends in `Can not find OpenCL library on this device`. | The manifest lacks the `uses-native-library` entries. | Add both entries from Step 1 inside `<application>`. |
| `close` throws `IllegalStateException`. | The engine or conversation was closed twice, or `initialize` threw. | Store the engine only after `initialize` returns, and set each field to `null` after `close`. |
| Build fails with `[Hilt] Provided Metadata instance has version 2.4.0`. | Hilt 2.59.2 can't read the Kotlin 2.4 metadata in `litertlm-android` 0.18.0. | Follow Path A or Path B in Step 1. |

## Checklist

- \[ \] Is `minSdk` set to `24` or higher and is `com.google.ai.edge.litertlm:litertlm-android:0.18.0` added in `app/build.gradle.kts`?
- \[ \] For the GPU backend, does `AndroidManifest.xml` declare `libvndksupport.so` and `libOpenCL.so` with `android:required="false"` inside `<application>`?
- \[ \] Is the `.litertlm` file loaded from an absolute path on the device's file system, never from `assets/`?
- \[ \] Do `initialize`, `sendMessageAsync`, `cancelProcess`, and `close` all run on the same single-thread background executor?
- \[ \] Is the conversation closed before the engine, each exactly once?
- \[ \] Does the project compile with `./gradlew assembleDebug`?

## Antipatterns

- **Never** call `Engine.initialize` on the Android main thread.
- **Never** place a `.litertlm` file in `assets/` or ship it inside the APK.
- **Never** call `close` on an engine whose `initialize` threw.
- **Never** close the engine while a reply is streaming; call `cancelProcess` until the flow ends first.
- **Never** close `Engine` or `Conversation` from `viewModelScope` in `onCleared`; that scope is already cancelled.
