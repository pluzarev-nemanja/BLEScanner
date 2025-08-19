package com.example.blescanner.presentation.scanner.screen

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.blescanner.R
import com.example.blescanner.data.model.BleDevice
import com.example.blescanner.presentation.permission.util.PermissionUtils
import com.example.blescanner.presentation.scanner.uiState.ScannerUiState
import com.example.blescanner.presentation.scanner.viewModel.ScannerViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun ScannerScreen(
    modifier: Modifier = Modifier,
    scannerViewModel: ScannerViewModel = koinViewModel()
) {
    val scannerUiState by scannerViewModel.scannerUiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionsArray = PermissionUtils.permissions.toTypedArray()

    var filter by remember { mutableStateOf("") }
    var pendingStart by remember { mutableStateOf(false) }
    var pendingFilters by remember { mutableStateOf<List<String>>(emptyList()) }

    fun allPermissionsGranted(): Boolean {
        return permissionsArray.all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    val permsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.all { it }
        if (granted && pendingStart) {
            pendingStart = false
            scannerViewModel.startScan(pendingFilters)
        }
    }

    val enableBtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter?.isEnabled == true && pendingStart) {
            pendingStart = false
            scannerViewModel.startScan(pendingFilters)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                label = { Text("Filter by name") },
                modifier = Modifier.weight(1f)
            )

            if (scannerUiState.isScanning) {
                Button(
                    onClick = { scannerViewModel.stopScan() },
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Stop")
                }
            } else {
                Button(
                    onClick = {
                        val filters = filter.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                        if (!allPermissionsGranted()) {
                            pendingStart = true
                            pendingFilters = filters
                            permsLauncher.launch(permissionsArray)
                            return@Button
                        }
                        val adapter = BluetoothAdapter.getDefaultAdapter()
                        if (adapter == null) {
                            return@Button
                        }
                        if (!adapter.isEnabled) {
                            pendingStart = true
                            pendingFilters = filters
                            enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                            return@Button
                        }

                        scannerViewModel.startScan(filters)
                    },
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Scan")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        scannerUiState.error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
        DevicesContent(scannerUiState)
    }
}

@Composable
fun DevicesContent(
    scannerUiState: ScannerUiState,
    modifier: Modifier = Modifier
) {
    val devices = scannerUiState.devices

    Column(modifier = modifier
        .fillMaxSize()
        .padding(16.dp)) {
        if (scannerUiState.isScanning && devices.isNotEmpty()) {
            Text("Scanning...", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {

            when {
                scannerUiState.isScanning && devices.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Scanning...", style = MaterialTheme.typography.titleMedium)
                    }
                }

                devices.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(R.drawable.ic_bluethooth),
                                contentDescription = null,
                                modifier = Modifier.size(45.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Start scanning", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(devices, key = { it.address }) { device ->
                            DeviceRow(device)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Devices: ${devices.size}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun DeviceRow(device: BleDevice) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(device.name ?: "(Unknown)", style = MaterialTheme.typography.titleMedium)
            Text(device.address, style = MaterialTheme.typography.bodyMedium)
            Text("RSSI: ${device.rssi} dBm", style = MaterialTheme.typography.bodySmall)
            device.txPower?.let {
                Text(
                    "TxPower: $it dBm",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (device.serviceUuids.isNotEmpty())
                Text(
                    "Services: ${device.serviceUuids.joinToString()}",
                    style = MaterialTheme.typography.bodySmall
                )
            if (device.manufacturerData.isNotEmpty())
                Text(
                    "Mfr Data: ${
                        device.manufacturerData.map { (id, bytes) -> "0x${id.toString(16)}=${bytes.toHexString()}" }
                            .joinToString()
                    }",
                    style = MaterialTheme.typography.bodySmall
                )
        }
    }
}