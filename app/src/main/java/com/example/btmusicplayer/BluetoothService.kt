package com.example.btmusicplayer

import android.app.*
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.view.KeyEvent

class BluetoothService : Service() {

    private val CHANNEL_ID = "bt_auto_play_channel"

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (BluetoothDevice.ACTION_ACL_CONNECTED == intent.action) {
                val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }

                val prefs = context.getSharedPreferences("BT_SETTINGS", Context.MODE_PRIVATE)
                val targetMac = prefs.getString("TARGET_MAC", "")
                val musicApp = prefs.getString("TARGET_APP", "SPOTIFY")

                if (!targetMac.isNullOrEmpty() && device?.address.equals(targetMac, ignoreCase = true)) {
                    triggerMusicPlay(context, musicApp ?: "SPOTIFY")
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val notification = builder
            .setContentTitle("藍牙自動播放運行中")
            .setContentText("等待指定藍牙裝置連線...")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(101, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(101, notification)
        }

        val filter = IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED)
        registerReceiver(bluetoothReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun triggerMusicPlay(context: Context, appType: String) {
        val downIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
        }
        val upIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
        }

        val pkgName = if (appType == "SPOTIFY") "com.spotify.music" else "com.apple.android.music"
        downIntent.setPackage(pkgName)
        upIntent.setPackage(pkgName)

        context.sendOrderedBroadcast(downIntent, null)
        context.sendOrderedBroadcast(upIntent, null)

        val launchIntent = context.packageManager.getLaunchIntentForPackage(pkgName)
        launchIntent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "藍牙播放監聽",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
