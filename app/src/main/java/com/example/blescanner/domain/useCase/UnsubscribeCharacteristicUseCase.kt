package com.example.blescanner.domain.useCase

import com.example.blescanner.domain.repository.ScannerRepository
import java.util.UUID

class UnsubscribeCharacteristicUseCase(
    private val repository: ScannerRepository
) {
    operator fun invoke(deviceAddress: String, serviceUuid: UUID, charUuid: UUID) =
        repository.unsubscribeCharacteristic(deviceAddress, serviceUuid, charUuid)
}