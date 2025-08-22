package com.example.blescanner.domain.useCase

import com.example.blescanner.domain.repository.ScannerRepository
import com.example.blescanner.presentation.scanner.uiState.NotificationUi
import kotlinx.coroutines.flow.Flow

class ObserveNotificationsUseCase(
    private val repository: ScannerRepository
) {
    /**
     * Emits Triple(deviceAddress, characteristicUuid, bytes)
     */
    operator fun invoke(): Flow<NotificationUi?> = repository.notifications
}