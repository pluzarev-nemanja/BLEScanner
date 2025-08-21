package com.example.blescanner.domain.event

import com.example.blescanner.data.model.ServiceInfo
import java.util.UUID

sealed class ConnectionEvent {
    data class Connected(val address: String) : ConnectionEvent()
    data class Disconnected(val address: String, val status: Int) : ConnectionEvent()
    data class ConnectFailed(val address: String, val throwable: Throwable) : ConnectionEvent()
    data class ServicesDiscovered(val address: String, val services: List<ServiceInfo>) : ConnectionEvent()
    data class ServicesDiscoveryFailed(val address: String, val status: Int) : ConnectionEvent()
    data class MtuChanged(val address: String, val mtu: Int, val status: Int) : ConnectionEvent()

    data class CharacteristicRead(val address: String, val uuid: UUID, val value: ByteArray) : ConnectionEvent() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as CharacteristicRead

            if (address != other.address) return false
            if (uuid != other.uuid) return false
            if (!value.contentEquals(other.value)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = address.hashCode()
            result = 31 * result + uuid.hashCode()
            result = 31 * result + value.contentHashCode()
            return result
        }
    }

    data class CharacteristicReadFailed(val address: String, val uuid: UUID, val status: Int) : ConnectionEvent()
    data class CharacteristicWriteResult(val address: String, val uuid: UUID, val status: Int) : ConnectionEvent()
}