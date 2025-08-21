package com.example.blescanner.domain.useCase

import com.example.blescanner.domain.repository.ScannerRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ObserveNotificationsUseCase(
    private val repository: ScannerRepository
) {
    /**
     * Emits Triple(deviceAddress, characteristicUuid, bytes)
     */
    operator fun invoke(): Flow<Triple<String, UUID, ByteArray>> = repository.notifications
}