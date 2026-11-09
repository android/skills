This guide covers implementing low-light features using **Night mode
extensions** and **Low Light Boost (LLB)**.

## Choosing the right tool

| Feature | Used for | Implementation | UX impact |
|---|---|---|---|
| **Night mode** | High-quality stills | `ExtensionsManager` | Possibly requires user to hold still for several seconds. |
| **LLB, AE mode** | Real-time preview or video | `Camera2Interop`, CameraX utility | Hardware drops the frame rate to increase brightness. |
| **LLB, Play services** | Real-time preview and video | `SurfaceProcessor` | Software-based brightening; maintains a higher frame rate. |

*** ** * ** ***

## Night mode extension

CameraX Extensions provide access to the device's built-in computational
photography pipeline.

### Basic setup

To set up the extension, initialize the extension manager:


```kotlin
/** Binds Night mode with ExtensionSessionConfig (CameraX 1.6.0 and higher). */
suspend fun bindNightMode(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    cameraProvider: ProcessCameraProvider,
    preview: Preview,
    imageCapture: ImageCapture,
): Boolean {
    // Suspend version of getInstanceAsync (CameraX 1.6.0 and higher).
    val extensionsManager = ExtensionsManager.getInstance(context, cameraProvider)
    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    if (!extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.NIGHT)) {
        return false // Bind without the extension instead.
    }
    // ImageAnalysis isn't supported in extension sessions.
    val config = ExtensionSessionConfig.Builder(ExtensionMode.NIGHT, extensionsManager)
        .addUseCase(preview)
        .addUseCase(imageCapture)
        .build()
    cameraProvider.unbindAll()
    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, config)
    return true
}
```

<br />

### Comprehensive features

- **Image postview** : Display a low-resolution image immediately while the multi-frame processing occurs.

  ```kotlin
  fun buildPostviewImageCapture(): ImageCapture =
      ImageCapture.Builder()
          .setPostviewEnabled(true)
          .build()
  ```
- **Extension strength** : Let users control the intensity of the night effect.

  ```kotlin
  /** Sets the active extension's strength, from 0 to 100. */
  suspend fun setExtensionStrength(
      context: Context,
      cameraProvider: ProcessCameraProvider,
      camera: Camera,
      strength: Int,
  ) {
      val extensionsManager = ExtensionsManager.getInstance(context, cameraProvider)
      // Null when the bound camera has no active extension.
      extensionsManager.getCameraExtensionsControl(camera.cameraControl)
          ?.setExtensionStrength(strength)
  }
  ```
- **Capture progress** : Show a UI progress bar for long exposures.

  ```kotlin
  /**
   * Captures to a file and reports progress for long exposures. The callbacks
   * fire only when ImageCapture.getImageCaptureCapabilities(cameraInfo) reports
   * support.
   */
  suspend fun captureWithProgress(
      imageCapture: ImageCapture,
      outputOptions: ImageCapture.OutputFileOptions,
      onProgress: (percent: Int) -> Unit,
      onPostview: (Bitmap) -> Unit,
  ) {
      try {
          val result = imageCapture.takePicture(
              outputOptions,
              onCaptureProcessProgressed = onProgress,
              onPostviewBitmapAvailable = onPostview,
          )
          Log.d("CameraX", "Saved ${result.savedUri}")
      } catch (e: ImageCaptureException) {
          Log.e("CameraX", "Capture failed", e)
      }
  }
  ```

*** ** * ** ***

## Low-light boost

LLB is designed for preview and video streams where you prefer high frame rates.

### AE mode

The built-in CameraX way to prioritize brightness. It modifies the hardware's
auto-exposure algorithm.

- **Activation** : Use `CameraControl.enableLowLightBoostAsync`.
- **Implementation** :

  ```kotlin
  /**
   * Enables AE-mode Low Light Boost. Requires CameraX 1.5.0 or higher,
   * Android 15 (API level 35) or higher, and device support.
   */
  fun enableLowLightBoost(
      camera: Camera,
      lifecycleOwner: LifecycleOwner,
      executor: Executor,
  ): Boolean {
      // lowLightBoostState is always OFF on unsupported devices, so it can't
      // be used as the availability check.
      if (!camera.cameraInfo.isLowLightBoostSupported) return false

      val future = camera.cameraControl.enableLowLightBoostAsync(true)
      future.addListener({
          try {
              future.get()
          } catch (e: ExecutionException) {
              // IllegalStateException: unsupported or a settings conflict.
              Log.w("CameraX", "Low Light Boost not enabled", e.cause)
          }
      }, executor)

      camera.cameraInfo.lowLightBoostState.observe(lifecycleOwner) { state ->
          val isBoosting = state == LowLightBoostState.ACTIVE
          Log.d("CameraX", "Low Light Boost active: $isBoosting")
      }
      return true
  }
  ```
- **Monitoring** : Observe `CameraInfo.lowLightBoostState` to track when the hardware actively applies the enhancement.

### Google Play services LLB

It's a multi-step implementation that uses a session-based `SurfaceProcessor`.

**Dependency** : `com.google.android.gms:play-services-camera-low-light-boost`

To implement Google Play services LLB, follow these core steps:

1. **Initialize client** : `val client = LowLightBoost.getClient`.
2. **Implement `SurfaceProcessor`** :
   - **Manage session** : Call `client.createSession`.
   - **Forward required metadata** : Observe the camera's `TotalCaptureResult` stream and forward every result to the session: `session.processCaptureResult`.
   - **Provide surface** : Get the input surface from the session, `session.getCameraSurface`, and provide it to the camera's `SurfaceRequest`.
   - **Lifecycle** : Release the session, `session.release`, when the processor is closed or the `SurfaceRequest` completes.
3. **Wire using `CameraEffect`** :

   ```kotlin
   /** CameraEffect's constructors are protected, so subclass it. */
   class LowLightBoostEffect(
       surfaceProcessor: SurfaceProcessor,
       executor: Executor,
       onError: Consumer<Throwable>,
   ) : CameraEffect(
       CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE,
       executor,
       surfaceProcessor,
       onError,
   )

   fun buildLowLightBoostUseCaseGroup(
       preview: Preview,
       videoCapture: VideoCapture<Recorder>,
       llbSurfaceProcessor: SurfaceProcessor,
       executor: Executor,
   ): UseCaseGroup {
       val effect = LowLightBoostEffect(llbSurfaceProcessor, executor) { error ->
           Log.e("CameraX", "Low Light Boost effect failed", error)
       }
       // Bind the group with cameraProvider.bindToLifecycle(owner, selector, group).
       return UseCaseGroup.Builder()
           .addUseCase(preview)
           .addUseCase(videoCapture)
           .addEffect(effect)
           .build()
   }
   ```
4. **Scene detection** : Use `session.setSceneDetectorCallback` to receive `boostStrength` updates for real-time UI indicators.

*** ** * ** ***

## Implementation notes

- **Thread safety** : Always handle `ExtensionsManager` and `LowLightBoostClient` initialization asynchronously.
- **FPS trade-offs**: AE mode LLB often drops the frame rate significantly to increase brightness.
- **Compatibility** : Extensions, Night Mode, possibly conflict with `ConcurrentCamera`. Always verify support before binding.