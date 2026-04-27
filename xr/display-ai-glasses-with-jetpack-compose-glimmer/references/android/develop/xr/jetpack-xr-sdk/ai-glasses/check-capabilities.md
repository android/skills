Different types of AI glasses have different capabilities. After [planning how
you'll support different types of AI devices](https://developer.android.com/develop/xr/jetpack-xr-sdk/ai-glasses/support-different-types), you can check for device
capabilities at runtime to provide the best experience for a user's device.

## Check whether a device has a display

Some AI glasses have a display where your app can show [UIs built with Jetpack
Compose Glimmer](https://developer.android.com/develop/xr/jetpack-xr-sdk/jetpack-compose-glimmer). The following example shows how to check whether a glasses
device has a display:


```kotlin
// Check device capabilities
val projectedDeviceController = ProjectedDeviceController.create(this@GlassesMainActivity)
isVisualUiSupported = projectedDeviceController.capabilities.contains(CAPABILITY_VISUAL_UI)
```

<br />

## React to display state changes

On AI glasses with a display, the display can time out or the user can turn off
the display. To design activities that run whether the display is on or off,
use [`addPresentationModeChangedListener`](https://developer.android.com/reference/kotlin/androidx/xr/projected/ProjectedDisplayController#addPresentationModeChangedListener(java.util.concurrent.Executor,java.util.function.Consumer)) to be notified when the display
state changes. You can tune your activity for the appropriate amount of
audio information depending on display state.

    ProjectedDisplayController.create(activity).addPresentationModeChangedListener {
        presentationModeFlags ->

        val areVisualsOff = !presentationModeFlags.hasPresentationMode(VISUALS_ON)
    }

> [!NOTE]
> **Note:** Monitor display state changes for the AI glasses directly within the [projected activity](https://developer.android.com/develop/xr/jetpack-xr-sdk/ai-glasses/support-different-types#activity-lifecycle).

## Keep the display on

On AI glasses with a display, you can request that the system keep the screen on
and prevent the screen from timing out using [`addLayoutParamsFlags`](https://developer.android.com/reference/kotlin/androidx/xr/projected/ProjectedDisplayController#addLayoutParamsFlags(kotlin.Int)).

> [!NOTE]
> **Note:** Keeping the device's screen on can drain the battery quickly. Ordinarily, you should let the device turn the screen off if the user isn't interacting with it. If you do need to keep the screen on, do so for as short a time as possible.

    var projectedDisplayController = ProjectedDisplayController.create(activity)

    projectedDisplayController.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)