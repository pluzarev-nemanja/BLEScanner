package com.example.blescanner.domain.useCase

import com.example.blescanner.domain.repository.ScannerRepository
import io.github.aakira.napier.Napier

class StartScanUseCase(
    private val scannerRepository: ScannerRepository
) {

    suspend operator fun invoke(filters: List<String> = emptyList()) {
        scannerRepository.runCatching {
            startScan(filters = filters)
        }.onFailure {
            Napier.d("Error in use case $it")
        }
    }
}