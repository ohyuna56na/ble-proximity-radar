package com.oyn.bleproximityradar.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.oyn.bleproximityradar.R
import com.oyn.bleproximityradar.databinding.FragmentHistoryBinding
import com.oyn.bleproximityradar.viewmodel.HistoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class HistoryFragment : Fragment(R.layout.fragment_history) {

    private val viewModel: HistoryViewModel by viewModels()
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var historyAdapter: HistoryAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHistoryBinding.bind(view)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter()
        binding.rvHistoryDevices.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }
    }

    private fun setupListeners() {
        binding.tvHapusRiwayat.setOnClickListener {
            viewModel.clearAllHistory()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.historyDevices.collect { historyList ->
                    historyAdapter.submitList(historyList)

                    val totalSaved = historyList.size

                    val todayCount = historyList.count { device ->
                        isToday(device.lastSeen)
                    }

                    binding.tvTotalSaved.text = totalSaved.toString()
                    binding.tvTodayCount.text = todayCount.toString()
                }
            }
        }
    }

    private fun isToday(timestampMillis: Long): Boolean {
        val deviceCal = Calendar.getInstance().apply {
            timeInMillis = timestampMillis
        }
        val todayCal = Calendar.getInstance()

        return deviceCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                deviceCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}