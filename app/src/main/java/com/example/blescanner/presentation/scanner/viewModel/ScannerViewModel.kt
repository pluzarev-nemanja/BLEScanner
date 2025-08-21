package com.example.blescanner.presentation.scanner.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blescanner.domain.useCase.ConnectBleDeviceUseCase
import com.example.blescanner.domain.useCase.DisconnectDeviceUseCase
import com.example.blescanner.domain.useCase.ObserveScanStateUseCase
import com.example.blescanner.domain.useCase.ReadCharacteristicUseCase
import com.example.blescanner.domain.useCase.StartScanUseCase
import com.example.blescanner.domain.useCase.StopScanUseCase
import com.example.blescanner.domain.useCase.SubscribeToCharacteristicUseCase
import com.example.blescanner.presentation.scanner.uiState.ScannerUiState
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.UUID

class ScannerViewModel(
    private val startScanUseCase: StartScanUseCase,
    private val stopScanUseCase: StopScanUseCase,
    private val observeScanStateUseCase: ObserveScanStateUseCase,
    private val connectBleDeviceUseCase: ConnectBleDeviceUseCase,
    private val disconnectDeviceUseCase: DisconnectDeviceUseCase,
    private val readCharacteristicUseCase: ReadCharacteristicUseCase,
    private val subscribeToCharacteristicUseCase: SubscribeToCharacteristicUseCase
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

    fun connectDevice(address: String) {
        viewModelScope.launch {
            connectBleDeviceUseCase(address)
        }
    }

    fun disconnectDevice(address: String) {
        viewModelScope.launch {
            disconnectDeviceUseCase(address)
        }
    }

    fun readCharacteristic(deviceAddress: String, serviceUuid: String, charUuid: String) {
        readCharacteristicUseCase(deviceAddress, serviceUuid.toUuid(), charUuid.toUuid())
    }

    fun subscribeToCharacteristic(deviceAddress: String, serviceUuid: String, charUuid: String) {
        subscribeToCharacteristicUseCase(deviceAddress, serviceUuid.toUuid(), charUuid.toUuid())
    }

    fun String.toUuid(): UUID = runCatching {
        UUID.fromString(this)
    }.onFailure {
        runCatching {
            val short = this.toLong(16)
            UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", short))
        }.onFailure {
            Napier.d(it.message.toString())
        }
    }.getOrThrow()

}