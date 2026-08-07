package com.accessnow

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val overlayPermissionRequest = requestPermissionLauncher(
        Manifest.permission.SYSTEM_ALERT_WINDOW
    )
    private val accessibilityPermissionRequest = requestPermissionLauncher(
        Manifest.permission.BIND_ACCESSIBILITY_SERVICE
    )

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestPermissionLauncher(permission: String) =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val granted = result.resultCode == Activity.RESULT_OK
            // No explicit action; services check permission at runtime.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btn_get_help).setOnClickListener { onGetHelp() }
        findViewById<Button>(R.id.btn_help_someone).setOnClickListener { onHelpSomeone() }
    }

    private fun onGetHelp() {
        // Request overlay permission
        if (!Settings.canDrawOverlays(this)) {
            overlayPermissionRequest.launch(Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            ))
        }
        // Start session timer service
        startService(Intent(this, SessionTimerService::class.java))
    }

    private fun onHelpSomeone() {
        // Launch WebRTC client UI
        val intent = Intent(this, WebRTCClient::class.java)
        startActivity(intent)
    }
}
