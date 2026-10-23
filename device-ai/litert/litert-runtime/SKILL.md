---
name: litert-runtime
description: Configures and implements LiteRT (.tflite) on-device inference on Android
  in Kotlin using the CompiledModel API on CPU and GPU. Use this skill for any Android
  LiteRT or .tflite task, including Gradle setup, image classification or vision/audio/embedding
  helpers, TensorBuffer reuse, CPU memory optimization, Bitmap preprocessing, AndroidViewModel
  GPU-to-CPU fallback with warm-up on a single-thread dispatcher, LiteRT in Google
  Play services (play-services-litert), migrating from legacy org.tensorflow.lite.Interpreter,
  GpuDelegate, or play-services-tflite, and latency benchmarking. Don't use for running
  generative text LLMs with .litertlm files or for Python model conversion.
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-10-22'
  keywords:
  - LiteRT
  - CompiledModel
  - tflite
  - Android app
  - GPU
  - TensorBuffer
---

This skill provides step-by-step guidance for building an Android app that runs
a `.tflite` model with the [LiteRT CompiledModel API](https://ai.google.dev/edge/litert/android)
(`com.google.ai.edge.litert:litert` 2.x; [source repository](https://github.com/google-ai-edge/litert/blob/main/README.md)) on the CPU or
the GPU. Don't use the deprecated `org.tensorflow.lite.Interpreter` API. For
open text language models using `.litertlm` files, suggest the `litert-lm`
skill.

## Prerequisites

- A Kotlin Android project (`minSdk` must be `24` or higher, as declared by the LiteRT 2.3.0 AARs).
- The dependencies in the app-level `build.gradle.kts` file from Google Maven: `implementation("com.google.ai.edge.litert:litert:2.3.0")` and `implementation("com.google.ai.edge.litert:litert-gpu:2.3.0")`. From 2.3.0 the GPU accelerator is the second artifact: with `litert` alone the app builds, and `CompiledModel.create` with `CompiledModel.Options(Accelerator.GPU)` throws `LiteRtException` because the APK has no GPU accelerator library. With `CompiledModel.Options(Accelerator.GPU, Accelerator.CPU)` it doesn't throw: the model runs on the CPU while the app reports the GPU, and only Logcat shows it (`GPU accelerator could not be loaded and registered`). No manifest entry is needed: `litert-api`, which `litert` depends on, declares the OpenCL driver libraries (`uses-native-library`) in its own manifest.
- A verified `.tflite` model and its input contract (input dimensions, mean, standard deviation, channel order, and tensor layout). For models with a verified recipe, inspect the [LiteRT model recipes](https://github.com/google-ai-edge/litert-samples/blob/main/models/README.md).

## Critical mandates

- **Single-thread confinement** : One single-thread executor or confined coroutine dispatcher must own the `CompiledModel` instance. Creation, inference, and closing must execute on that same background thread, never on the main thread.
- **Buffer lifecycle** : Allocate `TensorBuffer` instances once using `createInputBuffers` and `createOutputBuffers`, reuse them across inferences, and close all input and output buffers before closing the `CompiledModel`.
- **Synchronous timing** : `model.run` enqueues work on the GPU and returns before GPU execution completes. Always measure `model.run` and output buffer readback (`outputs[0].readFloat`) together.
- **Warm-up and exception safety** : Execute one warm-up inference inside the helper constructor's `init` block so first-run GPU shader compilation doesn't block the first user interaction, and call `close` in `catch` before rethrowing so neither `CompiledModel` nor `TensorBuffer` handles leak if buffer allocation or warm-up throws.
- **Migration** : When the app already uses `org.tensorflow.lite` code, you must read the "Migrating from legacy TFLite Interpreter" section in full before you edit any file. Migrated code must not import any `org.tensorflow.lite` class (including `DataType`), `GpuDelegate` becomes `CompiledModel.Options(Accelerator.GPU, Accelerator.CPU)`, and an NNAPI choice maps to that same GPU option.

## Detailed steps

### Step 1: Configure Gradle and place the model file

Search the project for `libs.versions.toml`, `gradle.properties`,
`app/build.gradle.kts`, and any existing `org.tensorflow.lite` imports before
editing build files. Configure `minSdk` and add the LiteRT runtime and GPU
accelerator dependencies in `app/build.gradle.kts`:

    android {
        defaultConfig {
            minSdk = 24
        }
    }

    dependencies {
        implementation("com.google.ai.edge.litert:litert:2.3.0")
        implementation("com.google.ai.edge.litert:litert-gpu:2.3.0")
    }

Place bundled models at `app/src/main/assets/model.tflite` (AGP stores `.tflite`
files uncompressed by default). Deliver a model that exceeds the base APK size
limit through [Play for On-Device AI](references/android/google/play/on-device-ai.md) as an AI pack, or download it into
`context.filesDir` (`bundletool --local-testing` installs AI packs locally
without Google Play). Read an install-time AI pack through `AssetManager`. Load
a fast-follow or on-demand AI pack or a downloaded file from its on-disk
`filePath` using `CompiledModel.create(filePath,
CompiledModel.Options(accelerator), environment)` in place of the asset call in
Step 2. Obtain the pack's directory from the AI Delivery library
(`com.google.android.play:ai-delivery`) using
`AiPackManagerFactory.getInstance(context).getPackLocation(name)?.assetsPath()`,
which returns `null` until the pack has finished downloading.

With 2.3.0, nothing is added to `gradle.properties`: `litert` 2.3.0 declares
the namespace `com.google.ai.edge.litert.impl` and its dependency `litert-api`
2.3.0 declares `com.google.ai.edge.litert`, so AGP 9.x builds the project. With
2.2.0 or 2.1.6, both libraries declare `com.google.ai.edge.litert`, and AGP
9.x stops at `processDebugMainManifest` (AGP 8.x reports it as a warning and
builds); for those two versions, add `android.uniquePackageNames=false` to
`gradle.properties`, and remove it when moving to 2.3.0 because it also hides
the same clash between any other two libraries. Excluding `litert-api` instead
doesn't work: `CompiledModel` and `Accelerator` are in `litert-api`, and the
build then fails at `compileDebugKotlin`.

### Step 2: Implement the inference helper class

Isolate `CompiledModel` and its `TensorBuffer` instances in a dedicated
`AutoCloseable` helper class free of Android UI types:

    import android.content.Context
    import com.google.ai.edge.litert.Accelerator
    import com.google.ai.edge.litert.CompiledModel
    import com.google.ai.edge.litert.Environment
    import com.google.ai.edge.litert.TensorBuffer

    const val INPUT_SIZE = 224 * 224 * 3
    private val environment by lazy { Environment.create() }

    class Classifier(context: Context, accelerator: Accelerator) : AutoCloseable {
        private val model = CompiledModel.create(
            context.assets,
            "model.tflite",
            CompiledModel.Options(accelerator),
            environment
        )
        private val inputs = mutableListOf<TensorBuffer>()
        private val outputs = mutableListOf<TensorBuffer>()

        init {
            try {
                inputs += model.createInputBuffers()
                outputs += model.createOutputBuffers()
                infer(FloatArray(INPUT_SIZE))
            } catch (e: Exception) {
                close()
                throw e
            }
        }

        fun infer(input: FloatArray): FloatArray {
            inputs[0].writeFloat(input)
            model.run(inputs, outputs)
            return outputs[0].readFloat()
        }

        override fun close() {
            (inputs + outputs).forEach { it.close() }
            model.close()
        }
    }

`Accelerator.CPU` requires nothing from the device. `Accelerator.GPU` compiles
the graph for the GPU; when the GPU can't execute the model, the constructor
throws `LiteRtException` (from `create` for an operator the GPU doesn't support,
and on the Android emulator from `create` or from buffer allocation). A missing
asset or a file that isn't a valid model also throws `LiteRtException` from
`create`.

The buffers are created once with the model, reused for every inference, and
closed before the model. One `Environment` serves every model in the process and
stays open, so LiteRT loads the GPU accelerator library once rather than on
every `create` call. The constructor ends with one warm-up inference (the first
GPU run includes shader compilation) and closes the model and its buffers if any
step in `init` throws. `INPUT_SIZE` is the model's input element count:
`writeFloat` throws on a longer array and writes a shorter one without an error,
so a wrong size produces an incorrect result. For `int8` or `uint8` tensors, use
`writeInt8` and `readInt8` on `TensorBuffer` instead of `writeFloat` and
`readFloat`.

### Step 3: Wire the ViewModel and Compose screen

Drive the helper from a single-thread executor inside an `AndroidViewModel`,
fall back to `Accelerator.CPU` when GPU compilation fails, and expose immutable
state to the Compose UI:

    import android.app.Application
    import androidx.lifecycle.AndroidViewModel
    import com.google.ai.edge.litert.Accelerator
    import com.google.ai.edge.litert.LiteRtException
    import java.util.concurrent.Executors
    import kotlinx.coroutines.CoroutineScope
    import kotlinx.coroutines.SupervisorJob
    import kotlinx.coroutines.asCoroutineDispatcher
    import kotlinx.coroutines.cancel
    import kotlinx.coroutines.flow.MutableStateFlow
    import kotlinx.coroutines.flow.StateFlow
    import kotlinx.coroutines.flow.asStateFlow
    import kotlinx.coroutines.flow.update
    import kotlinx.coroutines.launch

    data class UiState(
        val ready: Boolean = false,
        val accelerator: Accelerator = Accelerator.CPU,
        val result: List<Float>? = null,
        val error: String? = null
    )

    class MainViewModel(app: Application) : AndroidViewModel(app) {
        private val executor = Executors.newSingleThreadExecutor()
        private val scope = CoroutineScope(
            SupervisorJob() + executor.asCoroutineDispatcher()
        )
        private var classifier: Classifier? = null
        private val _uiState = MutableStateFlow(UiState())
        val uiState: StateFlow<UiState> = _uiState.asStateFlow()

        fun load(preferred: Accelerator = Accelerator.GPU) {
            scope.launch {
                if (classifier != null) return@launch
                val appContext = getApplication<Application>()
                val selected = try {
                    classifier = Classifier(appContext, preferred)
                    preferred
                } catch (e: LiteRtException) {
                    if (preferred != Accelerator.CPU) {
                        try {
                            classifier = Classifier(appContext, Accelerator.CPU)
                            Accelerator.CPU
                        } catch (cpuError: LiteRtException) {
                            _uiState.value = UiState(error = cpuError.message)
                            return@launch
                        }
                    } else {
                        _uiState.value = UiState(error = e.message)
                        return@launch
                    }
                }
                _uiState.value = UiState(ready = true, accelerator = selected)
            }
        }

        fun classify(input: FloatArray) {
            scope.launch {
                try {
                    classifier?.infer(input)?.let { result ->
                        _uiState.update {
                            it.copy(result = result.toList(), error = null)
                        }
                    }
                } catch (e: LiteRtException) {
                    _uiState.update { it.copy(error = e.message) }
                }
            }
        }

        override fun onCleared() {
            scope.launch { classifier?.close() }.invokeOnCompletion {
                scope.cancel()
            }
            executor.shutdown()
        }
    }

One single-thread executor owns the model: creation, inference, and closing
execute only on it, never on the main thread. The `ViewModel` keeps its own
scope because `viewModelScope` is canceled before `onCleared` runs, so a close
launched there wouldn't run; `onCleared` cancels `scope` once `close` completes.
`load` is a no-op when a model is already loaded across configuration changes; a
failed `load` leaves `classifier` `null`. `classify` reports any
`LiteRtException` (in package `com.google.ai.edge.litert`) in `error` and keeps
the model.

The following Compose screen takes the picture (`bitmap`) from the photo picker
or CameraX and runs `preprocess` in the click handler (a few milliseconds for
224x224; move `preprocess` onto the executor for images exceeding 224x224).
`viewModel` and `collectAsStateWithLifecycle` come from
`androidx.lifecycle:lifecycle-viewmodel-compose` and
`androidx.lifecycle:lifecycle-runtime-compose` (2.10.0):

    import android.graphics.Bitmap
    import androidx.compose.foundation.layout.Column
    import androidx.compose.material3.Button
    import androidx.compose.material3.Text
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.LaunchedEffect
    import androidx.compose.runtime.getValue
    import androidx.lifecycle.compose.collectAsStateWithLifecycle
    import androidx.lifecycle.viewmodel.compose.viewModel
    import com.google.ai.edge.litert.Accelerator

    @Composable
    fun MainScreen(
        bitmap: Bitmap,
        viewModel: MainViewModel = viewModel()
    ) {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) { viewModel.load(Accelerator.GPU) }
        Column {
            state.error?.let { Text(it) }
            Button(
                onClick = { viewModel.classify(preprocess(bitmap)) },
                enabled = state.ready
            ) {
                Text("Run (${state.accelerator})")
            }
            state.result?.let { scores ->
                Text("Top class: ${scores.indices.maxBy { i -> scores[i] }}")
            }
        }
    }

### Step 4: Preprocess inputs to match the model contract

Convert the `Bitmap` into the model's input with `preprocess`. Match the
model's exact input dimensions, normalization mean and standard deviation,
channel order, and memory layout (this example uses NHWC, RGB, scaled to
`-1.0..1.0`). Mismatched `mean` or `std` values produce corrupted predictions
that look like a broken model:

    import android.graphics.Bitmap

    fun preprocess(
        bitmap: Bitmap,
        size: Int = 224,
        mean: Float = 127.5f,
        std: Float = 127.5f
    ): FloatArray {
        val scaled = checkNotNull(
            Bitmap.createScaledBitmap(bitmap, size, size, true)
                .copy(Bitmap.Config.ARGB_8888, false)
        )
        val pixels = IntArray(size * size).also {
            scaled.getPixels(it, 0, size, 0, 0, size, size)
        }
        val out = FloatArray(size * size * 3)
        pixels.forEachIndexed { i, p ->
            out[i * 3] = ((p shr 16 and 0xFF) - mean) / std
            out[i * 3 + 1] = ((p shr 8 and 0xFF) - mean) / std
            out[i * 3 + 2] = ((p and 0xFF) - mean) / std
        }
        return out
    }

The scaled bitmap is copied to `Bitmap.Config.ARGB_8888` because `ImageDecoder`
returns `Bitmap.Config.HARDWARE` bitmaps by default on Android 9 and higher
(including from a photo picker `Uri`), scaling preserves that config, and
`getPixels` can't read `HARDWARE` bitmaps; the copy is of the 224x224 scaled
bitmap rather than the full image. Scaling doesn't preserve the aspect ratio;
crop first if the model requires center cropping. An audio or embedding model
replaces `preprocess` with its own input encoding and keeps the inference class.

### Step 5: Verify output on a physical device

Run the app on a physical device that runs Android from Android Studio. Compare
the app's output with the model's reference output for one fixed input on
`Accelerator.CPU` first and then on `Accelerator.GPU`; when verifying model
outputs on a physical device, you must follow [device verification](references/verify.md).

## Migrating from legacy TFLite Interpreter

An existing app on `org.tensorflow.lite.Interpreter` (from
`org.tensorflow:tensorflow-lite*` or from `com.google.ai.edge.litert:litert`
1.x) moves to the `CompiledModel` code of Steps 1 to 5 in seven steps, in this
order. Before you change any file of an
existing app, you must read [migration](references/migration.md), which gives the commands and code
for each step:

1. **Find the runtime**: Read the build files and the resolved dependencies, and note the delegate, the thread count, and the Support library classes that the app uses.
2. **Capture a golden output first** : While the legacy runtime is still in the project, record the app's output for one fixed input on the CPU. This step needs a device or an emulator. Run the capture test with `am instrument`, not `connectedDebugAndroidTest`, which uninstalls the app and deletes the file.
3. **Swap the dependencies** : Remove every legacy artifact and add the dependencies of Step 1. An app on the Support library keeps it as `com.google.ai.edge.litert:litert-support:2.3.0`, with the package `org.tensorflow.lite.support` renamed to `com.google.ai.edge.litert.support` ([LiteRT Support migration guide](https://github.com/google-ai-edge/LiteRT/blob/main/litert/support_lib/MIGRATION_GUIDE.md)).
4. **Replace the delegates** : `GpuDelegate` becomes `CompiledModel.Options(Accelerator.GPU, Accelerator.CPU)`, so the CPU runs the operators that the GPU doesn't support. An NNAPI choice in the app maps to the same GPU option, not to `Accelerator.CPU` alone or to an NPU accelerator. `setNumThreads(n)` becomes new CPU options with `cpuOptions = CompiledModel.CpuOptions(numThreads = n)`.
5. **Rewrite the inference code** : Replace `Interpreter` and `FileUtil.loadMappedFile` with the `Classifier` helper of Step 2, created and run only on the single-thread executor of Step 3, never in a constructor that the main thread calls.
6. **Verify in two stages**: The build passes and the searches for legacy references print nothing; then the migrated output matches the golden output on a device.
7. **Report what changed and what didn't migrate** : When the app used NNAPI, tell the user that it now runs on the GPU with the CPU as a second accelerator, which changes the speed and the numerical results of a quantized model that ran on a DSP or an NPU before; point them to [NPU acceleration with LiteRT](https://ai.google.dev/edge/litert/next/npu) for the NPU. Tell the user about LiteRT in Google Play services, the TensorFlow Lite Task Library, C++ modules, and Select TensorFlow operators, which this skill doesn't migrate.

Migration mandates that apply to every migrated app:

- **No `org.tensorflow.lite` imports** : Migrated code must not import any `org.tensorflow.lite` class, including `org.tensorflow.lite.DataType`; the LiteRT artifacts keep these classes only for backward compatibility. Replace Support calls that need `DataType` (`TensorImage(DataType.FLOAT32)`, `TensorBuffer.createFixedSize`, and `TensorLabel`) with `preprocess` of Step 4, the `CompiledModel` buffers, and labels paired with the output scores. Remove every existing `org.tensorflow.lite` import line, not only the ones that you add.

## Antipatterns

- **Never** create, run, or close `CompiledModel` on the Android main thread.
- **Never** allocate new `TensorBuffer` instances on every inference call; failing to close per-frame `TensorBuffer` objects leaks C++ heap memory.
- **Never** benchmark `model.run` without including output buffer readback (`outputs[0].readFloat`).
- **Never** mix `org.tensorflow:tensorflow-lite*` artifacts (such as `org.tensorflow:tensorflow-lite-support`) with `com.google.ai.edge.litert:*` artifacts; both ship the `org.tensorflow.lite.*` classes and cause duplicate class build failures.
- **Never** use `Accelerator.GPU` on LiteRT 2.3.0 without adding the `com.google.ai.edge.litert:litert-gpu:2.3.0` dependency alongside `com.google.ai.edge.litert:litert:2.3.0`; with the CPU as a second accelerator, the model silently runs on the CPU.

## Checklist

- \[ \] Is `minSdk` set to `24` or higher and are both `com.google.ai.edge.litert:litert:2.3.0` and `com.google.ai.edge.litert:litert-gpu:2.3.0` added in `app/build.gradle.kts`?
- \[ \] Are all legacy `org.tensorflow:tensorflow-lite*` dependencies and `org.tensorflow.lite.*` imports removed?
- \[ \] Do `CompiledModel.create`, `model.run`, and `close` all execute on the same single-thread background dispatcher?
- \[ \] Are `TensorBuffer` instances allocated once, reused across calls, and closed before `model.close`, including when `init` or warm-up throws?
- \[ \] Does `load` execute one warm-up inference and fall back to `Accelerator.CPU` if `Accelerator.GPU` throws `LiteRtException`?
- \[ \] Does the project compile with `./gradlew assembleDebug`?
