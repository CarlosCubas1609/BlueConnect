package com.ccubas.blueconnect.internal

import com.ccubas.blueconnect.core.model.BluetoothFrame
import com.ccubas.blueconnect.core.model.ConnectionState

/**
 * Bridge between transport-specific managers and the public coordinator.
 *
 * Each manager (BLE, Classic, Chipsea, Demo) reports lifecycle and data through this
 * listener. The coordinator owns the StateFlows; managers stay stateless on the observable side.
 */
internal interface BluetoothEventListener {

    fun onStateChange(state: ConnectionState)

    fun onFrame(frame: BluetoothFrame?)
}
