package com.example.chatwave.webrtc

import org.json.JSONArray
import org.json.JSONObject

data class RemotePublicIdentity(
  val chatwaveId: String,
  val displayName: String,
  val publicKey: String,
  val publicKeyVersion: Int,
  val fingerprint: String
)

data class RemoteCallParticipant(
  val chatwaveId: String,
  val role: String,
  val fingerprint: String,
  val ephemeralPublicKey: String
)

data class RemoteCallSession(
  val id: String,
  val state: String,
  val safetyBinding: String,
  val relayOnly: Boolean,
  val createdAt: Long,
  val expiresAt: Long,
  val endedAt: Long? = null,
  val participants: List<RemoteCallParticipant>
)

data class TurnCredentials(
  val username: String,
  val credential: String,
  val expires: Long,
  val urls: List<String>
)

sealed interface RealtimeEvent {
  data class AuthOk(val chatwaveId: String, val fingerprint: String) : RealtimeEvent
  data class IncomingCall(
    val sessionId: String,
    val caller: RemotePublicIdentity,
    val safetyCode: String,
    val relayOnly: Boolean,
    val expiresAt: Long
  ) : RealtimeEvent
  data class Signal(
    val sessionId: String,
    val from: String,
    val kind: String,
    val data: JSONObject,
    val nonce: String
  ) : RealtimeEvent
  data class Error(val code: String, val message: String) : RealtimeEvent
}

fun JSONObject.toPublicIdentity(): RemotePublicIdentity = RemotePublicIdentity(
  chatwaveId = optString("chatwaveId"),
  displayName = optString("displayName"),
  publicKey = optString("publicKey"),
  publicKeyVersion = optInt("publicKeyVersion", 1),
  fingerprint = optString("fingerprint")
)

fun JSONObject.toCallSession(): RemoteCallSession {
  val participantsJson = optJSONArray("participants") ?: JSONArray()
  val participants = buildList {
    for (i in 0 until participantsJson.length()) {
      val p = participantsJson.optJSONObject(i) ?: continue
      add(RemoteCallParticipant(
        chatwaveId = p.optString("chatwave_id"),
        role = p.optString("role"),
        fingerprint = p.optString("fingerprint"),
        ephemeralPublicKey = p.optString("ephemeral_public_key")
      ))
    }
  }
  return RemoteCallSession(
    id = optString("id"),
    state = optString("state"),
    safetyBinding = optString("safetyBinding"),
    relayOnly = optBoolean("relayOnly", false),
    createdAt = optLong("createdAt", 0L),
    expiresAt = optLong("expiresAt", 0L),
    endedAt = if (isNull("endedAt")) null else optLong("endedAt"),
    participants = participants
  )
}

fun JSONObject.toTurnCredentials(): TurnCredentials {
  val urlsJson = optJSONArray("urls") ?: JSONArray()
  return TurnCredentials(
    username = optString("username"),
    credential = optString("credential"),
    expires = optLong("expires", 0L),
    urls = buildList { for (i in 0 until urlsJson.length()) add(urlsJson.optString(i)) }
  )
}
