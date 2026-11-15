> [!IMPORTANT]
> **Important:** **ORCHESTRATION GATEWAY PREREQUISITE** : This document is a supplemental reference guide for [`../SKILL.md`](https://developer.android.com/agents/skills/migration/ios-to-android/skill). You **MUST** read and follow the primary orchestration protocol in `../SKILL.md` before applying any patterns from this guide. `SKILL.md` enforces mandatory incremental compilation gates (`./gradlew compileDebugKotlin`), vertical feature slicing, and strict anti-premature-exit execution discipline.

This skill maps iOS `AVFoundation` constructs and workflows to their Android
equivalents using the CameraX library.

**Official Reference Links:**

- [CameraX overview \| Android Developers](https://developer.android.com/media/camera/camerax)
- [CameraX architecture \| Android Developers](https://developer.android.com/media/camera/camerax/architecture)
- [Take a picture with CameraX \| Android Developers](https://developer.android.com/media/camera/camerax/take-photo)

## Prerequisites

Before using this skill, you **Must** ensure the following prerequisites are
met:

- **Minimum SDK:** CameraX requires a minimum SDK of 21 (Android 5.0).

## Limitations

This skill has the following limitations:

- **CameraX Focus:** Examples are tailored for the CameraX Jetpack library, which is the recommended approach for most apps as it simplifies camera development. It does not cover the low-level `Camera2` API in depth.
- **Advanced Controls:** While basic preview, photo capture, and video recording are covered, advanced manual controls such as custom ISO, shutter speed, and complex vendor extensions are not covered in depth.

## Glossary

| iOS Term (`AVFoundation`) | Android Equivalent (`CameraX`) | Definition |
|---|---|---|
| `AVCaptureSession` | `ProcessCameraProvider` | Manages use cases and camera lifecycle. |
| `AVCaptureDevice` | `CameraSelector` | Selects the physical camera (front/back). |
| `AVCapturePhotoOutput` | `ImageCapture` | The use case for capturing still images. |
| `AVCaptureVideoPreviewLayer` | `PreviewView` / `Preview` | The UI component and use case for displaying the stream. |
| `AVCaptureDeviceInput` | Implicit in CameraX | CameraX handles input device attachment automatically when binding use cases. |

## Setup \& Dependencies

You **Must** add the following dependencies to your `build.gradle.kts` file to
enable CameraX:

```kotlin
dependencies {
    val cameraxVersion = "1.3.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("androidx.camera:camera-extensions:$cameraxVersion")
}
```

It is **Mandatory** to declare the following permissions in your
`AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
```

> [!CAUTION]
> **Caution:** **Android Manifest Permissions** : The camera will fail to open or crash if you do not explicitly add the camera permission to the `AndroidManifest.xml` and request it at runtime.

## Instructions: Mapping Concepts

### 1.1 Initializing the Camera

Map iOS `AVCaptureSession` setup to Android `ProcessCameraProvider`
initialization.

- **Input:** Application context.
- **Output:** `ProcessCameraProvider` instance.

**iOS**
`swift
let captureSession = AVCaptureSession()
captureSession.beginConfiguration()
// Add inputs and outputs...
captureSession.commitConfiguration()
captureSession.startRunning()`

**Android**
\`\`\`kotlin
val context = LocalContext.current
val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

LaunchedEffect(cameraProviderFuture) {
val cameraProvider = cameraProviderFuture.get()
// Bind use cases to cameraProvider
}
\`\`\`

### 1.2 Requesting Permissions at Runtime

> [!IMPORTANT]
> **Important:** **When to request Camera Permission**: Always request camera permissions at the point of use (e.g., when the user taps a button to open the camera or scan a QR code) rather than upon app launch. This improves user trust and acceptance rates.

Map iOS permission requests to Android runtime permission flows, including
rationale handling.

- **Input:** Camera permission request.
- **Output:** Permission granted or denied.

**Logic:**
IF camera permission is already granted:
THEN proceed to initialize the camera.
ELSE IF should show rationale:
THEN show an educational UI explaining why the camera is needed.
ELSE:
THEN request the permission using the system dialog.

**Android Code Example:**
\`\`\`kotlin
val cameraPermissionLauncher = rememberLauncherForActivityResult(
ActivityResultContracts.RequestPermission()
) { onPermissionGranted -\>
if (onPermissionGranted) {
// Proceed with camera initialization
} else {
// Handle denial
}
}

val activity = LocalContext.current as Activity
val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
activity,
Manifest.permission.CAMERA
)

if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
// Already granted
} else if (shouldShowRationale) {
// Show rationale UI
} else {
cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
}
\`\`\`

### 1.3 Selecting a Camera (Conditional Logic)

Map iOS camera selection to Android `CameraSelector` with explicit branching
logic.

- **Input:** User preference or requirement.
- **Output:** `CameraSelector` instance.

**Logic:**
IF a front-facing camera is explicitly requested:
THEN use `CameraSelector.DEFAULT_FRONT_CAMERA`.
ELSE IF a specific lens is required (e.g., wide-angle) AND the device supports
it:
THEN iterate through `cameraProvider.getAvailableCameraInfos()` and select the matching device.
ELSE:
THEN use `CameraSelector.DEFAULT_BACK_CAMERA`.

**Android Code Example:**
\`\`\`kotlin
// Example for finding a wide-angle camera
val wideAngleCamera = cameraProvider.availableCameraInfos.firstOrNull { cameraInfo -\>
cameraInfo.lensFacing == CameraSelector.LENS_FACING_BACK \&\&
Camera2CameraInfo.from(cameraInfo)
.getCameraCharacteristics(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
?.any { focalLength -\> focalLength \< 24f } == true // Example: focal length \< 24mm is wide
}

val cameraSelector = if (isFrontCameraRequested) {
CameraSelector.DEFAULT_FRONT_CAMERA
} else if (isWideAngleRequested \&\& wideAngleCamera != null) {
CameraSelector.Builder().addCameraFilter { Collections.singletonList(wideAngleCamera) }.build()
} else {
CameraSelector.DEFAULT_BACK_CAMERA
}
\`\`\`

### 1.4 Setting Up Camera Preview

Map iOS `AVCaptureVideoPreviewLayer` to Android `Preview` use case and
`PreviewView`.

- **Input:** `PreviewView` and `ProcessCameraProvider`.
- **Output:** Camera stream displayed on screen.

**iOS**
`swift
let previewLayer = AVCaptureVideoPreviewLayer(session: captureSession)
previewLayer.videoGravity = .resizeAspectFill
view.layer.addSublayer(previewLayer)`

**Android**
\`\`\`kotlin
val preview = Preview.Builder().build()
val previewView = remember { PreviewView(context) }

preview.setSurfaceProvider(previewView.surfaceProvider)

// Bind to lifecycle
cameraProvider.bindToLifecycle(
lifecycleOwner,
CameraSelector.DEFAULT_BACK_CAMERA,
preview
)

// Display in Compose
AndroidView(
factory = { previewView },
modifier = Modifier.fillMaxSize()
)
\`\`\`

### 1.5 Capturing a Photo

Map iOS `AVCapturePhotoOutput` to Android `ImageCapture` use case.

- **Input:** `ImageCapture` instance and output file location.
- **Output:** Saved image file.

**iOS**
\`\`\`swift
let photoOutput = AVCapturePhotoOutput()
captureSession.addOutput(photoOutput)

let settings = AVCapturePhotoSettings()
photoOutput.capturePhoto(with: settings, delegate: self)
\`\`\`

**Android**
\`\`\`kotlin
val imageCapture = remember { ImageCapture.Builder().build() }

// Bind imageCapture along with preview
cameraProvider.bindToLifecycle(
lifecycleOwner,
CameraSelector.DEFAULT_BACK_CAMERA,
preview,
imageCapture
)

// To take a picture:
val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
imageCapture.takePicture(
outputFileOptions,
ContextCompat.getMainExecutor(context),
object : ImageCapture.OnImageSavedCallback {
override fun onImageSaved(onOutputFileResults: ImageCapture.OutputFileResults) {
// Photo saved successfully
}
override fun onError(onException: ImageCaptureException) {
// Handle error
}
}
)
\`\`\`

### 1.6 Migrating Photo Saving and Storage (Logic)

Map iOS photo storage mechanisms (`PHPhotoLibrary`,
`UIImageWriteToSavedPhotosAlbum`, or app sandbox files) to Android storage
patterns (Scoped Storage via `MediaStore` or app-specific files directory).

- **Input:** Captured image data (`ImageCapture.OutputFileOptions`).
- **Output:** Saved photo in app-specific storage or public MediaStore gallery.

**Logic:**
IF the captured photo needs to be saved to the public MediaStore (gallery
visible to other apps):
THEN use `MediaStore.Images.Media` content values with relative paths (Android 10+ Scoped Storage).
ELSE IF the photo is for internal app use only (sandbox storage):
THEN save to `context.filesDir` or `context.cacheDir`.

**Android Code Example (Saving to MediaStore / Public Gallery):**
\`\`\`kotlin
val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
val contentValues = ContentValues().apply {
put(MediaStore.MediaColumns.DISPLAY_NAME, name)
put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyApp")
}

val outputOptions = ImageCapture.OutputFileOptions.Builder(
contentResolver,
MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
contentValues
).build()

imageCapture.takePicture(
outputOptions,
ContextCompat.getMainExecutor(context),
object : ImageCapture.OnImageSavedCallback {
override fun onImageSaved(onOutputFileResults: ImageCapture.OutputFileResults) {
val savedUri = onOutputFileResults.savedUri
// Handle successful save to MediaStore
}
override fun onError(exception: ImageCaptureException) {
// Handle error
}
}
)
\`\`\`

### 1.7 Recording Video

Map iOS `AVCaptureMovieFileOutput` to Android `VideoCapture` use case.

- **Input:** `VideoCapture` instance and output file options.
- **Output:** Saved video file.

**iOS**
\`\`\`swift
let movieOutput = AVCaptureMovieFileOutput()
captureSession.addOutput(movieOutput)

let fileURL = URL(fileURLWithPath: "path/to/video.mp4")
movieOutput.startRecording(to: fileURL, recordingDelegate: self)
\`\`\`

**Android**
\`\`\`kotlin
val recorder = Recorder.Builder()
.setQualitySelector(QualitySelector.from(Quality.HIGHEST))
.build()
val videoCapture = VideoCapture.withOutput(recorder)

// Bind to lifecycle
cameraProvider.bindToLifecycle(
lifecycleOwner,
CameraSelector.DEFAULT_BACK_CAMERA,
preview,
videoCapture
)

// To start recording:
val fileOutputOptions = FileOutputOptions.Builder(file).build()
val recording = videoCapture.output
.prepareRecording(context, fileOutputOptions)
.start(ContextCompat.getMainExecutor(context)) { onRecordEvent -\>
when (onRecordEvent) {
is VideoRecordEvent.Start -\> { /\* Recording started \*/ }
is VideoRecordEvent.Finalize -\> {
if (!onRecordEvent.hasError()) {
// Video saved successfully
} else {
val recordError = onRecordEvent.error
// Log the error and notify the user.
// For example: Log.e("CameraXVideo", "Video save failed with error: $recordError")
}
}
}
}

// To stop recording:
recording.stop()
\`\`\`

### 1.8 Analyzing Image Streams

Map iOS `AVCaptureVideoDataOutput` to Android `ImageAnalysis` use case.

- **Input:** `ImageAnalysis` instance and an analyzer callback.
- **Output:** Image frames passed to the analyzer.

**iOS**
\`\`\`swift
let videoOutput = AVCaptureVideoDataOutput()
videoOutput.setSampleBufferDelegate(self, queue: DispatchQueue.main)
captureSession.addOutput(videoOutput)

// Delegate method
func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
// Process frame
}
\`\`\`

**Android**
\`\`\`kotlin
val imageAnalysis = ImageAnalysis.Builder()
.setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
.build()

imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { onImageProxy -\>
val rotationDegrees = onImageProxy.imageInfo.rotationDegrees
// Process image frame here

    // Must close the image proxy when done!
    onImageProxy.close()

}

// Bind to lifecycle
cameraProvider.bindToLifecycle(
lifecycleOwner,
CameraSelector.DEFAULT_BACK_CAMERA,
preview,
imageAnalysis
)
\`\`\`

### 1.9 Handling Device Rotation

CameraX handles rotation automatically in most cases, but you may need to handle
it explicitly when saving files or processing images.

- **Input:** Device orientation change.
- **Output:** Correctly rotated image or preview.

**Android Code Example:**
\`\`\`kotlin
val orientationEventListener = object : OrientationEventListener(context) {
override fun onOrientationChanged(orientation: Int) {
if (orientation == ORIENTATION_UNKNOWN) return

        val rotation = when (orientation) {
            in 45 until 135 -> Surface.ROTATION_270
            in 135 until 225 -> Surface.ROTATION_180
            in 225 until 315 -> Surface.ROTATION_90
            else -> Surface.ROTATION_0
        }

        imageCapture.targetRotation = rotation
        imageAnalysis.targetRotation = rotation
    }

}
orientationEventListener.enable()
\`\`\`

### 1.10 Error Handling

CameraX operations can fail due to various reasons (hardware in use,
insufficient storage, etc.). You **Must** handle these errors gracefully using
exhaustive matching or clear recovery strategies.

| Error Constant | Description | Mandatory Action |
|---|---|---|
| `ImageCapture.ERROR_UNKNOWN` | An unknown error occurred. | You must log the error and notify the user that an unexpected error occurred. |
| `ImageCapture.ERROR_FILE_IO` | Failed to save the image file. | You must check for available storage space and file system permissions. |
| `ImageCapture.ERROR_CAPTURE_FAILED` | The capture request failed. | You must retry the capture or notify the user to try again. |
| `ImageCapture.ERROR_CAMERA_CLOSED` | The camera was closed. | You must re-bind the use cases, as the camera may have been taken by another app or the lifecycle ended. |
| `ImageCapture.ERROR_INVALID_CAMERA` | The camera is invalid or not available. | You must verify that the requested camera exists and is accessible. |

**Example Error Handling in `takePicture`:**
`kotlin
override fun onError(onException: ImageCaptureException) {
when (onException.imageCaptureError) {
ImageCapture.ERROR_FILE_IO -> {
// Handle file save failure
}
ImageCapture.ERROR_CAMERA_CLOSED -> {
// Handle camera closed
}
ImageCapture.ERROR_CAPTURE_FAILED -> {
// Handle capture failure
}
ImageCapture.ERROR_INVALID_CAMERA -> {
// Handle invalid camera
}
else -> {
// Handle unknown or other errors
}
}
}`

## Antipatterns

### DO

- **DO** check for camera permission before attempting to initialize the camera.
- **DO** unbind all use cases or handle lifecycle correctly to release resources when the app goes to background.
- **DO** use `DEFAULT_BACK_CAMERA` or `DEFAULT_FRONT_CAMERA` for standard use cases instead of iterating through available devices manually unless needed.

### DON'T

- **DON'T** assume `ProcessCameraProvider.getInstance()` returns immediately; it returns a `ListenableFuture`.
- **DON'T** try to bind use cases that conflict or exceed hardware capabilities. An example of a conflict is binding two high-resolution ImageAnalysis streams simultaneously on hardware that does not support it.

### NEVER

- **NEVER** block the main thread waiting for `cameraProviderFuture.get()`. Always use listeners or `LaunchedEffect`.
- **NEVER** forget to release the camera resources when not in use, as it can drain battery and prevent other apps from using the camera.