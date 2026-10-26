This file gives the commands and code for each step of the migration summary in
the skill. The migrated app ends with the code of Steps 1 to 5 of the skill: the
`Classifier` helper of Step 2, the executor and CPU fallback of Step 3, and
`preprocess` of Step 4.

The code in this file uses these imports:

    import android.content.Context
    import com.google.ai.edge.litert.Accelerator
    import com.google.ai.edge.litert.CompiledModel
    import com.google.ai.edge.litert.TensorBuffer
    import com.google.ai.edge.litert.support.image.TensorImage
    import com.google.ai.edge.litert.support.tensorbuffer.TensorBufferFloat
    import java.io.File
    import kotlin.math.abs

## 1. Find the runtime

Read `app/build.gradle.kts` (or `app/build.gradle`),
`gradle/libs.versions.toml`, and the output of `./gradlew :app:dependencies`,
which also shows what other libraries pull in. A legacy app gets the Interpreter
runtime from one of these:

- **TensorFlow Lite** : `org.tensorflow:tensorflow-lite` and the other `org.tensorflow:tensorflow-lite*` artifacts.
- **LiteRT 1.x** : `com.google.ai.edge.litert:litert`, `litert-gpu`, `litert-support`, or `litert-metadata` at a 1.x version.

When the app depends on the Interpreter API in Google Play services
(`com.google.android.gms:play-services-tflite-java`, `-gpu`, or `-support`, with
`TfLite.initialize` in the code), follow [LiteRT in Google Play services](https://developer.android.com/agents/skills/device-ai/litert/litert-runtime/references/play-services)
instead. When it depends on the TensorFlow Lite Task Library
(`org.tensorflow:tensorflow-lite-task-vision`, `-text`, or `-audio`), stop and
tell the user: this skill doesn't migrate it.

Then find the code that the later steps change:

    grep -rnE 'org\.tensorflow\.lite\.|Interpreter|GpuDelegate|CompatibilityList|NnApiDelegate|NNAPI|[nN]umThreads|FlexDelegate|TfLite' --include='*.kt' --include='*.java' app/src

Note which accelerator each code path uses (the GPU delegate, NNAPI, or the CPU
with a thread count), and whether the app uses the TensorFlow Lite Support
Library (`ImageProcessor`, `TensorImage`, `TensorLabel`, `MetadataExtractor`).

## 2. Capture a golden output first

The golden output is the legacy app's output for one fixed input, recorded
before any dependency changes. Step 6 compares the migrated app with it. Pick
one path:

- **Path A (device or emulator connected)** : Write an instrumented test with
  the project's existing test setup. The test feeds one deterministic input
  through the app's own inference class and preprocessing on the CPU, without
  the GPU or NNAPI delegate. Use a bitmap of another size than the model input,
  never a camera frame. Write the output with `writeGoldenOutput` to a file in
  `InstrumentationRegistry.getInstrumentation().targetContext.filesDir`. When
  the model has signatures, also log the legacy
  `interpreter.getSignatureInputs(key)` and `getSignatureOutputs(key)`: the
  migrated by-name calls need these names (Step 5). When the model has no
  signatures, log `interpreter.getInputTensor(i).name()` and
  `getOutputTensor(i).name()` instead. Don't run the capture test
  with `./gradlew connectedDebugAndroidTest`: it uninstalls the app when the
  test ends, which deletes the file. Install both APKs, run the test with
  `am instrument`, and copy the file out of the debuggable app:

      ./gradlew installDebug installDebugAndroidTest
      adb shell pm list instrumentation  # shows <test package>/<runner> for the app
      adb shell am instrument -w -e class <capture test class> <test package>/<runner>
      mkdir -p app/src/androidTest/assets/golden
      adb shell run-as <application ID> cat files/golden.txt > app/src/androidTest/assets/golden/output.txt

- **Path B (no device)**: Don't capture. Report the capture and the device
  comparison of Step 6 to the user as post-implementation steps.

    // Before the dependency swap: record the legacy app's output for one fixed
    // input, one value per line.
    fun writeGoldenOutput(file: File, output: FloatArray) {
        file.writeText(output.joinToString("\n"))
    }

    // After the migration: compare the migrated output with the recorded one.
    // A difference smaller than 1e-3 passes for outputs of order 1.
    fun maxAbsDifference(golden: FloatArray, migrated: FloatArray): Float {
        check(golden.size == migrated.size) { "Size ${migrated.size} != ${golden.size}" }
        return golden.indices.maxOf { abs(golden[it] - migrated[it]) }
    }

    // The golden file is an asset of the test APK: pass
    // InstrumentationRegistry.getInstrumentation().context, not targetContext,
    // whose assets throw FileNotFoundException for it.
    fun readGoldenOutput(testContext: Context, path: String = "golden/output.txt"): FloatArray =
        testContext.assets.open(path).bufferedReader().use { reader ->
            reader.readLines().filter { it.isNotBlank() }.map { it.toFloat() }.toFloatArray()
        }

## 3. Swap the dependencies

Remove every legacy artifact, also when another library pulls it in. The LiteRT
artifacts carry the `org.tensorflow.lite` runtime classes under their original
names, so a build that mixes old and new artifacts fails with duplicate classes.
Then add the dependencies of the runtime that Step 1 of the skill picks, with
`minSdk` 24 or higher:

- **Google Play services runtime (default)** : You must read [LiteRT in Google Play services](https://developer.android.com/agents/skills/device-ai/litert/litert-runtime/references/play-services) before you edit the build file. Add `com.google.android.gms:play-services-litert:17.0.0` and `org.jetbrains.kotlinx:kotlinx-coroutines-play-services`, and follow its fallback to the bundled runtime when Gradle can't find `play-services-litert:17.0.0`.
- **Bundled runtime** : Add `litert:2.3.0` and `litert-gpu:2.3.0`.

### Support library

On the Google Play services runtime, replace the Support library classes as
the Play services reference of the skill describes: `FileUtil` with asset
reads, `ImageProcessor` and `TensorImage` with `preprocess` of Step 4 of the
skill, and `TensorLabel` with labels paired with the output scores. On the
bundled runtime, an app on the TensorFlow Lite Support Library keeps it. The
library ships as LiteRT Support, with the same classes and behavior. When the
app uses `ImageProcessor`, `TensorImage`, `TensorLabel`, or `MetadataExtractor`,
you must read the [LiteRT Support migration guide](https://github.com/google-ai-edge/LiteRT/blob/main/litert/support_lib/MIGRATION_GUIDE.md) before swapping the
coordinates:

- `org.tensorflow:tensorflow-lite-support` or `litert-support` 1.x becomes `com.google.ai.edge.litert:litert-support:2.3.0`.
- `org.tensorflow:tensorflow-lite-support-api` or `litert-support-api` 1.x becomes `litert-support-api:2.3.0`, next to `litert:2.3.0`.
- `tensorflow-lite-metadata` or `litert-metadata` 1.x becomes `litert-metadata:2.3.0`. It depends on `com.google.flatbuffers:flatbuffers-java:25.2.10`; when the app pins an older `flatbuffers-java`, upgrade it.

Keep `litert:2.3.0` in the build file. Then rename the package in the sources,
the R8 and ProGuard rules, and any reflection strings:

    grep -rl 'org\.tensorflow\.lite\.support' --include='*.java' --include='*.kt' --include='*.pro' app/ \
      | xargs sed -i 's/org\.tensorflow\.lite\.support/com.google.ai.edge.litert.support/g'

An app that never used the Support library doesn't add it. Without the Support
library, load labels from assets:

    // Replaces FileUtil.loadLabels in an app that drops the Support library.
    fun loadLabels(context: Context, name: String = "labels.txt"): List<String> =
        context.assets.open(name).bufferedReader().use { it.readLines() }

### Toolchain

The 2.3.0 artifacts need JDK 17 or later for the Gradle build, `compileSdk`
35, AGP 8.6.0 or later, and the Kotlin plugin 2.3 or later. Their classes carry
Kotlin 2.4 metadata. Ask the user before editing `gradle/libs.versions.toml` or
the root `build.gradle.kts`:

- Kotlin 2.2 stops with `The binary version of its metadata is 2.4.0`. A project that stays on Kotlin 2.2 adds `freeCompilerArgs += "-Xskip-metadata-version-check"`.
- With Kotlin 2.x, a Compose app applies the `org.jetbrains.kotlin.plugin.compose` plugin in place of `composeOptions`.
- Hilt 2.59.2 stops at `hiltJavaCompileDebug` with `Provided Metadata instance
  has version 2.4.0`. Raise Hilt to 2.60.1 or later.

## 4. Replace the delegates

- **GPU delegate** (`GpuDelegate`, `GpuDelegateFactory`): Create the GPU option of the Step 2 helper with the CPU as a second accelerator. `CompatibilityList` has no counterpart; the CPU fallback of Step 3 covers a device whose GPU can't run the model.
- **NNAPI** (`NnApiDelegate`, `setUseNNAPI(true)`): NNAPI is deprecated from Android 15, and the `CompiledModel` API has no NNAPI accelerator. Remove the delegate, and map an NNAPI choice in the app to the GPU option. You must tell the user about this change in the final report, because the speed and the numerical results of a quantized model that ran on a DSP or an NPU through NNAPI change on the GPU. NPU acceleration is a separate topic that this skill doesn't cover ([NPU acceleration with LiteRT](https://ai.google.dev/edge/litert/next/npu)).
- **Select TensorFlow operators** (`FlexDelegate`): No LiteRT 2.x artifact contains the Flex delegate. This skill doesn't cover it. Tell the user that the model needs to be exported again without Select TensorFlow operators.

    // Replaces GpuDelegate: the CPU runs the operators that the GPU doesn't
    // support, as it did with the delegate. Keep the CPU fallback of Step 3 for a
    // model that the GPU can't compile at all.
    val gpuWithCpuFallback = CompiledModel.Options(Accelerator.GPU, Accelerator.CPU)

## 5. Rewrite the inference code

Never search and replace `Interpreter`. Move the model into the `Classifier`
helper of Step 2 of the skill, owned by the single-thread executor of Step 3.
Keep the app's own classes and their public functions as the callers.

- **Model loading** : `Interpreter` from `FileUtil.loadMappedFile`, and the Support `Model.createModel`, become `CompiledModel.create` in the helper. No overload takes a `ByteBuffer`.
- **Inference** : `run` on `ByteBuffer` objects becomes `writeFloat`, `run`, and `readFloat` on buffers that the helper creates once. Use `writeInt8` and `readInt8` for int8 and uint8 tensors.
- **Thread count** : `setNumThreads` becomes `CpuOptions(numThreads = n)` on new CPU options; combine `numThreads` with `xnnPackFlags = 0x23` and `xnnPackWeightCachePath` in the same `CpuOptions` when memory also matters.
- **Tensor shapes** : `getInputTensor` and its `shape` become a lookup by name. `getInputTensorType` throws `LiteRtException` for `uint8` tensors; for the size, use `getInputBufferRequirements(name, signature).bufferSize`, which is in bytes for every element type. `resizeInput` has no counterpart in 2.3.0.
- **Signatures** : `runSignature` becomes `run` with maps from the signature's input and output names to buffers. Every by-name call (`run` with maps, `getInputTensorType`, `getInputBufferRequirements`) takes the names in the signature, such as `x` or `args_0` and `output_0`, not the graph tensor names such as `serving_default_x:0`. When the model has no signatures, the names are its input and output tensor names (such as `images` and `Softmax`). `CompiledModel` has no call that lists them, so take them from the legacy code (Step 2). A missing or wrong name in an output map crashes the process (JNI abort) instead of throwing, so check the names before the first `run`.
- **Closing** : Closing the interpreter becomes the helper's `close`.

    // Replaces Interpreter.Options.setNumThreads. Create new options; never change
    // the shared CompiledModel.Options.CPU.
    fun cpuOptionsWithThreads(numThreads: Int): CompiledModel.Options =
        CompiledModel.Options(Accelerator.CPU).apply {
            cpuOptions = CompiledModel.CpuOptions(numThreads = numThreads)
        }

    // Replaces interpreter.getInputTensor(0).shape() for float tensors. Look the
    // tensor up by its signature name; this throws for uint8 tensors.
    fun inputShape(model: CompiledModel, inputName: String): List<Int>? =
        model.getInputTensorType(inputName).layout?.dimensions

    // Replaces interpreter.getInputTensor(0).numBytes() for any element type.
    fun inputSizeBytes(model: CompiledModel, inputName: String, signature: String): Int =
        model.getInputBufferRequirements(inputName, signature).bufferSize

    // Replaces interpreter.runSignature. Create the buffers once with
    // model.createInputBuffers(signature) and model.createOutputBuffers(signature),
    // and reuse them for every call. The map keys are the signature's input and
    // output names (such as "args_0" and "output_0").
    fun runSignature(
        model: CompiledModel,
        signature: String,
        inputs: Map<String, TensorBuffer>,
        outputs: Map<String, TensorBuffer>
    ) {
        model.run(inputs, outputs, signature)
    }

### With the Support library

Migrated code must not import any `org.tensorflow.lite` class, including
`org.tensorflow.lite.DataType`. The LiteRT artifacts keep these classes only for
backward compatibility. Apply these rules to the Support code:

- `ImageProcessor`, `TensorImage`, and `FileUtil` stay under `com.google.ai.edge.litert.support` when the calls don't need `DataType`.
- The Support `Model` class runs on the Interpreter API. Replace it with the `Classifier` helper of Step 2 of the skill.
- Replace a Support call that needs `DataType`, such as `TensorImage(DataType.FLOAT32)`, `TensorBuffer.createFixedSize`, or `TensorLabel`, with `preprocess` of Step 4 or with the `CompiledModel` buffers.

The following code writes a processed image into the helper's buffers and pairs
the output scores with the labels, without `DataType`:

    // Replaces the Support library's Model class. ImageProcessor and TensorImage
    // stay; the processed image goes into the CompiledModel buffers directly, and
    // the scores pair with the labels without TensorLabel.
    fun runWithSupportImage(
        model: CompiledModel,
        inputs: List<TensorBuffer>,
        outputs: List<TensorBuffer>,
        image: TensorImage,
        inputElementCount: Int,
        labels: List<String>
    ): Map<String, Float> {
        // A uint8 image (one that skipped NormalizeOp) in a float input yields NaN.
        val pixels = (image.tensorBuffer as? TensorBufferFloat)?.floatArray
            ?: error("Normalize the image to float first")
        // writeFloat accepts a shorter array without an error.
        check(pixels.size == inputElementCount) { "Input size ${pixels.size}" }
        inputs[0].writeFloat(pixels)
        model.run(inputs, outputs)
        return labels.zip(outputs[0].readFloat().toList()).toMap()
    }

### Without the Support library

`preprocess` of Step 4 of the skill replaces the code that filled the input
`ByteBuffer`. Give it the model's input size and the legacy normalization: the
`mean` and `std` of a legacy `NormalizeOp(mean, std)`, or the app's own formula,
in the same channel order and layout.

## 6. Verify in two stages

**Stage 1, on the workstation** : `./gradlew assembleDebug` passes, and neither
of the following commands prints anything. The build alone doesn't reveal
leftover Interpreter code, because the `litert` 2.3.0 artifact still contains
`org.tensorflow.lite.Interpreter`:

    grep -rnE 'org\.tensorflow\.lite\.|GpuDelegate|NnApiDelegate|FlexDelegate|support\.model\.Model|org\.tensorflow:tensorflow-lite' --include='*.kt' --include='*.java' --include='*.kts' --include='*.gradle' --include='*.toml' --include='*.pro' --exclude-dir=build --exclude-dir=.gradle .
    ./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep -E 'org\.tensorflow:tensorflow-lite|com\.google\.ai\.edge\.litert:litert[a-z-]*:1\.'

**Stage 2, on a device**: Pick one path:

- **Path A (golden output captured in Step 2)** : Extend the instrumented test of Step 2. Run the same input through the migrated preprocessing and the helper on `Accelerator.CPU`, and read the golden file with `readGoldenOutput(InstrumentationRegistry.getInstrumentation().context)`. The test must fail when `maxAbsDifference` is 1e-3 or more. When the golden file holds only one value, the top-1 index must match; a wrong normalization often still passes this check. Then compare the CPU and the GPU as in Step 5 of the skill; on the bundled runtime, first check that Logcat shows `Dynamically loaded GPU accelerator` and not `GPU accelerator
  could not be loaded`, because the GPU and CPU option runs on the CPU without an error when `litert-gpu` is missing.
- **Path B (no device)**: Run Stage 1 only, and report that Stage 2 didn't run.

## Out of scope

This skill doesn't migrate the
TensorFlow Lite Task Library (`tensorflow-lite-task-vision`, `-text`, or
`-audio`), which has no LiteRT release.

This skill doesn't migrate C++ code. When the app has `CMakeLists.txt`,
`Android.mk`, or `.cpp` files that link `tensorflowlite_jni`, tell the user that
the C++ module moves to the LiteRT C++ SDK, and point them to
[Use prebuilt C++ library from LiteRT Maven](https://ai.google.dev/edge/litert/next/android_cpp_sdk).