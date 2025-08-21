package com.example.blescanner.domain.useCase

import com.example.blescanner.domain.event.ConnectionEvent
import com.example.blescanner.domain.repository.ScannerRepository
import kotlinx.coroutines.flow.Flow

class ObserveConnectionEventsUseCase(
    private val repository: ScannerRepository
) {
    operator fun invoke(): Flow<ConnectionEvent> = repository.connectionEvents
}