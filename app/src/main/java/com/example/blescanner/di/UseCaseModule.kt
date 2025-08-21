package com.example.blescanner.di

import com.example.blescanner.domain.useCase.ConnectBleDeviceUseCase
import com.example.blescanner.domain.useCase.DisconnectDeviceUseCase
import com.example.blescanner.domain.useCase.ObserveScanStateUseCase
import com.example.blescanner.domain.useCase.ReadCharacteristicUseCase
import com.example.blescanner.domain.useCase.StartScanUseCase
import com.example.blescanner.domain.useCase.StopScanUseCase
import com.example.blescanner.domain.useCase.SubscribeToCharacteristicUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val useCaseModule = module {
    factoryOf(::StartScanUseCase)
    factoryOf(::StopScanUseCase)
    factoryOf(::ObserveScanStateUseCase)
    factoryOf(::ConnectBleDeviceUseCase)
    factoryOf(::DisconnectDeviceUseCase)
    factoryOf(::ReadCharacteristicUseCase)
    factoryOf(::SubscribeToCharacteristicUseCase)
}