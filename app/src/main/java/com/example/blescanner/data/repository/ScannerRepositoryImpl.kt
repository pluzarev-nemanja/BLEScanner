package com.example.blescanner.data.repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import androidx.core.util.size
import com.example.blescanner.data.model.BleDevice
import com.example.blescanner.domain.repository.ScannerRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

@SuppressLint("MissingPermission")
class ScannerRepositoryImpl(
    private val context: Context
) : ScannerRepository {

    private val appScope = CoroutineScope(Dispatchers.Default)
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val scanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    private var currentFilters: List<String> = emptyList()
    private val connectedDevices = mutableMapOf<String, BluetoothGatt>()

    private val mutableIsScanning = MutableStateFlow(false)
    override val isScanning: Flow<Boolean> = mutableIsScanning.asStateFlow()

    private val devicesMap = mutableMapOf<String, BleDevice>()

    private val mutableDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    override val devices: Flow<List<BleDevice>> = mutableDevices.asStateFlow()

    private val mutableNotifications =
        MutableSharedFlow<Pair<UUID, ByteArray>>(extraBufferCapacity = 64)
    val notifications: Flow<Pair<UUID, ByteArray>> = mutableNotifications.asSharedFlow()

    private val callBack = object : ScanCallback() {
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

    override suspend fun startScan(filters: List<String>) {
        if (mutableIsScanning.value) return
        currentFilters = filters

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        val scanFilters = mutableListOf<ScanFilter>()

        synchronized(devicesMap) { devicesMap.clear() }
        mutableDevices.value = emptyList()

        try {
            val s = scanner
            if (s == null) {
                Napier.e("BluetoothLeScanner is null. Adapter present=${bluetoothAdapter != null}")
                return
            }
            s.startScan(scanFilters, settings, callBack)
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
            scanner?.stopScan(callBack)
        }.onFailure {
            Napier.w("Stop scanning error: $it")
        }
        mutableIsScanning.value = false
        Napier.d("BLE scan stopped.")
    }

    // ---------------- GATT Connection ----------------
    override fun connectToDevice(address: String) {
        val device = bluetoothAdapter?.getRemoteDevice(address)
        if (device == null) {
            Napier.e("Device $address not found in adapter")
            return
        }

        if (connectedDevices.containsKey(address)) {
            Napier.d("Already connected to $address")
            return
        }

        Napier.d("Connecting to $address ...")
        device.connectGatt(context, false, object : android.bluetooth.BluetoothGattCallback() {
            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int
            ) {
                when (newState) {
                    android.bluetooth.BluetoothProfile.STATE_CONNECTED -> {
                        Napier.d("Connected to $address, discovering services...")
                        connectedDevices[address] = gatt
                        gatt.discoverServices()
                    }

                    android.bluetooth.BluetoothProfile.STATE_DISCONNECTED -> {
                        Napier.w("Disconnected from $address (status=$status)")
                        connectedDevices.remove(address)
                        gatt.close()
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Napier.d("Services discovered for $address: ${gatt.services.size}")
                } else {
                    Napier.e("Service discovery failed for $address with status=$status")
                }
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: android.bluetooth.BluetoothGattCharacteristic,
                status: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val value = characteristic.value
                    Napier.d("Read from ${characteristic.uuid}: ${value?.decodeToString()}")
                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: android.bluetooth.BluetoothGattCharacteristic
            ) {
                val value = characteristic.value
                appScope.launch {
                    mutableNotifications.emit(characteristic.uuid to value)
                }
            }

            override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
                super.onMtuChanged(gatt, mtu, status)
                Napier.d("MTU changed: $mtu ; Status: $status")
            }
        })
    }

    override fun disconnectDevice(address: String) {
        connectedDevices[address]?.let { gatt ->
            Napier.d("Disconnecting $address")
            gatt.disconnect()
            gatt.close()
            connectedDevices.remove(address)
        } ?: Napier.w("No active connection for $address")
    }

    override fun readCharacteristic(
        deviceAddress: String,
        serviceUuid: UUID,
        charUuid: UUID
    ) {
        val gatt = connectedDevices[deviceAddress] ?: return
        val service = gatt.getService(serviceUuid) ?: return
        val characteristic = service.getCharacteristic(charUuid) ?: return
        gatt.readCharacteristic(characteristic)
    }

    override fun subscribeToCharacteristic(
        deviceAddress: String,
        serviceUuid: UUID,
        charUuid: UUID
    ) {
        val gatt = connectedDevices[deviceAddress] ?: return
        val service = gatt.getService(serviceUuid) ?: return
        val characteristic = service.getCharacteristic(charUuid) ?: return

        gatt.setCharacteristicNotification(characteristic, true)

        // Enable notification on descriptor if available
        val descriptor = characteristic.getDescriptor(
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        )
        descriptor?.let {
            it.value = android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(it)
        }
    }

    private fun handleResult(result: ScanResult?) {
        result?.rssi?.let { if (it < -70) return }
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
        synchronized(devicesMap) {
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
        }

        if (shouldEmit) {
            appScope.launch {
                val list: List<BleDevice> = synchronized(devicesMap) {
                    devicesMap.values.sortedByDescending { it.rssi }
                }
                mutableDevices.value = list
            }
        }
    }
}