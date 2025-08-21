package com.example.blescanner.domain.useCase

import com.example.blescanner.data.model.BleDevice
import com.example.blescanner.domain.repository.ScannerRepository
import kotlinx.coroutines.flow.Flow

class ObserveDevicesUseCase(
    private val repository: ScannerRepository
) {
    operator fun invoke(): Flow<List<BleDevice>> = repository.devices
}