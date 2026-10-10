package com.oyn.bleproximityradar.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.oyn.bleproximityradar.R
import com.oyn.bleproximityradar.databinding.ItemDeviceBinding
import com.oyn.bleproximityradar.model.BleDeviceModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter : ListAdapter<BleDeviceModel, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HistoryViewHolder(private val binding: ItemDeviceBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(device: BleDeviceModel) {
            val context = binding.root.context
            binding.tvDeviceName.text = device.name.ifEmpty { "Perangkat Tidak Dikenal" }
            binding.tvMacAddress.text = device.macAddress
            binding.tvRssi.text = String.format(Locale.getDefault(), "%d dBm", device.rssi)
            binding.tvCategory.text = device.signalCategory

            val sdf = SimpleDateFormat("dd MMM, HH.mm", Locale.getDefault())
            val lastSeenFormatted = sdf.format(Date(device.lastSeen))
            binding.tvDistance.text = String.format(Locale.getDefault(), "Terakhir %s · %.1f m", lastSeenFormatted, device.estimatedDistance)

            binding.btnLacak.visibility = android.view.View.GONE

            val textColorRes = when {
                device.rssi > -50 -> R.color.signal_strong
                device.rssi > -70 -> R.color.signal_good
                device.rssi > -80 -> R.color.signal_weak
                else -> R.color.signal_lost
            }

            val bgColorRes = when {
                device.rssi > -50 -> R.color.bg_signal_strong
                device.rssi > -70 -> R.color.bg_signal_good
                device.rssi > -80 -> R.color.bg_signal_weak
                else -> R.color.bg_signal_lost
            }

            val activeColor = ContextCompat.getColor(context, textColorRes)
            binding.tvRssi.setTextColor(activeColor)
            binding.tvCategory.setTextColor(activeColor)
            binding.cardCategory.setCardBackgroundColor(ContextCompat.getColor(context, bgColorRes))

            val bars = listOf(binding.bar1, binding.bar2, binding.bar3, binding.bar4)
            val activeBarCount = when {
                device.rssi > -50 -> 4
                device.rssi > -70 -> 3
                device.rssi > -80 -> 2
                else -> 1
            }
            for (i in bars.indices) {
                bars[i].backgroundTintList = ColorStateList.valueOf(
                    if (i < activeBarCount) activeColor else ContextCompat.getColor(context, R.color.card_stroke)
                )
            }
        }
    }

    class HistoryDiffCallback : DiffUtil.ItemCallback<BleDeviceModel>() {
        override fun areItemsTheSame(oldItem: BleDeviceModel, newItem: BleDeviceModel) = oldItem.macAddress == newItem.macAddress
        override fun areContentsTheSame(oldItem: BleDeviceModel, newItem: BleDeviceModel) = oldItem == newItem
    }
}