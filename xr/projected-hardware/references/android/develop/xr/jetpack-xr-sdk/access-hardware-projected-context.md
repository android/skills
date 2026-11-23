Applicable XR devices This guidance helps you build experiences for these types of XR devices. [Learn about XR device types →](https://developer.android.com/develop/xr/devices) ![](https://developer.android.com/static/images/develop/xr/ai-glasses-icon.svg) Audio \&  
Display Glasses [](https://developer.android.com/develop/xr/devices#audio-display) [Learn about XR device types →](https://developer.android.com/develop/xr/devices)

<br />

After you've [requested and been granted the necessary permissions](https://developer.android.com/develop/xr/jetpack-xr-sdk/request-hardware-permissions), your app
can access the hardware on the audio glasses or display glasses. The key to
accessing the glasses' hardware (instead of the phone's hardware), is to use a
[projected context](https://developer.android.com/develop/xr/jetpack-xr-sdk/glasses/support-different-types#projected-context).

There are two primary ways to get a projected context, depending on where your
code is executing:

## Get a projected context if your code is running in an projected activity

If your app's code is running from within your [projected activity](https://developer.android.com/develop/xr/jetpack-xr-sdk/glasses/support-different-types#activity-lifecycle), its own
activity context is already a [projected context](https://developer.android.com/develop/xr/jetpack-xr-sdk/glasses/support-different-types#projected-context). In this scenario, calls
made within that activity can already access the glasses' hardware.

## Get a projected context for code running in a phone app component

If a part of your app outside of your projected activity (such as a phone
activity or a service) needs to access the glasses' hardware, it must explicitly
obtain a projected context. To do this, use the
[`createProjectedDeviceContext`](https://developer.android.com/reference/kotlin/androidx/xr/projected/ProjectedContext#createProjectedDeviceContext(android.content.Context)) method:


```kotlin
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@OptIn(ExperimentalProjectedApi::class, ExperimentalCoroutinesApi::class)
fun monitorProjectedDeviceConnectivity(
    context: Context,
    onConnected: suspend (projectedContext: Context) -> (() -> Unit)?
) {
    val lifecycleScope = (context as? LifecycleOwner)?.lifecycleScope
        ?: CoroutineScope(Dispatchers.Main + SupervisorJob())

    lifecycleScope.launch {
        var cleanupAction: (() -> Unit)? = null

        try {
            // Before creating a projected context, verify that the projected device
            // is connected to the host device (phone).
            // While this flow collects true, the active projected context remains valid.
            ProjectedContext.isProjectedDeviceConnected(context, coroutineContext)
                .distinctUntilChanged()
                .collectLatest { isConnected ->
                    // Clean up any previous session resources prior to state transitions.
                    cleanupAction?.invoke()
                    cleanupAction = null

                    if (!isConnected) {
                        Log.i(TAG, "Projected device disconnected. Cleaning up hardware resources...")
                        return@collectLatest
                    }

                    // Inside a phone Activity or Service, create a projected context.
                    // Note: If you are already inside a projected Activity, the default context
                    // is already a projected context.
                    val projectedContext = runCatching {
                        ProjectedContext.createProjectedDeviceContext(context)
                    }.onFailure { e ->
                        Log.e(TAG, "Failed to create projected context", e)
                    }.getOrNull() ?: return@collectLatest

                    // Use the new, valid `projectedContext` to initialize system services (e.g., CameraManager).
                    Log.i(TAG, "Projected device connected. Initializing hardware...")
                    cleanupAction = onConnected(projectedContext)
                }
        } finally {
            // Guarantee final resource cleanup on Activity destruction or scope cancellation.
            cleanupAction?.invoke()
            cleanupAction = null
        }
    }
}
```

<br />

### Check for validity

Wrap the [`createProjectedDeviceContext`](https://developer.android.com/reference/kotlin/androidx/xr/projected/ProjectedContext#createProjectedDeviceContext(android.content.Context)) call within the
[`ProjectedContext.isProjectedDeviceConnected`](https://developer.android.com/reference/kotlin/androidx/xr/projected/ProjectedContext#isProjectedDeviceConnected(android.content.Context,kotlin.coroutines.CoroutineContext)). While this method returns
`true`, the projected context remains valid to the connected device, and your
phone app activity or service (such as a `CameraManager`) can access the glasses
hardware.

### Clean up on disconnect

The projected context is tied to the lifecycle of the connected device, so it is
destroyed when the device disconnects. When the device disconnects,
[`ProjectedContext.isProjectedDeviceConnected`](https://developer.android.com/reference/kotlin/androidx/xr/projected/ProjectedContext#isProjectedDeviceConnected(android.content.Context,kotlin.coroutines.CoroutineContext)) returns `false`. Your app
should listen for this change and clean up any system services (such as a
`CameraManager`) or resources that your app created using that projected
context.

### Re-initialize on reconnect

When the glasses reconnect, your app can obtain another projected context
instance using [`createProjectedDeviceContext`](https://developer.android.com/reference/kotlin/androidx/xr/projected/ProjectedContext#createProjectedDeviceContext(android.content.Context)), and then re-initialize any
system services or resources using the new projected context.

## Record audio with the glasses' microphone

You can record audio from the glasses using Bluetooth Hands-Free Profile (HFP).
In this mode, the glasses connect to the phone using standard Headset and
Advanced Audio Distribution Profile (A2DP) profiles, functioning like a typical
Bluetooth peripheral. If your app is already designed for standard Bluetooth
recording, you can use this method to record audio from the glasses without any
additional development.

### Record audio using Bluetooth HFP

To record audio using Bluetooth HFP, first request the required runtime
permissions, and then record the audio using the [`AudioManager`](https://developer.android.com/reference/kotlin/android/media/AudioManager) API, as
described in the following sections.

#### Request permissions

As with any standard Bluetooth audio device, the [`RECORD_AUDIO`](https://developer.android.com/reference/kotlin/android/Manifest.permission#record_audio),
[`BLUETOOTH_CONNECT`](https://developer.android.com/reference/kotlin/android/Manifest.permission#bluetooth_connect), and other related permissions are controlled by the
phone and not the connected device (such as audio glasses or display glasses).

Follow these steps to request the permissions:

1. [Declare following permissions](https://developer.android.com/develop/xr/jetpack-xr-sdk/request-hardware-permissions#declare-permissions) in your app's manifest file:

   - [`RECORD_AUDIO`](https://developer.android.com/reference/kotlin/android/Manifest.permission#record_audio)
   - [`BLUETOOTH_CONNECT`](https://developer.android.com/reference/kotlin/android/Manifest.permission#bluetooth_connect)
   - [`MODIFY_AUDIO_SETTINGS`](https://developer.android.com/reference/kotlin/android/Manifest.permission#modify_audio_settings)
2. Request both the `RECORD_AUDIO` and `BLUETOOTH_CONNECT` permissions at
   runtime using the [standard Android permission flow](https://developer.android.com/training/permissions/requesting).

#### Use AudioManager to route audio

After the user has granted your app the necessary runtime permissions, use the
[`AudioManager`](https://developer.android.com/reference/kotlin/android/media/AudioManager) API to set the communication device to
[`TYPE_BLUETOOTH_SCO`](https://developer.android.com/reference/kotlin/android/media/AudioDeviceInfo#type_bluetooth_sco) to route the audio through Bluetooth HFP. This
directs the system to retrieve audio from the Bluetooth peripheral.


```kotlin
val audioManager = context.getSystemService(AudioManager::class.java) ?: return
val hfpDevice = audioManager.availableCommunicationDevices
    .firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO } ?: return

// HFP input is limited to 16 kHz, with mono or stereo channel configurations.
val sampleRate = 16_000
val channelMask = AudioFormat.CHANNEL_IN_MONO
val encoding = AudioFormat.ENCODING_PCM_16BIT

// Use a small multiple of the minimum buffer size to keep chunks short for low latency.
val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelMask, encoding)
if (minBufferSize <= 0) return

val audioRecord = AudioRecord.Builder()
    .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
    .setAudioFormat(
        AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setChannelMask(channelMask)
            .setEncoding(encoding)
            .build()
    )
    .setBufferSizeInBytes(minBufferSize * 2)
    .build()

try {
    // Route voice communication audio through the Bluetooth device.
    if (!audioManager.setCommunicationDevice(hfpDevice)) return
    audioRecord.startRecording()
    // Read audio with audioRecord.read() on a background thread.
} finally {
    // Stop and release when done.
    if (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) audioRecord.stop()
    audioRecord.release()
    audioManager.clearCommunicationDevice()
}
```

<br />

> [!NOTE]
> **Note:** To learn more about Bluetooth connectivity, refer to our [documentation](https://developer.android.com/develop/connectivity/bluetooth).

## Capture an image with the glasses' camera

To capture an image with the glasses' camera, monitor connectivity with
[`monitorProjectedDeviceConnectivity`](https://developer.android.com/develop/xr/jetpack-xr-sdk/access-hardware-projected-context#phone-activity-service) to obtain a projected device
context, create an isolated [`LifecycleCameraProvider`](https://developer.android.com/reference/kotlin/androidx/camera/lifecycle/LifecycleCameraProvider) bound to that
context, listen for camera presence, and bind CameraX's [`ImageCapture`](https://developer.android.com/reference/kotlin/androidx/camera/core/ImageCapture)
[use case](https://developer.android.com/media/camera/camerax#ease-of-use).

First, monitor device connectivity and initialize
[`LifecycleCameraProvider.createInstance`](https://developer.android.com/reference/kotlin/androidx/camera/lifecycle/LifecycleCameraProvider#createInstance(android.content.Context,androidx.camera.core.CameraXConfig)) with a single-stream
[`CameraXConfig.Builder`](https://developer.android.com/reference/kotlin/androidx/camera/core/CameraXConfig.Builder):


```kotlin
@OptIn(ExperimentalCameraProviderConfiguration::class)
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@SuppressLint("RestrictedApi")
fun setUpGlassesCamera(activity: ComponentActivity) {
    monitorProjectedDeviceConnectivity(activity) { projectedContext ->
        val cameraManager = projectedContext.getSystemService(CameraManager::class.java)
            ?: run {
                Log.e(TAG, "CameraManager not available on projected context")
                return@monitorProjectedDeviceConnectivity null
            }

        // `setRepeatingStreamForced()` is currently internal but will be public soon.
        val cameraConfig = CameraXConfig.Builder
            .fromConfig(Camera2Config.defaultConfig())
            .setRepeatingStreamForced(false)
            .build()

        val cameraProvider = try {
            // Acquire CameraX provider specifically bound to the projected context.
            // If the device disconnects while suspending, collectLatest cancels this coroutine.
            LifecycleCameraProvider.createInstance(projectedContext, cameraConfig)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to initialize projected camera services", e)
            return@monitorProjectedDeviceConnectivity null
        }

        // Set up the listener to react to camera availability changes asynchronously.
        val availabilityCallback = registerAvailabilityListener(
            cameraManager = cameraManager,
            cameraProvider = cameraProvider,
            executor = ContextCompat.getMainExecutor(activity),
            onCameraAvailable = { cameraId ->
                // Delegate camera selection, resolution querying, and use-case binding.
                bindGlassesImageCapture(cameraProvider, cameraId, activity)
            }
        )

        // Return a clean-up closure invoked automatically by monitorProjectedDeviceConnectivity
        // on disconnect, coroutine cancellation, or Activity destruction.
        return@monitorProjectedDeviceConnectivity {
            Log.i(TAG, "Projected camera setup scope ended. Tearing down resources...")
            try {
                cameraManager.unregisterAvailabilityCallback(availabilityCallback)
            } finally {
                cameraProvider.unbindAll()
            }
        }
    }
}
```

<br />

On CameraX 1.6.0 and higher, register a [`CameraPresenceListener`](https://developer.android.com/reference/kotlin/androidx/camera/core/CameraPresenceListener) using
[`addCameraPresenceListener`](https://developer.android.com/reference/kotlin/androidx/camera/core/CameraProvider#addCameraPresenceListener(java.util.concurrent.Executor,androidx.camera.core.CameraPresenceListener)) to react when the glasses' camera is added or
removed:


```kotlin
@OptIn(ExperimentalCameraProviderConfiguration::class)
fun registerAvailabilityListener(
    cameraManager: CameraManager,
    cameraProvider: LifecycleCameraProvider,
    executor: Executor,
    onCameraAvailable: (cameraId: String) -> Unit
): CameraManager.AvailabilityCallback {
    val callback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraAvailable(cameraId: String) {
            super.onCameraAvailable(cameraId)
            Log.i(TAG, "Glasses camera available (ID: $cameraId). Binding use case...")
            // Fires only after the OS finishes re-registering the glasses' camera.
            // Pass the fresh cameraId directly---never cache it, as it may change when recreated.
            onCameraAvailable(cameraId)
        }

        override fun onCameraUnavailable(cameraId: String) {
            super.onCameraUnavailable(cameraId)
            Log.i(TAG, "Glasses camera unavailable (ID: $cameraId). Releasing resources...")
            // Gracefully release the pipeline when the projected camera becomes unavailable.
            cameraProvider.unbindAll()
        }
    }

    cameraManager.registerAvailabilityCallback(executor, callback)
    return callback
}
```

<br />

Next, select the glasses' camera from [`availableCameraInfos`](https://developer.android.com/reference/kotlin/androidx/camera/core/CameraProvider#availableCameraInfos()) using the
[`CameraIdentifier`](https://developer.android.com/reference/kotlin/androidx/camera/core/CameraIdentifier) provided by the listener and bind `ImageCapture`:


```kotlin
/**
 * Selects the projected camera matching dynamic [cameraId] from
 * [LifecycleCameraProvider.availableCameraInfos], verifies availability, and binds an
 * [ImageCapture] use case targeting a 16:9 resolution to [lifecycleOwner].
 *
 * Note: Depending on your application's needs, [ImageCapture] can be swapped out for other
 * standard CameraX use cases:
 * - Use [ImageAnalysis] for real-time streaming workloads (e.g., OCR, AI vision, or VPS).
 * - Use [VideoCapture] for video recording pipelines.
 */
@OptIn(ExperimentalCamera2Interop::class, ExperimentalCameraProviderConfiguration::class)
fun bindGlassesImageCapture(
    cameraProvider: LifecycleCameraProvider,
    cameraId: String,
    lifecycleOwner: LifecycleOwner,
    preferredResolution: Size = Size(1920, 1080)
): ImageCapture? {
    return try {
        // Discover available cameras dynamically via `availableCameraInfos`
        // Narrow it down by the dynamic `cameraId` because the projected camera's ID may
        // change when recreated
        val cameraInfo = cameraProvider.availableCameraInfos.firstOrNull { info ->
            Camera2CameraInfo.from(info).cameraId == cameraId
        }

        // Pre-binding check: Verify the selected camera is registered and available on the device.
        // The projected camera may be unavailable (for example, if the user took off the glasses).
        if (cameraInfo == null) {
            Log.w(TAG, "Glasses camera (ID: $cameraId) is not currently available.")
            return null
        }
        val cameraSelector = cameraInfo.cameraSelector

        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(
                ResolutionStrategy(
                    preferredResolution,
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER
                )
            )
            .build()

        // Initialize the ImageCapture use case.
        val imageCapture = ImageCapture.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()

        // Unbind any previously bound use cases before rebinding to the lifecycle owner.
        cameraProvider.unbindAll()

        // Bind the configured use case. You can swap `imageCapture` out for an `ImageAnalysis`
        // (e.g., from `setupCameraStreamingFps`) or `VideoCapture` instance here to match your use case.
        // `bindToLifecycle()` can throw IllegalArgumentException if use cases
        // are incompatible, or IllegalStateException if the LifecycleOwner is destroyed.
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            imageCapture
        )
        Log.i(TAG, "ImageCapture successfully bound for camera ID $cameraId")
        imageCapture
    } catch (exc: CameraInfoUnavailableException) {
        Log.e(TAG, "Unable to access camera info for ID $cameraId", exc)
        null
    } catch (exc: IllegalArgumentException) {
        Log.e(TAG, "Invalid camera configuration or use case combination for ID $cameraId", exc)
        null
    } catch (exc: IllegalStateException) {
        Log.e(TAG, "Camera is already bound to another lifecycle or closed", exc)
        null
    }
}
```

<br />

### Key points about the code

- **Per-device `LifecycleCameraProvider`:** Use `LifecycleCameraProvider.createInstance` to create an isolated camera provider scoped to the active projected context. Because the projected context's `deviceId` can change when the glasses disconnect and reconnect while your app process stays alive, creating a per-device provider ensures CameraX binds to the active connected device.
- **Listen for camera presence:** On CameraX 1.6.0 and higher, register a [`CameraPresenceListener`](https://developer.android.com/reference/kotlin/androidx/camera/core/CameraPresenceListener) using [`cameraProvider.addCameraPresenceListener`](https://developer.android.com/reference/kotlin/androidx/camera/core/CameraProvider#addCameraPresenceListener(java.util.concurrent.Executor,androidx.camera.core.CameraPresenceListener)) instead of opening the camera immediately when the glasses connect. Asynchronous setup between the glasses and host must finish before the OS finishes recreating the glasses' camera, and the glasses' camera can also disappear dynamically while connected (such as during thermal mitigation).
  - **Camera addition (`onCamerasAdded`):** Fires after [`availableCameraInfos`](https://developer.android.com/reference/kotlin/androidx/camera/core/CameraProvider#availableCameraInfos()) is updated with the added camera, avoiding timing races during setup.
  - **Camera removal (`onCamerasRemoved`):** Tracks camera presence rather than in-use state, so unbinding there releases resources only when the glasses' camera is removed instead of reacting when your own app opens the camera.
- **Clean up on disconnect:** The clean-up closure returned to [`monitorProjectedDeviceConnectivity`](https://developer.android.com/develop/xr/jetpack-xr-sdk/access-hardware-projected-context#phone-activity-service) calls [`removeCameraPresenceListener`](https://developer.android.com/reference/kotlin/androidx/camera/core/CameraProvider#removeCameraPresenceListener(androidx.camera.core.CameraPresenceListener)) and `unbindAll()` when the glasses disconnect or the activity is destroyed.
- **Dynamic camera selection:** Matches the [`CameraIdentifier`](https://developer.android.com/reference/kotlin/androidx/camera/core/CameraIdentifier) passed to `onCamerasAdded` against `availableCameraInfos` using `isOf` to select the glasses' camera dynamically.
- **Custom resolution:** Builds a [`ResolutionSelector`](https://developer.android.com/reference/kotlin/androidx/camera/core/resolutionselector/ResolutionSelector) targeting an example 16:9 aspect ratio and your preferred resolution (falling back to the closest lower supported size) for `ImageCapture`. Supported aspect ratios and resolutions vary across glasses devices and modes (such as 4:3 and 16:9), so query [`StreamConfigurationMap`](https://developer.android.com/reference/kotlin/android/hardware/camera2/params/StreamConfigurationMap) if you need to check exact supported sizes.
- **Lifecycle binding:** Binds `ImageCapture` to the `LifecycleOwner`, automatically managing camera open and close transitions with the activity's lifecycle.
- **Configuring for single-stream hardware:** The glasses' camera pipeline is constrained to a single active stream at a time. When `ImageCapture` is bound alone, CameraX attaches an internal repeating stream (`MeteringRepeating`) for focus and metering by default, which causes the single-stream pipeline to stall. Pass [`setRepeatingStreamForced(false)`](https://developer.android.com/reference/kotlin/androidx/camera/core/CameraXConfig.Builder#setRepeatingStreamForced(kotlin.Boolean)) in the `CameraXConfig` passed to `LifecycleCameraProvider.createInstance` so `ImageCapture` can bind without a preview stream.

After the glasses' camera is set up, you can capture an image with the CameraX's
`ImageCapture` class. Refer to the CameraX's documentation to learn about using
[`takePicture`](https://developer.android.com/media/camera/camerax/take-photo#take_a_picture) to [capture an image](https://developer.android.com/media/camera/camerax/take-photo#take_a_picture).

## Capture a stream with the glasses' camera

To capture streams instead of an image, replace the `ImageCapture` component
from the [capture image example](https://developer.android.com/develop/xr/jetpack-xr-sdk/access-hardware-projected-context#capture-image) with [`VideoCapture`](https://developer.android.com/reference/kotlin/androidx/camera/video/VideoCapture) or
[`ImageAnalysis`](https://developer.android.com/reference/kotlin/androidx/camera/core/ImageAnalysis) components and change the capture logic.

For more information about these APIs how to use them, see the [CameraX video
capture](https://developer.android.com/media/camera/camerax/video-capture#using-videocapture-api) and [CameraX image analysis](https://developer.android.com/media/camera/camerax/analyze) documentation.

### Choose a resolution and frame rate

You should query the device to find its supported resolutions and their
corresponding maximum frame rates. The following table lists examples of values
you might see, depending on your app's use case. To conserve battery power and
reduce heat on the glasses, choose the lowest resolution and frame rate that
work for your use case.

| Use case | Resolution | Frame rate |
|---|---|---|
| Text recognition (OCR) or world sensing | 1920 x 1080 | 1 FPS |
| AI video streaming (multimodal assistants) | 1024 x 768 | 1 FPS |
| Localization and visual positioning (VPS) | 640 x 360 or 640 x 480 | 10 FPS |
| Video calling | 640 x 360 or 640 x 480 | 24 FPS |

> [!IMPORTANT]
> **Important:** Supported resolutions and their maximum frame rates vary by device and can change. You should query them at runtime instead of hardcoding them.

### Query the maximum frame rate for each resolution

[`CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES`](https://developer.android.com/reference/kotlin/android/hardware/camera2/CameraCharacteristics#CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) reports the device's overall
frame rate bounds, which can be higher than the streaming rate that a given
resolution supports. To get the actual maximum for each resolution on audio and
display glasses, read its minimum frame duration and convert it to FPS with
[`StreamConfigurationMap.getOutputMinFrameDuration(format, size)`](https://developer.android.com/reference/kotlin/android/hardware/camera2/params/StreamConfigurationMap#getOutputMinFrameDuration(int,%20android.util.Size)):


```kotlin
@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
fun getMaxFpsBySize(cameraInfo: CameraInfo, format: Int): Map<Size, Int> {
    val map = Camera2CameraInfo.from(cameraInfo).getCameraCharacteristic(
        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
    ) ?: return emptyMap()

    return map.getOutputSizes(format).associateWith { size ->
        val minFrameDurationNs = map.getOutputMinFrameDuration(format, size)
        if (minFrameDurationNs > 0) (1_000_000_000L / minFrameDurationNs).toInt() else 0
    }
}
```

<br />

Next, use that value to calculate the `minFps` and `maxFps` bounds for your
target frame rate range. Cap the lower bound (`minFps`) at the resolution's
maximum streaming frame rate calculated from `getOutputMinFrameDuration`, and
set a higher upper bound (`maxFps`) as a catch-up window:


```kotlin
val maxFpsBySize = getMaxFpsBySize(cameraInfo, ImageFormat.YUV_420_888)
val maxSupportedFps = maxFpsBySize[size] ?: return ImageAnalysis.Builder().build()

// minFps is the target streaming frame rate and should be at most maxSupportedFps.
val minFps = minOf(desiredFps, maxSupportedFps)
// Set the upper bound to 2x minFps so the stream can catch up if frames are delayed.
val maxFps = minFps * 2
```

<br />

#### Key points about the code

- Use the format that matches your use case. For [`ImageAnalysis`](https://developer.android.com/reference/kotlin/androidx/camera/core/ImageAnalysis), that's [`YUV_420_888`](https://developer.android.com/reference/kotlin/android/graphics/ImageFormat#YUV_420_888).
- For [`VideoCapture`](https://developer.android.com/reference/kotlin/androidx/camera/video/VideoCapture), use the `Class` overloads instead: [`getOutputSizes(MediaRecorder::class.java)`](https://developer.android.com/reference/kotlin/android/hardware/camera2/params/StreamConfigurationMap#getOutputSizes(java.lang.Class%3CT%3E)) and [`getOutputMinFrameDuration(MediaRecorder::class.java, size)`](https://developer.android.com/reference/kotlin/android/hardware/camera2/params/StreamConfigurationMap#getOutputMinFrameDuration(java.lang.Class%3CT%3E,%20android.util.Size)).
- Specify a range where `maxFps` is greater than `minFps` (such as `minFps*2`) rather than setting both bounds to the same value like `Range(minFps,
  minFps)`. The lower bound (`minFps`) sets the target streaming frame rate and should be at most the value calculated from `getOutputMinFrameDuration`, while the upper bound (`maxFps`) acts as a catch-up window for delayed frames to prevent persistent latency. For example, \[1,2\] or \[1,3\] for a 1 FPS stream, and \[24,30\] or \[24,48\] for a 24 FPS stream.

> [!NOTE]
> **Note:** Using the lower bound as the streaming frame rate reflects current device behavior and might change on future devices.

### Configure the frame rate

To configure the frame rate range based on your CameraX use case:


```kotlin
// Create and return the ImageAnalysis builder with proper frame rate ranges
// ImageAnalysis: OCR, AI streaming, and VPS
val analysisBuilder = ImageAnalysis.Builder()
    .setResolutionSelector(resolutionSelector)

Camera2Interop.Extender(analysisBuilder)
    .setCaptureRequestOption(
        CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
        Range(minFps, maxFps)
    )
return analysisBuilder.build()
```

<br />

This example shows configuration for both `VideoCapture` and `ImageAnalysis`,
but you can implement whichever use cases your app supports.

### Understand how the stream behaves

Keep the following system behaviors in mind while working with capture streams:

- **Unsupported resolutions are rejected**: If you request a size that isn't in the list, Camera2 rejects the configuration before the stream opens.
- **Requesting frame rates above the maximum**: If you request a minimum frame rate above the maximum supported for your resolution, the session still opens, but the target frame rate from the glasses is clamped to that supported maximum while the upper bound continues to act as the catch-up rate limit. For example, requesting \[30,60\] at 640 x 480 clamps the target frame rate to 24 FPS (behaving like \[24,60\]).
- **Thermal throttling**: When the glasses get warm, the frame rate drops but the resolution stays the same. Build your app to handle lower or uneven frame rates, for example by checking frame timestamps rather than assuming a fixed rate.

> [!NOTE]
> **Note:** There's a known issue where the camera stream is interrupted without the phone camera permissions. Request the phone camera permissions in addition to the glasses' permissions to avoid this issue.

## Access a phone's hardware from a projected activity

A [projected activity](https://developer.android.com/develop/xr/jetpack-xr-sdk/glasses/support-different-types#activity-lifecycle) can also access the phone's hardware (such as the
camera or microphone) by using [`createHostDeviceContext(context)`](https://developer.android.com/reference/kotlin/androidx/xr/projected/ProjectedContext#createHostDeviceContext(android.content.Context)) to get
the host device's (phone) context:


```kotlin
@OptIn(ExperimentalProjectedApi::class)
fun getHostDeviceContext(context: Context): Context {
    // Obtain the host device (phone) context.
    // The `context` argument passed in can be any context (projected, host, Service, or App).
    return ProjectedContext.createHostDeviceContext(context)
}
```

<br />

When accessing hardware or resources that are specific to the host device
(phone) in a hybrid app (an app containing both mobile and glasses experiences),
you must explicitly select the correct context to make sure your app can access
the correct hardware:

- Use the [`Activity`](https://developer.android.com/reference/kotlin/android/app/Activity) context from the phone `Activity` or the [`ProjectedContext.createHostDeviceContext`](https://developer.android.com/reference/kotlin/androidx/xr/projected/ProjectedContext#createHostDeviceContext(android.content.Context)) to get the phone's context.
- Don't use [`getApplicationContext`](https://developer.android.com/reference/kotlin/android/content/Context#getapplicationcontext) because the application context can incorrectly return the glasses' context if a projected activity was the most-recently-launched component.