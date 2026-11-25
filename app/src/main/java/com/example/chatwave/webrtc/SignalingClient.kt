package com.example.chatwave.webrtc

import android.util.Log
import com.example.chatwave.crypto.CanonicalJson
import com.example.chatwave.security.IdentityKeyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SignalingClient(
  private val baseUrlProvider: () -> String,
  private val identity: () -> Triple<String, Int, String>?,
  private val identityKeyManager: IdentityKeyManager,
  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
  private val client = OkHttpClient.Builder()
    .readTimeout(0, TimeUnit.MILLISECONDS)
    .pingInterval(20, TimeUnit.SECONDS)
    .build()

  private var socket: WebSocket? = null
  private var manuallyClosed = false
  private var retry = 0

  private val _connected = MutableStateFlow(false)
  val connected: StateFlow<Boolean> = _connected.asStateFlow()

  private val _events = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 128)
  val events: SharedFlow<RealtimeEvent> = _events.asSharedFlow()

  fun connect() {
    if (identity() == null) return
    manuallyClosed = false
    if (socket != null) return
    val wsUrl = baseUrlProvider().trimEnd('/')
      .replace("http://", "ws://")
      .replace("https://", "wss://") + "/ws"
    val request = Request.Builder().url(wsUrl).build()
    socket = client.newWebSocket(request, object : WebSocketListener() {
      override fun onOpen(webSocket: WebSocket, response: Response) {
        retry = 0
        _connected.value = true
      }

      override fun onMessage(webSocket: WebSocket, text: String) {
        handle(text, webSocket)
      }

      override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        socket = null; _connected.value = false; scheduleReconnect()
      }

      override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.w("SignalingClient", "ws failure", t)
        socket = null; _connected.value = false; scheduleReconnect()
      }
    })
  }

  fun disconnect() {
    manuallyClosed = true
    socket?.close(1000, "logout")
    socket = null
    _connected.value = false
  }

  fun sendSignal(sessionId: String, envelope: JSONObject): Boolean =
    socket?.send(JSONObject().apply {
      put("type", "signal")
      put("sessionId", sessionId)
      put("envelope", envelope)
    }.toString()) ?: false

  private fun handle(text: String, ws: WebSocket) {
    val msg = runCatching { JSONObject(text) }.getOrNull() ?: return
    when (msg.optString("type")) {
      "auth_challenge" -> {
        val id = identity() ?: return
        val unsigned = mapOf(
          "type" to "auth",
          "chatwaveId" to id.first,
          "publicKeyVersion" to id.second,
          "deviceId" to id.third,
          "nonce" to msg.optString("nonce")
        )
        val auth = JSONObject().apply {
          unsigned.forEach { (k, v) -> put(k, v) }
          put("signature", identityKeyManager.sign(unsigned, id.second))
        }
        ws.send(auth.toString())
      }
      "auth_ok" -> _events.tryEmit(RealtimeEvent.AuthOk(msg.optString("chatwaveId"), msg.optString("fingerprint")))
      "incoming_call" -> {
        val caller = msg.optJSONObject("caller")?.toPublicIdentity() ?: return
        _events.tryEmit(RealtimeEvent.IncomingCall(
          sessionId = msg.optString("sessionId"),
          caller = caller,
          safetyCode = msg.optString("safetyCode"),
          relayOnly = msg.optBoolean("relayOnly", false),
          expiresAt = msg.optLong("expiresAt", 0L)
        ))
      }
      "signal" -> _events.tryEmit(RealtimeEvent.Signal(
        sessionId = msg.optString("sessionId"),
        from = msg.optString("from"),
        kind = msg.optString("kind"),
        data = msg.optJSONObject("data") ?: JSONObject(),
        nonce = msg.optString("nonce")
      ))
      "incoming_call_signal" -> _events.tryEmit(RealtimeEvent.Signal(
        sessionId = msg.optString("sessionId"),
        from = msg.optString("from"),
        kind = "incoming_call_signal",
        data = JSONObject(),
        nonce = msg.optString("nonce")
      ))
      "error", "auth_error" -> _events.tryEmit(RealtimeEvent.Error(msg.optString("code"), msg.optString("message")))
    }
  }

  private fun scheduleReconnect() {
    if (manuallyClosed || identity() == null) return
    scope.launch {
      val waitMs = minOf(1000L * (1 shl retry.coerceAtMost(5)), 30_000L)
      retry++
      delay(waitMs)
      connect()
    }
  }
}
