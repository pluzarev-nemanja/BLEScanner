package com.example.blescanner.presentation.scanner.uiState

import java.util.UUID

data class NotificationUi(
    val deviceAddress: String,
    val serviceUuid: UUID? = null,
    val characteristicUuid: UUID,
    val data: ByteArray,
    val timestamp: Long = System.currentTimeMillis(),
    val rssi: Int? = null,
    val isIndication: Boolean = false
) {
    fun payloadHex(): String = data.toHexString()
    fun payloadUtf8(): String? = data.toUtf8OrNull()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NotificationUi

        if (timestamp != other.timestamp) return false
        if (rssi != other.rssi) return false
        if (isIndication != other.isIndication) return false
        if (deviceAddress != other.deviceAddress) return false
        if (serviceUuid != other.serviceUuid) return false
        if (characteristicUuid != other.characteristicUuid) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + (rssi ?: 0)
        result = 31 * result + isIndication.hashCode()
        result = 31 * result + deviceAddress.hashCode()
        result = 31 * result + (serviceUuid?.hashCode() ?: 0)
        result = 31 * result + characteristicUuid.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

fun ByteArray.toUtf8OrNull(): String? =
    try {
        String(this, Charsets.UTF_8)
    } catch (_: Exception) {
        null
    }
