LiteRT in Google Play services runs the same `CompiledModel` API as the bundled
runtime of the skill, without the LiteRT runtime in the APK. The
`com.google.ai.edge.litert` classes (`CompiledModel`, `TensorBuffer`,
`Accelerator`, and `Environment`) are the same; only three things change:

- **Dependency** : `com.google.android.gms:play-services-litert` replaces `com.google.ai.edge.litert:litert` and `litert-gpu`.
- **Environment** : `LiteRt.initialize(context, options)` returns the `Environment` as a `Task`, in place of `Environment.create`.
- **GPU** : The initialization options enable the GPU with `setEnableGpu(true)`.

When the user asks about Terms of Service, data use, or APIs, see
[LiteRT in Google Play services](https://developers.google.com/edge/litert/android/play_services).

## Choose the runtime

Use the first row that matches the app:

| The app | Runtime |
|---|---|
| Needs the NPU, custom operators, or a Support library class without a replacement in the skill (such as `MetadataExtractor`) | Bundled runtime of the skill. Tell the user why. |
| The user wants to control when the app picks up a new LiteRT version (Google Play services updates the runtime without an app release) | Bundled runtime of the skill. |
| Targets only devices with Google Play services, or already uses `play-services-tflite-*` | Play services runtime. |
| Also targets devices without Google Play services, such as builds for app stores other than Google Play | Bundled runtime of the skill, which runs on every device. Tell the user that a separate product flavor or app target on the Play services runtime is an option for Google Play. |
| None of the preceding rows, and the user didn't name a runtime | Play services runtime. |

You must never put `play-services-litert` and `com.google.ai.edge.litert:litert`
in the same build variant (such as both on `implementation`). Both share
`litert-api`, so the compile step passes while that variant's APK silently
carries both runtimes. Don't split the project into product flavors unless the
user asks for them: keep a single build on the bundled runtime, and tell the
user that a separate product flavor (with flavor-specific dependencies and
`Environment` source sets) or a separate app target is an option for Google
Play.

## Gradle

In `app/build.gradle.kts`, remove `com.google.ai.edge.litert:litert` and
`litert-gpu`, and add:

    android {
        defaultConfig {
            minSdk = 24
        }
    }

    dependencies {
        implementation("com.google.android.gms:play-services-litert:17.0.0")
        // For Task.await() in coroutines.
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    }

Use version `17.0.0` exactly, also in `gradle/libs.versions.toml`, and run
`./gradlew assembleDebug` (not `--dry-run`, which doesn't resolve
dependencies) right after you change the dependencies:

- When the error names another version of `play-services-litert`, fix the version to `17.0.0` and build again.
- Only when it reports `Could not find
  com.google.android.gms:play-services-litert:17.0.0`, Google Maven doesn't serve the artifact. Then restore the bundled runtime of Step 1 of the skill, keep `Environment.create`, skip the rest of this file, and tell the user that LiteRT in Google Play services isn't available yet. Do this also when the user asked for Google Play services: the bundled runtime is the only one that builds, and the build must pass. Never assume this without running the build.
- Never add another Maven repository to find the artifact.

`play-services-litert` brings `com.google.ai.edge.litert:litert-api` with it,
so don't declare `litert-api`. The GPU needs no other artifact. The artifact
also requires:

- **`minSdk` 24** : A lower `minSdk` fails at manifest merging.
- **Hilt 2.60.1 or later** : `play-services-litert` brings `kotlin-stdlib` 2.4.0, whose Kotlin 2.4 metadata Hilt 2.59.2 can't read (`hiltJavaCompileDebug`: "Provided Metadata instance has version 2.4.0"). Raise Hilt to 2.60.1 and tell the user. The app's Kotlin plugin doesn't need to change.

Remove `play-services-tflite-java`, `play-services-tflite-gpu`, and
`play-services-tflite-support` (the last two bring the first): next to
`play-services-litert`, they fail the build with duplicate classes such as
`com.google.android.gms.tflite.java.TfLite`.

## Create the Environment

One `Environment` serves every model in the process and stays open. You must
add `PlayServicesEnvironment` to the app in every change to the Play services
runtime, also when nothing calls the migrated class yet: an `environment`
constructor parameter alone leaves the app without a way to get the
`Environment`. Create it on the executor of Step 3 of the skill, never on the
main thread:

    import android.content.Context
    import com.google.ai.edge.litert.Environment
    import com.google.android.gms.litert.LiteRt
    import com.google.android.gms.litert.client.LiteRtInitializationOptions
    import kotlinx.coroutines.sync.Mutex
    import kotlinx.coroutines.sync.withLock
    import kotlinx.coroutines.tasks.await

    object PlayServicesEnvironment {
        private val mutex = Mutex()
        private var environment: Environment? = null

        suspend fun get(context: Context): Environment = mutex.withLock {
            environment ?: LiteRt.initialize(
                context.applicationContext,
                LiteRtInitializationOptions.builder().setEnableGpu(true).build()
            ).await().also { environment = it }
        }
    }

`setEnableGpu` is `false` by default; without it, `CompiledModel.create` with
`Accelerator.GPU` fails. When the device can't load the GPU accelerator,
`initialize` still succeeds, and `create` with `Accelerator.GPU` throws
`LiteRtException`, which the CPU fallback of Step 3 handles. On some devices
without the Play services GPU module, `initialize` with `setEnableGpu(true)`
takes tens of seconds or longer before it succeeds, so keep the UI state in
loading until the task completes. When Google
Play services can't provide the runtime, the task fails; show the error in the
UI state.

Never call `Environment.create` with the Play services runtime: the APK has
no LiteRT runtime library. Don't call `env.getAvailableAccelerators`: on
devices with an older Play services LiteRT module, it crashes the process with
`SIGBUS`; rely on `CompiledModel.create` with the CPU fallback instead. Use the
`LiteRtInitializationOptions.builder` form, not the
`initializationOptions { }` Kotlin function of `LiteRtDsl`, which stops the
Kotlin compiler with an internal error in `17.0.0`.

## Changes to Steps 2 and 3

- **Step 2** : Remove the `environment` property and give `Classifier` an `environment: Environment` constructor parameter, which `CompiledModel.create` receives. When no `ViewModel` caller retries on the CPU as in Step 3, pass `CompiledModel.Options(accelerator, Accelerator.CPU)` so the model runs on the CPU when the Play services GPU module isn't loaded. Everything else in Step 2 stays.
- **Step 3** : In `load`, get the `Environment` before creating the `Classifier`, inside the same `scope.launch`:

    val environment = try {
        PlayServicesEnvironment.get(appContext)
    } catch (e: Exception) {
        _uiState.value = UiState(error = e.message)
        return@launch
    }
    classifier = Classifier(appContext, preferred, environment)

Pass the same `environment` to the CPU retry. The GPU-to-CPU fallback, the
buffers, `infer`, and `close` stay as in the skill.

## Migrate to the Play services runtime

### From the bundled CompiledModel runtime

1. Check the first table row. When it matches, stop and tell the user.
2. Swap the dependencies as in "Gradle".
3. Replace `Environment.create` with `PlayServicesEnvironment`, and pass the `Environment` to every `CompiledModel.create` call. When a `create` call omits the `Environment`, add it.
4. Keep the buffer and inference code. When no caller retries on the CPU as in Step 3, pass `Accelerator.CPU` next to `Accelerator.GPU` in `CompiledModel.Options`.

### From the bundled TensorFlow Lite Interpreter

Follow the migration guide of the skill. In its step 3, add the dependencies of
"Gradle" instead of the bundled ones, and in its step 5, add
`PlayServicesEnvironment` of "Create the Environment" and create the
`Classifier` with its `Environment`. For a signature call, pass
the `List<TensorBuffer>` lists from `createInputBuffers(signature)` and
`createOutputBuffers(signature)` to `model.run(inputs, outputs, signature)`,
not `Map<String, TensorBuffer>`, which aborts with `SIGABRT` in `17.0.0`
looking for `libLiteRt.so`. The Play services runtime
doesn't support `litert-support`, so replace every Support class, including the
ones under `com.google.ai.edge.litert.support`, with `preprocess` of Step 4 of
the skill and labels read from assets.

### From the Interpreter API in Google Play services

The app uses `com.google.android.gms:play-services-tflite-java`, `-gpu`, or
`-support` with `TfLite.initialize` and `InterpreterApi`. Capture a golden
output and verify as in the migration guide of the skill, and replace:

| Interpreter API in Google Play services | CompiledModel on Google Play services |
|---|---|
| `play-services-tflite-java`, `-gpu`, `-support` | `play-services-litert` (see "Gradle") |
| `TfLite.initialize(context, TfLiteInitializationOptions...)` | `PlayServicesEnvironment.get(context)` |
| `setEnableGpuDelegateSupport(true)` | `setEnableGpu(true)` |
| `InterpreterApi.create(model, InterpreterApi.Options().setRuntime(TfLiteRuntime.FROM_SYSTEM_ONLY))` | `Classifier` of Step 2 with the Play services `Environment` |
| `TfLiteGpu.isGpuDelegateAvailable` and `GpuDelegateFactory` | `Accelerator.GPU` with the CPU fallback of Step 3 |
| `setNumThreads(n)` | `CompiledModel.Options(Accelerator.CPU)` with `cpuOptions = CompiledModel.CpuOptions(numThreads = n)` |
| `interpreter.run(input, output)` | `infer` of Step 2 |
| `play-services-tflite-support` classes | `preprocess` of Step 4 and labels read from assets |

Migrate to `CompiledModel`, not to the `Interpreter` of `play-services-litert`:
`play-services-litert` still contains `TfLite` and `InterpreterApi`, so most of
the legacy code compiles, but that `Interpreter` runs on the CPU only, and the
GPU classes (`TfLiteGpu`, `GpuDelegateFactory`, `addDelegateFactory`) no longer
resolve. Tell the user about this in the report.

## Report

After the build passes, tell the user:

- Which runtime the app uses, and why.
- That `play-services-litert` requires Google Play services on the device, and that the first `initialize` call downloads the runtime module when the device doesn't have it yet.
- What didn't migrate (NPU, custom operators, Support library classes).