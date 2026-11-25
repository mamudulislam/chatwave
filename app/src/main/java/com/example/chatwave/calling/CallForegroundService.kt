package com.example.chatwave.calling

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.example.chatwave.di.AppGraph

class CallForegroundService : Service() {
  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val action = intent?.action
    val sessionId = intent?.getStringExtra(CallNotifications.EXTRA_SESSION_ID).orEmpty()
    val callerName = intent?.getStringExtra(CallNotifications.EXTRA_CALLER_NAME) ?: "Unknown"
    val callerId = intent?.getStringExtra(CallNotifications.EXTRA_CALLER_ID) ?: "Unknown"
    val isVideo = intent?.getBooleanExtra(CallNotifications.EXTRA_VIDEO, false) ?: false
    when (action) {
      ACTION_INCOMING -> {
        val notif = CallNotifications.incoming(this, sessionId, callerId, callerName, isVideo)
        startFg(1001, notif, isVideo)
      }
      ACTION_ONGOING -> {
        val notif = CallNotifications.ongoing(this, sessionId, callerName, isVideo)
        startFg(1001, notif, isVideo)
      }
      ACTION_STOP -> stopForegroundSafely()
    }
    return START_NOT_STICKY
  }

  private fun startFg(id: Int, notif: android.app.Notification, isVideo: Boolean) {
    val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      var t = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
      if (isVideo) t = t or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
      t
    } else 0
    ServiceCompat.startForeground(this, id, notif, type)
  }

  private fun stopForegroundSafely() {
    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  companion object {
    const val ACTION_INCOMING = "com.example.chatwave.call.INCOMING"
    const val ACTION_ONGOING = "com.example.chatwave.call.ONGOING"
    const val ACTION_STOP = "com.example.chatwave.call.STOP"

    fun incoming(context: Context, sessionId: String, callerId: String, callerName: String, isVideo: Boolean) {
      val i = Intent(context, CallForegroundService::class.java).apply {
        action = ACTION_INCOMING
        putExtra(CallNotifications.EXTRA_SESSION_ID, sessionId)
        putExtra(CallNotifications.EXTRA_CALLER_ID, callerId)
        putExtra(CallNotifications.EXTRA_CALLER_NAME, callerName)
        putExtra(CallNotifications.EXTRA_VIDEO, isVideo)
      }
      context.startForegroundService(i)
    }

    fun ongoing(context: Context, sessionId: String, peerName: String, isVideo: Boolean) {
      val i = Intent(context, CallForegroundService::class.java).apply {
        action = ACTION_ONGOING
        putExtra(CallNotifications.EXTRA_SESSION_ID, sessionId)
        putExtra(CallNotifications.EXTRA_CALLER_NAME, peerName)
        putExtra(CallNotifications.EXTRA_VIDEO, isVideo)
      }
      context.startForegroundService(i)
    }

    fun stop(context: Context) {
      context.startService(Intent(context, CallForegroundService::class.java).apply { action = ACTION_STOP })
    }
  }
}
