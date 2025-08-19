package com.example.blescanner.domain.repository

import com.example.blescanner.data.model.BleDevice
import kotlinx.coroutines.flow.Flow

interface ScannerRepository {

    val isScanning: Flow<Boolean>
    val devices: Flow<List<BleDevice>>

    suspend fun startScan(filters: List<String> = emptyList())
    suspend fun stopScan()
}