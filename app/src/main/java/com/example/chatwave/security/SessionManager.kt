package com.example.chatwave.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(
  context: Context,
  private val keyStoreManager: KeyStoreManager,
  private val timeoutMs: Long = 300_000L,
  private val now: () -> Long = System::currentTimeMillis
) {
  private val preferences: SharedPreferences =
    context.applicationContext.getSharedPreferences("chatwave_session", Context.MODE_PRIVATE)
  private val _isUnlocked = MutableStateFlow(false)
  val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

  init {
    _isUnlocked.value = lastUnlockAt() > 0L && now() - lastUnlockAt() <= timeoutMs
  }

  fun unlock() {
    preferences.edit().putString(KEY_LAST_UNLOCK, keyStoreManager.encryptString(ALIAS, now().toString(), AAD)).apply()
    _isUnlocked.value = true
  }

  fun touch() {
    if (_isUnlocked.value) unlock()
  }

  fun lock() {
    preferences.edit().remove(KEY_LAST_UNLOCK).apply()
    _isUnlocked.value = false
  }

  fun checkExpiration(): Boolean {
    if (!_isUnlocked.value) return false
    val valid = now() - lastUnlockAt() <= timeoutMs
    if (!valid) lock()
    return !valid
  }

  fun isExpired(): Boolean = lastUnlockAt() <= 0L || now() - lastUnlockAt() > timeoutMs

  private fun lastUnlockAt(): Long = try {
    preferences.getString(KEY_LAST_UNLOCK, null)?.let {
      keyStoreManager.decryptString(ALIAS, it, AAD).toLongOrNull()
    } ?: 0L
  } catch (_: Exception) {
    0L
  }

  private companion object {
    const val KEY_LAST_UNLOCK = "last_unlock"
    const val ALIAS = "cw_session_v1"
    val AAD = "chatwave-session-v1".toByteArray(Charsets.UTF_8)
  }
}
