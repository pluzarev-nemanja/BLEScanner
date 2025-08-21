package com.example.blescanner.domain.useCase

import com.example.blescanner.domain.repository.ScannerRepository

class ConnectBleDeviceUseCase(
    private val scannerRepository: ScannerRepository
) {

    operator fun invoke(address: String, retries: Int = 0, backoffMs: Long = 1000L) =
        scannerRepository.connectToDevice(address, retries, backoffMs)
}