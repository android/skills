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
/**
 * Use cases shared by both UI options. Each UI option builds its own Preview
 * with a surface provider and passes it to [bindUseCases].
 */
class CameraUseCases {
    val imageCapture: ImageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()

    // Replaces a Camera2 ImageReader that was used for frame processing.
    val imageAnalysis: ImageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
}

/**
 * Replaces CameraManager.openCamera and the session callbacks. Returns null
 * when the device has no camera for [lensFacing].
 */
suspend fun bindUseCases(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    preview: Preview,
    useCases: CameraUseCases,
    lensFacing: Int = CameraSelector.LENS_FACING_BACK,
): Camera? {
    val cameraProvider = ProcessCameraProvider.awaitInstance(context)
    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(lensFacing)
        .build()
    if (!cameraProvider.hasCamera(cameraSelector)) return null

    cameraProvider.unbindAll()
    return cameraProvider.bindToLifecycle(
        lifecycleOwner,
        cameraSelector,
        preview,
        useCases.imageCapture,
        useCases.imageAnalysis,
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
fun startCamera(
    activity: ComponentActivity,
    previewView: PreviewView,
    useCases: CameraUseCases,
) {
    // Attach the surface provider before binding, or the preview stays blank.
    val preview = Preview.Builder().build()
    preview.setSurfaceProvider(previewView.surfaceProvider)

    activity.lifecycleScope.launch {
        val camera = bindUseCases(activity, activity, preview, useCases)
            ?: return@launch

        // Tap-to-focus: meteringPointFactory handles rotation and scaling.
        previewView.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val point = previewView.meteringPointFactory.createPoint(event.x, event.y)
                camera.cameraControl.startFocusAndMetering(
                    FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF).build()
                )
                view.performClick()
            }
            true
        }
    }
}
```

<br />

### Option B: For Jetpack Compose

Use `androidx.camera.compose.CameraXViewfinder`.


```kotlin
@Composable
fun CameraXPreview(useCases: CameraUseCases, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    val coordinateTransformer = remember { MutableCoordinateTransformer() }

    // Build the Preview once, with its surface provider attached.
    val preview = remember {
        Preview.Builder().build().apply {
            setSurfaceProvider { request -> surfaceRequest = request }
        }
    }

    LaunchedEffect(lifecycleOwner) {
        cameraControl =
            bindUseCases(context, lifecycleOwner, preview, useCases)?.cameraControl
    }

    surfaceRequest?.let { request ->
        CameraXViewfinder(
            surfaceRequest = request,
            coordinateTransformer = coordinateTransformer,
            modifier = modifier.pointerInput(request, cameraControl) {
                detectTapGestures { offset ->
                    val surfaceCoords = with(coordinateTransformer) { offset.transform() }
                    val point = SurfaceOrientedMeteringPointFactory(
                        request.resolution.width.toFloat(),
                        request.resolution.height.toFloat(),
                    ).createPoint(surfaceCoords.x, surfaceCoords.y)
                    cameraControl?.startFocusAndMetering(
                        FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF).build()
                    )
                }
            },
        )
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
/**
 * Replaces an ImageReader and TEMPLATE_STILL_CAPTURE flow. CameraX still
 * needs an app-supplied [executor] for the callback, for example
 * `ContextCompat.getMainExecutor(context)` or an app-owned background
 * executor.
 */
fun capturePhoto(
    imageCapture: ImageCapture,
    executor: Executor,
    lensFacing: Int,
    onPhoto: (Bitmap) -> Unit,
) {
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                image.use { proxy ->
                    val buffer = proxy.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ?: return
                    val matrix = Matrix()
                    matrix.postRotate(proxy.imageInfo.rotationDegrees.toFloat())
                    if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                        matrix.postScale(-1f, 1f)
                    }
                    onPhoto(
                        Bitmap.createBitmap(
                            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                        )
                    )
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraX", "Capture failed", exception)
            }
        },
    )
}
```

<br />

*** ** * ** ***

## Implement image analysis

If you were using `ImageReader` in Camera2 to access raw frames, e.g., for QR
scanning or ML,, replace it with the CameraX `ImageAnalysis` use case.


```kotlin
/**
 * For raw frame access only. For ML Kit barcode, face, or text detection,
 * use MlKitAnalyzer instead. [analysisExecutor] is app-owned, for example
 * `Executors.newSingleThreadExecutor()`; shut it down when the camera closes.
 */
fun setUpAnalysis(imageAnalysis: ImageAnalysis, analysisExecutor: Executor) {
    imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
        // use {} closes the ImageProxy so the next frame can arrive.
        imageProxy.use { proxy ->
            val rotationDegrees = proxy.imageInfo.rotationDegrees
            val yPlane = proxy.planes[0].buffer
            Log.d("CameraX", "Frame ${proxy.width}x${proxy.height}, " +
                "rotation=$rotationDegrees, bytes=${yPlane.remaining()}")
        }
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
/**
 * Applies Camera2 manual exposure to ImageCapture. Camera2Interop.Extender is
 * deprecated from CameraX 1.7.0 in favor of the camera2Interop Kotlin builder.
 */
@OptIn(ExperimentalCamera2Interop::class)
fun buildManualExposureImageCapture(
    exposureTimeNs: Long,
    iso: Int,
): ImageCapture {
    val builder = ImageCapture.Builder()
    @Suppress("DEPRECATION") // Deprecated in CameraX 1.7.0; see the note in the guide.
    Camera2Interop.Extender(builder)
        // AE off: the app supplies exposure time and sensitivity.
        .setCaptureRequestOption(
            CaptureRequest.CONTROL_AE_MODE,
            CaptureRequest.CONTROL_AE_MODE_OFF,
        )
        .setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureTimeNs)
        .setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, iso)
    return builder.build()
}
```

<br />

*** ** * ** ***

## Follow constraints

- **Don't open the CameraDevice manually** : Let CameraX manage device opening and closing using `bindToLifecycle`.
- **Don't forget to close `ImageProxy`** : In both `ImageCapture` and `ImageAnalysis` callbacks, you **must** call `image.close()`. Failure to do so will block the camera pipeline.
- **Don't block the ImageAnalysis thread** : The analyzer runs frame-by-frame. If you need to perform heavy computations, offload them to a background worker and close the `ImageProxy` as soon as possible.