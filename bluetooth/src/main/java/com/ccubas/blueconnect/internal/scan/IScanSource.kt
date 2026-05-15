package com.ccubas.blueconnect.internal.scan

import android.bluetooth.BluetoothDevice
import com.ccubas.blueconnect.core.model.ScanError
import kotlinx.coroutines.flow.Flow

/**
 * One transport-specific source of scan results (bonded devices, BLE advertisements,
 * Classic inquiry, demo, …).
 *
 * Sources are independent: each one knows how to start its own discovery, emit results
 * through a cold [Flow], and tear down in [kotlinx.coroutines.flow.callbackFlow]'s
 * `awaitClose` when the collector cancels. The coordinator merges them with `flatMapMerge`
 * so adding a new transport is just dropping a new implementation into the factory.
 */
internal interface IScanSource {

    /** Stable identifier — used for logging and error attribution. */
    val name: String

    /**
     * Cold flow. Collecting begins the scan; cancelling the collecting coroutine stops it
     * (each implementation cleans up in `awaitClose`).
     */
    fun scan(): Flow<ScanEvent>
}

/** Result of one scan source emitting through its [IScanSource.scan] flow. */
internal sealed interface ScanEvent {

    /** A device was discovered (or re-discovered with a fresher RSSI). */
    data class DeviceFound(val device: BluetoothDevice, val rssi: Int) : ScanEvent

    /** The source hit a non-fatal error. Other sources keep running. */
    data class Error(val error: ScanError) : ScanEvent
}
