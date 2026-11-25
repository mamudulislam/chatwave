package com.example.chatwave.webrtc

import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription

enum class CallDirection { INCOMING, OUTGOING }

enum class CallUiState {
  IDLE, RINGING_OUT, RINGING_IN, CONNECTING, CONNECTED, RECONNECTING, ENDED, FAILED
}

data class IceServerConfig(
  val urls: List<String>,
  val username: String? = null,
  val credential: String? = null
)

data class WebRtcSignalPayload(
  val type: String? = null,
  val sdp: String? = null,
  val sdpMid: String? = null,
  val sdpMLineIndex: Int? = null,
  val candidate: String? = null,
  val reason: String? = null
)

data class CallSessionState(
  val sessionId: String,
  val peerId: String,
  val peerName: String,
  val peerAvatarColorHex: String,
  val direction: CallDirection,
  val isVideo: Boolean,
  val relayOnly: Boolean,
  val state: CallUiState,
  val safetyCode: String = "",
  val peerFingerprint: String = "",
  val peerEphemeralFingerprint: String = "",
  val isMuted: Boolean = false,
  val isCameraEnabled: Boolean = true,
  val isSpeakerEnabled: Boolean = false,
  val isFrontCamera: Boolean = true,
  val connectionState: PeerConnection.IceConnectionState? = null,
  val durationSeconds: Long = 0L,
  val error: String? = null
)

fun SessionDescription.toPayload(): WebRtcSignalPayload = WebRtcSignalPayload(type = type.canonicalForm(), sdp = description)
fun IceCandidate.toPayload(): WebRtcSignalPayload = WebRtcSignalPayload(sdpMid = sdpMid, sdpMLineIndex = sdpMLineIndex, candidate = sdp)
fun WebRtcSignalPayload.toSessionDescription(): SessionDescription = SessionDescription(SessionDescription.Type.fromCanonicalForm(type ?: "offer"), sdp.orEmpty())
fun WebRtcSignalPayload.toIceCandidate(): IceCandidate = IceCandidate(sdpMid, sdpMLineIndex ?: 0, candidate.orEmpty())
