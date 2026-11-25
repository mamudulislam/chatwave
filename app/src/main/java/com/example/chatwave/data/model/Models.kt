package com.example.chatwave.data.model

enum class ScreenDestination {
  SPLASH, FIRST_LAUNCH, CREATE_ACCOUNT, UNLOCK, HOME, ADD_CONTACT, INVITE_CONTACT, INCOMING_INVITES,
  CALL_HISTORY, OUTGOING_CALL, INCOMING_CALL, ACTIVE_AUDIO_CALL, ACTIVE_VIDEO_CALL, CALL_SUMMARY,
  SETTINGS, DEVICE_SECURITY, DIAGNOSTICS
}

data class UserProfileUi(
  val chatwaveId: String = "",
  val displayName: String = "",
  val fingerprint: String = "",
  val publicKeyVersion: Int = 1,
  val deviceId: String = "",
  val backendUrl: String = ""
)

data class ContactUi(
  val chatwaveId: String,
  val displayName: String,
  val fingerprint: String,
  val verified: Boolean,
  val lastSeenAt: Long? = null
)

data class InviteUi(
  val invitationId: String,
  val senderChatwaveId: String,
  val senderDisplayName: String,
  val senderFingerprint: String,
  val createdAt: Long,
  val status: String
)

data class CallHistoryUi(
  val sessionId: String,
  val peerId: String,
  val peerName: String,
  val direction: String,
  val callType: String,
  val status: String,
  val startedAt: Long,
  val durationSeconds: Long
)

data class CallUiStateModel(
  val sessionId: String = "",
  val peerId: String = "",
  val peerName: String = "",
  val callType: String = "audio",
  val direction: String = "outgoing",
  val state: String = "idle",
  val durationSeconds: Long = 0,
  val isMuted: Boolean = false,
  val isSpeaker: Boolean = false,
  val isCameraEnabled: Boolean = true,
  val relayOnly: Boolean = false,
  val safetyCode: String = "",
  val peerFingerprint: String = "",
  val error: String? = null
)

data class DiagnosticsUi(
  val backendUrl: String = "",
  val signalingConnected: Boolean = false,
  val turnConfigured: Boolean = false,
  val relayOnly: Boolean = false,
  val lastError: String? = null,
  val localFingerprint: String = "",
  val keyVersion: Int = 1
)
