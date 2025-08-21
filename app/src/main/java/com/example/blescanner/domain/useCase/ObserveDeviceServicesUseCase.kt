package com.example.blescanner.domain.useCase

import com.example.blescanner.data.model.ServiceInfo
import com.example.blescanner.domain.repository.ScannerRepository
import kotlinx.coroutines.flow.Flow

class ObserveDeviceServicesUseCase(
    private val repository: ScannerRepository
) {
    /**
     * Emits a map of deviceAddress -> list of discovered ServiceInfo
     */
    operator fun invoke(): Flow<Map<String, List<ServiceInfo>>> = repository.deviceServices
}