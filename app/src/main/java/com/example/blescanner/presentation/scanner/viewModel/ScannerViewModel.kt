package com.example.blescanner.presentation.scanner.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blescanner.data.model.ServiceInfo
import com.example.blescanner.domain.event.ConnectionEvent
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
import com.example.blescanner.presentation.scanner.uiState.ConnectionState
import com.example.blescanner.presentation.scanner.uiState.NotificationUi
import com.example.blescanner.presentation.scanner.uiState.ScannerUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class ScannerViewModel(
    private val startScanUseCase: StartScanUseCase,
    private val stopScanUseCase: StopScanUseCase,
    private val observeScanStateUseCase: ObserveScanStateUseCase,
    private val observeDevicesUseCase: ObserveDevicesUseCase,
    private val connectDeviceUseCase: ConnectBleDeviceUseCase,
    private val disconnectDeviceUseCase: DisconnectDeviceUseCase,
    private val observeDeviceServicesUseCase: ObserveDeviceServicesUseCase,
    private val readCharacteristicUseCase: ReadCharacteristicUseCase,
    private val subscribeToCharacteristicUseCase: SubscribeToCharacteristicUseCase,
    private val unsubscribeCharacteristicUseCase: UnsubscribeCharacteristicUseCase,
    private val observeNotificationsUseCase: ObserveNotificationsUseCase,
    private val observeConnectionEventsUseCase: ObserveConnectionEventsUseCase,
    private val requestMtuUseCase: RequestMtuUseCase
) : ViewModel() {

    private val mutableState = MutableStateFlow(ScannerUiState())
    val scannerUiState: StateFlow<ScannerUiState> = mutableState.asStateFlow()

    private val connectionsMap = LinkedHashMap<String, ConnectionState>()
    private val servicesMap = LinkedHashMap<String, List<ServiceInfo>>()
    private val filterTextFlow = MutableStateFlow("")
    private var filterObserverJob: Job? = null
    private var currentObserverFilters: List<String> = emptyList()

    init {
        observeScanFlag()
        observeDevices()
        observeNotifications()
        observeConnectionEvents()
        observeDeviceServices()
    }

    fun onFilterTextChanged(text: String) {
        filterTextFlow.value = text
    }

    private fun observeDeviceServices() {
        viewModelScope.launch {
            observeDeviceServicesUseCase().catch { e ->
                mutableState.value = mutableState.value.copy(error = e.message)
            }.collectLatest { services ->
                mutableState.value = mutableState.value.copy(services = services)
            }
        }
    }

    private fun observeScanFlag() {
        viewModelScope.launch {
            observeScanStateUseCase().catch { e ->
                mutableState.value = mutableState.value.copy(error = e.message)
            }.collectLatest { scanning ->
                mutableState.value = mutableState.value.copy(isScanning = scanning)
            }
        }
    }

    private fun observeDevices() {
        viewModelScope.launch {
            observeDevicesUseCase().catch { e ->
                mutableState.value = mutableState.value.copy(error = e.message)
            }.collectLatest { devices ->
                mutableState.value = mutableState.value.copy(devices = devices)
            }
        }
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            observeNotificationsUseCase().catch { e ->
                mutableState.value = mutableState.value.copy(error = e.message)
            }.collectLatest { notificationUi ->
                mutableState.value = mutableState.value.copy(latestNotification = notificationUi)
            }
        }
    }

    private fun observeConnectionEvents() {
        viewModelScope.launch {
            observeConnectionEventsUseCase().catch { e ->
                mutableState.value = mutableState.value.copy(error = e.message)
            }.collectLatest { ev ->
                when (ev) {
                    is ConnectionEvent.Connected -> {
                        connectionsMap[ev.address] = ConnectionState.CONNECTED
                    }

                    is ConnectionEvent.Disconnected -> {
                        connectionsMap[ev.address] = ConnectionState.DISCONNECTED
                        servicesMap.remove(ev.address)
                    }

                    is ConnectionEvent.ConnectFailed -> {
                        connectionsMap[ev.address] = ConnectionState.DISCONNECTED
                        mutableState.value = mutableState.value.copy(error = ev.throwable.message)
                    }

                    is ConnectionEvent.ServicesDiscovered -> {
                        connectionsMap[ev.address] = ConnectionState.CONNECTED
                        servicesMap[ev.address] = ev.services
                    }

                    is ConnectionEvent.ServicesDiscoveryFailed -> {
                        mutableState.value =
                            mutableState.value.copy(error = "Services discovery failed: ${ev.status}")
                    }

                    is ConnectionEvent.MtuChanged -> {
                    }

                    is ConnectionEvent.CharacteristicRead -> {
                        val note = NotificationUi(
                            deviceAddress = ev.address,
                            characteristicUuid = ev.uuid,
                            data = ev.value,
                            timestamp = System.currentTimeMillis(),
                            isIndication = true
                        )
                        mutableState.value = mutableState.value.copy(latestNotification = note)
                    }

                    is ConnectionEvent.CharacteristicReadFailed -> {
                        mutableState.value =
                            mutableState.value.copy(error = "Read failed: ${ev.uuid} status=${ev.status}")
                    }

                    is ConnectionEvent.CharacteristicWriteResult -> {
                    }
                }
                mutableState.value = mutableState.value.copy(
                    connections = connectionsMap.toMap(),
                    services = servicesMap.toMap()
                )
            }
        }
    }

    fun startScan(filters: List<String> = emptyList()) {
        viewModelScope.launch {
            try {
                startScanUseCase(filters)
                mutableState.value = mutableState.value.copy(isScanning = true)
                if (filterObserverJob?.isActive != true) {
                    currentObserverFilters = filters
                    filterObserverJob = viewModelScope.launch {
                        filterTextFlow
                            .debounce(500L)
                            .map { raw ->
                                raw.split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                            }
                            .distinctUntilChanged()
                            .collectLatest { newFilters ->
                                if (!mutableState.value.isScanning) return@collectLatest
                                if (newFilters == currentObserverFilters) return@collectLatest

                                currentObserverFilters = newFilters
                                try {
                                    stopScanUseCase()
                                } catch (t: Throwable) {
                                    mutableState.value = mutableState.value.copy(error = t.message)
                                }
                                try {
                                    startScanUseCase(newFilters)
                                } catch (t: Throwable) {
                                    mutableState.value = mutableState.value.copy(error = t.message)
                                }
                            }
                    }
                } else {
                    currentObserverFilters - filters
                }
            } catch (t: Throwable) {
                mutableState.value = mutableState.value.copy(error = t.message)
            }
        }
    }

    fun stopScan() {
        viewModelScope.launch {
            try {
                stopScanUseCase()
            } catch (t: Throwable) {
                mutableState.value = mutableState.value.copy(error = t.message)
            } finally {
                filterObserverJob?.cancel()
                filterObserverJob = null
                currentObserverFilters = emptyList()
                mutableState.value = mutableState.value.copy(isScanning = false)
            }
        }
    }

    fun connectDevice(address: String, retries: Int = 3, backoffMs: Long = 1000L) {
        connectionsMap[address] = ConnectionState.CONNECTING
        mutableState.value = mutableState.value.copy(connections = connectionsMap.toMap())
        connectDeviceUseCase(address, retries, backoffMs)
    }

    fun disconnectDevice(address: String) {
        disconnectDeviceUseCase(address)
        connectionsMap[address] = ConnectionState.DISCONNECTED
        servicesMap.remove(address)
        mutableState.value = mutableState.value.copy(
            connections = connectionsMap.toMap(),
            services = servicesMap.toMap()
        )
    }

    fun readCharacteristic(deviceAddress: String, serviceUuidStr: String, charUuidStr: String) {
        val serviceUuid = serviceUuidStr.toUuid() ?: run {
            mutableState.value = mutableState.value.copy(error = "Invalid service UUID")
            return
        }
        val charUuid = charUuidStr.toUuid() ?: run {
            mutableState.value = mutableState.value.copy(error = "Invalid characteristic UUID")
            return
        }
        readCharacteristicUseCase(deviceAddress, serviceUuid, charUuid)
    }

    fun subscribeToCharacteristic(
        deviceAddress: String,
        serviceUuidStr: String,
        charUuidStr: String
    ) {
        val serviceUuid = serviceUuidStr.toUuid() ?: run {
            mutableState.value = mutableState.value.copy(error = "Invalid service UUID")
            return
        }
        val charUuid = charUuidStr.toUuid() ?: run {
            mutableState.value = mutableState.value.copy(error = "Invalid characteristic UUID")
            return
        }
        subscribeToCharacteristicUseCase(deviceAddress, serviceUuid, charUuid)
    }

    fun unsubscribeCharacteristic(
        deviceAddress: String,
        serviceUuidStr: String,
        charUuidStr: String
    ) {
        val serviceUuid = serviceUuidStr.toUuid() ?: run {
            mutableState.value = mutableState.value.copy(error = "Invalid service UUID")
            return
        }
        val charUuid = charUuidStr.toUuid() ?: run {
            mutableState.value = mutableState.value.copy(error = "Invalid characteristic UUID")
            return
        }
        unsubscribeCharacteristicUseCase(deviceAddress, serviceUuid, charUuid)
    }

    fun requestMtu(deviceAddress: String, mtu: Int) {
        requestMtuUseCase(deviceAddress, mtu)
    }

    fun clearError() {
        mutableState.value = mutableState.value.copy(error = null)
    }

    private fun String.toUuid(): UUID? {
        val cleaned = removePrefix("0x").padStart(4, '0')
        return try {
            UUID.fromString("$cleaned-0000-1000-8000-00805f9b34fb")
        } catch (e: Exception) {
            try {
                UUID.fromString(this)
            } catch (_: Exception) {
                null
            }
        }
    }
}