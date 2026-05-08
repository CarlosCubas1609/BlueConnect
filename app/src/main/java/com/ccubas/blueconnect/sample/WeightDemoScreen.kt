package com.ccubas.blueconnect.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ccubas.blueconnect.core.BlueConnectClient
import com.ccubas.blueconnect.core.model.BluetoothFrame
import com.ccubas.blueconnect.parser.weight.WeightFrameParser
import com.ccubas.blueconnect.parser.weight.WeightReading
import com.ccubas.blueconnect.ui.FrameViewerScreen

/**
 * Demo screen that layers the weight parser on top of the generic [FrameViewerScreen].
 *
 * This is intentionally in the sample app, not in the library: the SDK stays generic and the
 * weight-specific UI sits next to the consumer code that actually needs it.
 */
@Composable
fun WeightDemoScreen(client: BlueConnectClient) {
    FrameViewerScreen(client = client) { slots ->
        item { ParsedWeightCard(frame = slots.frame) }
    }
}

@Composable
private fun ParsedWeightCard(frame: BluetoothFrame?) {
    val reading: WeightReading? = remember(frame) {
        frame?.let { WeightFrameParser.parse(it) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Parsed weight",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )

            val text = when {
                frame == null -> "No frame received yet."
                reading == null -> "Frame did not match any known weight protocol."
                else -> "${"%.3f".format(reading.weight)} ${reading.unit} · " +
                    "stable=${reading.isStable} · protocol=${reading.protocol}"
            }

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ParsedWeightCardPreview_Lp7516() {
    ParsedWeightCard(frame = BluetoothFrame(data = "ST,GS,+ 12.345kg", bytes = null))
}

@Preview(showBackground = true)
@Composable
private fun ParsedWeightCardPreview_Empty() {
    ParsedWeightCard(frame = null)
}

@Preview(showBackground = true)
@Composable
private fun ParsedWeightCardPreview_Unparseable() {
    ParsedWeightCard(frame = BluetoothFrame(data = "anything", bytes = null))
}
