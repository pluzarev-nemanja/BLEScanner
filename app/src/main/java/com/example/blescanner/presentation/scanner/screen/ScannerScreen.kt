package com.example.blescanner.presentation.scanner.screen

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.blescanner.R
import com.example.blescanner.data.model.BleDevice
import com.example.blescanner.data.model.CharacteristicInfo
import com.example.blescanner.data.model.ServiceInfo
import com.example.blescanner.presentation.permission.util.PermissionUtils
import com.example.blescanner.presentation.scanner.uiState.ConnectionState
import com.example.blescanner.presentation.scanner.uiState.NotificationUi
import com.example.blescanner.presentation.scanner.uiState.ScannerUiState
import com.example.blescanner.presentation.scanner.util.toByteArrayFromInput
import com.example.blescanner.presentation.scanner.util.toDateTimeString
import com.example.blescanner.presentation.scanner.viewModel.ScannerViewModel
import com.example.blescanner.ui.theme.ChipBlue
import com.example.blescanner.ui.theme.ChipGreen
import com.example.blescanner.ui.theme.ChipOrange
import com.example.blescanner.ui.theme.ChipRed
import org.koin.androidx.compose.koinViewModel
import java.util.UUID

@OptIn(ExperimentalAnimationApi::class)
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

    Scaffold(
        modifier = modifier
            .fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    Toast.makeText(
                        context,
                        "Found: ${scannerUiState.devices.size} devices",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                content = {
                    Text("Devices: ${scannerUiState.devices.size}")
                }
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                OutlinedTextField(
                    value = filter,
                    onValueChange = {
                        filter = it
                        scannerViewModel.onFilterTextChanged(it)
                    },
                    label = { Text("Filter by name") },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        if (scannerUiState.isScanning) {
                            scannerViewModel.stopScan()
                        } else {
                            val filters = filter.split(",").map { it.trim() }
                                .filter { it.isNotEmpty() }

                            if (!allPermissionsGranted()) {
                                pendingStart = true
                                pendingFilters = filters
                                permsLauncher.launch(permissionsArray)
                                return@Button
                            }
                            val adapter =
                                BluetoothAdapter.getDefaultAdapter() ?: return@Button
                            if (!adapter.isEnabled) {
                                pendingStart = true
                                pendingFilters = filters
                                enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                                return@Button
                            }

                            scannerViewModel.startScan(filters)
                        }
                    }
                ) {
                    AnimatedContent(
                        targetState = scannerUiState.isScanning,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) with fadeOut(
                                animationSpec = tween(
                                    300
                                )
                            )
                        }
                    ) { isScanning ->
                        if (isScanning) {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = "Stop"
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play"
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            DevicesContent(scannerUiState, scannerViewModel)
        }
    }
}

@Composable
fun DevicesContent(
    scannerUiState: ScannerUiState,
    scannerViewModel: ScannerViewModel,
    modifier: Modifier = Modifier
) {
    val devices = scannerUiState.devices
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    val subscribed = remember { mutableStateMapOf<String, Boolean>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
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
                            val connState =
                                scannerUiState.connections[addr] ?: ConnectionState.DISCONNECTED

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
                                connectionState = connState,
                                onToggleConnection = {
                                    when (connState) {
                                        ConnectionState.CONNECTED -> scannerViewModel.disconnectDevice(
                                            addr
                                        )

                                        ConnectionState.CONNECTING -> {} // do nothing
                                        else -> scannerViewModel.connectDevice(addr)
                                    }
                                },
                                onRead = { serviceUuid, charUuid ->
                                    scannerViewModel.readCharacteristic(addr, serviceUuid, charUuid)
                                },
                                onSubscribeToggle = { serviceUuid, charUuid, shouldSubscribe ->
                                    subscribed[addr] = shouldSubscribe
                                    if (shouldSubscribe) {
                                        scannerViewModel.subscribeToCharacteristic(
                                            addr,
                                            serviceUuid,
                                            charUuid
                                        )
                                    } else {
                                        scannerViewModel.unsubscribeCharacteristic(
                                            addr,
                                            serviceUuid,
                                            charUuid
                                        )
                                    }
                                },
                                services = scannerUiState.services,
                                notification = scannerUiState.latestNotification,
                                scannerViewModel = scannerViewModel,
                                subscribedCharacteristics = emptySet()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceRow(
    modifier: Modifier = Modifier,
    device: BleDevice,
    isExpanded: Boolean,
    connectionState: ConnectionState,
    services: Map<String, List<ServiceInfo>>,
    subscribedCharacteristics: Set<String>,
    notification: NotificationUi?,
    scannerViewModel: ScannerViewModel,
    onRead: (String, String) -> Unit,
    onSubscribeToggle: (String, String, Boolean) -> Unit,
    onToggleConnection: () -> Unit,
    onToggleExpanded: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300)
    )

    ElevatedCard(
        modifier
            .fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BleDeviceItem(
                    modifier = Modifier.weight(1f),
                    device = device
                )
                Controller(
                    modifier = Modifier.fillMaxHeight(),
                    connectionState,
                    onToggleConnection,
                    onToggleExpanded,
                    isExpanded,
                    rotation
                )
            }

            if (isExpanded) {
                ServicesList(
                    modifier = Modifier.fillMaxWidth(),
                    servicesMap = services,
                    onRead = onRead,
                    notification = notification,
                    onSubscribeToggle = onSubscribeToggle,
                    subscribedCharacteristics = subscribedCharacteristics,
                    deviceAddress = device.address,
                    onWrite = { deviceAddress, serviceUuid, charUuid, value ->
                        scannerViewModel.writeToCharacteristic(
                            deviceAddress,
                            serviceUuid,
                            charUuid,
                            value
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun ServicesList(
    modifier: Modifier = Modifier,
    servicesMap: Map<String, List<ServiceInfo>>,
    onRead: (String, String) -> Unit,
    notification: NotificationUi?,
    deviceAddress: String,
    onSubscribeToggle: (String, String, Boolean) -> Unit,
    onWrite: (String, UUID, UUID, ByteArray) -> Unit,
    subscribedCharacteristics: Set<String>
) {
    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }
    var showPopUpMenu by remember { mutableStateOf(false) }
    var writeDialogData by remember { mutableStateOf<Triple<String, UUID, UUID>?>(null) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        servicesMap.forEach { (_, services) ->
            services.forEach { serviceInfo ->
                val expanded = expandedMap[serviceInfo.uuid.toString()] == true
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            expandedMap[serviceInfo.uuid.toString()] = !expanded
                        }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "• Service: ",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = serviceInfo.uuid.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (expanded) {
                        Spacer(Modifier.height(6.dp))
                        serviceInfo.characteristics.forEach { characteristic ->
                            val charKey = "${serviceInfo.uuid}:${characteristic.uuid}"
                            val isSubscribed = subscribedCharacteristics.contains(charKey)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        text = "• Characteristic: ",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Light
                                    )
                                    Text(
                                        text = characteristic.uuid.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                CharacteristicPropertiesRow(characteristic)

                                Spacer(Modifier.height(4.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (characteristic.canRead) {
                                        Button(onClick = {
                                            onRead(
                                                serviceInfo.uuid.toString(),
                                                characteristic.uuid.toString()
                                            )
                                            showPopUpMenu = true
                                        }) {
                                            Text("Read")
                                        }
                                    }
                                    if (characteristic.canWrite) {
                                        Button(onClick = {
                                            writeDialogData = Triple(
                                                deviceAddress,
                                                serviceInfo.uuid,
                                                characteristic.uuid
                                            )
                                        }) {
                                            Text("Write")
                                        }
                                    }
                                    if (characteristic.canNotify) {
                                        Button(onClick = {
                                            onSubscribeToggle(
                                                serviceInfo.uuid.toString(),
                                                characteristic.uuid.toString(),
                                                isSubscribed
                                            )
                                        }) {
                                            Text(if (isSubscribed) "Unsubscribe" else "Subscribe")
                                        }
                                    }
                                }
                                Divider()
                            }
                        }
                    }
                }
            }
        }

        writeDialogData?.let { (deviceAddress, serviceUuid, charUuid) ->
            WriteCharacteristicPopUp(
                onWrite = { bytes ->
                    onWrite(deviceAddress, serviceUuid, charUuid, bytes)
                },
                onDismiss = {
                    writeDialogData = null
                }
            )
        }
        notification?.let {
            if (showPopUpMenu)
                NotificationDialog(
                    notification = it,
                    onDismiss = {
                        showPopUpMenu = false
                    }
                )
        }
    }
}

@Composable
fun WriteCharacteristicPopUp(
    onDismiss: () -> Unit,
    onWrite: (ByteArray) -> Unit
) {
    var textToWrite by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Write to Characteristic") },
        text = {
            Column {
                Text(
                    "Enter value in HEX (e.g., 01 0A FF) or UTF-8 text:",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = textToWrite,
                    onValueChange = { textToWrite = it },
                    placeholder = { Text("e.g., 01 0A FF") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onWrite(textToWrite.toByteArrayFromInput())
                onDismiss()
            }) {
                Text("Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CharacteristicPropertiesRow(characteristic: CharacteristicInfo) {
    Row(
        modifier = Modifier.padding(top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ChipItem("Props:${characteristic.properties}", ChipGreen)
        if (characteristic.canRead) {
            ChipItem("READ", ChipOrange)
        }
        if (characteristic.canNotify) {
            ChipItem("NOTIFY", ChipRed)
        }
        if (characteristic.canWrite) {
            ChipItem("WRITE", ChipBlue)
        }
    }
}

@Composable
fun ChipItem(text: String, backgroundColor: Color) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        shadowElevation = 0.dp
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun Controller(
    modifier: Modifier = Modifier,
    connectionState: ConnectionState,
    onToggleConnection: () -> Unit,
    onToggleExpanded: () -> Unit,
    isExpanded: Boolean,
    rotation: Float
) {
    var showDescriptionArrow by rememberSaveable {
        mutableStateOf(false)
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        when (connectionState) {
            ConnectionState.CONNECTING -> {
                showDescriptionArrow = false
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Connecting")
                    }
                }
            }

            ConnectionState.CONNECTED -> {
                showDescriptionArrow = true
                Button(
                    onClick = onToggleConnection,
                    modifier = Modifier.height(36.dp)
                ) { Text("Disconnect") }
            }

            ConnectionState.DISCONNECTED -> {
                showDescriptionArrow = false
                Button(
                    onClick = onToggleConnection,
                    modifier = Modifier.height(36.dp)
                ) { Text("Connect") }
            }
        }
        Spacer(modifier = Modifier.height(15.dp))
        if (showDescriptionArrow) {
            IconButton(
                onClick = onToggleExpanded,
                modifier = Modifier
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(rotation)
                )
            }
        }
    }
}

@Composable
fun BleDeviceItem(
    device: BleDevice,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Text(
            text = device.displayName,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Address: ${device.address}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "RSSI: ${device.rssi} dBm",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Last Seen: ${device.lastSeen}",
            style = MaterialTheme.typography.bodySmall
        )

        if (device.serviceUuids.isNotEmpty()) {
            Text(
                text = "Services: ${device.serviceUuids.joinToString()}",
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (device.manufacturerData.isNotEmpty()) {
            Text(
                text = "Manufacturer Data: ${device.manufacturerData.keys.joinToString()}",
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (device.serviceData.isNotEmpty()) {
            Text(
                text = "Service Data: ${device.serviceData.keys.joinToString()}",
                style = MaterialTheme.typography.bodySmall
            )
        }

        device.txPower?.let {
            Text(
                text = "TX Power: $it dBm",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun NotificationDialog(
    notification: NotificationUi,
    onDismiss: (() -> Unit)? = null
) {
    val clipboard = LocalClipboardManager.current
    val ctx = LocalContext.current
    val scroll = rememberScrollState()


    val formattedTs = notification.timestamp.toDateTimeString()
    val payloadHex = notification.payloadHex()
    val payloadUtf8 = notification.payloadUtf8()


    AlertDialog(
        onDismissRequest = { onDismiss?.invoke() ?: Unit },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(notification.deviceAddress, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = {
                    val text = buildString {
                        appendLine("Device: ${notification.deviceAddress}")
                        appendLine("Service: ${notification.serviceUuid}")
                        appendLine("Characteristic: ${notification.characteristicUuid}")
                        appendLine("Timestamp: $formattedTs")
                        appendLine("RSSI: ${notification.rssi ?: "N/A"}")
                        appendLine("Indication: ${notification.isIndication}")
                        appendLine()
                        appendLine("Payload (hex): $payloadHex")
                        appendLine("Payload (utf8): ${payloadUtf8 ?: "(not valid UTF-8)"}")
                    }
                    clipboard.setText(AnnotatedString(text))
                    Toast.makeText(ctx, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                SelectionContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scroll)
                            .heightIn(max = 360.dp)
                    ) {
                        Text(
                            text = "Service: ${notification.serviceUuid}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Characteristic: ${notification.characteristicUuid}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Timestamp: $formattedTs",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "RSSI: ${notification.rssi ?: "N/A"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Indication: ${notification.isIndication}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Payload (hex):",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = payloadHex, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Payload (utf8):",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = payloadUtf8 ?: "(not valid UTF-8)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss?.invoke() ?: Unit }) { Text("Close") }
        }
    )
}