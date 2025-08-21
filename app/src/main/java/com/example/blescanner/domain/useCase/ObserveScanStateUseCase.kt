package com.example.blescanner.domain.useCase

import com.example.blescanner.domain.repository.ScannerRepository
import kotlinx.coroutines.flow.Flow

class ObserveScanStateUseCase(
    private val scannerRepository: ScannerRepository
) {
    operator fun invoke(): Flow<Boolean> = scannerRepository.isScanning
}