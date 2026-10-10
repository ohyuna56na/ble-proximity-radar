package com.oyn.bleproximityradar.ui

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.oyn.bleproximityradar.R
import com.oyn.bleproximityradar.databinding.FragmentScannerBinding
import com.oyn.bleproximityradar.viewmodel.ScannerViewModel
import com.oyn.bleproximityradar.viewmodel.TrackingViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class ScannerFragment : Fragment(R.layout.fragment_scanner) {

    private val viewModel: ScannerViewModel by viewModels()
    private val trackingViewModel: TrackingViewModel by activityViewModels()
    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!
    private lateinit var deviceAdapter: DeviceAdapter

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            checkBluetoothAndScan()
        } else {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Izin Diperlukan")
                .setMessage("Aplikasi membutuhkan izin Bluetooth dan Lokasi untuk memindai perangkat di sekitar. Mohon berikan izin untuk melanjutkan.")
                .setPositiveButton("Mengerti", null)
                .show()
        }
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.startScanning()
        } else {
            Toast.makeText(requireContext(), "Bluetooth harus dinyalakan untuk memindai", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentScannerBinding.bind(view)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        deviceAdapter = DeviceAdapter { device ->
            trackingViewModel.setTargetDevice(device)
            requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigation).selectedItemId = R.id.trackingFragment
        }
        binding.rvDevices.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = deviceAdapter
        }
    }

    private fun setupListeners() {
        binding.btnStartScan.setOnClickListener {
            if (viewModel.isScanning.value) {
                viewModel.stopScanning()
            } else {
                checkPermissionsFirst()
            }
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.updateSearchQuery(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.btnFilter60.setOnClickListener { viewModel.updateRssiFilter(-60) }
        binding.btnFilter70.setOnClickListener { viewModel.updateRssiFilter(-70) }
        binding.btnFilter80.setOnClickListener { viewModel.updateRssiFilter(-80) }
        binding.btnFilterAll.setOnClickListener { viewModel.updateRssiFilter(-100) }
    }

    private fun checkPermissionsFirst() {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            checkBluetoothAndScan()
        } else {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun checkBluetoothAndScan() {
        val bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter

        if (adapter == null) {
            Toast.makeText(requireContext(), "Perangkat ini tidak mendukung Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        if (!adapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        } else {
            viewModel.startScanning()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.isScanning.collect { isScanning ->
                        binding.btnStartScan.text = if (isScanning) "Stop Scan" else "Start Scan"

                        if (isScanning) {
                            val pulseAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse)
                            binding.btnStartScan.startAnimation(pulseAnim)
                        } else {
                            binding.btnStartScan.clearAnimation()
                        }
                    }
                }

                launch {
                    viewModel.filteredDevices.collect { devices ->
                        deviceAdapter.submitList(devices)
                        binding.tvDeviceCountLabel.text = String.format(Locale.getDefault(), "%d perangkat", devices.size)
                        binding.tvActiveDevices.text = devices.size.toString()

                        if (devices.isNotEmpty()) {
                            val closest = devices.first()
                            binding.tvStrongestRssi.text = String.format(Locale.getDefault(), "%d dBm", closest.rssi)
                            binding.tvClosestDevice.text = closest.name.ifEmpty { "Perangkat Tidak Dikenal" }

                            val timeFormat = SimpleDateFormat("HH.mm", Locale.getDefault())
                            binding.tvLastUpdate.text = timeFormat.format(Date(closest.lastSeen))
                        } else {
                            binding.tvStrongestRssi.text = "-"
                            binding.tvClosestDevice.text = "-"
                            binding.tvLastUpdate.text = "-"
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