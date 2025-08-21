package com.example.blescanner.domain.useCase

import com.example.blescanner.domain.repository.ScannerRepository
import io.github.aakira.napier.Napier
import java.util.UUID

class SubscribeToCharacteristicUseCase(
    private val scannerRepository: ScannerRepository
) {

    operator fun invoke(
        deviceAddress: String,
        serviceUuid: UUID,
        charUuid: UUID
    ) {
        scannerRepository.runCatching {
            subscribeToCharacteristic(
                deviceAddress = deviceAddress,
                serviceUuid = serviceUuid,
                charUuid = charUuid
            )
        }.onFailure {
            Napier.d("Error in use case $it")
        }
    }
}