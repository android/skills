Developing camera features for Wear OS is rarely about the watch's own lens, if
it even has one. It's almost always about creating a **Remote Viewfinder** to
control the phone's camera.

## The Wear OS remote mindset

| Feature | Phone camera app | Wear OS remote app |
|---|---|---|
| **Screen** | Rectangular (Large) | **Circular and less than 2 inches in size** |
| **Connectivity** | Local Hardware | **Bluetooth or Wi-Fi data layer API** |
| **Latency** | Direct and less than 20 ms | **Networked, 100 ms to 500 ms** |
| **Interaction** | Multi-touch gestures | **Rotary input or single taps** |

*** ** * ** ***

## Follow the implementation guide

### The circular UI challenge

Wear OS devices are often round. Standard rectangular layouts clip corner
buttons.

Follow these blueprint recommendations:

- Use `Horologist` or `Wear Compose` libraries.
- Use `ScalingLazyColumn` for lists so that items stay within the "safe zone" of the circular display.
- **Preview scaling**: Crop the center of the rectangular phone viewfinder to fit the circular watch screen.

### Stream the viewfinder

You can't send a raw 60 fps stream over Bluetooth. compress and throttle.


```kotlin
/** Phone side: sends a compressed viewfinder frame to the watch. */
suspend fun sendPreviewFrame(context: Context, previewView: PreviewView) {
    val bitmap = previewView.bitmap ?: return // Capture current frame on the main thread
    val compressed = withContext(Dispatchers.Default) { // Compress off the main thread
        ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream)
            stream.toByteArray()
        }
    }
    val request = PutDataMapRequest.create("/camera/preview").apply {
        dataMap.putAsset("image", Asset.createFromBytes(compressed))
    }
    Wearable.getDataClient(context).putDataItem(request.asPutDataRequest())
}
```

<br />

**Optimization** : Cap the watch preview at **10-15 fps** to preserve battery and
bandwidth.

### Remote triggers and syncing

Use the `MessageClient` for low-latency commands like "Take Photo" or "Switch
Camera."


```kotlin
/** Watch side: sends a capture trigger to the phone node. */
fun sendCaptureTrigger(context: Context, phoneNodeId: String) {
    Wearable.getMessageClient(context).sendMessage(phoneNodeId, "/camera/capture", null)
}
```

<br />

### Rotary input support

On devices that support it, use the physical crown, Rotary Input, to control
**Zoom** or **Exposure**.

*** ** * ** ***

## Wear OS pitfalls

- **Corner clipping** : Placing a **Close** button in the top-right corner of a square layout makes it impossible to tap the button on a round watch display.
- **Battery drain**: Sustained Bluetooth data transfer, viewfinder sync, drains watch battery. Proactively close the remote app if the phone screen is turned off.
- **Node discovery** : The phone is possibly connected to multiple "Nodes" (watches, earbuds, or tablets). Ensure your `CapabilityClient` filters for the specific `camera_remote_host` capability.
- **Disconnect handling**: If the watch disconnects, the phone camera must stop its high-power preview to save energy.