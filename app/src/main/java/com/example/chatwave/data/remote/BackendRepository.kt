package com.example.chatwave.data.remote

import com.example.chatwave.crypto.CanonicalJson
import com.example.chatwave.crypto.Hash
import com.example.chatwave.crypto.InvitationCodeGenerator
import com.example.chatwave.data.local.InvitationEntity
import com.example.chatwave.data.local.UserProfileEntity
import com.example.chatwave.security.IdentityKeyManager
import com.example.chatwave.webrtc.RemoteCallSession
import com.example.chatwave.webrtc.RemotePublicIdentity
import com.example.chatwave.webrtc.TurnCredentials
import com.example.chatwave.webrtc.toCallSession
import com.example.chatwave.webrtc.toPublicIdentity
import com.example.chatwave.webrtc.toTurnCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

data class CreatedInvitation(val code: String, val formattedCode: String, val codeHash: String, val expiresAt: Long)
data class RedeemedInvitation(val inviter: RemotePublicIdentity, val safetyCode: String)

class BackendRepository(
  private val api: BackendApi,
  private val identityKeyManager: IdentityKeyManager,
  private val invitationCodeGenerator: InvitationCodeGenerator = InvitationCodeGenerator()
) {
  fun updateBaseUrl(url: String) = api.updateBaseUrl(url)

  suspend fun register(profile: UserProfileEntity): RemotePublicIdentity = withContext(Dispatchers.IO) {
    val payload = mapOf(
      "chatwaveId" to profile.chatwaveId,
      "displayName" to profile.displayName,
      "publicKey" to profile.publicKey,
      "publicKeyVersion" to profile.publicKeyVersion,
      "recoveryHash" to profile.recoveryHash
    )
    val body = JSONObject().apply {
      put("chatwaveId", profile.chatwaveId)
      put("displayName", profile.displayName)
      put("publicKey", profile.publicKey)
      put("publicKeyVersion", profile.publicKeyVersion)
      put("recoveryHash", profile.recoveryHash)
      put("deviceId", profile.deviceId)
      put("envelope", signedEnvelope("user.register", profile, payload))
    }
    api.post("/api/users/register", body).toPublicIdentity()
  }

  suspend fun recoverIdentity(profile: UserProfileEntity, recoveryToken: String, newPublicKey: String, newVersion: Int): RemotePublicIdentity = withContext(Dispatchers.IO) {
    val payload = mapOf("chatwaveId" to profile.chatwaveId, "publicKey" to newPublicKey, "publicKeyVersion" to newVersion)
    val body = JSONObject().apply {
      put("chatwaveId", profile.chatwaveId)
      put("publicKey", newPublicKey)
      put("publicKeyVersion", newVersion)
      put("recoveryToken", recoveryToken)
      put("envelope", signedEnvelope("identity.recover", profile, payload, keyVersionOverride = newVersion, signingVersion = newVersion))
    }
    api.post("/api/identity/recover", body).toPublicIdentity()
  }

  suspend fun lookup(chatwaveId: String): RemotePublicIdentity = withContext(Dispatchers.IO) {
    api.get("/api/users/$chatwaveId").toPublicIdentity()
  }

  suspend fun createInvitation(profile: UserProfileEntity, ttlSeconds: Long = 300L): CreatedInvitation = withContext(Dispatchers.IO) {
    val code = invitationCodeGenerator.generate()
    val codeHash = Hash.sha256Hex(code)
    val expiresAt = System.currentTimeMillis() + ttlSeconds * 1000L
    val payload = mapOf("codeHash" to codeHash, "expiresAt" to expiresAt)
    val body = JSONObject().apply {
      put("codeHash", codeHash)
      put("expiresAt", expiresAt)
      put("envelope", signedEnvelope("invitation.create", profile, payload))
    }
    api.post("/api/invitations", body)
    CreatedInvitation(code, invitationCodeGenerator.format(code), codeHash, expiresAt)
  }

  suspend fun redeemInvitation(code: String, profile: UserProfileEntity): RedeemedInvitation = withContext(Dispatchers.IO) {
    val normalized = invitationCodeGenerator.normalize(code)
    val codeHash = Hash.sha256Hex(normalized)
    val payload = mapOf("codeHash" to codeHash, "chatwaveId" to profile.chatwaveId)
    val body = JSONObject().apply {
      put("code", normalized)
      put("envelope", signedEnvelope("invitation.redeem", profile, payload))
    }
    val res = api.post("/api/invitations/redeem", body)
    RedeemedInvitation(res.getJSONObject("inviter").toPublicIdentity(), res.optString("safetyCode"))
  }

  suspend fun cancelInvitation(codeHash: String, profile: UserProfileEntity) = withContext(Dispatchers.IO) {
    val payload = mapOf("codeHash" to codeHash)
    val body = JSONObject().apply { put("envelope", signedEnvelope("invitation.cancel", profile, payload)) }
    api.post("/api/invitations/cancel", body)
    Unit
  }

  suspend fun createCall(
    calleeId: String,
    profile: UserProfileEntity,
    relayOnly: Boolean,
    ephemeralPublicKeyDerB64: String,
    callerFingerprint: String,
    safetyBinding: String
  ): RemoteCallSession = withContext(Dispatchers.IO) {
    val payload = mapOf(
      "calleeId" to calleeId,
      "ephemeralPublicKey" to ephemeralPublicKeyDerB64,
      "fingerprint" to callerFingerprint,
      "safetyBinding" to safetyBinding,
      "relayOnly" to relayOnly
    )
    val body = JSONObject().apply {
      put("calleeId", calleeId)
      put("envelope", signedEnvelope("call.create", profile, payload, toId = calleeId))
    }
    api.post("/api/calls", body).toCallSession()
  }

  suspend fun acceptCall(
    sessionId: String,
    callerId: String,
    profile: UserProfileEntity,
    ephemeralPublicKeyDerB64: String,
    fingerprint: String,
    safetyBinding: String
  ): RemoteCallSession = withContext(Dispatchers.IO) {
    val payload = mapOf(
      "sessionId" to sessionId,
      "ephemeralPublicKey" to ephemeralPublicKeyDerB64,
      "fingerprint" to fingerprint,
      "safetyBinding" to safetyBinding
    )
    val body = JSONObject().apply {
      put("envelope", signedEnvelope("call.accept", profile, payload, toId = callerId, sessionId = sessionId))
    }
    api.post("/api/calls/$sessionId/accept", body).toCallSession()
  }

  suspend fun declineCall(sessionId: String, profile: UserProfileEntity): RemoteCallSession = withContext(Dispatchers.IO) {
    val payload = mapOf("sessionId" to sessionId)
    val body = JSONObject().apply { put("envelope", signedEnvelope("call.decline", profile, payload, sessionId = sessionId)) }
    api.post("/api/calls/$sessionId/decline", body).toCallSession()
  }

  suspend fun endCall(sessionId: String, profile: UserProfileEntity): RemoteCallSession = withContext(Dispatchers.IO) {
    val payload = mapOf("sessionId" to sessionId)
    val body = JSONObject().apply { put("envelope", signedEnvelope("call.end", profile, payload, sessionId = sessionId)) }
    api.post("/api/calls/$sessionId/end", body).toCallSession()
  }

  suspend fun sendSignal(sessionId: String, kind: String, data: Map<String, Any?>, profile: UserProfileEntity): JSONObject = withContext(Dispatchers.IO) {
    val payload = mapOf("sessionId" to sessionId, "kind" to kind, "data" to data)
    val body = JSONObject().apply { put("envelope", signedEnvelope("call.signal", profile, payload, sessionId = sessionId)) }
    api.post("/api/calls/$sessionId/signal", body)
  }

  suspend fun turnCredentials(profile: UserProfileEntity): TurnCredentials = withContext(Dispatchers.IO) {
    val payload = mapOf("chatwaveId" to profile.chatwaveId)
    val body = JSONObject().apply { put("envelope", signedEnvelope("turn.credentials", profile, payload)) }
    api.post("/api/turn/credentials", body).toTurnCredentials()
  }

  fun createSignalEnvelope(sessionId: String, kind: String, data: Map<String, Any?>, profile: UserProfileEntity): JSONObject {
    val payload = mapOf("sessionId" to sessionId, "kind" to kind, "data" to data)
    return signedEnvelope("call.signal", profile, payload, sessionId = sessionId)
  }

  private fun signedEnvelope(
    type: String,
    profile: UserProfileEntity,
    payload: Map<String, Any?>,
    toId: String? = null,
    sessionId: String? = null,
    keyVersionOverride: Int? = null,
    signingVersion: Int? = null
  ): JSONObject {
    val keyVersion = keyVersionOverride ?: profile.publicKeyVersion
    val canonicalPayload = CanonicalJson.canonicalize(payload)
    val unsigned = linkedMapOf<String, Any?>(
      "type" to type,
      "fromId" to profile.chatwaveId,
      "toId" to toId,
      "sessionId" to sessionId,
      "payload" to canonicalPayload,
      "timestamp" to System.currentTimeMillis(),
      "nonce" to Hash.randomToken(18),
      "keyVersion" to keyVersion
    )
    val signature = identityKeyManager.sign(unsigned, signingVersion ?: profile.publicKeyVersion)
    return JSONObject().apply {
      unsigned.forEach { (k, v) -> when (v) { null -> put(k, JSONObject.NULL); else -> put(k, v) } }
      put("signature", signature)
    }
  }

  companion object {
    fun maskInvitation(code: String): String {
      val digits = code.filter(Char::isDigit)
      return if (digits.length <= 3) "***" else "*** *** ${digits.takeLast(3)}"
    }
    fun toInvitationEntity(profile: UserProfileEntity, created: CreatedInvitation): InvitationEntity =
      InvitationEntity(created.codeHash, maskInvitation(created.code), profile.chatwaveId, created.expiresAt, System.currentTimeMillis(), "CREATED")
  }
}
