package com.example.blescanner.domain.useCase

import com.example.blescanner.data.model.BleDevice
import com.example.blescanner.domain.repository.ScannerRepository
import kotlinx.coroutines.flow.Flow

class ObserveScanStateUseCase(
    private val scannerRepository: ScannerRepository
) {
    val isScanning: Flow<Boolean> get() = scannerRepository.isScanning
    val devices: Flow<List<BleDevice>> get() = scannerRepository.devices
}