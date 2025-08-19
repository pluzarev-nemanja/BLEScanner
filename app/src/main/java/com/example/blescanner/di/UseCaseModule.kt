package com.example.blescanner.di

import com.example.blescanner.domain.useCase.ObserveScanStateUseCase
import com.example.blescanner.domain.useCase.StartScanUseCase
import com.example.blescanner.domain.useCase.StopScanUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val useCaseModule = module {
    factoryOf(::StartScanUseCase)
    factoryOf(::StopScanUseCase)
    factoryOf(::ObserveScanStateUseCase)
}