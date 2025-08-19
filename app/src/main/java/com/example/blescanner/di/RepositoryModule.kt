package com.example.blescanner.di

import com.example.blescanner.data.repository.ScannerRepositoryImpl
import com.example.blescanner.domain.repository.ScannerRepository
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val repositoryModule = module {
    single<ScannerRepository> { ScannerRepositoryImpl(androidApplication()) }
}