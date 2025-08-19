package com.example.blescanner.presentation.scanner.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blescanner.domain.useCase.ObserveScanStateUseCase
import com.example.blescanner.domain.useCase.StartScanUseCase
import com.example.blescanner.presentation.scanner.uiState.ScannerUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ScannerViewModel(
    private val startScanUseCase: StartScanUseCase,
    private val stopScanUseCase: StartScanUseCase,
    private val observeScanStateUseCase: ObserveScanStateUseCase
) : ViewModel() {

    private val mutableScannerUiState: MutableStateFlow<ScannerUiState> =
        MutableStateFlow(ScannerUiState())
    val scannerUiState: StateFlow<ScannerUiState> = mutableScannerUiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeScanStateUseCase.isScanning
                .combine(observeScanStateUseCase.devices) { scanning, devices ->
                    ScannerUiState(isScanning = scanning, devices = devices)
                }
                .catch { e ->
                    mutableScannerUiState.value =
                        mutableScannerUiState.value.copy(error = e.message)
                }
                .collect { uiState ->
                    mutableScannerUiState.value = uiState
                }
        }
    }

    fun startScan(filters: List<String> = emptyList()) {
        viewModelScope.launch {
            startScanUseCase(filters)
        }
    }

    fun stopScan() {
        viewModelScope.launch {
            stopScanUseCase()
        }
    }

}