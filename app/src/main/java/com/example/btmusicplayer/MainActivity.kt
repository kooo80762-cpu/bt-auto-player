package com.example.btmusicplayer

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var spinnerDevices: Spinner
    private lateinit var radioGroup: RadioGroup
    private lateinit var btnStart: Button
    private val pairedDeviceList = mutableListOf<BluetoothDevice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        spinnerDevices = Spinner(this)
        radioGroup = RadioGroup(this).apply {
            val rbSpotify = RadioButton(this@MainActivity).apply { text = "Spotify"; id = 1; isChecked = true }
            val rbApple = RadioButton(this@MainActivity).apply { text = "Apple Music"; id = 2 }
            addView(rbSpotify)
            addView(rbApple)
        }
        btnStart = Button(this).apply { text = "儲存設定並啟動監聽服務" }

        layout.addView(TextView(this).apply { text = "選擇目標藍牙裝置："; textSize = 16f })
        layout.addView(spinnerDevices)
        layout.addView(TextView(this).apply { text = "\n選擇播放平台："; textSize = 16f })
        layout.addView(radioGroup)
        layout.addView(btnStart)
        setContentView(layout)

        checkPermissions()

        btnStart.setOnClickListener {
            saveAndStartService()
        }
    }

    @SuppressLint("MissingPermission")
    private fun loadPairedDevices() {
        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        val adapter = bluetoothManager?.adapter
        val paired = adapter?.bondedDevices ?: emptySet()
        
        pairedDeviceList.clear()
        pairedDeviceList.addAll(paired)

        val deviceNames = pairedDeviceList.map { "${it.name ?: "未知裝置"} (${it.address})" }
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, deviceNames)
        spinnerDevices.adapter = spinnerAdapter
    }

    private fun saveAndStartService() {
        if (pairedDeviceList.isEmpty()) {
            Toast.makeText(this, "未找到已配對的藍牙裝置", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedDevice = pairedDeviceList[spinnerDevices.selectedItemPosition]
        val appChoice = if (radioGroup.checkedRadioButtonId == 1) "SPOTIFY" else "APPLE_MUSIC"

        val prefs = getSharedPreferences("BT_SETTINGS", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("TARGET_MAC", selectedDevice.address)
            .putString("TARGET_APP", appChoice)
            .apply()

        val serviceIntent = Intent(this, BluetoothService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        Toast.makeText(this, "服務已啟動！連線至該藍牙時將自動播放", Toast.LENGTH_LONG).show()
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val ungranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, ungranted.toTypedArray(), 100)
        } else {
            loadPairedDevices()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            loadPairedDevices()
        }
    }
}
