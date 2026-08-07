package com.accessnow

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RequiresApi
import org.webrtc.PeerConnection

/**
 * Foreground service that imposes a 15‑minute session limiting WebRTC data channels.
 * When the countdown reaches zero it closes all active PeerConnections and removes
 * accessibility permissions acquired by the app.
 */
@RequiresApi(Build.VERSION_CODES.O)
class SessionTimerService : Service() {
    private var timer: CountDownTimer? = null
    private val sessionDurationMs = 15L * 60L * 1000L // 15 minutes
    private val TAG = "SessionTimerService"

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        startTimer()
    }

    private fun startForegroundService() {
        // Creates minimal notification channel – real project should expose user‑visible
        // notification text.  Simplified for the example.
        val channelId = "accessnow_session"
        val notification = android.app.Notification.Builder(this, channelId)
            .setContentTitle("AccessNow Session")
            .setContentText("Session active – 15 minutes remaining")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .build()
        startForeground(1, notification)
    }

    private fun startTimer() {
        timer = object : CountDownTimer(sessionDurationMs, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                // Optionally update foreground notification with remaining time.
            }

            override fun onFinish() {
                Log.i(TAG, "Session expired – closing connections and revoking permissions")
                closePeerConnections()
                revokeAccessibilityPermissions()
                stopSelf()
            }
        }.start()
    }

    private fun closePeerConnections() {
        // In a real implementation the service would keep a registry of active
        // PeerConnection instances.  Here we demonstrate a placeholder.
        PeerConnectionRegistry.getAll().forEach { pc ->
            try { pc.close() } catch (e: Exception) {}
        }
    }

    private fun revokeAccessibilityPermissions() {
        // Walking backwards through grant operations – real code would call
        // revokeRights on the AccessibilityManager or clear the app's granted
        // permissions via Settings.
        val am = getSystemService(AccessibilityService::class.java)
        try {
            am?.setAccessibilityEnabled(false)
        } catch (e: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
