package com.example.blescanner.presentation.scanner.screen

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.mutableStateMapOf
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
import java.nio.charset.Charset
import java.util.UUID

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
        DevicesContent(scannerUiState, scannerViewModel)
    }
}

@Composable
fun DevicesContent(
    scannerUiState: ScannerUiState,
    scannerViewModel: ScannerViewModel,
    modifier: Modifier = Modifier
) {
    val devices = scannerUiState.devices
    val notifications = scannerUiState.notifications
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    val serviceInput = remember { mutableStateMapOf<String, String>() }
    val charInput = remember { mutableStateMapOf<String, String>() }
    val subscribed = remember { mutableStateMapOf<String, Boolean>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
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
                        verticalArrangement = Arrangement.spacedBy(8.dp),

                        ) {
                        items(devices, key = { it.address }) { device ->
                            val addr = device.address
                            val isExpanded = expanded[addr] == true

                            DeviceRow(
                                modifier = Modifier.animateItem(
                                    fadeInSpec = tween(500),
                                    fadeOutSpec = tween(500),
                                ),
                                device = device,
                                isExpanded = isExpanded,
                                onToggleExpanded = {
                                    expanded[addr] = !(expanded[addr] ?: false)
                                },
                                serviceText = serviceInput[addr] ?: "",
                                charText = charInput[addr] ?: "",
                                onServiceChange = { serviceInput[addr] = it },
                                onCharChange = { charInput[addr] = it },
                                isSubscribed = subscribed[addr] == true,
                                onConnect = { scannerViewModel.connectDevice(addr) },
                                onDisconnect = { scannerViewModel.disconnectDevice(addr) },
                                onRead = {
                                    val s = (serviceInput[addr] ?: "").ifBlank { return@DeviceRow }
                                    val c = (charInput[addr] ?: "").ifBlank { return@DeviceRow }
                                    scannerViewModel.readCharacteristic(addr, s, c)
                                },
                                onSubscribeToggle = {
                                    val s = (serviceInput[addr] ?: "").ifBlank { return@DeviceRow }
                                    val c = (charInput[addr] ?: "").ifBlank { return@DeviceRow }
                                    val new = subscribed[addr] != true
                                    subscribed[addr] = new
                                    if (new) {
                                        scannerViewModel.subscribeToCharacteristic(addr, s, c)
                                    }
                                }
                            )
                        }
                        if (notifications.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Notifications:", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            items(notifications) { note ->
                                NotificationRow(note.deviceAddress, note.characteristicUuid, note.data)
                            }
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
fun DeviceRow(
    modifier: Modifier = Modifier,
    device: BleDevice,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    serviceText: String,
    charText: String,
    onServiceChange: (String) -> Unit,
    onCharChange: (String) -> Unit,
    isSubscribed: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onRead: () -> Unit,
    onSubscribeToggle: () -> Unit
) {
    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(device.name ?: "(Unknown)", style = MaterialTheme.typography.titleMedium)
                    Text(device.address, style = MaterialTheme.typography.bodyMedium)
                    Text("RSSI: ${device.rssi} dBm", style = MaterialTheme.typography.bodySmall)
                }

                // Simple connect/disconnect buttons — you can adjust by connection state
                Column(horizontalAlignment = Alignment.End) {
                    Button(
                        onClick = onConnect,
                        modifier = Modifier.height(36.dp)
                    ) { Text("Connect") }
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = onDisconnect,
                        modifier = Modifier.height(36.dp)
                    ) { Text("Disconnect") }
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(onClick = onToggleExpanded, modifier = Modifier.height(36.dp)) {
                        Text(if (isExpanded) "Hide" else "Details")
                    }
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = serviceText,
                    onValueChange = onServiceChange,
                    label = { Text("Service UUID (short or full)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = charText,
                    onValueChange = onCharChange,
                    label = { Text("Characteristic UUID") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onRead) { Text("Read") }
                    Button(onClick = onSubscribeToggle) { Text(if (isSubscribed) "Unsubscribe" else "Subscribe") }
                }

                Spacer(modifier = Modifier.height(8.dp))
                if (device.serviceUuids.isNotEmpty()) {
                    Text(
                        "Advertised services: ${device.serviceUuids.joinToString()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (device.manufacturerData.isNotEmpty()) {
                    Text("Manufacturer data present", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun NotificationRow(deviceAddress: String, uuid: UUID, bytes: ByteArray) {
    val asText = try {
        bytes.toString(Charset.forName("UTF-8"))
    } catch (t: Throwable) {
        null
    }
    val asHex = bytes.joinToString(separator = " ") { "%02X".format(it) }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(deviceAddress, style = MaterialTheme.typography.bodyMedium)
                Text(uuid.toString(), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text("Hex: $asHex", style = MaterialTheme.typography.bodySmall)
            asText?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Text: $it", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}