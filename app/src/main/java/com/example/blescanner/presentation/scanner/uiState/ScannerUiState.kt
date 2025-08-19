package com.example.blescanner.presentation.scanner.uiState

import com.example.blescanner.data.model.BleDevice

data class ScannerUiState(
    val isScanning: Boolean = false,
    val devices: List<BleDevice> = emptyList(),
    val error: String? = null
)
