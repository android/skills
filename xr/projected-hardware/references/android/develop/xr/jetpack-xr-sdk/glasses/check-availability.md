<br />


Applicable XR devices This guidance helps you build experiences for these types of XR devices. [Learn about XR device types →](https://developer.android.com/develop/xr/devices) ![](https://developer.android.com/static/images/develop/xr/ai-glasses-icon.svg) Audio \&  
Display Glasses [](https://developer.android.com/develop/xr/devices#audio-display) [Learn about XR device types →](https://developer.android.com/develop/xr/devices)

<br />

As a user goes through their day, their audio glasses or display glasses might
lose their connection to the host device (such as the user's phone) or their
glasses might be temporarily unavailable if they take their glasses off. To
account for these kinds of changes in device availability, your app can use the
XR Device Availability API, which consolidates device availability signals into
the standard Android [`Lifecycle.State`](https://developer.android.com/reference/kotlin/androidx/lifecycle/Lifecycle.State) values. Use this API to help manage
audio routing, hotword activation, and to know when to expect user input based
on when the glasses are available.

## Understand lifecycle states

The following table lists how device availability signals map to the
`Lifecycle.State` values.

| Lifecycle state | Device status | Description |
|---|---|---|
| `INITIALIZED` | Created | The lifecycle object is created but not yet observed. |
| `CREATED` | Inactive | The service is connected, but the user is not wearing the device. |
| `STARTED` | Active | The user is wearing the device. |
| `DESTROYED` | Disconnected | The device is disconnected or the service connection is lost. |
| `RESUMED` | N/A | The device lifecycle doesn't currently use or emit this state. |

## Check and monitor device availability

To check and monitor a device's availability, you'll use a projected context
together with the lifecycle state to determine how your app should handle each
case:


```kotlin
    // In your phone activity or service, check for projected device connection state before
    // attempting to create a projected device context and get the device lifecycle.
    ProjectedContext.isProjectedDeviceConnected(context, currentCoroutineContext())
        .flatMapLatest { isConnected ->
            if (isConnected) {
                try {
                    // Create the projected device context on connection
                    val projectedContext = ProjectedContext.createProjectedDeviceContext(context)
                    val xrDevice = XrDevice.getCurrentDevice(projectedContext)

                    // Get the device lifecycle
                    xrDevice.getLifecycle().currentStateFlow
                } catch (e: IllegalStateException) {
                    flowOf(Lifecycle.State.DESTROYED)
                }
            } else {
                flowOf(Lifecycle.State.DESTROYED)
            }
        }
        .collect { state ->
            when (state) {
                Lifecycle.State.STARTED -> { /* Device is available (worn) */ }
                Lifecycle.State.CREATED -> { /* Device is unavailable (not worn) */ }
                Lifecycle.State.DESTROYED -> { /* Device is disconnected from host phone */ }
                else -> { /* Handle other states */ }
            }
        }
}
```

<br />

### Key points about the code

- **Check for a connection** : Before accessing the device lifecycle, call [`ProjectedContext.isProjectedDeviceConnected`](https://developer.android.com/reference/kotlin/androidx/xr/projected/ProjectedContext#isProjectedDeviceConnected(android.content.Context,kotlin.coroutines.CoroutineContext)) to verify that the projected device is connected to the host device.
- **Obtain a `ProjectedContext`** : Only call [`ProjectedContext.createProjectedDeviceContext`](https://developer.android.com/reference/kotlin/androidx/xr/projected/ProjectedContext#createProjectedDeviceContext(android.content.Context)) after verifying the connection, and make sure you pass this context into your [`XrDevice`](https://developer.android.com/reference/kotlin/androidx/xr/runtime/XrDevice) instance.
- **Handle context invalidation** : A new `deviceId` is generated every time a projected device connects. Once the state reaches `DESTROYED`, the current `ProjectedContext` is invalid. Stop using it immediately, and wait for a new connection.
- **Optimize battery and resources** : Gracefully handle app functionality based on the lifecycle state to preserve system resources and reduce battery consumption. For example, you should release glasses-specific resources, such as a camera data stream, when the state transitions from `STARTED` back to `CREATED`. The `CREATED` state indicates the device is no longer being worn, so stopping these processes is essential to prevent unnecessary battery drain and to promote user privacy.