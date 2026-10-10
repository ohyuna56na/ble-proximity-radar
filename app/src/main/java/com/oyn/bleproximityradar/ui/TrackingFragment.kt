package com.oyn.bleproximityradar.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.oyn.bleproximityradar.R
import com.oyn.bleproximityradar.databinding.FragmentTrackingBinding
import com.oyn.bleproximityradar.viewmodel.TrackingViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class TrackingFragment : Fragment(R.layout.fragment_tracking) {

    private val viewModel: TrackingViewModel by activityViewModels()
    private var _binding: FragmentTrackingBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTrackingBinding.bind(view)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnBukaScanner.setOnClickListener {
            requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigation).selectedItemId = R.id.scannerFragment
        }

        binding.btnStopTracking.setOnClickListener {
            viewModel.clearTargetDevice()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.targetDevice.collect { target ->
                        if (target == null) {
                            binding.cardEmptyState.visibility = View.VISIBLE
                            binding.containerActiveTracking.visibility = View.GONE
                            binding.radarSweep.clearAnimation()
                        } else {
                            binding.cardEmptyState.visibility = View.GONE
                            binding.containerActiveTracking.visibility = View.VISIBLE

                            if (binding.radarSweep.animation == null) {
                                val rotateAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.radar_rotate)
                                binding.radarSweep.startAnimation(rotateAnim)
                            }

                            val isOutOfRange = viewModel.isOutOfRange.value

                            binding.tvTargetName.text = target.name.ifEmpty { "Perangkat Tidak Dikenal" }
                            binding.tvTargetMac.text = target.macAddress

                            if (isOutOfRange) {
                                binding.deviceDot.visibility = View.INVISIBLE
                                binding.tvTargetDistance.text = "> 20 m"
                                binding.tvTargetCategory.text = "Sinyal Hilang (Di luar jangkauan)"
                                binding.tvTargetRssi.text = "Kekuatan Sinyal: Terputus (-99 dBm)"

                                val lostColor = ContextCompat.getColor(requireContext(), R.color.signal_lost)
                                val lostBgColor = ContextCompat.getColor(requireContext(), R.color.bg_signal_lost)

                                binding.tvTargetDistance.setTextColor(lostColor)
                                binding.tvTargetCategory.setTextColor(lostColor)
                                binding.cardTargetCategory.setCardBackgroundColor(lostBgColor)

                            } else {
                                binding.deviceDot.visibility = View.VISIBLE

                                val maxDistance = 20.0
                                val distanceRatio = (target.estimatedDistance / maxDistance).coerceIn(0.0, 1.0)
                                val maxOffsetPx = 130 * resources.displayMetrics.density
                                val offset = (distanceRatio * maxOffsetPx).toFloat()

                                binding.deviceDot.translationX = offset * 0.7f
                                binding.deviceDot.translationY = -offset * 0.7f

                                binding.tvTargetDistance.text = String.format(Locale.getDefault(), "%.1f m", target.estimatedDistance)
                                binding.tvTargetCategory.text = target.signalCategory
                                binding.tvTargetRssi.text = String.format(Locale.getDefault(), "Kekuatan Sinyal: %d dBm", target.rssi)

                                val textColorRes = when {
                                    target.rssi > -50 -> R.color.signal_strong
                                    target.rssi > -70 -> R.color.signal_good
                                    target.rssi > -80 -> R.color.signal_weak
                                    else -> R.color.signal_lost
                                }

                                val bgColorRes = when {
                                    target.rssi > -50 -> R.color.bg_signal_strong
                                    target.rssi > -70 -> R.color.bg_signal_good
                                    target.rssi > -80 -> R.color.bg_signal_weak
                                    else -> R.color.bg_signal_lost
                                }

                                val activeColor = ContextCompat.getColor(requireContext(), textColorRes)
                                val activeBgColor = ContextCompat.getColor(requireContext(), bgColorRes)

                                binding.tvTargetDistance.setTextColor(activeColor)
                                binding.tvTargetCategory.setTextColor(activeColor)
                                binding.cardTargetCategory.setCardBackgroundColor(activeBgColor)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}