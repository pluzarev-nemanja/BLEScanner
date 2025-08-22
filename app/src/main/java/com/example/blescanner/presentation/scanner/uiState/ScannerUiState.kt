package com.example.blescanner.presentation.scanner.uiState

import com.example.blescanner.data.model.BleDevice
import com.example.blescanner.data.model.ServiceInfo

data class ScannerUiState(
    val isScanning: Boolean = false,
    val devices: List<BleDevice> = emptyList(),
    val connections: Map<String, ConnectionState> = emptyMap(),
    val services: Map<String, List<ServiceInfo>> = emptyMap(),
    val latestNotification: NotificationUi? = null,
    val error: String? = null
)
