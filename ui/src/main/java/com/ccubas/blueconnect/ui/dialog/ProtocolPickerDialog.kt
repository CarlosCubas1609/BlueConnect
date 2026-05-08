package com.ccubas.blueconnect.ui.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ccubas.blueconnect.core.ConnectionStrategy

/**
 * Default set of strategies offered to the user: the three single-protocol options.
 *
 * Multi-protocol "Auto" strategies ([ConnectionStrategy.BleFirst],
 * [ConnectionStrategy.ClassicFirst], [ConnectionStrategy.ChipseaFirst]) are intentionally
 * excluded — most apps that show this picker want the user to pick a concrete protocol,
 * and let the SDK auto-detect when no picker is shown. Pass `strategies =` to override.
 *
 * [ConnectionStrategy.DemoOnly] is also excluded; demo mode is a separate global toggle.
 */
val DefaultPickerStrategies: List<ConnectionStrategy> = listOf(
    ConnectionStrategy.BleOnly,
    ConnectionStrategy.ClassicOnly,
    ConnectionStrategy.ChipseaOnly,
)

/** All multi-protocol "Auto" strategies. Useful as `strategies =` if you want them in the picker. */
val AutoPickerStrategies: List<ConnectionStrategy> = listOf(
    ConnectionStrategy.BleFirst,
    ConnectionStrategy.ClassicFirst,
    ConnectionStrategy.ChipseaFirst,
)

/**
 * Material 3 dialog that asks the user which [ConnectionStrategy] to use for a given device.
 *
 * The SDK already lets you call `client.connect(device, strategy)` directly — this composable
 * is just a convenient picker for apps that want the user to make that decision.
 *
 * @param deviceLabel Friendly label shown in the title (e.g. "LP7516 (AA:BB:…)").
 * @param onDismiss Called when the user taps outside or hits Cancel.
 * @param onSelect Called with the chosen strategy when the user confirms.
 * @param strategies Strategies to offer. Defaults to [DefaultPickerStrategies].
 * @param initialSelection Pre-selected option. If null, the first strategy is selected.
 * @param confirmLabel Confirm button label. Defaults to "Connect".
 * @param dismissLabel Cancel button label. Defaults to "Cancel".
 */
@Composable
fun ProtocolPickerDialog(
    deviceLabel: String,
    onDismiss: () -> Unit,
    onSelect: (ConnectionStrategy) -> Unit,
    strategies: List<ConnectionStrategy> = DefaultPickerStrategies,
    initialSelection: ConnectionStrategy? = null,
    confirmLabel: String = "Connect",
    dismissLabel: String = "Cancel",
) {
    var selected by remember(strategies, initialSelection) {
        mutableStateOf(initialSelection ?: strategies.first())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Choose connection protocol",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = deviceLabel,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                strategies.forEach { strategy ->
                    StrategyRow(
                        strategy = strategy,
                        selected = strategy == selected,
                        onClick = { selected = strategy },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(selected) }) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel)
            }
        },
    )
}

@Composable
private fun StrategyRow(
    strategy: ConnectionStrategy,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = strategy.displayName(),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = strategy.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Friendly name used in [ProtocolPickerDialog]. */
fun ConnectionStrategy.displayName(): String = when (this) {
    ConnectionStrategy.BleOnly -> "BLE"
    ConnectionStrategy.ClassicOnly -> "Classic (SPP)"
    ConnectionStrategy.ChipseaOnly -> "Chipsea (advertisement)"
    ConnectionStrategy.BleFirst -> "Auto — BLE first"
    ConnectionStrategy.ClassicFirst -> "Auto — Classic first"
    ConnectionStrategy.ChipseaFirst -> "Auto — Chipsea first"
    ConnectionStrategy.DemoOnly -> "Demo (simulated)"
}

@Preview(showBackground = true)
@Composable
private fun ProtocolPickerDialogPreview_Default() {
    ProtocolPickerDialog(
        deviceLabel = "LP7516 Scale (AA:BB:CC:DD:EE:01)",
        onDismiss = {},
        onSelect = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun ProtocolPickerDialogPreview_WithAutos() {
    ProtocolPickerDialog(
        deviceLabel = "Unknown device (AA:BB:CC:DD:EE:02)",
        onDismiss = {},
        onSelect = {},
        strategies = DefaultPickerStrategies + AutoPickerStrategies,
        initialSelection = ConnectionStrategy.BleFirst,
    )
}
