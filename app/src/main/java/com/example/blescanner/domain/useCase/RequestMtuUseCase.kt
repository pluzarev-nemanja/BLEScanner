package com.example.blescanner.domain.useCase

import com.example.blescanner.domain.repository.ScannerRepository

class RequestMtuUseCase(
    private val repository: ScannerRepository
) {
    operator fun invoke(deviceAddress: String, mtu: Int) = repository.requestMtu(deviceAddress, mtu)
}