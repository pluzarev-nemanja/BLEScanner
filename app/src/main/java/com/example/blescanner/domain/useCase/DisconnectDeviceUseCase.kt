package com.example.blescanner.domain.useCase

import com.example.blescanner.domain.repository.ScannerRepository
import io.github.aakira.napier.Napier

class DisconnectDeviceUseCase(
    private val scannerRepository: ScannerRepository
) {

    operator fun invoke(address: String) {
        scannerRepository.runCatching {
            disconnectDevice(address)
        }.onFailure {
            Napier.d("Error in use case $it")
        }
    }
}