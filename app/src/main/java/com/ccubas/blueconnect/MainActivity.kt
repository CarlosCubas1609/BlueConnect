package com.ccubas.blueconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ccubas.blueconnect.core.BlueConnectClient
import com.ccubas.blueconnect.sample.WeightDemoScreen
import com.ccubas.blueconnect.storage.datastore.DataStoreSessionStorage
import com.ccubas.blueconnect.ui.permission.RequestBluetoothPermissions
import com.ccubas.blueconnect.ui.theme.BlueConnectTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlueConnectTheme {
                BlueConnectSampleApp()
            }
        }
    }
}

@Composable
private fun BlueConnectSampleApp() {
    val context = LocalContext.current
    val client: BlueConnectClient = remember(context) {
        BlueConnect.create(
            context = context.applicationContext,
            storage = DataStoreSessionStorage(context.applicationContext),
        )
    }

    var permissionsState by remember { mutableStateOf<PermissionsState>(PermissionsState.Pending) }

    when (permissionsState) {
        PermissionsState.Pending -> {
            RequestBluetoothPermissions(
                onPermissionsGranted = { permissionsState = PermissionsState.Granted },
                onPermissionsDenied = { permissionsState = PermissionsState.Denied },
            )
            PermissionPlaceholder(text = "Requesting Bluetooth permissions…")
        }

        PermissionsState.Granted -> {
            WeightDemoScreen(client = client)
        }

        PermissionsState.Denied -> {
            PermissionPlaceholder(text = "Bluetooth permissions are required. Open system settings to grant them, then relaunch the app.")
        }
    }
}

@Composable
private fun PermissionPlaceholder(text: String) {
    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.wrapContentSize(),
            )
        }
    }
}

private sealed interface PermissionsState {
    data object Pending : PermissionsState
    data object Granted : PermissionsState
    data object Denied : PermissionsState
}
