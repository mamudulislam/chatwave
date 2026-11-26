package com.example.chatwave.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatwave.calling.CallCommand
import com.example.chatwave.calling.CallCommandBus
import com.example.chatwave.calling.CallForegroundService
import com.example.chatwave.crypto.ChatWaveIdGenerator
import com.example.chatwave.crypto.InvitationCodeGenerator
import com.example.chatwave.crypto.SafetyCode
import com.example.chatwave.data.local.CallRecordEntity
import com.example.chatwave.data.local.DeviceEntity
import com.example.chatwave.data.local.InvitationEntity
import com.example.chatwave.data.local.SecuritySettingsEntity
import com.example.chatwave.data.local.TrustedContactEntity
import com.example.chatwave.data.local.UserProfileEntity
import com.example.chatwave.data.model.CallHistoryUi
import com.example.chatwave.data.model.CallUiStateModel
import com.example.chatwave.data.model.ContactUi
import com.example.chatwave.data.model.DiagnosticsUi
import com.example.chatwave.data.model.InviteUi
import com.example.chatwave.data.model.ScreenDestination
import com.example.chatwave.data.model.UserProfileUi
import com.example.chatwave.di.AppGraph
import com.example.chatwave.webrtc.CallDirection
import com.example.chatwave.webrtc.CallUiState
import com.example.chatwave.webrtc.RealtimeEvent
import com.example.chatwave.webrtc.RemoteCallSession
import com.example.chatwave.webrtc.RemotePublicIdentity
import com.example.chatwave.webrtc.WebRtcCallManager
import com.example.chatwave.webrtc.toSignalPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ChatWaveViewModel(application: Application) : AndroidViewModel(application) {
  private val local = AppGraph.localRepository
  private val backend = AppGraph.backendRepository
  private val identity = AppGraph.identityKeyManager
  private val pinManager = AppGraph.pinManager
  private val sessionManager = AppGraph.sessionManager
  private val recoveryManager = AppGraph.recoveryManager
  private val callManager: WebRtcCallManager = AppGraph.webRtcCallManager
  private val signaling = AppGraph.signalingClient
  private val idGenerator = ChatWaveIdGenerator()


  private val _screen = MutableStateFlow(ScreenDestination.SPLASH)
  val screen: StateFlow<ScreenDestination> = _screen.asStateFlow()
  private val _loading = MutableStateFlow(false)
  val loading: StateFlow<Boolean> = _loading.asStateFlow()
  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()
  private val _notice = MutableStateFlow<String?>(null)
  val notice: StateFlow<String?> = _notice.asStateFlow()
  private val _incomingCall = MutableStateFlow<RealtimeEvent.IncomingCall?>(null)
  val incomingCall: StateFlow<RealtimeEvent.IncomingCall?> = _incomingCall.asStateFlow()

  val profile = local.profile.map { it?.toUi() ?: UserProfileUi(backendUrl = AppGraph.backendApi.baseUrl) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfileUi(backendUrl = AppGraph.backendApi.baseUrl))
  val contacts = local.contacts.map { list -> list.map { it.toUi() } }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<ContactUi>())
  val invites = local.invitations.map { list -> list.sortedByDescending { it.createdAt }.map { it.toUi() } }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<InviteUi>())
  val history = local.callHistory.map { list -> list.sortedByDescending { it.startedAt }.map { it.toUi() } }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<CallHistoryUi>())
  val settings = local.settings.map { it ?: SecuritySettingsEntity() }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SecuritySettingsEntity())
  val callState = callManager.state.map { it?.toUi() ?: CallUiStateModel() }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CallUiStateModel())
  val diagnostics = combine(settings, signaling.connected, profile, error) { s, connected, p, err ->
    DiagnosticsUi(AppGraph.backendApi.baseUrl, connected, AppGraph.backendApi.baseUrl.isNotBlank(), s.relayOnly, err, p.fingerprint, p.publicKeyVersion)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DiagnosticsUi())

  init {
    viewModelScope.launch {
      val p = local.getProfile()
      _screen.value = when {
        p == null -> ScreenDestination.FIRST_LAUNCH
        pinManager.isLocked() || sessionManager.isExpired() -> ScreenDestination.UNLOCK
        else -> ScreenDestination.HOME
      }
      if (p != null && !sessionManager.isExpired()) signaling.connect()
    }
    viewModelScope.launch {
      signaling.events.collect { event ->
        when (event) {
          is RealtimeEvent.IncomingCall -> handleIncomingCall(event)
          is RealtimeEvent.Signal -> handleSignal(event)
          is RealtimeEvent.Error -> _error.value = event.message
          else -> {}
        }
      }
    }
    viewModelScope.launch {
      CallCommandBus.commands.collect { cmd ->
        when (cmd) {
          is CallCommand.Accept -> acceptIncomingCall(cmd.sessionId)
          is CallCommand.Decline -> declineIncomingCall(cmd.sessionId)
          is CallCommand.End -> endCall(cmd.sessionId)
        }
      }
    }
    viewModelScope.launch {
      callManager.state.collect { s ->
        if (s == null) return@collect
        when (s.state) {
          CallUiState.CONNECTED -> CallForegroundService.ongoing(getApplication(), s.sessionId, s.peerName, s.isVideo)
          CallUiState.ENDED, CallUiState.FAILED, CallUiState.IDLE -> CallForegroundService.stop(getApplication())
          else -> {}
        }
        if (s.state == CallUiState.ENDED || s.state == CallUiState.FAILED) {
          val existing = local.callHistory.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList()).value.find { it.id == s.sessionId }
          if (existing != null && existing.endedAt == null) local.finishCallRecord(s.sessionId, s.state.name.lowercase(), s.durationSeconds)
        }
      }
    }
  }

  fun clearError() { _error.value = null }
  fun clearNotice() { _notice.value = null }
  fun navigate(screen: ScreenDestination) { _screen.value = screen }

  fun createAccount(displayName: String, pin: String, backendUrl: String) = viewModelScope.launch {
    _loading.value = true; _error.value = null
    runCatching {
      val trimmed = displayName.trim()
      require(trimmed.length in 2..40) { "Display name must be 2-40 characters" }
      val baseUrl = backendUrl.trim()
      require(baseUrl.isNotBlank()) { "Backend URL is required" }
      AppGraph.backendApi.updateBaseUrl(baseUrl)
      val recoveryToken = recoveryManager.generateToken()
      val pk = identity.initialize(1, false)
      val now = System.currentTimeMillis()
      val profile = UserProfileEntity(
        chatwaveId = idGenerator.generate(),
        displayName = trimmed,
        publicKey = android.util.Base64.encodeToString(pk.der, android.util.Base64.NO_WRAP),
        publicKeyVersion = pk.version,
        fingerprint = pk.fingerprint,
        recoveryHash = recoveryManager.hash(recoveryToken),
        deviceId = UUID.randomUUID().toString(),
        createdAt = now,
        updatedAt = now
      )
      pinManager.createPin(pin)
      local.saveProfile(profile)
      local.getSettings()
      val remote = backend.register(profile)
      local.saveProfile(profile.copy(
        fingerprint = remote.fingerprint.ifBlank { pk.fingerprint },
        registeredRemotely = true,
        updatedAt = System.currentTimeMillis()
      ))
      sessionManager.unlock()
      signaling.connect()
      _notice.value = "Recovery token (save offline): ${recoveryManager.formatForDisplay(recoveryToken)}"
      _screen.value = ScreenDestination.HOME
    }.onFailure { _error.value = it.message ?: "create_account_failed" }
    _loading.value = false
  }

  fun unlock(pin: String) = viewModelScope.launch {
    _loading.value = true; _error.value = null
    val result = pinManager.verifyPin(pin)
    if (result.isSuccess) {
      sessionManager.unlock(); signaling.connect(); _screen.value = ScreenDestination.HOME
    } else _error.value = result.exceptionOrNull()?.message ?: "unlock_failed"
    _loading.value = false
  }

  fun lock() { sessionManager.lock(); signaling.disconnect(); _screen.value = ScreenDestination.UNLOCK }

  fun addContact(chatwaveId: String) = viewModelScope.launch {
    val p = local.getProfile() ?: return@launch
    _loading.value = true; _error.value = null
    runCatching {
      val remote = backend.lookup(chatwaveId.trim().uppercase())
      val now = System.currentTimeMillis()
      val safetyCode = SafetyCode.compute(p.chatwaveId, p.fingerprint, remote.chatwaveId, remote.fingerprint)
      local.saveContact(TrustedContactEntity(
        chatwaveId = remote.chatwaveId,
        displayName = remote.displayName,
        publicKey = remote.publicKey,
        publicKeyVersion = remote.publicKeyVersion,
        fingerprint = remote.fingerprint,
        safetyCode = safetyCode,
        verified = false,
        keyChangeDetected = false,
        createdAt = now,
        updatedAt = now
      ))
      _notice.value = "Contact added. Verify safety code before first call."
    }.onFailure { _error.value = it.message ?: "lookup_failed" }
    _loading.value = false
  }

  fun createInvitation() = viewModelScope.launch {
    val p = local.getProfile() ?: return@launch
    _loading.value = true; _error.value = null
    runCatching {
      val created = backend.createInvitation(p)
      local.saveInvitation(com.example.chatwave.data.remote.BackendRepository.toInvitationEntity(p, created))
      _notice.value = "Invite code: ${created.formattedCode}"
    }.onFailure { _error.value = it.message ?: "invite_create_failed" }
    _loading.value = false
  }

  fun redeemInvitation(code: String) = viewModelScope.launch {
    val p = local.getProfile() ?: return@launch
    _loading.value = true; _error.value = null
    runCatching {
      val res = backend.redeemInvitation(code, p)
      val now = System.currentTimeMillis()
      local.saveContact(TrustedContactEntity(
        chatwaveId = res.inviter.chatwaveId,
        displayName = res.inviter.displayName,
        publicKey = res.inviter.publicKey,
        publicKeyVersion = res.inviter.publicKeyVersion,
        fingerprint = res.inviter.fingerprint,
        safetyCode = res.safetyCode,
        verified = false,
        keyChangeDetected = false,
        createdAt = now,
        updatedAt = now
      ))
      _notice.value = "Contact added. Safety code: ${res.safetyCode}"
    }.onFailure { _error.value = it.message ?: "invite_redeem_failed" }
    _loading.value = false
  }

  fun verifyContact(contactId: String) = viewModelScope.launch { local.verifyContact(contactId); _notice.value = "Contact verified" }

  fun startCall(contact: ContactUi, video: Boolean) = viewModelScope.launch {
    val p = local.getProfile() ?: return@launch
    val s = settings.value
    _loading.value = true; _error.value = null
    runCatching {
      val remoteIdentity = backend.lookup(contact.chatwaveId)
      val existing = local.getContact(contact.chatwaveId)
      val fingerprint = remoteIdentity.fingerprint
      if (existing != null && existing.publicKey.isNotBlank() && existing.fingerprint != fingerprint) {
        local.markKeyChange(contact.chatwaveId)
        throw IllegalStateException("identity_key_changed")
      }
      val safetyCode = SafetyCode.compute(p.chatwaveId, p.fingerprint, remoteIdentity.chatwaveId, remoteIdentity.fingerprint)
      val now = System.currentTimeMillis()
      local.saveContact(TrustedContactEntity(
        chatwaveId = remoteIdentity.chatwaveId,
        displayName = remoteIdentity.displayName,
        publicKey = remoteIdentity.publicKey,
        publicKeyVersion = remoteIdentity.publicKeyVersion,
        fingerprint = remoteIdentity.fingerprint,
        safetyCode = safetyCode,
        verified = existing?.verified == true,
        keyChangeDetected = false,
        createdAt = now,
        updatedAt = now
      ))
      val localEphemeral = AppGraph.sessionKeyManager.generateEphemeralKeyPair()
      val session = backend.createCall(contact.chatwaveId, p, s.relayOnly, android.util.Base64.encodeToString(localEphemeral.public.encoded, android.util.Base64.NO_WRAP), p.fingerprint, safetyCode)
      local.addCallRecord(CallRecordEntity(
        id = session.id,
        peerId = contact.chatwaveId,
        peerName = contact.displayName,
        direction = "outgoing",
        state = "ringing",
        startedAt = now,
        endedAt = null,
        durationSeconds = 0,
        video = video,
        safetyCode = safetyCode,
        relayOnly = s.relayOnly
      ))
      callManager.startOutgoing(session, remoteIdentity, p, video, s.relayOnly, safetyCode)
      _screen.value = if (video) ScreenDestination.ACTIVE_VIDEO_CALL else ScreenDestination.ACTIVE_AUDIO_CALL
    }.onFailure { _error.value = it.message ?: "call_failed" }
    _loading.value = false
  }

  private fun handleIncomingCall(event: RealtimeEvent.IncomingCall) {
    viewModelScope.launch {
      val contact = local.getContact(event.caller.chatwaveId)
      if (contact != null && contact.fingerprint.isNotBlank() && contact.fingerprint != event.caller.fingerprint) {
        local.markKeyChange(event.caller.chatwaveId)
        _error.value = "Caller identity key changed. Verify before accepting."
      }
      val now = System.currentTimeMillis()
      val verified = contact?.verified == true && contact.fingerprint == event.caller.fingerprint
      local.saveContact(TrustedContactEntity(
        chatwaveId = event.caller.chatwaveId,
        displayName = event.caller.displayName,
        publicKey = event.caller.publicKey,
        publicKeyVersion = event.caller.publicKeyVersion,
        fingerprint = event.caller.fingerprint,
        safetyCode = event.safetyCode,
        verified = verified,
        keyChangeDetected = false,
        createdAt = now,
        updatedAt = now
      ))
      _incomingCall.value = event
      CallForegroundService.incoming(getApplication(), event.sessionId, event.caller.chatwaveId, event.caller.displayName, false)
      local.addCallRecord(CallRecordEntity(
        id = event.sessionId,
        peerId = event.caller.chatwaveId,
        peerName = event.caller.displayName,
        direction = "incoming",
        state = "ringing",
        startedAt = now,
        endedAt = null,
        durationSeconds = 0,
        video = false,
        safetyCode = event.safetyCode,
        relayOnly = event.relayOnly
      ))
      _screen.value = ScreenDestination.INCOMING_CALL
    }
  }

  fun acceptIncomingCall(sessionId: String? = null) = viewModelScope.launch {
    val event = _incomingCall.value ?: return@launch
    if (sessionId != null && event.sessionId != sessionId) return@launch
    val p = local.getProfile() ?: return@launch
    val s = settings.value
    _loading.value = true; _error.value = null
    runCatching {
      val localEphemeral = AppGraph.sessionKeyManager.generateEphemeralKeyPair()
      val safetyCode = SafetyCode.compute(p.chatwaveId, p.fingerprint, event.caller.chatwaveId, event.caller.fingerprint)
      val session = backend.acceptCall(event.sessionId, event.caller.chatwaveId, p, android.util.Base64.encodeToString(localEphemeral.public.encoded, android.util.Base64.NO_WRAP), p.fingerprint, safetyCode)
      callManager.prepareIncoming(session, event.caller, p, false, s.relayOnly || event.relayOnly, event.safetyCode.ifBlank { safetyCode })
      callManager.acceptIncoming()
      _incomingCall.value = null
      CallForegroundService.ongoing(getApplication(), event.sessionId, event.caller.displayName, false)
      _screen.value = ScreenDestination.ACTIVE_AUDIO_CALL
    }.onFailure { _error.value = it.message ?: "accept_failed" }
    _loading.value = false
  }

  fun declineIncomingCall(sessionId: String? = null) = viewModelScope.launch {
    val event = _incomingCall.value ?: return@launch
    if (sessionId != null && event.sessionId != sessionId) return@launch
    runCatching { callManager.declineCall() }
    _incomingCall.value = null
    CallForegroundService.stop(getApplication())
    _screen.value = ScreenDestination.HOME
  }

  fun endCall(sessionId: String? = null) = viewModelScope.launch {
    val s = callManager.state.value ?: return@launch
    if (sessionId != null && s.sessionId != sessionId) return@launch
    runCatching { callManager.endCall() }
    CallForegroundService.stop(getApplication())
    _screen.value = ScreenDestination.CALL_SUMMARY
  }

  private fun handleSignal(event: RealtimeEvent.Signal) {
    viewModelScope.launch {
      val payload = event.data.toSignalPayload()
      when (event.kind) {
        "offer" -> {
          val isVideo = payload.sdp?.contains("m=video") == true
          callManager.state.value?.let { st -> if (st.isVideo != isVideo) {} }
          runCatching { callManager.onRemoteOffer(event.sessionId, payload) }
          val st = callManager.state.value
          if (st != null) _screen.value = if (st.isVideo) ScreenDestination.ACTIVE_VIDEO_CALL else ScreenDestination.ACTIVE_AUDIO_CALL
        }
        "answer" -> runCatching { callManager.onRemoteAnswer(event.sessionId, payload) }
        "ice" -> callManager.onRemoteIce(event.sessionId, payload)
        "end" -> { callManager.markEnded(); CallForegroundService.stop(getApplication()); _screen.value = ScreenDestination.CALL_SUMMARY }
        "decline" -> { callManager.markEnded(); _incomingCall.value = null; CallForegroundService.stop(getApplication()); _screen.value = ScreenDestination.HOME }
        "busy" -> { callManager.fail("Peer busy"); CallForegroundService.stop(getApplication()) }
        "restart" -> if (payload.type == "offer") runCatching { callManager.onRemoteOffer(event.sessionId, payload) }
      }
    }
  }

  fun toggleMute() = callManager.toggleMute()
  fun toggleSpeaker() = callManager.toggleSpeaker()
  fun toggleCamera() = callManager.toggleCamera()
  fun switchCamera() = callManager.switchCamera()

  fun updateSettings(mutator: (SecuritySettingsEntity) -> SecuritySettingsEntity) = viewModelScope.launch {
    local.saveSettings(mutator(local.getSettings()))
  }

  fun saveBackendUrl(url: String) {
    val trimmed = url.trim()
    if (trimmed.isBlank()) {
      _error.value = "Backend URL is required"
      return
    }
    AppGraph.backendApi.updateBaseUrl(trimmed)
    _notice.value = "Backend URL updated"
  }

  fun setRelayOnly(enabled: Boolean) = updateSettings { it.copy(relayOnly = enabled) }
  fun wipeLocalData() = viewModelScope.launch {
    runCatching { signaling.disconnect(); callManager.clear(); AppGraph.database.clearAllTables(); pinManager.clear(); sessionManager.lock(); _screen.value = ScreenDestination.FIRST_LAUNCH }
      .onFailure { _error.value = it.message ?: "wipe_failed" }
  }

  private fun UserProfileEntity.toUi() = UserProfileUi(chatwaveId, displayName, fingerprint, publicKeyVersion, deviceId, AppGraph.backendApi.baseUrl, biometricEnabled)
  private fun TrustedContactEntity.toUi() = ContactUi(chatwaveId, displayName, fingerprint, verified, updatedAt)
  private fun InvitationEntity.toUi() = InviteUi(codeHash, inviterId, inviterId, maskedCode, createdAt, status)
  private fun CallRecordEntity.toUi() = CallHistoryUi(id, peerId, peerName, direction, if (video) "video" else "audio", state, startedAt, durationSeconds)
  private fun com.example.chatwave.webrtc.CallSessionState.toUi() = CallUiStateModel(sessionId, peerId, peerName, if (isVideo) "video" else "audio", direction.name.lowercase(), state.name.lowercase(), durationSeconds, isMuted, isSpeakerEnabled, isCameraEnabled, relayOnly, safetyCode, peerFingerprint, error)
}
