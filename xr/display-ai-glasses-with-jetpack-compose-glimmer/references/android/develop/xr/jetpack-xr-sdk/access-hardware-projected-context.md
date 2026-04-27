Applicable XR devices This guidance helps you build experiences for these types of XR devices. [Learn about XR device types →](https://developer.android.com/develop/xr/devices) ![](https://developer.android.com/static/images/develop/xr/ai-glasses-icon.svg) AI Glasses [](https://developer.android.com/develop/xr/devices#ai-glasses) [Learn about XR device types →](https://developer.android.com/develop/xr/devices)

<br />

After you've [requested and been granted the necessary permissions](https://developer.android.com/develop/xr/jetpack-xr-sdk/request-hardware-permissions), your app
can access the AI glasses hardware. The key to accessing the glasses' hardware
(instead of the phone's hardware), is to use a [projected context](https://developer.android.com/develop/xr/jetpack-xr-sdk/ai-glasses/support-different-types#projected-context).

There are two primary ways to get a projected context, depending on where your
code is executing:

## Get a projected context if your code is running in an AI glasses activity

If your app's code is running from within your AI glasses activity, its own
activity context is already a projected context. In this scenario, calls made
within that activity can already access the glasses' hardware.

## Get a projected context for code running in a phone app component

If a part of your app outside of your AI glasses activity (such as a phone
activity or a service) needs to access the glasses' hardware, it must explicitly
obtain a projected context. To do this, use the
[`createProjectedDeviceContext()`](https://developer.android.com/reference/kotlin/androidx/xr/projected/ProjectedContext#createProjectedDeviceContext(android.content.Context)) method:


```kotlin
@OptIn(ExperimentalProjectedApi::class)
private fun getGlassesContext(context: Context): Context? {
    return try {
        // From a phone Activity or Service, get a context for the AI glasses.
        ProjectedContext.createProjectedDeviceContext(context)
    } catch (e: IllegalStateException) {
        Log.e(TAG, "Failed to create projected device context", e)
        null
    }
}
```

<br />

### Check for validity

Wrap the [`createProjectedDeviceContext`](https://developer.android.com/reference/kotlin/androidx/xr/projected/ProjectedContext#createProjectedDeviceContext(android.content.Context)) call within the
[`ProjectedContext.isProjectedDeviceConnected`](https://developer.android.com/reference/kotlin/androidx/xr/projected/ProjectedContext#isProjectedDeviceConnected(android.content.Context,kotlin.coroutines.CoroutineContext)). While this method returns
`true`, the projected context remains valid to the connected device, and your
phone app activity or service (such as a `CameraManager`) can access the
AI glasses hardware.

### Clean up on disconnect

The projected context is tied to the lifecycle of the connected device, so it is
destroyed when the device disconnects. When the device disconnects,
[`ProjectedContext.isProjectedDeviceConnected`](https://developer.android.com/reference/kotlin/androidx/xr/projected/ProjectedContext#isProjectedDeviceConnected(android.content.Context,kotlin.coroutines.CoroutineContext)) returns `false`. Your app
should listen for this change and clean up any system services (such as a
`CameraManager`) or resources that your app created using that projected
context.

### Re-initialize on reconnect

When the AI glasses device reconnects, your app can obtain another projected
context instance using [`createProjectedDeviceContext()`](https://developer.android.com/reference/kotlin/androidx/xr/projected/ProjectedContext#createProjectedDeviceContext(android.content.Context)), and then
re-initialize any system services or resources using the new projected context.

## Access audio using bluetooth

Currently, AI glasses connect to your phone as a standard Bluetooth audio
device. Both the headset and the A2DP (Advanced Audio Distribution Profile)
[profiles](https://developer.android.com/develop/connectivity/bluetooth/profiles) are supported. Using this approach lets any Android app that
supports audio input or output work on glasses, even if they were not
purposefully built to support glasses. In some cases, using Bluetooth might work
better for your app's use case as an alternative to accessing the glasses'
hardware using a projected context.

As with any standard Bluetooth audio device, the permission to grant the
[`RECORD_AUDIO`](https://developer.android.com/reference/kotlin/android/Manifest.permission#record_audio) permission is controlled by the phone and not the glasses.

## Capture an image with the AI glasses' camera

To capture an image with the AI glasses' camera, set up and bind the CameraX's
[`ImageCapture`](https://developer.android.com/reference/kotlin/androidx/camera/core/ImageCapture) [use case](https://developer.android.com/media/camera/camerax#ease-of-use) to the glasses' camera using the correct
context for your app:


```kotlin
private fun startCameraOnGlasses(activity: ComponentActivity) {
    // 1. Get the CameraProvider using the projected context.
    // When using the projected context, DEFAULT_BACK_CAMERA maps to the AI glasses' camera.
    val projectedContext = try {
        ProjectedContext.createProjectedDeviceContext(activity)
    } catch (e: IllegalStateException) {
        Log.e(TAG, "AI Glasses context could not be created", e)
        return
    }

    val cameraProviderFuture = ProcessCameraProvider.getInstance(projectedContext)

    cameraProviderFuture.addListener({
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        // 2. Check for the presence of a camera.
        if (!cameraProvider.hasCamera(cameraSelector)) {
            Log.w(TAG, "The selected camera is not available.")
            return@addListener
        }

        // 3. Query supported streaming resolutions using Camera2 Interop.
        val cameraInfo = cameraProvider.getCameraInfo(cameraSelector)
        val camera2CameraInfo = Camera2CameraInfo.from(cameraInfo)
        val cameraCharacteristics = camera2CameraInfo.getCameraCharacteristic(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
        )

        // 4. Define the resolution strategy.
        val targetResolution = Size(1920, 1080)
        val resolutionStrategy = ResolutionStrategy(
            targetResolution,
            ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER
        )
        val resolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(resolutionStrategy)
            .build()

        // 5. If you have other continuous use cases bound, such as Preview or ImageAnalysis,
        // you can use  Camera2 Interop's CaptureRequestOptions to set the FPS
        val fpsRange = Range(30, 60)
        val captureRequestOptions = CaptureRequestOptions.Builder()
            .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
            .build()

        // 6. Initialize the ImageCapture use case with options.
        val imageCapture = ImageCapture.Builder()
            // Optional: Configure resolution, format, etc.
            .setResolutionSelector(resolutionSelector)
            .build()

        try {
            // Unbind use cases before rebinding.
            cameraProvider.unbindAll()

            // Bind use cases to camera using the Activity as the LifecycleOwner.
            cameraProvider.bindToLifecycle(
                activity,
                cameraSelector,
                imageCapture
            )
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }, ContextCompat.getMainExecutor(activity))
}
```

<br />

### Key points about the code

- Obtains an instance of the [`ProcessCameraProvider`](https://developer.android.com/reference/kotlin/androidx/camera/lifecycle/ProcessCameraProvider) using the [**projected device context**](https://developer.android.com/develop/xr/jetpack-xr-sdk/access-hardware-projected-context#phone-activity-service).
- Within the projected context's scope, the AI glasses' primary, outward-pointing camera maps to the `DEFAULT_BACK_CAMERA` when selecting a camera.
- A pre-binding check uses [`cameraProvider.hasCamera(cameraSelector)`](https://developer.android.com/reference/kotlin/androidx/camera/lifecycle/ProcessCameraProvider#hasCamera(androidx.camera.core.CameraSelector)) to verify that the selected camera is available on the device before proceeding.
- Uses **Camera2 Interop** with [`Camera2CameraInfo`](https://developer.android.com/reference/kotlin/androidx/camera/camera2/interop/Camera2CameraInfo) to read the underlying [`CameraCharacteristics#SCALER_STREAM_CONFIGURATION_MAP`](https://developer.android.com/reference/kotlin/android/hardware/camera2/CameraCharacteristics#scaler_stream_configuration_map), which can be useful for advanced checks on supported resolutions.
- A custom [`ResolutionSelector`](https://developer.android.com/reference/kotlin/androidx/camera/core/resolutionselector/ResolutionSelector) is built to precisely control the output image resolution for [`ImageCapture`](https://developer.android.com/reference/kotlin/androidx/camera/core/ImageCapture).
- Creates an `ImageCapture` use case that is configured with a custom `ResolutionSelector`.
- Binds the `ImageCapture` use case to the activity's lifecycle. This automatically manages the opening and closing of the camera based on the activity's state (for example, stopping the camera when the activity is paused).

After the AI glasses' camera is set up, you can capture an image with the
CameraX's `ImageCapture` class. Refer to the CameraX's documentation to learn
about using [`takePicture()`](https://developer.android.com/media/camera/camerax/take-photo#take_a_picture) to [capture an image](https://developer.android.com/media/camera/camerax/take-photo#take_a_picture).

### Capture a video with the AI glasses' camera

To capture a video instead of an image with the AI glasses' camera, replace the
`ImageCapture` components with the corresponding [`VideoCapture`](https://developer.android.com/reference/kotlin/androidx/camera/video/VideoCapture) components
and modify the capture execution logic.

The main changes involve using a different use case, creating a different output
file, and initiating the capture using the appropriate video recording method.
For more information about the `VideoCapture` API and how to use it, see the
[CameraX's video capture documentation](https://developer.android.com/media/camera/camerax/video-capture#using-videocapture-api).

> [!IMPORTANT]
> **Important:** Battery power and thermal dissipation on AI glasses devices is often limited. When developing for glasses, pay close attention to the requested **output format** and **resolution**, as these can severely impact system power and temperature.

The following table shows the recommended resolution and frame rate depending on
your app's use case:

| Use case | Resolution | Frame rate |
|---|---|---|
| Video Communication | 1280 x 720 | 15 FPS |
| Computer Vision | 640 x 480 | 10 FPS |
| AI Video Streaming | 640 x 480 | 1 FPS |

## Access a phone's hardware from an AI glasses activity

An AI glasses activity can also access the phone's hardware (such as the camera
or microphone) by using [`createHostDeviceContext(context)`](https://developer.android.com/reference/kotlin/androidx/xr/projected/ProjectedContext#createHostDeviceContext(android.content.Context)) to get the host
device's (phone) context:


```kotlin
@OptIn(ExperimentalProjectedApi::class)
private fun getPhoneContext(activity: ComponentActivity): Context? {
    return try {
        // From an AI glasses Activity, get a context for the phone.
        ProjectedContext.createHostDeviceContext(activity)
    } catch (e: IllegalStateException) {
        Log.e(TAG, "Failed to create host device context", e)
        null
    }
}
```

<br />

When accessing hardware or resources that are specific to the host device
(phone) in a hybrid app (an app containing both mobile and AI glasses
experiences), you must explicitly select the correct context to make sure your
app can access the correct hardware:

- Use the [`Activity`](https://developer.android.com/reference/kotlin/android/app/Activity) context from the phone `Activity` or the [`ProjectedContext.createHostDeviceContext()`](https://developer.android.com/reference/kotlin/androidx/xr/projected/ProjectedContext#createHostDeviceContext(android.content.Context)) to get the phone's context.
- Don't use [`getApplicationContext()`](https://developer.android.com/reference/kotlin/android/content/Context#getapplicationcontext) because the application context can incorrectly return the AI glasses' context if a glasses activity was the most-recently-launched component.