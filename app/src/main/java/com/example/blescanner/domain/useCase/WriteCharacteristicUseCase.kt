package com.example.blescanner.domain.useCase

import com.example.blescanner.domain.repository.ScannerRepository
import java.util.UUID

class WriteCharacteristicUseCase(
    private val repository: ScannerRepository
) {
    /**
     * writeType: BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT or WRITE_TYPE_NO_RESPONSE (int).
     */
    operator fun invoke(
        deviceAddress: String,
        serviceUuid: UUID,
        charUuid: UUID,
        value: ByteArray,
        writeType: Int = android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
    ) = repository.writeCharacteristic(deviceAddress, serviceUuid, charUuid, value, writeType)
}
