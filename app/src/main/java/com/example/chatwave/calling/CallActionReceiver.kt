package com.example.chatwave.calling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CallActionReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action ?: return
    val sessionId = intent.getStringExtra(CallNotifications.EXTRA_SESSION_ID).orEmpty()
    when (action) {
      CallNotifications.ACTION_ACCEPT -> CallCommandBus.emit(CallCommand.Accept(sessionId))
      CallNotifications.ACTION_DECLINE -> CallCommandBus.emit(CallCommand.Decline(sessionId))
      CallNotifications.ACTION_END -> CallCommandBus.emit(CallCommand.End(sessionId))
    }
  }
}
