## Remove `Camera1` implementation

1. Delete all `android.hardware.Camera` instances.
2. Delete `SurfaceView` and `SurfaceHolder.Callback` implementations `surfaceCreated`, `surfaceChanged`, and `surfaceDestroyed`.
3. Remove custom lifecycle handling that opens or releases the camera in `onResume` or `onPause`.
4. Remove manual matrix calculations for orientation.

## Initialize `ProcessCameraProvider`

Request the `ProcessCameraProvider` and bind use cases to the Activity or
Fragment lifecycle.


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

  cameraProvider.unbindAll() // Unbind before rebinding

  val camera = cameraProvider.bindToLifecycle(
    lifecycleOwner,
    cameraSelector,
    preview,
    imageCapture
  )
  val cameraControl = camera.cameraControl
}
```

<br />

## Implement the preview and tap-to-focus

Choose exactly one of the following patterns based on the app's UI toolkit:

### Option A: For Android Views

Use `androidx.camera.view.PreviewView`.

1. **Set up preview**:


   ```kotlin
   preview.setSurfaceProvider(previewView.surfaceProvider)
   ```

   <br />

2. **Handle tap-to-focus**:


   ```kotlin
   val factory = previewView.meteringPointFactory
   val point = factory.createPoint(x, y) // x, y from touch event
   val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF).build()
   cameraControl?.startFocusAndMetering(action)
   ```

   <br />

### Option B: For Jetpack Compose

Use `androidx.camera.compose.CameraXViewfinder`.

1. **Set up preview and SurfaceRequest**:


   ```kotlin
   var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
   val preview = remember {
     Preview.Builder().build().apply {
       setSurfaceProvider { request -> surfaceRequest = request }
     }
   }
   ```

   <br />

2. **Render viewfinder**:


   ```kotlin
   surfaceRequest?.let { request ->
     CameraXViewfinder(
       surfaceRequest = request,
       coordinateTransformer = coordinateTransformer,
       modifier = Modifier
     )
   }
   ```

   <br />

3. **Handle tap-to-focus in Compose**:


   ```kotlin
   // Inside your tap gesture handler...
   val surfaceCoords = with(coordinateTransformer) { offset.transform() }
   val factory = SurfaceOrientedMeteringPointFactory(
     request.resolution.width.toFloat(),
     request.resolution.height.toFloat()
   )
   val point = factory.createPoint(surfaceCoords.x, surfaceCoords.y)
   val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF).build()
   cameraControl?.startFocusAndMetering(action)
   ```

   <br />

4. **Update target rotation for Compose**:


   ```kotlin
   LaunchedEffect(configuration) {
     if (!view.isInEditMode) {
       val rotation = view.display?.rotation ?: Surface.ROTATION_0
       imageCapture.targetRotation = rotation
       preview.targetRotation = rotation
     }
   }
   ```

   <br />

## Capture a photo

Use the `ImageCapture` use case to take the picture. The `ImageProxy` handles
rotation directly.


```kotlin
imageCapture.takePicture(
  cameraExecutor,
  object : ImageCapture.OnImageCapturedCallback() {
    override fun onCaptureSuccess(image: ImageProxy) {
      val buffer = image.planes[0].buffer
      val bytes = ByteArray(buffer.remaining())
      buffer.get(bytes)
      val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

      // Adjust rotation natively via ImageProxy
      val matrix = Matrix()
      matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
      if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
        matrix.postScale(-1f, 1f) // Mirror for front camera
      }

      val rotatedBitmap = Bitmap.createBitmap(
        bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
      )

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

## Switch cameras

To flip between front and rear cameras, change the `CameraSelector` and
retrigger the `ProcessCameraProvider` logic.


```kotlin
lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
  CameraSelector.LENS_FACING_FRONT
} else {
  CameraSelector.LENS_FACING_BACK
}
```

<br />

## Follow constraints

- **Don't manage the camera lifecycle manually** : Bind the camera to a `LifecycleOwner` through the `ProcessCameraProvider`. Avoid manual camera open or close logic in `onResume` or `onPause`.
- **Don't calculate focus matrices manually** : `MeteringPointFactory` handles coordinate transformations, including device rotation offsets. Avoid custom matrix implementations.
- **Don't forget to close the `ImageProxy`** : Remember to invoke `image.close()` in the capture callback. Skipping this call locks the capture pipeline and interrupts subsequent photos.
- **Don't wrap `PreviewView` in `AndroidView` for Compose code** : For Compose UI layouts, use `CameraXViewfinder`. Compiling `PreviewView` in an `AndroidView` is an earlier fallback option that introduces resizing issues.