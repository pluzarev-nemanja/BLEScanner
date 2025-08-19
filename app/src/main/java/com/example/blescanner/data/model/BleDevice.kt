package com.example.blescanner.data.model

import java.util.UUID

data class BleDevice(
    val address: String,
    val name: String?,
    val rssi: Int,
    val lastSeen: Long,
    val serviceUuids: List<UUID> = emptyList(),
    val manufacturerData: Map<Int, ByteArray> = emptyMap(),
    val serviceData: Map<UUID, ByteArray> = emptyMap(),
    val txPower: Int? = null
)