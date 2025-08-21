package com.example.blescanner.di

import com.example.blescanner.domain.useCase.ConnectBleDeviceUseCase
import com.example.blescanner.domain.useCase.DisconnectDeviceUseCase
import com.example.blescanner.domain.useCase.ObserveConnectionEventsUseCase
import com.example.blescanner.domain.useCase.ObserveDeviceServicesUseCase
import com.example.blescanner.domain.useCase.ObserveDevicesUseCase
import com.example.blescanner.domain.useCase.ObserveNotificationsUseCase
import com.example.blescanner.domain.useCase.ObserveScanStateUseCase
import com.example.blescanner.domain.useCase.ReadCharacteristicUseCase
import com.example.blescanner.domain.useCase.RequestMtuUseCase
import com.example.blescanner.domain.useCase.StartScanUseCase
import com.example.blescanner.domain.useCase.StopScanUseCase
import com.example.blescanner.domain.useCase.SubscribeToCharacteristicUseCase
import com.example.blescanner.domain.useCase.UnsubscribeCharacteristicUseCase
import com.example.blescanner.domain.useCase.WriteCharacteristicUseCase
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
    factoryOf(::ObserveDevicesUseCase)
    factoryOf(::ObserveConnectionEventsUseCase)
    factoryOf(::ObserveDeviceServicesUseCase)
    factoryOf(::WriteCharacteristicUseCase)
    factoryOf(::UnsubscribeCharacteristicUseCase)
    factoryOf(::RequestMtuUseCase)
    factoryOf(::ObserveNotificationsUseCase)
}