package com.example.chatwave.webrtc

import org.json.JSONObject

fun JSONObject.toSignalPayload(): WebRtcSignalPayload = WebRtcSignalPayload(
  type = optString("type").takeIf { it.isNotBlank() },
  sdp = optString("sdp").takeIf { it.isNotBlank() },
  sdpMid = optString("sdpMid").takeIf { it.isNotBlank() },
  sdpMLineIndex = if (has("sdpMLineIndex")) optInt("sdpMLineIndex") else null,
  candidate = optString("candidate").takeIf { it.isNotBlank() },
  reason = optString("reason").takeIf { it.isNotBlank() }
)
