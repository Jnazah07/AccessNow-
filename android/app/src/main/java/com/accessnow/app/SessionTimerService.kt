package com.accessnow.app

import android.app.Service
import android.content.Intent
import android.os.IBinder

class SessionTimerService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
