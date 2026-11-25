package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.chatwave.di.AppGraph

class ChatWaveApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    AppGraph.init(this)
    createNotificationChannels()
  }

  private fun createNotificationChannels() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val nm = getSystemService(NotificationManager::class.java)
    nm.createNotificationChannel(NotificationChannel(
      CALL_CHANNEL_ID,
      "Calls",
      NotificationManager.IMPORTANCE_HIGH
    ).apply {
      description = "Incoming and active ChatWave calls"
      setSound(null, null)
      enableVibration(true)
    })
    nm.createNotificationChannel(NotificationChannel(
      SERVICE_CHANNEL_ID,
      "Call service",
      NotificationManager.IMPORTANCE_LOW
    ).apply { description = "Active call foreground service" })
  }

  companion object {
    const val CALL_CHANNEL_ID = "chatwave_calls"
    const val SERVICE_CHANNEL_ID = "chatwave_call_service"
  }
}
