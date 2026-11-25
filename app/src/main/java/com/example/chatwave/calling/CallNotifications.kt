package com.example.chatwave.calling

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.example.ChatWaveApplication
import com.example.MainActivity
import com.example.R

object CallNotifications {
  const val ACTION_ACCEPT = "com.example.chatwave.action.ACCEPT_CALL"
  const val ACTION_DECLINE = "com.example.chatwave.action.DECLINE_CALL"
  const val ACTION_END = "com.example.chatwave.action.END_CALL"
  const val EXTRA_SESSION_ID = "session_id"
  const val EXTRA_CALLER_ID = "caller_id"
  const val EXTRA_CALLER_NAME = "caller_name"
  const val EXTRA_VIDEO = "is_video"

  fun incoming(context: Context, sessionId: String, callerId: String, callerName: String, isVideo: Boolean): Notification {
    val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra(EXTRA_SESSION_ID, sessionId)
      putExtra("open_call", true)
    }
    val fullScreenPi = PendingIntent.getActivity(context, 100, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val acceptPi = actionPi(context, 101, ACTION_ACCEPT, sessionId)
    val declinePi = actionPi(context, 102, ACTION_DECLINE, sessionId)
    val person = Person.Builder().setName(callerName).setKey(callerId).build()
    return NotificationCompat.Builder(context, ChatWaveApplication.CALL_CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle(if (isVideo) "Incoming video call" else "Incoming audio call")
      .setContentText("$callerName ($callerId)")
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setCategory(NotificationCompat.CATEGORY_CALL)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setOngoing(true)
      .setAutoCancel(false)
      .setFullScreenIntent(fullScreenPi, true)
      .setContentIntent(fullScreenPi)
      .addAction(0, "Decline", declinePi)
      .addAction(0, "Accept", acceptPi)
      .setStyle(NotificationCompat.CallStyle.forIncomingCall(person, declinePi, acceptPi))
      .build()
  }

  fun ongoing(context: Context, sessionId: String, peerName: String, isVideo: Boolean): Notification {
    val contentIntent = PendingIntent.getActivity(
      context, 110,
      Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP; putExtra(EXTRA_SESSION_ID, sessionId) },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val endPi = actionPi(context, 111, ACTION_END, sessionId)
    return NotificationCompat.Builder(context, ChatWaveApplication.SERVICE_CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle(if (isVideo) "Video call" else "Audio call")
      .setContentText("In call with $peerName")
      .setOngoing(true)
      .setCategory(NotificationCompat.CATEGORY_CALL)
      .setContentIntent(contentIntent)
      .addAction(0, "End", endPi)
      .build()
  }

  private fun actionPi(context: Context, req: Int, action: String, sessionId: String): PendingIntent =
    PendingIntent.getBroadcast(
      context, req,
      Intent(context, CallActionReceiver::class.java).apply { this.action = action; putExtra(EXTRA_SESSION_ID, sessionId) },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
