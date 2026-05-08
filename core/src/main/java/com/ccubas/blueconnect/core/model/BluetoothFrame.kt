package com.ccubas.blueconnect.core.model

/**
 * One frame as it arrived from a connected Bluetooth device. The SDK does not interpret it:
 * transports forward whatever they receive (a text line for SPP / classic, raw bytes for
 * BLE GATT or advertisement payloads) and the consumer decides how to decode it.
 *
 * @param data Text representation. For text protocols this is the line as received; for
 *             binary protocols it is the hex dump of [bytes] for easy logging.
 * @param bytes Original binary payload, when the source is binary (BLE GATT, manufacturer
 *              advertisement data). Null for text-only protocols (SPP, Demo).
 */
data class BluetoothFrame(
    val data: String,
    val bytes: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BluetoothFrame

        if (data != other.data) return false
        if (bytes != null) {
            if (other.bytes == null) return false
            if (!bytes.contentEquals(other.bytes)) return false
        } else if (other.bytes != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.hashCode()
        result = 31 * result + (bytes?.contentHashCode() ?: 0)
        return result
    }
}
