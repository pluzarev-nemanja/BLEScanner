package com.example.blescanner.domain.repository

import com.example.blescanner.data.model.BleDevice
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface ScannerRepository {

    val isScanning: Flow<Boolean>
    val devices: Flow<List<BleDevice>>

    suspend fun startScan(filters: List<String> = emptyList())
    suspend fun stopScan()

    fun connectToDevice(address: String)
    fun disconnectDevice(address: String)
    fun readCharacteristic(deviceAddress: String, serviceUuid: UUID, charUuid: UUID)
    fun subscribeToCharacteristic(deviceAddress: String, serviceUuid: UUID, charUuid: UUID)
}