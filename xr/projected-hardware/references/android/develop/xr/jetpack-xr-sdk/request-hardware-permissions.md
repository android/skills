<br />


Applicable XR devices This guidance helps you build experiences for these types of XR devices. [Learn about XR device types →](https://developer.android.com/develop/xr/devices) ![](https://developer.android.com/static/images/develop/xr/ai-glasses-icon.svg) Audio \&  
Display Glasses [](https://developer.android.com/develop/xr/devices#audio-display) [Learn about XR device types →](https://developer.android.com/develop/xr/devices)

<br />

Just like on a phone, accessing sensitive hardware like the camera and
microphone on audio glasses and display glasses requires explicit user consent.
These are considered
**glasses-specific permissions**, and your app must request them at runtime,
even if it already has the corresponding permissions on the phone.

## Declare the permissions in your app's manifest

Before requesting permissions, you must [declare them in your app's manifest](https://developer.android.com/training/permissions/declaring)
file using the [`<uses-permission>`](https://developer.android.com/guide/topics/manifest/uses-permission-element) element. This declaration remains the
same whether the permission is for a phone or a glasses-specific feature, but
you must still explicitly request it for glasses-specific hardware or
functionality.

    <manifest ...>
        <!-- Only declare permissions that your app actually needs. In this example,
        we declare permissions for the camera. -->
        <uses-permission android:name="android.permission.CAMERA"/>
        <application ...>
            ...
        </application>
    </manifest>

## Before requesting glasses permissions

Review the [permission principles and workflows](https://developer.android.com/training/permissions/requesting#principles) to make sure you provide the
best experience for your users, like [checking whether the user has already
granted the runtime permissions](https://developer.android.com/training/permissions/requesting#already-granted) your app requires and [whether your app
should show a rationale](https://developer.android.com/training/permissions/requesting#explain) to the user before requesting glasses-specific
permissions.

## Permission scenarios

There are different scenarios you might encounter when requesting [dangerous
runtime permissions](https://developer.android.com/guide/topics/permissions/requesting#normal-dangerous) on audio and display glasses:

- Request runtime permissions from a projected activity
- Request runtime permissions from a phone activity

See the following sections in this guide for details about each scenario.

### Request runtime permissions from a projected activity

To request runtime permissions for audio or display glasses from a projected
activity, use the [`ProjectedActivityCompat.requestPermissions`](https://developer.android.com/reference/kotlin/androidx/xr/projected/ProjectedActivityCompat#requestPermissions(android.app.Activity,kotlin.Array,kotlin.Int)) method.

Calling this method triggers a coordinated user flow across both the glasses and
the host phone.

1. **On the glasses**: The system opens a projected companion activity on the glasses, instructing the user to check their phone to respond to the permission request.
2. **On the phone**: An Android system permission dialog appears on the phone screen. The dialog explicitly indicates that the user is granting the permission for the glasses rather than the phone itself. The user can then grant or deny the request.

The following example shows how to request glasses permissions from a projected
activity:


```kotlin
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
private fun requestCameraPermission() {
    lifecycleScope.launch {
        try {
            ProjectedActivityCompat.requestPermissions(
                this@PermissionsFromProjectedActivity,
                arrayOf(Manifest.permission.CAMERA),
                // CAMERA_PERMISSION_REQUEST_CODE is a developer defined constant
                CAMERA_PERMISSION_REQUEST_CODE,
            )
        } catch (e: IllegalStateException) {
            // Thrown when the projected system service can't be bound.
            Log.e(TAG, "Camera permission request failed: projected service unavailable.", e)
        }
    }
}
```

<br />

#### Key points about the code

- Use `ProjectedActivityCompat.requestPermissions` instead of the standard [`ActivityCompat`](https://developer.android.com/reference/kotlin/androidx/core/app/ActivityCompat) method. This method explicitly targets the runtime permissions for audio and display glasses rather than the host phone.

#### Handle the permission results

Once the user responds to the permission dialog on the phone, their decision is
delivered to your app by invoking the [`onRequestPermissionsResult`](https://developer.android.com/reference/kotlin/androidx/core/app/ActivityCompat.OnRequestPermissionsResultCallback#onRequestPermissionsResult(int,java.lang.String%5B%5D,int%5B%5D))
callback.

The system closes the projected companion activity automatically. On audio
glasses, this occurs after the audio alert finishes playing. On display glasses,
it happens after the user responds to all the permission dialogs.

To handle the response, override `onRequestPermissionsResult()` within the
`Activity` instance that initiated the permission request.

The following code snippet shows how to handle the callback and verify whether
the user granted the requested permissions:


```kotlin
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray,
    deviceId: Int,
) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
    if (requestCode != CAMERA_PERMISSION_REQUEST_CODE) return

    val isCameraGranted =
        grantResults.getOrNull(permissions.indexOf(Manifest.permission.CAMERA)) ==
            PackageManager.PERMISSION_GRANTED

    if (isCameraGranted) {
        initializeCameraFeatures()
    } else {
        onCameraPermissionDenied()
    }
}
```

<br />

##### Key points about the code

- Use the `deviceId` overload with the [`onRequestPermissionsResult`](https://developer.android.com/reference/kotlin/androidx/core/app/ActivityCompat.OnRequestPermissionsResultCallback#onRequestPermissionsResult(int,java.lang.String%5B%5D,int%5B%5D)) callback to ensure the permission status is correctly mapped to the specific context, such as the audio or display glasses rather than the host phone.
- Your app should handle both granted and denied results gracefully.

### Request runtime permissions in a phone activity

If your app is running in a phone activity but requires permissions for audio or
display glasses---for example, to let a user switch a video stream to the glasses'
camera for a first-person point of view---request the permissions using the
device-specific [`Activity#requestPermissions(permissions, requestCode,
deviceId)`](https://developer.android.com/reference/kotlin/android/app/Activity#requestpermissions_1) method:


```kotlin
// Request the projected permission from phone activity
requestPermissions(
    arrayOf(Manifest.permission.CAMERA),
    // REQUEST_CODE_GLASSES_CAMERA is a developer-defined constant
    REQUEST_CODE_GLASSES_CAMERA,
    projectedDeviceId
)
```

<br />

#### Key points about the code

- To target audio and display glasses, obtain the appropriate device ID from a projected context that you've created using [`createProjectedDeviceContext`](https://developer.android.com/reference/kotlin/androidx/xr/projected/ProjectedContext#createProjectedDeviceContext(android.content.Context)). Pass this ID with your call to the `requestPermissions` method.

#### Handle the permission results

Once the user responds to the permission dialog on the phone, their decision is
delivered to your app by invoking the [`onRequestPermissionsResult`](https://developer.android.com/reference/kotlin/androidx/core/app/ActivityCompat.OnRequestPermissionsResultCallback#onRequestPermissionsResult(int,java.lang.String%5B%5D,int%5B%5D))
callback.

To handle the response, override `onRequestPermissionsResult` within the
`Activity` instance that initiated the permission request.

The following code snippet shows how to handle the callback and verify whether
the user granted the requested permissions:


```kotlin
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray,
    deviceId: Int,
) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
    if (requestCode != CAMERA_PERMISSION_REQUEST_CODE) return

    val isCameraGranted =
        grantResults.getOrNull(permissions.indexOf(Manifest.permission.CAMERA)) ==
            PackageManager.PERMISSION_GRANTED

    if (isCameraGranted) {
        initializeCameraFeatures()
    } else {
        onCameraPermissionDenied()
    }
}
```

<br />

##### Key points about the code

- Use the `deviceId` overload with the `onRequestPermissionsResult` callback to ensure the permission status is correctly mapped to the specific context, such as the audio or display glasses rather than the host phone.
- Your app should handle both granted and denied results gracefully.