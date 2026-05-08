# cc-blueconnect-sdk

Lightweight Android Bluetooth toolkit for **scanning, connecting, and reading frames** from
BLE GATT, Bluetooth Classic (SPP/RFCOMM), and Chipsea-style advertisement-broadcasting devices
— behind a single coroutine-friendly API.

The SDK is **transport-generic**: it forwards raw frames as `BluetoothFrame(data, bytes)` and
lets you decode them yourself. Domain-specific parsers (e.g. weight scales) live in optional
add-on modules so the core stays small.

---

## Modules

| Module                | Artifact                                        | Purpose                                                            |
|-----------------------|-------------------------------------------------|--------------------------------------------------------------------|
| `:core`               | `com.ccubas:cc-blueconnect-core:1.0.0`               | Public API surface, models, `ConnectionStrategy`, storage interface |
| `:bluetooth`          | `com.ccubas:cc-blueconnect-bluetooth:1.0.0`          | Real implementation: GATT, Classic, Chipsea, Demo, scanning, permissions |
| `:storage-datastore`  | `com.ccubas:cc-blueconnect-storage-datastore:1.0.0`  | Optional Preferences DataStore adapter for auto-reconnect          |
| `:parser-weight`      | `com.ccubas:cc-blueconnect-parser-weight:1.0.0`      | Optional decoder for LP7516, BLE Weight Scale, Chipsea v1.1/v2.0   |
| `:ui`                 | `com.ccubas:cc-blueconnect-ui:1.0.0`                 | Optional Compose helpers: permission flow + generic frame viewer    |
| `:app`                | (not published)                                 | Sample app showing how to wire everything together                 |

A consumer that only needs scan + connect can depend on `:core` + `:bluetooth`. The other
three are opt-in.

---

## Installation

Gradle Kotlin DSL:

```kotlin
dependencies {
    implementation("com.ccubas:cc-blueconnect-core:1.0.0")
    implementation("com.ccubas:cc-blueconnect-bluetooth:1.0.0")

    // Optional add-ons:
    implementation("com.ccubas:cc-blueconnect-storage-datastore:1.0.0")
    implementation("com.ccubas:cc-blueconnect-parser-weight:1.0.0")
    implementation("com.ccubas:cc-blueconnect-ui:1.0.0")
}
```

Minimum supported Android: **API 24** (Android 7.0).

---

## Quick start

### 1. Create a client

```kotlin
import com.ccubas.blueconnect.BlueConnect
import com.ccubas.blueconnect.storage.datastore.DataStoreSessionStorage

val client = BlueConnect.create(
    context = applicationContext,
    storage = DataStoreSessionStorage(applicationContext), // optional, persists last device
)
```

If you don't need persistence, omit `storage` (defaults to `InMemorySessionStorage`).

### 2. Request runtime permissions

The Compose helper handles the right permissions for each API level:

```kotlin
RequestBluetoothPermissions(
    onPermissionsGranted = { /* you can scan now */ },
    onPermissionsDenied  = { /* show rationale */ },
)
```

If you're not on Compose, request them yourself — the SDK exposes
`BluetoothPermissionUtils.getRequiredPermissions()`.

### 3. Scan and connect

```kotlin
viewModelScope.launch {
    client.startScan(durationMs = 15_000L)
}

// Observe results
viewModelScope.launch {
    client.discoveredDevices.collect { devices ->
        // devices: Map<String /*MAC*/, DeviceInfo>
    }
}

// When the user picks one
client.connect(device)              // auto-detects strategy
client.connect(device, ConnectionStrategy.BleOnly) // or force one
```

### 4. Read frames

```kotlin
viewModelScope.launch {
    client.lastFrame.collect { frame ->
        // frame: BluetoothFrame? (data: String, bytes: ByteArray?)
        if (frame != null) handle(frame)
    }
}
```

Decode the frame however you like. If your device is a weight scale, the
`:parser-weight` module ships ready-made decoders:

```kotlin
import com.ccubas.blueconnect.parser.weight.WeightFrameParser

val reading = WeightFrameParser.parse(frame) // WeightReading? — null if no protocol matched
reading?.let {
    println("${it.weight} ${it.unit} (stable=${it.isStable}, protocol=${it.protocol})")
}
```

### 5. Disconnect

```kotlin
client.disconnect()
```

---

## Connection strategies

When a device speaks one protocol, just connect and you're done. When it might speak several
(or you don't know yet), pass a `ConnectionStrategy` to control fallback order:

| Strategy        | Order of attempts                       | When to use                                       |
|-----------------|-----------------------------------------|---------------------------------------------------|
| `BleOnly`       | BLE                                     | Known BLE-only device                             |
| `ClassicOnly`   | Classic (SPP)                           | Known Classic-only device (e.g. LP7516)           |
| `ChipseaOnly`   | Chipsea advertisement                   | Known Chipsea / OKOK / similar broadcast scale    |
| `BleFirst`      | BLE → Classic → Chipsea                 | Unknown device, BLE-capable hardware              |
| `ChipseaFirst`  | Chipsea → BLE → Classic                 | Unknown device, looks broadcast-only              |
| `DemoOnly`      | Synthetic                               | Tests / offline development                        |

If you don't pass one, the coordinator picks a sensible default from `device.type` and the
device name.

---

## Demo mode

Useful when you don't have hardware or want UI tests without a real adapter:

```kotlin
client.setDemoMode(true)
```

While demo mode is on, scans return six fake devices and connections produce simulated
LP7516-style frames once a second.

---

## Auto-reconnect

After a successful connection the client persists `(deviceAddress, deviceName, protocol,
timestamp)` through whatever `BluetoothSessionStorage` you injected. Restore the last device
on app start:

```kotlin
val reconnected = client.reconnectToLastDevice()
```

Implementations shipped:

- **`InMemorySessionStorage`** (default) — process-lifetime only, no persistence.
- **`NoOpSessionStorage`** — silently drops every save (use it to opt out).
- **`DataStoreSessionStorage(context)`** — Preferences DataStore, file
  `cc_blueconnect_session.preferences_pb`. From `:storage-datastore`.

If your app already has its own storage layer, implement `BluetoothSessionStorage` directly.

---

## Generic frame viewer (Compose)

The `:ui` module ships a `FrameViewerScreen(client)` that renders scan controls, the connection
state, the device list, and the latest frame (text + hex). Useful as a debug screen for any
device, regardless of payload format.

```kotlin
FrameViewerScreen(client = client)
```

You can layer your own panels on top via `extraSections`:

```kotlin
FrameViewerScreen(client = client) { slots ->
    // slots: { frame: BluetoothFrame?, connectionState: ConnectionState }
    item {
        MyOwnDecoderCard(frame = slots.frame)
    }
}
```

The sample app uses that hook to add a "Parsed weight" card driven by
`WeightFrameParser`.

---

## Permissions

The `:bluetooth` module already declares the permissions it needs in its manifest, so they're
merged into your app automatically:

```xml
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"
    android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH"          android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"    android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" android:maxSdkVersion="30" />
```

You still need to request them at runtime on Android 6+ — see the quick-start section above.

---

## Public API at a glance

```kotlin
interface BlueConnectClient {
    val isScanning:         StateFlow<Boolean>
    val scanError:          SharedFlow<ScanError>
    val discoveredDevices:  StateFlow<Map<String, DeviceInfo>>
    val connectionState:    StateFlow<ConnectionState>
    val lastFrame:          StateFlow<BluetoothFrame?>

    suspend fun startScan(durationMs: Long = 15_000L)
    fun stopScan()
    fun clearDevices()

    fun connect(device: BluetoothDevice, forcedStrategy: ConnectionStrategy? = null): Boolean
    fun disconnect()
    fun resetDeviceConnectionHistory(deviceAddress: String)
    fun setDemoMode(enabled: Boolean)

    suspend fun saveSuccessfulConnection(deviceAddress: String, deviceName: String?, protocol: String)
    suspend fun getLastSession(): SavedDevice?
    suspend fun clearSession()
    suspend fun reconnectToLastDevice(): Boolean
}

data class BluetoothFrame(val data: String, val bytes: ByteArray? = null)
data class DeviceInfo(val device: BluetoothDevice, val rssi: Int)
data class SavedDevice(val deviceAddress: String, val deviceName: String?, val successfulProtocol: String, val lastConnectionTimestamp: Long)

sealed class ConnectionState        // Idle / Scanning / Connecting / Connected / ConnectionFailed / Disconnected
sealed class ScanError              // BluetoothDisabled / AdapterNotAvailable / ScanFailed / PermissionDenied
sealed class ConnectionStrategy     // BleOnly / ClassicOnly / ChipseaOnly / BleFirst / ChipseaFirst / DemoOnly

interface BluetoothSessionStorage { /* save / clear / observe */ }

object BlueConnect {
    fun create(context: Context, storage: BluetoothSessionStorage = InMemorySessionStorage()): BlueConnectClient
}
```

---

## Hilt

The SDK does **not** depend on Hilt. If your app uses it, expose the client through your own
module:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object BlueConnectModule {
    @Provides
    @Singleton
    fun provideClient(@ApplicationContext ctx: Context): BlueConnectClient =
        BlueConnect.create(ctx, DataStoreSessionStorage(ctx))
}
```

---

## Sample app

The `:app` module is a working demo: permission flow → generic frame viewer → "Parsed weight"
card driven by the optional weight parser. Build and install it on a real device:

```bash
./gradlew :app:installDebug
```

> Note: the bundled `gradlew.bat` ships with a known Java 20+ incompatibility (passes an empty
> `-classpath`). On Windows + JDK 20+, run via `bash gradlew …` or upgrade the wrapper:
> `./gradlew wrapper --gradle-version 8.13.1`.

---

## Project layout

```
.
├── core/                 # public API + models + parser interface (no transport code)
├── bluetooth/            # GATT / Classic / Chipsea / Demo managers + scanning
├── storage-datastore/    # Preferences DataStore session adapter (opt-in)
├── parser-weight/        # WeightFrameParser + WeightReading (opt-in)
├── ui/                   # RequestBluetoothPermissions + FrameViewerScreen (opt-in)
└── app/                  # sample app; not published
```

---

## Roadmap

- Maven publishing setup (`maven-publish` + signing) — TBD.
- Optional `:hilt` module to skip the boilerplate above.
- `BluetoothFrame` write API for two-way protocols (GATT writes, SPP writes).
- More built-in decoders as they're requested (BLE Heart Rate, Battery Service, etc.).
