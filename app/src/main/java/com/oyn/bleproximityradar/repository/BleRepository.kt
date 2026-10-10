package com.oyn.bleproximityradar.repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import com.oyn.bleproximityradar.model.BleDeviceModel
import com.oyn.bleproximityradar.model.DeviceDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
class BleRepository(
    private val bluetoothAdapter: BluetoothAdapter,
    private val deviceDao: DeviceDao
) {
    private val bleScanner = bluetoothAdapter.bluetoothLeScanner
    private val repositoryScope = CoroutineScope(Dispatchers.IO)
    private var cleanupJob: Job? = null

    private val _scannedDevices = MutableStateFlow<Map<String, BleDeviceModel>>(emptyMap())
    val scannedDevices: StateFlow<Map<String, BleDeviceModel>> = _scannedDevices

    val historyDevices: Flow<List<BleDeviceModel>> = deviceDao.getAllDevices()

    suspend fun clearHistory() {
        deviceDao.clearHistory()
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val rssi = result.rssi
            val macAddress = device.address
            val name = device.name ?: "Perangkat Tidak Dikenal"

            val newDevice = BleDeviceModel(
                macAddress = macAddress,
                name = name,
                rssi = rssi,
                lastSeen = System.currentTimeMillis()
            )

            val currentMap = _scannedDevices.value.toMutableMap()
            currentMap[macAddress] = newDevice
            _scannedDevices.value = currentMap

            repositoryScope.launch {
                deviceDao.insertDevice(newDevice)
            }
        }
    }

    fun startScan() {
        if (bluetoothAdapter.isEnabled) {
            _scannedDevices.value = emptyMap()
            bleScanner?.startScan(scanCallback)

            cleanupJob?.cancel()
            cleanupJob = repositoryScope.launch {
                while (isActive) {
                    delay(5000)
                    val currentTime = System.currentTimeMillis()

                    val activeDevices = _scannedDevices.value.filter {
                        (currentTime - it.value.lastSeen) < 10000
                    }

                    if (activeDevices.size != _scannedDevices.value.size) {
                        _scannedDevices.value = activeDevices
                    }
                }
            }
        }
    }

    fun stopScan() {
        bleScanner?.stopScan(scanCallback)
        cleanupJob?.cancel()

        _scannedDevices.value = emptyMap()
    }
}