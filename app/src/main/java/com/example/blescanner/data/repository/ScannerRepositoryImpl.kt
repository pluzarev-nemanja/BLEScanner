package com.example.blescanner.data.repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import androidx.core.util.size
import com.example.blescanner.data.model.BleDevice
import com.example.blescanner.data.model.ServiceInfo
import com.example.blescanner.domain.event.ConnectionEvent
import com.example.blescanner.domain.repository.ScannerRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@SuppressLint("MissingPermission")
class ScannerRepositoryImpl(
    private val context: Context
) : ScannerRepository {

    private val appScope = CoroutineScope(Dispatchers.Default)
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val scanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    private val mutableIsScanning = MutableStateFlow(false)
    override val isScanning: Flow<Boolean> = mutableIsScanning.asStateFlow()

    private val devicesMap = ConcurrentHashMap<String, BleDevice>()
    private val mutableDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    override val devices: Flow<List<BleDevice>> = mutableDevices.asStateFlow()
    private val mutableNotifications =
        MutableSharedFlow<Triple<String, UUID, ByteArray>>(extraBufferCapacity = 128)
    override val notifications: Flow<Triple<String, UUID, ByteArray>> =
        mutableNotifications.asSharedFlow()

    private val mutableConnectionEvents = MutableSharedFlow<ConnectionEvent>(extraBufferCapacity = 64)
    override val connectionEvents: Flow<ConnectionEvent> = mutableConnectionEvents.asSharedFlow()
    private val servicesMap = ConcurrentHashMap<String, List<ServiceInfo>>()
    private val mutableDeviceServices = MutableStateFlow<Map<String, List<ServiceInfo>>>(emptyMap())
    override val deviceServices: Flow<Map<String, List<ServiceInfo>>> =
        mutableDeviceServices.asStateFlow()
    private val gattMap = ConcurrentHashMap<String, BluetoothGatt>()
    private var currentFilters: List<String> = emptyList()

    // -------- Scan callback (GAP) ----------
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            handleResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            Napier.d("onBatchScanResults: size=${results?.size ?: 0}")
            results?.forEach { handleResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            Napier.e("Scan failed: $errorCode")
            mutableIsScanning.value = false
        }
    }

    // -------- Start / Stop scanning ----------
    override suspend fun startScan(filters: List<String>) {
        if (mutableIsScanning.value) return
        currentFilters = filters

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val scanFilters = mutableListOf<ScanFilter>()

        devicesMap.clear()
        mutableDevices.value = emptyList()

        try {
            val s = scanner
            if (s == null) {
                Napier.e("BluetoothLeScanner is null. Adapter present=${bluetoothAdapter != null}")
                return
            }
            s.startScan(scanFilters, settings, scanCallback)
            mutableIsScanning.value = true
            Napier.d("BLE scan started...")
        } catch (t: Throwable) {
            Napier.e(t) { "Failed to start scan" }
            mutableIsScanning.value = false
        }
    }

    override suspend fun stopScan() {
        if (!mutableIsScanning.value) return
        runCatching {
            scanner?.stopScan(scanCallback)
        }.onFailure {
            Napier.w("Stop scanning error: $it")
        }
        mutableIsScanning.value = false
        Napier.d("BLE scan stopped.")
    }

    // --------- Connect / Disconnect (GATT) -----------
    /**
     * Connect with optional retry logic. If retries = 0, try once.
     */
    override fun connectToDevice(address: String, retries: Int, backoffMs: Long) {
        appScope.launch {
            var attempt = 0
            while (attempt <= retries) {
                val success = connectOnce(address)
                if (success) return@launch
                attempt++
                if (attempt <= retries) {
                    Napier.w("Connect attempt $attempt failed for $address. Backoff ${backoffMs * attempt}ms")
                    delay(backoffMs * attempt)
                } else {
                    Napier.e("All connect attempts failed for $address")
                }
            }
        }
    }

    /**
     * Try connecting one time synchronously (returns true if connection was initiated successfully).
     * Actual connected/disconnected state is emitted via _connectionEvents.
     */
    private fun connectOnce(address: String): Boolean {
        val device = try {
            bluetoothAdapter?.getRemoteDevice(address)
        } catch (t: Throwable) {
            Napier.e(t) { "Invalid address $address" }
            null
        }
        if (device == null) {
            Napier.e("Device $address not available on adapter")
            return false
        }

        val existing = gattMap[address]
        if (existing != null) {
            Napier.d("Already have a gatt for $address, reusing")
            return true
        }

        Napier.d("Connecting to $address ...")
        val callback = object : BluetoothGattCallback() {

            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                Napier.d("onConnectionStateChange addr=$address status=$status state=$newState")
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Napier.d("Connected to $address")
                    gattMap[address] = gatt
                    appScope.launch { mutableConnectionEvents.emit(ConnectionEvent.Connected(address)) }
                    if (!gatt.discoverServices()) {
                        Napier.w("discoverServices() returned false for $address")
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Napier.w("Disconnected from $address (status=$status)")
                    gattMap.remove(address)
                    try {
                        gatt.close()
                    } catch (t: Throwable) {
                        Napier.w(t) { "Error closing gatt for $address" }
                    }
                    appScope.launch {
                        mutableConnectionEvents.emit(
                            ConnectionEvent.Disconnected(
                                address,
                                status
                            )
                        )
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val services = gatt.services.map { svc ->
                        Napier.d("Service UUID: ${svc.uuid}")
                        ServiceInfo(svc.uuid, svc.characteristics.map {
                            Napier.d("Characteristic UUID: ${it.uuid}")
                            it.uuid
                        })
                    }
                    servicesMap[address] = services
                    mutableDeviceServices.value = servicesMap.toMap()
                    Napier.d("Services discovered for $address: ${services.size}")
                    logServicesAndCharacteristics(address)
                    appScope.launch {
                        mutableConnectionEvents.emit(
                            ConnectionEvent.ServicesDiscovered(
                                address,
                                services
                            )
                        )
                    }
                } else {
                    Napier.e("Service discovery failed for $address with status=$status")
                    appScope.launch {
                        mutableConnectionEvents.emit(
                            ConnectionEvent.ServicesDiscoveryFailed(
                                address,
                                status
                            )
                        )
                    }
                }
            }

            override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
                Napier.d("onMtuChanged for $address mtu=$mtu status=$status")
                appScope.launch {
                    mutableConnectionEvents.emit(
                        ConnectionEvent.MtuChanged(
                            address,
                            mtu,
                            status
                        )
                    )
                }
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val value = characteristic.value ?: ByteArray(0)
                    Napier.d("Read from ${characteristic.uuid}: ${value.size} bytes : properties: ${characteristic.properties}")
                    appScope.launch {
                        mutableNotifications.emit(
                            Triple(
                                address,
                                characteristic.uuid,
                                value
                            )
                        )
                    }
                    appScope.launch {
                        mutableConnectionEvents.emit(
                            ConnectionEvent.CharacteristicRead(
                                address,
                                characteristic.uuid,
                                value
                            )
                        )
                    }
                } else {
                    Napier.w("Characteristic read failed: ${characteristic.uuid} status=$status")
                    appScope.launch {
                        mutableConnectionEvents.emit(
                            ConnectionEvent.CharacteristicReadFailed(
                                address,
                                characteristic.uuid,
                                status
                            )
                        )
                    }
                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                val value = characteristic.value ?: ByteArray(0)
                Napier.d("Notification from ${characteristic.uuid} (${address}): ${value.size} bytes")
                appScope.launch { mutableNotifications.emit(Triple(address, characteristic.uuid, value)) }
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                Napier.d("Characteristic write result ${characteristic.uuid} status=$status")
                appScope.launch {
                    mutableConnectionEvents.emit(
                        ConnectionEvent.CharacteristicWriteResult(
                            address,
                            characteristic.uuid,
                            status
                        )
                    )
                }
            }

            override fun onDescriptorWrite(
                gatt: BluetoothGatt,
                descriptor: BluetoothGattDescriptor,
                status: Int
            ) {
                Napier.d("Descriptor write result ${descriptor.uuid} status=$status")
            }
        }

        return try {
            val gatt = device.connectGatt(context, false, callback)
            gattMap[address] = gatt
            true
        } catch (t: Throwable) {
            Napier.e(t) { "connectGatt failed for $address" }
            appScope.launch { mutableConnectionEvents.emit(ConnectionEvent.ConnectFailed(address, t)) }
            false
        }
    }

    override fun disconnectDevice(address: String) {
        val gatt = gattMap.remove(address)
        if (gatt == null) {
            Napier.w("No GATT to disconnect for $address")
            return
        }
        try {
            Napier.d("Disconnecting $address")
            gatt.disconnect()
            gatt.close()
        } catch (t: Throwable) {
            Napier.w(t) { "Error during disconnect for $address" }
        } finally {
            servicesMap.remove(address)
            mutableDeviceServices.value = servicesMap.toMap()
            appScope.launch {
                mutableConnectionEvents.emit(
                    ConnectionEvent.Disconnected(
                        address, /*status*/
                        -1
                    )
                )
            }
        }
    }

    // ---------- Read / Write / Subscribe / Unsubscribe ----------
    override fun readCharacteristic(deviceAddress: String, serviceUuid: UUID, charUuid: UUID) {
        val gatt = gattMap[deviceAddress]
        if (gatt == null) {
            Napier.w("readCharacteristic: no gatt for $deviceAddress")
            return
        }
        val service = gatt.getService(serviceUuid)
        if (service == null) {
            Napier.w("readCharacteristic: service $serviceUuid not found on $deviceAddress")
            return
        }
        val characteristic = service.getCharacteristic(charUuid)
        if (characteristic == null) {
            Napier.w("readCharacteristic: characteristic $charUuid not found on $deviceAddress")
            return
        }
        gatt.readCharacteristic(characteristic)
    }

    override fun writeCharacteristic(
        deviceAddress: String,
        serviceUuid: UUID,
        charUuid: UUID,
        value: ByteArray,
        writeType: Int
    ) {
        val gatt = gattMap[deviceAddress] ?: run {
            Napier.w("writeCharacteristic: no gatt for $deviceAddress")
            return
        }
        val service = gatt.getService(serviceUuid) ?: run {
            Napier.w("writeCharacteristic: service not found")
            return
        }
        val characteristic = service.getCharacteristic(charUuid) ?: run {
            Napier.w("writeCharacteristic: characteristic not found")
            return
        }
        characteristic.writeType = writeType
        characteristic.value = value
        val ok = gatt.writeCharacteristic(characteristic)
        if (!ok) Napier.w("writeCharacteristic: writeCharacteristic returned false")
    }

    override fun subscribeToCharacteristic(
        deviceAddress: String,
        serviceUuid: UUID,
        charUuid: UUID
    ) {
        val gatt = gattMap[deviceAddress] ?: run {
            Napier.w("subscribeToCharacteristic: no gatt for $deviceAddress")
            return
        }
        val service = gatt.getService(serviceUuid) ?: run {
            Napier.w("subscribeToCharacteristic: service not found")
            return
        }
        val characteristic = service.getCharacteristic(charUuid) ?: run {
            Napier.w("subscribeToCharacteristic: characteristic not found")
            return
        }

        val localEnabled = gatt.setCharacteristicNotification(characteristic, true)
        if (!localEnabled) {
            Napier.w("subscribeToCharacteristic: setCharacteristicNotification returned false")
        }

        val descriptor = characteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG))
        if (descriptor != null) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            val ok = gatt.writeDescriptor(descriptor)
            if (!ok) Napier.w("subscribeToCharacteristic: writeDescriptor returned false")
        } else {
            Napier.d("subscribeToCharacteristic: descriptor not found for ${characteristic.uuid}, notifications may still work if device uses different endpoint")
        }
    }

    override fun unsubscribeCharacteristic(
        deviceAddress: String,
        serviceUuid: UUID,
        charUuid: UUID
    ) {
        val gatt = gattMap[deviceAddress] ?: run {
            Napier.w("unsubscribeCharacteristic: no gatt for $deviceAddress")
            return
        }
        val service = gatt.getService(serviceUuid) ?: run {
            Napier.w("unsubscribeCharacteristic: service not found")
            return
        }
        val characteristic = service.getCharacteristic(charUuid) ?: run {
            Napier.w("unsubscribeCharacteristic: characteristic not found")
            return
        }

        gatt.setCharacteristicNotification(characteristic, false)
        val descriptor = characteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG))
        descriptor?.let {
            it.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            val ok = gatt.writeDescriptor(it)
            if (!ok) Napier.w("unsubscribeCharacteristic: writeDescriptor returned false")
        }
    }

    override fun requestMtu(deviceAddress: String, mtu: Int) {
        val gatt = gattMap[deviceAddress] ?: return
        gatt.requestMtu(mtu)
    }

    // ---------- utilities & scan result handling ----------
    private fun handleResult(result: ScanResult?) {
        result?.rssi?.let { if (it < -90) return } // slightly more permissive default
        val device = result?.device ?: return
        val record = result.scanRecord
        val name = record?.deviceName ?: device.name

        if (currentFilters.isNotEmpty()) {
            val n = name?.lowercase().orEmpty()
            if (currentFilters.none { n.contains(it.lowercase()) }) return
        }

        val serviceUuids: List<UUID> = record?.serviceUuids?.map { it.uuid } ?: emptyList()

        val manufacturerData: Map<Int, ByteArray> = buildMap {
            record?.manufacturerSpecificData?.let { sa ->
                for (i in 0 until sa.size) {
                    sa.valueAt(i)?.let { put(sa.keyAt(i), it) }
                }
            }
        }

        val serviceData: Map<UUID, ByteArray> = buildMap {
            record?.serviceData?.forEach { (parcelUuid: ParcelUuid, bytes) ->
                if (bytes != null) put(parcelUuid.uuid, bytes)
            }
        }

        val txPower: Int? = record?.txPowerLevel?.takeIf { it != Int.MIN_VALUE }

        val model = BleDevice(
            address = device.address,
            name = name,
            rssi = result.rssi,
            lastSeen = System.currentTimeMillis(),
            serviceUuids = serviceUuids,
            manufacturerData = manufacturerData,
            serviceData = serviceData,
            txPower = txPower
        )

        var shouldEmit = false
        val existing = devicesMap[model.address]
        if (existing == null ||
            existing.rssi != model.rssi ||
            existing.name != model.name ||
            existing.txPower != model.txPower ||
            existing.serviceUuids != model.serviceUuids ||
            existing.manufacturerData.keys != model.manufacturerData.keys
        ) {
            devicesMap[model.address] = model
            shouldEmit = true
        } else {
            devicesMap[model.address] = existing.copy(lastSeen = System.currentTimeMillis())
        }

        if (shouldEmit) {
            appScope.launch {
                val list = devicesMap.values.sortedByDescending { it.rssi }
                mutableDevices.value = list
            }
        }
    }

    private fun logServicesAndCharacteristics(deviceAddress: String) {
        val gatt = gattMap[deviceAddress]
        if (gatt == null) {
            Napier.w("No GATT for $deviceAddress")
            return
        }

        gatt.services.forEach { service ->
            Napier.d("Service UUID: ${service.uuid}")
            service.characteristics.forEach { char ->
                val props = char.properties
                val readable = props and BluetoothGattCharacteristic.PROPERTY_READ != 0
                val notifiable = props and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
                Napier.d(
                    "  Char UUID: ${char.uuid}, " +
                            "READ=$readable, NOTIFY=$notifiable, PROPS=$props"
                )
            }
        }
    }

    companion object {
        private const val CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"
    }
}