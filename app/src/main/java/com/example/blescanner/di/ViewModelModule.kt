package com.example.blescanner.di

import com.example.blescanner.presentation.scanner.viewModel.ScannerViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::ScannerViewModel)
}