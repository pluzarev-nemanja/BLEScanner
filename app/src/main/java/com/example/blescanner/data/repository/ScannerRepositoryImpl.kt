package com.example.blescanner.data.repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
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
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val mutableIsScanning = MutableStateFlow(false)
    override val isScanning: Flow<Boolean> = mutableIsScanning.asStateFlow()

    private val devicesMap = mutableMapOf<String, BleDevice>()
    private val mutableDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    override val devices: Flow<List<BleDevice>> = mutableDevices.asStateFlow()

    private val callBack = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            Napier.d("Found device: ${result?.device?.address} name: ${result?.device?.name} RSSI: ${result?.rssi}")
            handleResult(result)
        }

        override fun onBatchScanResults(results: List<ScanResult?>?) {
            results?.forEach(::handleResult)
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
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val scanFilters = mutableListOf<ScanFilter>()

        devicesMap.clear()
        mutableDevices.emit(emptyList())
        scanner?.startScan(scanFilters, settings, callBack)
        mutableIsScanning.value = true
        Napier.d("BLE scan started...")
    }

    override suspend fun stopScan() {
        if (mutableIsScanning.value) return
        runCatching {
            scanner?.stopScan(callBack)
        }.onFailure {
            Napier.w("Stop scanning error: $it")
        }
        mutableIsScanning.value = false
        Napier.d("BLE scan stopped.")
    }

    private fun handleResult(result: ScanResult?) {
        val device = result?.device ?: return
        val record = result.scanRecord
        val name = record?.deviceName ?: device.name

        if (currentFilters.isNotEmpty()) {
            val n = name?.lowercase().orEmpty()
            if (currentFilters.none { n.contains(it.lowercase()) }) return
        }

        val serviceUuids: List<UUID> = record?.serviceUuids?.map { it.uuid } ?: emptyList()

        val manufacturerData: Map<Int, ByteArray> = buildMap {
            record?.manufacturerSpecificData?.let { sparseArray ->
                for (i in 0 until sparseArray.size) {
                    val key = sparseArray.keyAt(i)
                    val value = sparseArray.valueAt(i)
                    if (value != null) put(key, value)
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

        devicesMap[model.address] = model
        appScope.launch {
            mutableDevices.value = devicesMap.values.sortedBy { it.name ?: "~" }
        }
    }
}