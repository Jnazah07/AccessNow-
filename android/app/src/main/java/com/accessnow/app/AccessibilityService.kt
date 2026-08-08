package com.accessnow.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import androidx.annotation.RequiresApi

/**
 * AccessibilityService that receives remote coordinate packets and performs safe
 * click actions. It also shows an un‑hiddable overlay indicating an active
 * remote session.
 */
@RequiresApi(Build.VERSION_CODES.N)
class RemoteAccessService : AccessibilityService() {
    private val TAG = "RemoteAccessService"
    private var overlayWindow: WindowManager? = null
    private var overlayView: TextView? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Accessibility service connected")
        setupOverlay()
        // Configure the service to listen for custom intent packets.
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            packageNames = null
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 500
        }
        serviceInfo = info
    }

    private fun setupOverlay() {
        overlayView = LayoutInflater.from(this)
            .inflate(android.R.layout.simple_list_item_1, null) as TextView
        overlayView?.apply {
            text = "AccessNow Active Session"
            setBackgroundColor(0x880000ff.toInt())
            setTextColor(0xffffffff.toInt())
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = 0
        }
        overlayWindow = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayWindow?.addView(overlayView, params)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used – we handle remote packets via a custom broadcast.
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    /**
     * This method would be bound to a broadcast receiver that receives remote
     * coordinate packets. For the example, we keep it simple.
     */
    fun handleRemotePacket(x: Float, y: Float) {
        // Convert into a MotionEvent and inject via AccessibilityService#performGlobalAction
        // Real implementation would use the AccessibilityService#dispatchGesture API.
        Log.i(TAG, "Handling remote packet: ($x,$y)")
        // Danger: only perform click when the user has granted permission.
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let { overlayWindow?.removeView(it) }
    }
}
