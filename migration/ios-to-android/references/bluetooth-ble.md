> [!IMPORTANT]
> **Important:** **ORCHESTRATION GATEWAY PREREQUISITE** : This document is a supplemental reference guide for [`../SKILL.md`](https://developer.android.com/agents/skills/migration/ios-to-android/skill). You **MUST** read and follow the primary orchestration protocol in `../SKILL.md` before applying any patterns from this guide. `SKILL.md` enforces mandatory incremental compilation gates (`./gradlew compileDebugKotlin`), vertical feature slicing, and strict anti-premature-exit execution discipline.

This skill provides instructions and patterns for migrating Bluetooth Low Energy
(BLE) functionality from iOS (CoreBluetooth) to Android.

**Official Reference Links:**

- [Bluetooth Low Energy overview \| Android Developers](https://developer.android.com/develop/connectivity/bluetooth/ble/ble-overview)
- [Bluetooth permissions \| Android Developers](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions)

## Prerequisites

Before using this skill, you **Must** ensure the following prerequisites are
met:

- **Source Context:** Access to the existing iOS CoreBluetooth implementation is **Mandatory**.
- **Hardware Support:** The target Android device **Must** have a Bluetooth LE-compatible chipset.
- **SDK Version:** The Android project's `minSdkVersion` **Must** be at least 21 (Android 5.0) for BLE support.

## Limitations

This skill has the following limitations:

- **Central Role Only:** This skill only covers the "Central" role (scanning for devices and connecting). It does not cover implementing the "Peripheral" role (advertising).
- **Advanced Topics:** It does not cover advanced topics like bonding, L2CAP, or Mesh networking.

## Glossary

| iOS Term | Android Equivalent | Definition |
|---|---|---|
| `CBCentralManager` | `BluetoothLeScanner` / `BluetoothAdapter` | The object that scans for, discovers, connects to, and manages peripherals. |
| `CBPeripheral` | `BluetoothDevice` / `BluetoothGatt` | Represents a remote device that the central is interacting with. |
| `CBService` | `BluetoothGattService` | A collection of data and associated behaviors for accomplishing a function. |
| `CBCharacteristic` | `BluetoothGattCharacteristic` | A data point exposed by the service, containing a value and properties. |
| `GATT` | `GATT` | Generic Attribute Profile, the protocol used for BLE data transfer. |

## Setup \& Dependencies

You **Must** declare the following permissions in your `AndroidManifest.xml`.

### Manifest Requirements

```xml
<!-- Android 11 or lower -->
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" android:maxSdkVersion="30" />

<!-- Android 12 and higher -->
<!-- Set neverForLocation if you do NOT use Bluetooth scan results to derive location -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```

## Instructions: Mapping Concepts

### 1. Permission Handling

> [!CAUTION]
> **Caution:** **Android 12+ Split Permissions**: In API 31 (Android 12), Google split Bluetooth permissions into specific granular use cases.

**IF** `Build.VERSION.SDK_INT` \>= 31 **THEN**
Request
`["android.permission.BLUETOOTH_SCAN", "android.permission.BLUETOOTH_CONNECT"]`
**ELSE**
Request `["android.permission.ACCESS_FINE_LOCATION"]`
**ENDIF**

### 2. Scanning for Devices

Map iOS `CBCentralManager` scanning to Android `BluetoothLeScanner`.

- **Input:** `serviceUUIDs` (Optional array of UUIDs to filter).
- **Output:** Discovered devices delivered asynchronously via callbacks.

**iOS CBCentralManager**
\`\`\`swift
centralManager.scanForPeripherals(withServices: \[uuid\], options: \[CBCentralManagerScanOptionAllowDuplicatesKey: false\])

func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: \[String : Any\], rssi RSSI: NSNumber) { ... }
\`\`\`

**Android BluetoothLeScanner**
\`\`\`kotlin
val scanner = bluetoothAdapter.bluetoothLeScanner
val scanFilter = ScanFilter.Builder().setServiceUuid(ParcelUuid(uuid)).build()
val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

val scanCallback = object : ScanCallback() {
override fun onScanResult(callbackType: Int, result: ScanResult) {
val device = result.device // BluetoothDevice
val rssi = result.rssi
// Handle discovered device
}
}
// Requires BLUETOOTH_SCAN permission!
scanner.startScan(listOf(scanFilter), settings, scanCallback)
\`\`\`

#### Scan Options Mapping

| iOS `scanForPeripherals` Option | Android `ScanSettings` Equivalent | Description |
|---|---|---|
| `CBCentralManagerScanOptionAllowDuplicatesKey` | `setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)` | iOS controls whether to filter duplicate advertisements. Android controls this via callback type or manual filtering in `onScanResult`. |
| (Implicit power management) | `setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)` | Android allows explicit control over scan frequency and power consumption: `SCAN_MODE_LOW_POWER`, `SCAN_MODE_BALANCED`, `SCAN_MODE_LOW_LATENCY`. |

### 3. Connecting to a Peripheral

- **Input:** `device` (a `BluetoothDevice` object obtained from a scan).
- **Output:** The connection result is delivered asynchronously to the `onConnectionStateChange` method within the `gattCallback` object.

**iOS**
`swift
centralManager.connect(peripheral, options: nil)`

**Android**
`kotlin
// autoConnect = false is recommended for immediate connection
val gatt = device.connectGatt(context, false, gattCallback)`

### 4. Discovering Services

- **Input:** Connected `BluetoothGatt` instance.
- **Output:** Discovered services accessible via `gatt.getServices()`.

**iOS**
`swift
peripheral.discoverServices([serviceUUID])`

**Android**
\`\`\`kotlin
val gattCallback = object : BluetoothGattCallback() {
override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
if (newState == BluetoothProfile.STATE_CONNECTED) {
// Connection successful, now discover services
gatt.discoverServices()
} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
// Handle disconnection
}
}

override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
// Robust Error Handling
if (status == BluetoothGatt.GATT_SUCCESS) {
// Services discovered
val service = gatt.getService(serviceUUID)
} else {
// Handle discovery failure
Log.e("BluetoothGatt", "Service discovery failed with status: $status")
}
}
}
\`\`\`

### 5. Reading and Writing Characteristics

- **Input:** Target `BluetoothGattCharacteristic` and data (for writes).
- **Output:** Read values or write confirmation delivered to callbacks.

**iOS**
`swift
peripheral.readValue(for: characteristic)
peripheral.writeValue(data, for: characteristic, type: .withResponse)`

**Android**
\`\`\`kotlin
// Reading
gatt.readCharacteristic(characteristic)

// Writing
characteristic.value = data
gatt.writeCharacteristic(characteristic)

// Callbacks
override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
if (status == BluetoothGatt.GATT_SUCCESS) {
val value = characteristic.value
} else {
// Handle read failure
Log.e("BluetoothGatt", "Characteristic read failed with status: $status")
}
}

override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
if (status == BluetoothGatt.GATT_SUCCESS) {
// Write successful
} else {
// Handle write failure
Log.e("BluetoothGatt", "Characteristic write failed with status: $status")
}
}
\`\`\`

### 6. Subscribing to Notifications

- **Input:** Target `BluetoothGattCharacteristic`.
- **Output:** Value updates delivered to `onCharacteristicChanged`.

**iOS**
`swift
peripheral.setNotifyValue(true, for: characteristic)`

**Android**
\`\`\`kotlin
// 1. Enable local notifications
gatt.setCharacteristicNotification(characteristic, true)

// 2. Write to the descriptor to enable remote notifications
val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
gatt.writeDescriptor(descriptor)

// Callback
override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
val newValue = characteristic.value
}
\`\`\`

### 7. Disconnecting and Cleanup

You **Must** properly close the GATT client to release resources.

- **Input:** Active `BluetoothGatt` instance.
- **Output:** Connection closed and resources freed.

**iOS**
`swift
centralManager.cancelPeripheralConnection(peripheral)`

**Android**
`kotlin
gatt.disconnect()
gatt.close() // Critical to call close() to free resources`

### 8. Modern Android Bluetooth Architecture

To reduce callback complexity and improve testability, you **Must** wrap
Bluetooth callbacks in suspend functions or expose them as a Flow.

#### Coroutines and Flows for Asynchronous Operations

- **Input:** An asynchronous callback-based API like `BluetoothLeScanner`.
- **Output:** A Kotlin `Flow` that emits `ScanResult` objects and can be collected in a structured concurrency scope.

```kotlin
// Example: Exposing scan results as a Flow
fun scanForDevices(scanner: BluetoothLeScanner, filter: ScanFilter): Flow<ScanResult> = callbackFlow {
   val callback = object : ScanCallback() {
       override fun onScanResult(callbackType: Int, result: ScanResult) {
           trySend(result)
       }
       override fun onScanFailed(errorCode: Int) {
           close(Exception("Scan failed with error: $errorCode"))
       }
   }
   scanner.startScan(listOf(filter), ScanSettings.Builder().build(), callback)
   awaitClose { scanner.stopScan(callback) }
}
```

#### Background Operations \& Android 14+ Constraints

- **Input:** Data to sync or connection to maintain.
- **Output:** A running Foreground Service that keeps the connection alive.

- **Background Scanning:** For power-efficient background scanning, use
  `BluetoothLeScanner.startScan()` with a `PendingIntent` instead of a running
  service. This allows the system to wake your app only when a matching device
  is found.

- **Foreground Services:** For long-running connections (e.g., data sync), you
  **Must** use a Foreground Service. On Android 14 (API 34) and higher, you
  **Must** specify the foreground service type as `connectedDevice` in your
  manifest:
  `xml
  <service
  android:name=".MyBluetoothService"
  android:foregroundServiceType="connectedDevice" />`

#### Companion Device Manager

- **Input:** `AssociationRequest` defining the device profile.
- **Output:** A `ScanResult` for the chosen device, with the system handling the UI and permissions.

For devices that serve as companions (wearables, fitness trackers), use the
`CompanionDeviceManager` API. It simplifies scanning and pairing, providing a
better user experience and potentially bypassing the need for location
permissions.

```kotlin
val deviceManager = context.getSystemService(CompanionDeviceManager::class.java)
val request = AssociationRequest.Builder()
   .addDeviceFilter(BluetoothLeDeviceFilter.Builder().setNamePattern(Pattern.compile("MyDevice.*")).build())
   .setSingleDevice(true)
   .build()

deviceManager.associate(request, object : CompanionDeviceManager.Callback() {
   override fun onDeviceFound(chooserLauncher: IntentSender) {
       // Launch the system UI chooser
       intentSenderLauncher.launch(IntentSenderRequest.Builder(chooserLauncher).build())
   }
   override fun onFailure(error: CharSequence?) {
       // Handle failure
   }
}, null)
```

#### Dependency Injection (Hilt)

- **Input:** App or Activity context.
- **Output:** Injected Bluetooth dependencies.

For testability and lifecycle management, use Hilt to inject `BluetoothAdapter`
and `BluetoothLeScanner` into your ViewModels or repositories.

## Quality Control: Antipatterns

### DO

- **DO** check `Build.VERSION.SDK_INT` at runtime before requesting permissions to handle the Android 12 split.
- **DO** filter logcat by specific tags like "BluetoothGatt" to isolate relevant errors during test execution.
- **DO** call `gatt.close()` when you are done with a connection to prevent resource leaks.
- **DO** use Kotlin Coroutines or Flows to handle asynchronous Bluetooth operations instead of raw callbacks where possible.
- **DO** specify `foregroundServiceType="connectedDevice"` on Android 14+ for long-running Bluetooth connections.

### DON'T

- **DON'T** assume the `advertisementData` byte format is identical between iOS and Android.
- **DON'T** perform heavy operations on the GATT callback thread; offload them to a background worker or coroutine.
- **DON'T** start a scan without first checking if the `BluetoothAdapter` is enabled on the device.
- **DON'T** forget to request runtime permissions before calling `startScan()`.

### NEVER

- **NEVER** request `ACCESS_FINE_LOCATION` on Android 12+ for Bluetooth scanning without the `android:usesPermissionFlags="neverForLocation"` flag in the manifest if your app does not derive physical location from Bluetooth scans. Failure to do so may cause Google Play policy violations.