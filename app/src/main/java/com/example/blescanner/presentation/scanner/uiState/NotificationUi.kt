package com.example.blescanner.presentation.scanner.uiState

import java.util.UUID

data class NotificationUi(
    val deviceAddress: String,
    val characteristicUuid: UUID,
    val data: ByteArray,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NotificationUi

        if (timestamp != other.timestamp) return false
        if (deviceAddress != other.deviceAddress) return false
        if (characteristicUuid != other.characteristicUuid) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + deviceAddress.hashCode()
        result = 31 * result + characteristicUuid.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}