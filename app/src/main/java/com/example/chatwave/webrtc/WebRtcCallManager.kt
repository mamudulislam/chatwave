package com.example.chatwave.webrtc

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Base64
import android.util.Log
import com.example.chatwave.data.local.UserProfileEntity
import com.example.chatwave.data.remote.BackendRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.webrtc.*
import java.security.MessageDigest
import java.security.PrivateKey
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class WebRtcCallManager(
  private val context: Context,
  private val backendRepository: BackendRepository,
  private val signalingClient: SignalingClient
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val eglBase: EglBase = EglBase.create()
  private val factory: PeerConnectionFactory
  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  private var audioFocusRequest: AudioFocusRequest? = null

  private var pc: PeerConnection? = null
  private var localAudioSource: AudioSource? = null
  private var localVideoSource: VideoSource? = null
  private var localAudioTrack: AudioTrack? = null
  private var localVideoTrack: VideoTrack? = null
  private var capturer: CameraVideoCapturer? = null
  private var surfaceTextureHelper: SurfaceTextureHelper? = null
  private var localRenderer: SurfaceViewRenderer? = null
  private var remoteRenderer: SurfaceViewRenderer? = null
  private val pendingRemoteIce = ConcurrentLinkedQueue<IceCandidate>()
  private var remoteDescriptionSet = false
  private var profile: UserProfileEntity? = null
  private var timerJob: Job? = null
  private var isOfferer = false
  private var reconnectAttempts = 0

  private val _state = MutableStateFlow<CallSessionState?>(null)
  val state: StateFlow<CallSessionState?> = _state.asStateFlow()

  init {
    PeerConnectionFactory.initialize(
      PeerConnectionFactory.InitializationOptions.builder(context)
        .setEnableInternalTracer(false)
        .createInitializationOptions()
    )
    factory = PeerConnectionFactory.builder()
      .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
      .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
      .setOptions(PeerConnectionFactory.Options())
      .createPeerConnectionFactory()
  }

  fun attachLocalRenderer(renderer: SurfaceViewRenderer) {
    localRenderer = renderer
    initRenderer(renderer)
    localVideoTrack?.addSink(renderer)
  }

  fun attachRemoteRenderer(renderer: SurfaceViewRenderer) {
    remoteRenderer = renderer
    initRenderer(renderer)
  }

  fun detachRenderers() {
    localVideoTrack?.removeSink(localRenderer)
    releaseRenderer(localRenderer)
    releaseRenderer(remoteRenderer)
    localRenderer = null
    remoteRenderer = null
  }

  private fun initRenderer(renderer: SurfaceViewRenderer) {
    runCatching {
      renderer.init(eglBase.eglBaseContext, null)
      renderer.setMirror(false)
      renderer.setEnableHardwareScaler(true)
      renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
    }
  }

  private fun releaseRenderer(renderer: SurfaceViewRenderer?) {
    runCatching { renderer?.clearImage() }
    runCatching { renderer?.release() }
  }

  suspend fun startOutgoing(session: RemoteCallSession, peer: RemotePublicIdentity, currentProfile: UserProfileEntity, video: Boolean, relayOnly: Boolean, safetyCode: String) {
    cleanupCall(emitIdle = false)
    profile = currentProfile
    isOfferer = true
    reconnectAttempts = 0
    _state.value = CallSessionState(session.id, peer.chatwaveId, peer.displayName, "#075E54", CallDirection.OUTGOING, video, relayOnly, CallUiState.RINGING_OUT, safetyCode = safetyCode, peerFingerprint = peer.fingerprint)
    val creds = runCatching { backendRepository.turnCredentials(currentProfile) }.getOrNull()
    createPeer(relayOnly, creds)
    createLocalMedia(video)
    val offer = createOffer()
    setLocalDescription(offer)
    sendSignal(session.id, "offer", offer.toPayload().toSignalMap())
    updateState { it.copy(state = CallUiState.CONNECTING) }
  }

  suspend fun prepareIncoming(session: RemoteCallSession, caller: RemotePublicIdentity, currentProfile: UserProfileEntity, video: Boolean, relayOnly: Boolean, safetyCode: String) {
    cleanupCall(emitIdle = false)
    profile = currentProfile
    isOfferer = false
    reconnectAttempts = 0
    _state.value = CallSessionState(session.id, caller.chatwaveId, caller.displayName, "#075E54", CallDirection.INCOMING, video, relayOnly, CallUiState.RINGING_IN, safetyCode = safetyCode, peerFingerprint = caller.fingerprint)
  }

  suspend fun acceptIncoming() {
    val s = _state.value ?: return
    val currentProfile = profile ?: return
    if (pc != null) return
    val creds = runCatching { backendRepository.turnCredentials(currentProfile) }.getOrNull()
    createPeer(s.relayOnly, creds)
    createLocalMedia(s.isVideo)
    updateState { it.copy(state = CallUiState.CONNECTING) }
  }

  suspend fun onRemoteOffer(sessionId: String, payload: WebRtcSignalPayload) {
    val s = _state.value ?: return
    if (s.sessionId != sessionId) return
    if (pc == null) acceptIncoming()
    setRemoteDescription(payload.toSessionDescription())
    val answer = createAnswer()
    setLocalDescription(answer)
    sendSignal(sessionId, "answer", answer.toPayload().toSignalMap())
    flushPendingIce()
  }

  suspend fun onRemoteAnswer(sessionId: String, payload: WebRtcSignalPayload) {
    val s = _state.value ?: return
    if (s.sessionId != sessionId) return
    setRemoteDescription(payload.toSessionDescription())
    flushPendingIce()
  }

  fun onRemoteIce(sessionId: String, payload: WebRtcSignalPayload) {
    val s = _state.value ?: return
    if (s.sessionId != sessionId) return
    val candidate = payload.toIceCandidate()
    if (remoteDescriptionSet && pc != null) pc?.addIceCandidate(candidate) else pendingRemoteIce.add(candidate)
  }

  fun setPeerEphemeralFingerprint(der: ByteArray?) {
    val fp = der?.let { fingerprint(it) }.orEmpty()
    updateState { it.copy(peerEphemeralFingerprint = fp) }
  }

  fun toggleMute() {
    val track = localAudioTrack ?: return
    val enabled = !track.enabled()
    track.setEnabled(enabled)
    updateState { it.copy(isMuted = !enabled) }
  }

  fun toggleCamera() {
    val track = localVideoTrack ?: return
    val enabled = !track.enabled()
    track.setEnabled(enabled)
    updateState { it.copy(isCameraEnabled = enabled) }
  }

  fun switchCamera() {
    (capturer as? CameraVideoCapturer)?.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
      override fun onCameraSwitchDone(front: Boolean) { updateState { it.copy(isFrontCamera = front) } }
      override fun onCameraSwitchError(err: String?) { Log.w(TAG, "camera switch error: $err") }
    })
  }

  fun setSpeaker(enabled: Boolean) {
    audioManager.isSpeakerphoneOn = enabled
    updateState { it.copy(isSpeakerEnabled = enabled) }
  }

  fun toggleSpeaker() { setSpeaker(!(_state.value?.isSpeakerEnabled ?: false)) }

  suspend fun endCall(reason: String = "ended") {
    val s = _state.value ?: return
    val currentProfile = profile
    runCatching {
      if (currentProfile != null) {
        backendRepository.sendSignal(s.sessionId, "end", mapOf("reason" to reason), currentProfile)
        backendRepository.endCall(s.sessionId, currentProfile)
      }
    }
    cleanupCall(emitIdle = true, finalState = CallUiState.ENDED)
  }

  suspend fun declineCall() {
    val s = _state.value ?: return
    val currentProfile = profile ?: return
    runCatching { backendRepository.declineCall(s.sessionId, currentProfile) }
    cleanupCall(emitIdle = true, finalState = CallUiState.ENDED)
  }

  fun fail(error: String) { cleanupCall(emitIdle = true, finalState = CallUiState.FAILED, error = error) }
  fun markEnded() { cleanupCall(emitIdle = true, finalState = CallUiState.ENDED) }
  fun clear() { cleanupCall(emitIdle = true, finalState = CallUiState.IDLE) }

  private fun createPeer(relayOnly: Boolean, turn: TurnCredentials?) {
    val iceServers = buildIceServers(relayOnly, turn)
    val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
      sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
      continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
      iceTransportsType = if (relayOnly) PeerConnection.IceTransportsType.RELAY else PeerConnection.IceTransportsType.ALL
    }
    pc = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
      override fun onIceCandidate(candidate: IceCandidate) {
        val s = _state.value ?: return
        scope.launch { runCatching { sendSignal(s.sessionId, "ice", candidate.toPayload().toSignalMap()) } }
      }
      override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
        updateState { it.copy(connectionState = state) }
        when (state) {
          PeerConnection.IceConnectionState.CONNECTED, PeerConnection.IceConnectionState.COMPLETED -> {
            reconnectAttempts = 0
            startTimer()
            updateState { it.copy(state = CallUiState.CONNECTED, error = null) }
            val s = _state.value
            val p = profile
            if (s != null && p != null) scope.launch { runCatching { backendRepository.sendSignal(s.sessionId, "active", mapOf("at" to System.currentTimeMillis()), p) } }
          }
          PeerConnection.IceConnectionState.DISCONNECTED -> updateState { it.copy(state = CallUiState.RECONNECTING) }
          PeerConnection.IceConnectionState.FAILED -> attemptReconnect()
          else -> {}
        }
      }
      override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
        val track = receiver?.track() ?: return
        if (track is VideoTrack) scope.launch { remoteRenderer?.let { track.addSink(it) } }
      }
      override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
      override fun onIceConnectionReceivingChange(p0: Boolean) {}
      override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
      override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
      override fun onAddStream(p0: MediaStream?) {}
      override fun onRemoveStream(p0: MediaStream?) {}
      override fun onDataChannel(p0: DataChannel?) {}
      override fun onRenegotiationNeeded() {}
      override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
        if (newState == PeerConnection.PeerConnectionState.FAILED) attemptReconnect()
      }
    })
  }

  private fun buildIceServers(relayOnly: Boolean, turn: TurnCredentials?): List<PeerConnection.IceServer> {
    val list = mutableListOf<PeerConnection.IceServer>()
    if (turn != null) {
      turn.urls.forEach { url ->
        val isTurn = url.startsWith("turn:", true) || url.startsWith("turns:", true)
        if (relayOnly && !isTurn) return@forEach
        val b = PeerConnection.IceServer.builder(url)
        if (isTurn) { b.setUsername(turn.username); b.setPassword(turn.credential) }
        list.add(b.createIceServer())
      }
    }
    if (!relayOnly) list.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
    return list
  }

  private fun createLocalMedia(video: Boolean) {
    localAudioSource = factory.createAudioSource(MediaConstraints())
    localAudioTrack = factory.createAudioTrack("AUDIO", localAudioSource).apply { setEnabled(true) }
    pc?.addTrack(localAudioTrack, listOf("ARDAMS"))

    if (video) {
      val camEnum = Camera2Enumerator(context)
      val deviceName = camEnum.deviceNames.firstOrNull { camEnum.isFrontFacing(it) } ?: camEnum.deviceNames.firstOrNull()
      if (deviceName != null) {
        capturer = camEnum.createCapturer(deviceName, null)
        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        localVideoSource = factory.createVideoSource(capturer!!.isScreencast)
        capturer!!.initialize(surfaceTextureHelper, context, localVideoSource!!.capturerObserver)
        capturer!!.startCapture(1280, 720, 30)
        localVideoTrack = factory.createVideoTrack("VIDEO", localVideoSource).apply { setEnabled(true) }
        localRenderer?.let { localVideoTrack?.addSink(it) }
        pc?.addTrack(localVideoTrack, listOf("ARDAMS"))
      }
    }
    requestAudioFocus()
  }

  private suspend fun createOffer(): SessionDescription = suspendCoroutine { cont ->
    pc?.createOffer(object : SimpleSdpObserver() {
      override fun onCreateSuccess(sdp: SessionDescription) { cont.resume(sdp) }
      override fun onCreateFailure(error: String?) { cont.resumeWithException(RuntimeException(error ?: "createOffer failed")) }
    }, MediaConstraints().apply {
      mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
      mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
    }) ?: cont.resumeWithException(IllegalStateException("pc null"))
  }

  private suspend fun createAnswer(): SessionDescription = suspendCoroutine { cont ->
    pc?.createAnswer(object : SimpleSdpObserver() {
      override fun onCreateSuccess(sdp: SessionDescription) { cont.resume(sdp) }
      override fun onCreateFailure(error: String?) { cont.resumeWithException(RuntimeException(error ?: "createAnswer failed")) }
    }, MediaConstraints().apply {
      mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
      mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
    }) ?: cont.resumeWithException(IllegalStateException("pc null"))
  }

  private suspend fun setLocalDescription(sdp: SessionDescription) = suspendCoroutine<Unit> { cont ->
    pc?.setLocalDescription(object : SimpleSdpObserver() {
      override fun onSetSuccess() { cont.resume(Unit) }
      override fun onSetFailure(error: String?) { cont.resumeWithException(RuntimeException(error ?: "setLocalDescription failed")) }
    }, sdp) ?: cont.resumeWithException(IllegalStateException("pc null"))
  }

  private suspend fun setRemoteDescription(sdp: SessionDescription) = suspendCoroutine<Unit> { cont ->
    pc?.setRemoteDescription(object : SimpleSdpObserver() {
      override fun onSetSuccess() { remoteDescriptionSet = true; cont.resume(Unit) }
      override fun onSetFailure(error: String?) { cont.resumeWithException(RuntimeException(error ?: "setRemoteDescription failed")) }
    }, sdp) ?: cont.resumeWithException(IllegalStateException("pc null"))
  }

  private fun flushPendingIce() { while (true) { val c = pendingRemoteIce.poll() ?: break; pc?.addIceCandidate(c) } }

  private fun sendSignal(sessionId: String, kind: String, data: Map<String, Any?>) {
    val p = profile ?: return
    val env = backendRepository.createSignalEnvelope(sessionId, kind, data, p)
    val sent = signalingClient.sendSignal(sessionId, env)
    if (!sent) scope.launch { runCatching { backendRepository.sendSignal(sessionId, kind, data, p) } }
  }

  private fun attemptReconnect() {
    val s = _state.value ?: return
    if (reconnectAttempts >= 3) { fail("connection_failed"); return }
    reconnectAttempts++
    updateState { it.copy(state = CallUiState.RECONNECTING) }
    scope.launch {
      delay(1500L * reconnectAttempts)
      runCatching {
        if (isOfferer) {
          pc?.restartIce()
          val offer = createOffer()
          setLocalDescription(offer)
          sendSignal(s.sessionId, "restart", offer.toPayload().toSignalMap())
        }
      }.onFailure { Log.w(TAG, "reconnect failed", it) }
    }
  }

  private fun startTimer() {
    if (timerJob?.isActive == true) return
    val start = System.currentTimeMillis()
    timerJob = scope.launch { while (true) { delay(1000); updateState { it.copy(durationSeconds = (System.currentTimeMillis() - start) / 1000) } } }
  }

  private fun requestAudioFocus() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        .build()
      audioManager.requestAudioFocus(audioFocusRequest!!)
    } else {
      @Suppress("DEPRECATION") audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
    }
    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
  }

  private fun abandonAudioFocus() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
    else @Suppress("DEPRECATION") audioManager.abandonAudioFocus(null)
    audioManager.mode = AudioManager.MODE_NORMAL
  }

  private fun cleanupCall(emitIdle: Boolean, finalState: CallUiState = CallUiState.ENDED, error: String? = null) {
    timerJob?.cancel(); timerJob = null
    runCatching { capturer?.stopCapture() }; runCatching { capturer?.dispose() }; capturer = null
    localVideoTrack?.removeSink(localRenderer)
    runCatching { localAudioTrack?.dispose() }; runCatching { localVideoTrack?.dispose() }
    runCatching { localAudioSource?.dispose() }; runCatching { localVideoSource?.dispose() }
    runCatching { surfaceTextureHelper?.dispose() }
    localAudioTrack = null; localVideoTrack = null; localAudioSource = null; localVideoSource = null; surfaceTextureHelper = null
    runCatching { pc?.close() }; runCatching { pc?.dispose() }; pc = null
    pendingRemoteIce.clear(); remoteDescriptionSet = false
    abandonAudioFocus()
    if (emitIdle) {
      val old = _state.value
      _state.value = old?.copy(state = finalState, error = error)
    }
  }

  fun release() { cleanupCall(false); detachRenderers(); runCatching { eglBase.release() }; runCatching { factory.dispose() } }

  private fun WebRtcSignalPayload.toSignalMap(): Map<String, Any?> = mapOf(
    "type" to type, "sdp" to sdp, "sdpMid" to sdpMid, "sdpMLineIndex" to sdpMLineIndex, "candidate" to candidate, "reason" to reason
  ).filterValues { it != null }

  private fun fingerprint(der: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(der).joinToString(":") { "%02X".format(it) }

  private inline fun updateState(fn: (CallSessionState) -> CallSessionState) { _state.value = _state.value?.let(fn) }

  companion object { private const val TAG = "WebRtcCallManager" }
}

private open class SimpleSdpObserver : SdpObserver {
  override fun onCreateSuccess(sdp: SessionDescription) {}
  override fun onSetSuccess() {}
  override fun onCreateFailure(error: String?) {}
  override fun onSetFailure(error: String?) {}
}
