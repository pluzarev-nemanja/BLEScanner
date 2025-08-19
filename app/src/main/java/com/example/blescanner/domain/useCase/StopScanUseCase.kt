package com.example.blescanner.domain.useCase

import com.example.blescanner.domain.repository.ScannerRepository
import io.github.aakira.napier.Napier

class StopScanUseCase(
    private val scannerRepository: ScannerRepository
) {
    suspend operator fun invoke() {
        scannerRepository.runCatching {
            stopScan()
        }.onFailure {
            Napier.d("Error in use case: $it")
        }
    }
}