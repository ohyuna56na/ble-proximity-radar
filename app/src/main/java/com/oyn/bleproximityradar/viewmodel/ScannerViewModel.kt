package com.oyn.bleproximityradar.viewmodel

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import com.oyn.bleproximityradar.repository.BleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val repository: BleRepository
) : ViewModel() {

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val searchQuery = MutableStateFlow("")
    private val minRssiFilter = MutableStateFlow(-100)

    val filteredDevices = combine(
        repository.scannedDevices,
        searchQuery,
        minRssiFilter
    ) { devicesMap, query, minRssi ->
        devicesMap.values.toList()
            .filter { device ->
                val matchesSearch = device.name?.contains(query, ignoreCase = true) == true ||
                        device.macAddress.contains(query, ignoreCase = true)
                val matchesRssi = device.rssi >= minRssi

                matchesSearch && matchesRssi
            }
            .sortedByDescending { it.rssi }
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun updateRssiFilter(minRssi: Int) {
        minRssiFilter.value = minRssi
    }

    fun startScanning() {
        _isScanning.value = true
        repository.startScan()
    }

    fun stopScanning() {
        _isScanning.value = false
        repository.stopScan()
    }

    @SuppressLint("EmptySuperCall")
    override fun onCleared() {
        super.onCleared()
        if (_isScanning.value) {
            stopScanning()
        }
    }
}