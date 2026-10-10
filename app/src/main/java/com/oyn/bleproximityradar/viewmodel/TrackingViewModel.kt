package com.oyn.bleproximityradar.viewmodel

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oyn.bleproximityradar.model.BleDeviceModel
import com.oyn.bleproximityradar.repository.BleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackingViewModel @Inject constructor(
    private val repository: BleRepository
) : ViewModel() {

    private val _targetDevice = MutableStateFlow<BleDeviceModel?>(null)
    val targetDevice: StateFlow<BleDeviceModel?> = _targetDevice

    private val _isOutOfRange = MutableStateFlow(false)
    val isOutOfRange: StateFlow<Boolean> = _isOutOfRange

    init {
        viewModelScope.launch {
            repository.scannedDevices.collectLatest { devicesMap ->
                val currentTarget = _targetDevice.value
                if (currentTarget != null) {
                    if (devicesMap.containsKey(currentTarget.macAddress)) {
                        _targetDevice.value = devicesMap[currentTarget.macAddress]
                        _isOutOfRange.value = false
                    }
                }
            }
        }

        viewModelScope.launch {
            while (isActive) {
                delay(3000)
                val target = _targetDevice.value
                if (target != null) {
                    val timeDiff = System.currentTimeMillis() - target.lastSeen
                    if (timeDiff > 10000) {
                        _isOutOfRange.value = true
                    }
                }
            }
        }
    }

    fun setTargetDevice(device: BleDeviceModel) {
        _targetDevice.value = device
        _isOutOfRange.value = false
    }

    fun clearTargetDevice() {
        _targetDevice.value = null
        _isOutOfRange.value = false
    }

    @SuppressLint("EmptySuperCall")
    override fun onCleared() {
        super.onCleared()
        clearTargetDevice()
    }
}