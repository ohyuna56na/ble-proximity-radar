package com.oyn.bleproximityradar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oyn.bleproximityradar.model.BleDeviceModel
import com.oyn.bleproximityradar.repository.BleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: BleRepository
) : ViewModel() {

    val historyDevices: StateFlow<List<BleDeviceModel>> = repository.historyDevices
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}