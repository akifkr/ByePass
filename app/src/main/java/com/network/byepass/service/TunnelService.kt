package com.network.byepass.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.ProxyInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.network.byepass.core.LocalProxyServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TunnelService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var proxy: LocalProxyServer? = null

    companion object {
        const val ACTION_DISCONNECT = "com.network.byepass.DISCONNECT"
        private const val CHANNEL_ID = "vpn_channel"
        private const val NOTIFICATION_ID = 1

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning

        private val _logs = MutableStateFlow<List<String>>(emptyList())
        val logs: StateFlow<List<String>> = _logs

        fun log(msg: String) {
            _logs.value = (_logs.value + msg).takeLast(100)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISCONNECT) {
            stopTunnel()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        startTunnel()
        return START_STICKY
    }

    private fun startTunnel() {
        if (_isRunning.value) return

        try {
            log("Lokal motor başlatılıyor (127.0.0.1:10808)...")
            proxy = LocalProxyServer(10808)
            proxy?.start()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val builder = this.Builder()
                    .setSession("ByePass")
                    .setMtu(1500)
                    .addAddress("10.233.233.2", 32)
                    .addRoute("10.233.233.0", 24)
                    .setHttpProxy(ProxyInfo.buildDirectProxy("127.0.0.1", 10808))

                builder.addDisallowedApplication(packageName)
                vpnInterface = builder.establish()
                log("Sistem HTTP proxy aktif edildi (Android 10+).")
            } else {
                log("Proxy hazır. (Android 7-9: Wi-Fi proxy 127.0.0.1:10808 gereklidir).")
            }

            _isRunning.value = true
            log("Bypass devrede. Trafik işleniyor.")
        } catch (e: Exception) {
            log("Hata: ${e.message}")
            stopTunnel()
        }
    }

    private fun stopTunnel() {
        _isRunning.value = false
        try {
            proxy?.stop()
            proxy = null
            vpnInterface?.close()
            vpnInterface = null
        } catch (_: Exception) {}

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        log("Servis durduruldu.")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "ByePass Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, TunnelService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val pIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ByePass Aktif")
            .setContentText("DPI bypass ve DNS koruması devrede.")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Durdur", pIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopTunnel()
        super.onDestroy()
    }
}