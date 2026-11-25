package com.example.chatwave.di

import android.content.Context
import com.example.BuildConfig
import com.example.chatwave.crypto.CanonicalJson
import com.example.chatwave.crypto.InvitationCodeGenerator
import com.example.chatwave.crypto.SessionKeyManager
import com.example.chatwave.crypto.SignatureManager
import com.example.chatwave.data.local.AppDatabase
import com.example.chatwave.data.remote.BackendApi
import com.example.chatwave.data.remote.BackendRepository
import com.example.chatwave.data.repository.LocalRepository
import com.example.chatwave.security.BiometricAuthenticator
import com.example.chatwave.security.EncryptedPinStateStore
import com.example.chatwave.security.IdentityKeyManager
import com.example.chatwave.security.KeyStoreManager
import com.example.chatwave.security.PinManager
import com.example.chatwave.security.RecoveryManager
import com.example.chatwave.security.SessionManager
import com.example.chatwave.webrtc.SignalingClient
import com.example.chatwave.webrtc.WebRtcCallManager

object AppGraph {
  @Volatile private var initialized = false
  lateinit var appContext: Context; private set
  lateinit var database: AppDatabase; private set
  lateinit var localRepository: LocalRepository; private set
  lateinit var keyStoreManager: KeyStoreManager; private set
  lateinit var identityKeyManager: IdentityKeyManager; private set
  lateinit var pinManager: PinManager; private set
  lateinit var sessionManager: SessionManager; private set
  lateinit var recoveryManager: RecoveryManager; private set
  lateinit var biometricAuthenticator: BiometricAuthenticator; private set
  lateinit var backendApi: BackendApi; private set
  lateinit var backendRepository: BackendRepository; private set
  lateinit var signalingClient: SignalingClient; private set
  lateinit var sessionKeyManager: SessionKeyManager; private set
  lateinit var webRtcCallManager: WebRtcCallManager; private set

  fun init(context: Context) {
    if (initialized) return
    synchronized(this) {
      if (initialized) return
      appContext = context.applicationContext
      database = AppDatabase.get(appContext)
      localRepository = LocalRepository(database)
      keyStoreManager = KeyStoreManager(appContext)
      identityKeyManager = IdentityKeyManager(keyStoreManager, SignatureManager(CanonicalJson))
      pinManager = PinManager(EncryptedPinStateStore(appContext, keyStoreManager))
      sessionManager = SessionManager(appContext, keyStoreManager)
      recoveryManager = RecoveryManager()
      biometricAuthenticator = BiometricAuthenticator(appContext)
      backendApi = BackendApi(BuildConfig.BACKEND_BASE_URL)
      backendRepository = BackendRepository(backendApi, identityKeyManager, InvitationCodeGenerator())
      sessionKeyManager = SessionKeyManager()
      signalingClient = SignalingClient(
        baseUrlProvider = { backendApi.baseUrl },
        identity = {
          val profile = kotlinx.coroutines.runBlocking { localRepository.getProfile() }
          profile?.let { Triple(it.chatwaveId, it.publicKeyVersion, it.deviceId) }
        },
        identityKeyManager = identityKeyManager
      )
      webRtcCallManager = WebRtcCallManager(appContext, backendRepository, signalingClient)
      initialized = true
    }
  }
}
