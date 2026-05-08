package com.ccubas.blueconnect.parser.weight

/**
 * Parsed weight reading produced by [WeightFrameParser].
 *
 * Pure domain type — no Android dependencies. Holds the numeric value, the unit string as
 * advertised by the device, a stability flag, and (when known) the protocol it was decoded from.
 */
data class WeightReading(
    val weight: Double,
    val unit: String,
    val isStable: Boolean,
    /** Protocol the parser matched (e.g. "LP7516", "BLE", "Chipsea", "CustomChipsea"). */
    val protocol: String? = null,
)
