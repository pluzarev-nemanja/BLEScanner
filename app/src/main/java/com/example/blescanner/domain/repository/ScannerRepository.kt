package com.example.blescanner.domain.repository

import com.example.blescanner.data.model.BleDevice
import com.example.blescanner.data.model.ServiceInfo
import com.example.blescanner.domain.event.ConnectionEvent
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface ScannerRepository {

    val isScanning: Flow<Boolean>
    val devices: Flow<List<BleDevice>>
    val notifications: Flow<Triple<String, UUID, ByteArray>>
    val deviceServices: Flow<Map<String, List<ServiceInfo>>>
    val connectionEvents: Flow<ConnectionEvent>

    suspend fun startScan(filters: List<String> = emptyList())
    suspend fun stopScan()

    fun connectToDevice(address: String, retries: Int, backoffMs: Long)
    fun writeCharacteristic(
        deviceAddress: String,
        serviceUuid: UUID,
        charUuid: UUID,
        value: ByteArray,
        writeType: Int
    )

    fun disconnectDevice(address: String)
    fun readCharacteristic(deviceAddress: String, serviceUuid: UUID, charUuid: UUID)
    fun subscribeToCharacteristic(deviceAddress: String, serviceUuid: UUID, charUuid: UUID)
    fun unsubscribeCharacteristic(deviceAddress: String, serviceUuid: UUID, charUuid: UUID)
    fun requestMtu(deviceAddress: String, mtu: Int)
}