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
import java.util.Locale

class DeviceAdapter(
    private val onTrackClicked: (BleDeviceModel) -> Unit
) : ListAdapter<BleDeviceModel, DeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DeviceViewHolder(private val binding: ItemDeviceBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(device: BleDeviceModel) {
            val context = binding.root.context
            binding.tvDeviceName.text = device.name.ifEmpty { "Perangkat Tidak Dikenal" }
            binding.tvMacAddress.text = device.macAddress
            binding.tvRssi.text = String.format(Locale.getDefault(), "%d dBm", device.rssi)
            binding.tvDistance.text = String.format(Locale.getDefault(), "%.1f m", device.estimatedDistance)
            binding.tvCategory.text = device.signalCategory

            val textColorRes: Int
            val bgColorRes: Int
            val activeBarCount: Int

            when {
                device.rssi > -50 -> {
                    textColorRes = R.color.signal_strong
                    bgColorRes = R.color.bg_signal_strong
                    activeBarCount = 4
                }
                device.rssi > -70 -> {
                    textColorRes = R.color.signal_good
                    bgColorRes = R.color.bg_signal_good
                    activeBarCount = 3
                }
                device.rssi > -80 -> {
                    textColorRes = R.color.signal_weak
                    bgColorRes = R.color.bg_signal_weak
                    activeBarCount = 2
                }
                else -> {
                    textColorRes = R.color.signal_lost
                    bgColorRes = R.color.bg_signal_lost
                    activeBarCount = 1
                }
            }

            val activeColor = ContextCompat.getColor(context, textColorRes)
            val inactiveColor = ContextCompat.getColor(context, R.color.card_stroke)

            binding.tvRssi.setTextColor(activeColor)
            binding.tvCategory.setTextColor(activeColor)
            binding.cardCategory.setCardBackgroundColor(ContextCompat.getColor(context, bgColorRes))

            val bars = listOf(binding.bar1, binding.bar2, binding.bar3, binding.bar4)
            for (i in bars.indices) {
                if (i < activeBarCount) {
                    bars[i].backgroundTintList = ColorStateList.valueOf(activeColor)
                } else {
                    bars[i].backgroundTintList = ColorStateList.valueOf(inactiveColor)
                }
            }

            binding.btnLacak.setOnClickListener {
                onTrackClicked(device)
            }
        }
    }

    class DeviceDiffCallback : DiffUtil.ItemCallback<BleDeviceModel>() {
        override fun areItemsTheSame(oldItem: BleDeviceModel, newItem: BleDeviceModel) = oldItem.macAddress == newItem.macAddress
        override fun areContentsTheSame(oldItem: BleDeviceModel, newItem: BleDeviceModel) = oldItem == newItem
    }
}