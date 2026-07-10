Camera2 offers granular control but introduces boilerplate: managing
`CameraDevice` states, `CameraCaptureSession` lifecycles, background threads,
`HandlerThread`, and manual orientation calculations.

CameraX simplifies this by binding high-level `UseCase`s such as `Preview`,
`ImageCapture`, and `ImageAnalysis` directly to Android lifecycles, handling
thread management and device-specific workarounds automatically.

*** ** * ** ***

## Remove Camera2 boilerplate

Migrating to CameraX removes manual setup code:

1. **Delete manual thread handling** : Remove `HandlerThread`, `Handler`, and executors dedicated to camera background tasks. CameraX manages its own threads.
2. **Remove session and device callbacks** : Delete `CameraDevice.StateCallback` and `CameraCaptureSession.StateCallback` implementations.
3. **Remove surface management** : Remove manual routing of `Surface` objects from `TextureView` or `SurfaceView` to the camera device.
4. **Remove manual orientation logic** : Delete calculations involving `CameraCharacteristics.SENSOR_ORIENTATION` and display rotation for adjusting preview and capture orientation.

*** ** * ** ***

## Initialize `ProcessCameraProvider`

Request the `ProcessCameraProvider` and bind your use cases to the
`LifecycleOwner` activity or fragment. This replaces the
`CameraManager.openCamera` flow.


```kotlin
val context = LocalContext.current
val lifecycleOwner = LocalLifecycleOwner.current
LaunchedEffect(context, lifecycleOwner) {
  val cameraProvider = ProcessCameraProvider.getInstance(context).await()

  val cameraSelector = CameraSelector.Builder()
    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
    .build()

  val preview = Preview.Builder().build()
  val imageCapture = ImageCapture.Builder()
    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
    .build()

  // ImageAnalysis is common when migrating from Camera2 ImageReader
  val imageAnalysis = ImageAnalysis.Builder()
    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
    .build()

  cameraProvider.unbindAll()

  val camera = cameraProvider.bindToLifecycle(
    lifecycleOwner,
    cameraSelector,
    preview,
    imageCapture,
    imageAnalysis
  )
}
```

<br />

*** ** * ** ***

## Implement the preview and tap-to-focus

CameraX handles surface configuration automatically. Choose based on your UI
toolkit:

### Option A: For Android Views

Use `androidx.camera.view.PreviewView` in your layout, and bind it to the
`Preview` use case.


```kotlin
preview.setSurfaceProvider(previewView.surfaceProvider)
```

<br />

### Option B: For Jetpack Compose

Use `androidx.camera.compose.CameraXViewfinder`.


```kotlin
var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
val preview = remember {
  Preview.Builder().build().apply {
    setSurfaceProvider { request -> surfaceRequest = request }
  }
}
```

<br />

*** ** * ** ***

## Capture a photo

Replace `ImageReader` capture flows and
`CaptureRequest.Builder.TEMPLATE_STILL_CAPTURE` with the `ImageCapture` use
case. CameraX handles the rotation natively using the returned `ImageProxy`.


```kotlin
imageCapture.takePicture(
  cameraExecutor,
  object : ImageCapture.OnImageCapturedCallback() {
    override fun onCaptureSuccess(image: ImageProxy) {
      val buffer = image.planes[0].buffer
      val bytes = ByteArray(buffer.remaining())
      buffer.get(bytes)
      val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
      if (bitmap != null) {
        val matrix = Matrix()
        matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
        if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
          matrix.postScale(-1f, 1f)
        }

        val rotatedBitmap = Bitmap.createBitmap(
          bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
      }

      // MUST close proxy
      image.close()
    }

    override fun onError(exception: ImageCaptureException) {
      Log.e("CameraX", "Capture failed: ${exception.message}", exception)
    }
  }
)
```

<br />

*** ** * ** ***

## Implement image analysis

If you were using `ImageReader` in Camera2 to access raw frames, e.g., for QR
scanning or ML,, replace it with the CameraX `ImageAnalysis` use case.


```kotlin
imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
  try {
    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
    // Process image here (e.g., run object detection)
    // ...
  } finally {
    // MUST close the imageProxy to avoid blocking the pipeline
    imageProxy.close()
  }
}
```

<br />

*** ** * ** ***

## Use Camera2 interop

If your app requires specific Camera2 configuration options, such as custom
exposure modes or flash settings, that aren't exposed directly in CameraX,
use `Camera2Interop` to apply them to your CameraX use cases.


```kotlin
// Use Camera2Interop to set Camera2-specific capture options
val extender = Camera2Interop.Extender(imageCaptureBuilder)
extender.setCaptureRequestOption(
  CaptureRequest.CONTROL_AE_MODE,
  CaptureRequest.CONTROL_AE_MODE_OFF
).setCaptureRequestOption(
  CaptureRequest.FLASH_MODE,
  CaptureRequest.FLASH_MODE_TORCH
)
```

<br />

*** ** * ** ***

## Follow constraints

- **Don't open the CameraDevice manually** : Let CameraX manage device opening and closing using `bindToLifecycle`.
- **Don't forget to close `ImageProxy`** : In both `ImageCapture` and `ImageAnalysis` callbacks, you **must** call `image.close()`. Failure to do so will block the camera pipeline.
- **Don't block the ImageAnalysis thread** : The analyzer runs frame-by-frame. If you need to perform heavy computations, offload them to a background worker and close the `ImageProxy` as soon as possible.