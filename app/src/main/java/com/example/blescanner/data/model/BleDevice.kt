package com.example.blescanner.data.model

import java.util.UUID

data class BleDevice(
    val address: String,
    val name: String?,
    val rssi: Int,
    val lastSeen: String,
    val serviceUuids: List<UUID> = emptyList(),
    val manufacturerData: Map<Int, ByteArray> = emptyMap(),
    val serviceData: Map<UUID, ByteArray> = emptyMap(),
    val txPower: Int? = null
) {
    val displayName: String
        get() {
            name?.let { return it }
            manufacturerData.entries.firstOrNull()?.value?.let { bytes ->
                return try {
                    val str = bytes.toString(Charsets.UTF_16).trim()
                    str.ifEmpty { address }
                } catch (e: Exception) {
                    address
                }
            }
            return address
        }
}