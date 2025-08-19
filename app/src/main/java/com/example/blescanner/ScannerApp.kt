package com.example.blescanner

import android.app.Application
import com.example.blescanner.di.repositoryModule
import com.example.blescanner.di.useCaseModule
import com.example.blescanner.di.viewModelModule
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ScannerApp: Application() {

    override fun onCreate() {
        super.onCreate()
        Napier.base(DebugAntilog())
        startKoin {
            androidContext(this@ScannerApp)
            modules(repositoryModule.plus(useCaseModule).plus(viewModelModule))
        }
    }
}