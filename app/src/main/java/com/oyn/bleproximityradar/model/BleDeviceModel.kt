package com.oyn.bleproximityradar.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.math.pow

@Entity(tableName = "devices")
data class BleDeviceModel(
    @PrimaryKey val macAddress: String,
    val name: String,
    var rssi: Int,
    var lastSeen: Long
) {
    val estimatedDistance: Double
        get() {
            if (rssi == 0) return -1.0

            val ratio = (rssi * 1.0) / -59.0

            return if (ratio < 1.0) {
                ratio.pow(10.0)
            } else {
                (0.89976 * ratio.pow(7.7095)) + 0.111
            }
        }

    val signalCategory: String
        get() = when {
            rssi > -30 -> "Sangat Kuat"
            rssi > -50 -> "Kuat"
            rssi > -70 -> "Cukup baik"
            rssi > -80 -> "Lemah"
            else -> "Sangat lemah"
        }
}