package com.ccubas.blueconnect.parser.weight

import android.util.Log
import com.ccubas.blueconnect.core.model.BluetoothFrame

/**
 * Optional helper that decodes [BluetoothFrame]s coming out of common weight-scale protocols.
 *
 * The SDK itself stays generic — it just hands raw frames to consumers. Pull this module in
 * (`cc-blueconnect-parser-weight`) when your device speaks one of these formats:
 *
 * - **LP7516 (Classic SPP)** — text frames like `ST,GS,+ 10.5kg`.
 * - **BLE Weight Scale Service (GATT)** — standard binary format (flags + UINT16 weight).
 * - **Chipsea v2.0 / v1.1** — manufacturer advertisement payloads (15+ bytes, magic `0xCA`).
 * - **Custom Chipsea (0x??C0)** — reverse-engineered variant; weight in bytes 0–1 big-endian / 100.
 * - **Plain text** — `12.34 kg`, `12.34kg`, `12.34`.
 *
 * Format detection is automatic: callers pass the [BluetoothFrame] they received from the
 * client and get back a [WeightReading], or null if no parser matched.
 */
object WeightFrameParser {

    private const val TAG = "WeightFrameParser"

    /** Try every known format; return the first match. */
    fun parse(frame: BluetoothFrame): WeightReading? {
        Log.d(TAG, "Attempting to parse frame: ${frame.data}")

        frame.bytes?.let { bytes ->
            if (bytes.isNotEmpty() && bytes[0] == 0xCA.toByte()) {
                Log.d(TAG, "Detected Chipsea format (magic byte 0xCA)")
                parseChipseaData(bytes)?.let { return it }
            }

            if (bytes.size >= 6) {
                parseCustomChipseaData(bytes)?.let {
                    Log.d(TAG, "Detected Custom Chipsea format")
                    return it
                }
            }

            if (bytes.size >= 3) {
                parseBLEData(bytes)?.let {
                    Log.d(TAG, "Detected BLE Weight Scale format")
                    return it
                }
            }

            parseSimpleBinary(bytes)?.let {
                Log.d(TAG, "Detected simple binary format")
                return it
            }
        }

        parseTextData(frame.data)?.let {
            Log.d(TAG, "Detected text format: ${it.protocol}")
            return it
        }

        Log.w(TAG, "Could not parse frame in any known format")
        return null
    }

    // ==================== TEXT PARSERS ====================

    private fun parseTextData(textData: String): WeightReading? = try {
        when {
            textData.matches(Regex("(ST|US),(GS|NT),([+-])\\s*([\\d.]+)(kg|g|lb|oz)", RegexOption.IGNORE_CASE)) ->
                parseLP7516Format(textData)

            textData.matches(Regex(".*([+-])?\\s*([\\d.]+)\\s*(kg|g|lb|oz).*", RegexOption.IGNORE_CASE)) ->
                parseSimpleTextFormat(textData)

            textData.matches(Regex("([+-])?\\s*([\\d.]+)")) ->
                parseNumberOnlyFormat(textData)

            else -> null
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error parsing text data", e)
        null
    }

    private fun parseLP7516Format(line: String): WeightReading? {
        val match = Regex("(ST|US),(GS|NT),([+-])\\s*([\\d.]+)(kg|g|lb|oz)", RegexOption.IGNORE_CASE)
            .find(line) ?: return null

        val state = match.groupValues[1]
        val polarity = match.groupValues[3]
        val value = match.groupValues[4]
        val unit = match.groupValues[5]

        val isStable = state.equals("ST", ignoreCase = true)
        val weightValue = value.toDoubleOrNull() ?: 0.0
        val finalWeight = if (polarity == "-") -weightValue else weightValue

        return WeightReading(
            weight = finalWeight,
            unit = unit.lowercase(),
            isStable = isStable,
            protocol = "LP7516",
        )
    }

    private fun parseSimpleTextFormat(line: String): WeightReading? {
        val match = Regex("([+-])?\\s*([\\d.]+)\\s*(kg|g|lb|oz)", RegexOption.IGNORE_CASE).find(line)
            ?: return null

        val polarity = match.groupValues[1].ifEmpty { "+" }
        val value = match.groupValues[2]
        val unit = match.groupValues[3]

        val weightValue = value.toDoubleOrNull() ?: 0.0
        val finalWeight = if (polarity == "-") -weightValue else weightValue

        return WeightReading(
            weight = finalWeight,
            unit = unit.lowercase(),
            isStable = true,
            protocol = "Simple",
        )
    }

    private fun parseNumberOnlyFormat(line: String): WeightReading? {
        val match = Regex("([+-])?\\s*([\\d.]+)").find(line) ?: return null

        val polarity = match.groupValues[1].ifEmpty { "+" }
        val value = match.groupValues[2]

        val weightValue = value.toDoubleOrNull() ?: 0.0
        val finalWeight = if (polarity == "-") -weightValue else weightValue

        return WeightReading(
            weight = finalWeight,
            unit = "kg",
            isStable = true,
            protocol = "NumberOnly",
        )
    }

    // ==================== BINARY PARSERS ====================

    private fun parseBLEData(data: ByteArray): WeightReading? = try {
        if (data.size < 3) {
            null
        } else {
            val flags = data[0].toInt()
            val isKg = (flags and 0x01) == 0
            val weightValue = ((data[2].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
            val weight = if (isKg) weightValue * 0.005 else weightValue * 0.01
            val unit = if (isKg) "kg" else "lb"
            val isStable = (flags and 0x04) != 0

            WeightReading(
                weight = weight,
                unit = unit,
                isStable = isStable,
                protocol = "BLE",
            )
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error parsing BLE format", e)
        null
    }

    private fun parseChipseaData(data: ByteArray): WeightReading? {
        if (data.size < 15) return null
        if (data[0] != 0xCA.toByte()) return null

        return try {
            val scaleProperty = data[7].toInt() and 0xFF
            val unit = when (scaleProperty and 0x0F) {
                0x00 -> "kg"
                0x01 -> "lb"
                0x02 -> "st"
                else -> "kg"
            }

            val measurementStatus = data[8].toInt() and 0xFF
            val isStable = measurementStatus == 0x01

            val weightRaw = ((data[11].toInt() and 0xFF) shl 8) or (data[10].toInt() and 0xFF)
            val weight = weightRaw / 10.0

            WeightReading(
                weight = weight,
                unit = unit,
                isStable = isStable,
                protocol = "Chipsea",
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Chipsea data", e)
            null
        }
    }

    private fun parseCustomChipseaData(data: ByteArray): WeightReading? {
        if (data.size < 6) return null

        return try {
            val weightRaw = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
            val weight = weightRaw / 100.0

            if (weight < 0.0 || weight > 500.0) return null

            val unitByte = if (data.size > 4) data[4].toInt() and 0xFF else 0x0A
            val unit = when (unitByte) {
                0x0A -> "kg"
                0x0B -> "lb"
                0x0C -> "jin"
                else -> "kg"
            }

            val isStable = weight > 0.0

            WeightReading(
                weight = weight,
                unit = unit,
                isStable = isStable,
                protocol = "CustomChipsea",
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Custom Chipsea data", e)
            null
        }
    }

    private fun parseSimpleBinary(data: ByteArray): WeightReading? = when (data.size) {
        2 -> parseSimpleBinary2Bytes(data)
        4 -> parseSimpleBinary4Bytes(data)
        else -> null
    }

    private fun parseSimpleBinary2Bytes(data: ByteArray): WeightReading? = try {
        val weight = ((data[1].toInt() and 0xFF) shl 8) or (data[0].toInt() and 0xFF)
        WeightReading(
            weight = weight / 100.0,
            unit = "kg",
            isStable = true,
            protocol = "Simple2B",
        )
    } catch (e: Exception) {
        null
    }

    private fun parseSimpleBinary4Bytes(data: ByteArray): WeightReading? = try {
        val weight = java.nio.ByteBuffer.wrap(data)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .float.toDouble()

        WeightReading(
            weight = weight,
            unit = "kg",
            isStable = true,
            protocol = "Simple4B",
        )
    } catch (e: Exception) {
        null
    }
}
