package com.example.chatwave.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class PinState(
  val salt: String,
  val verifier: String,
  val wrappedMasterKey: String,
  val iterations: Int,
  val failedAttempts: Int = 0,
  val lastFailureAt: Long = 0L,
  val lockedUntil: Long = 0L
)

interface PinStateStore {
  fun load(): PinState?
  fun save(state: PinState)
  fun clear()
}

class EncryptedPinStateStore(
  context: Context,
  private val keyStoreManager: KeyStoreManager
) : PinStateStore {
  private val preferences: SharedPreferences =
    context.applicationContext.getSharedPreferences("chatwave_pin", Context.MODE_PRIVATE)

  override fun load(): PinState? {
    val encoded = preferences.getString(KEY_PIN_STATE, null) ?: return null
    return try {
      val json = JSONObject(keyStoreManager.decryptString(ALIAS, encoded, AAD))
      PinState(
        salt = json.getString("salt"),
        verifier = json.getString("verifier"),
        wrappedMasterKey = json.getString("wrappedMasterKey"),
        iterations = json.getInt("iterations"),
        failedAttempts = json.optInt("failedAttempts", 0),
        lastFailureAt = json.optLong("lastFailureAt", 0L),
        lockedUntil = json.optLong("lockedUntil", 0L)
      )
    } catch (_: Exception) {
      null
    }
  }

  override fun save(state: PinState) {
    val json = JSONObject().apply {
      put("salt", state.salt)
      put("verifier", state.verifier)
      put("wrappedMasterKey", state.wrappedMasterKey)
      put("iterations", state.iterations)
      put("failedAttempts", state.failedAttempts)
      put("lastFailureAt", state.lastFailureAt)
      put("lockedUntil", state.lockedUntil)
    }
    preferences.edit()
      .putString(KEY_PIN_STATE, keyStoreManager.encryptString(ALIAS, json.toString(), AAD))
      .apply()
  }

  override fun clear() = preferences.edit().remove(KEY_PIN_STATE).apply()

  private companion object {
    const val KEY_PIN_STATE = "state"
    const val ALIAS = "cw_pin_state_v1"
    val AAD = "chatwave-pin-state-v1".toByteArray(Charsets.UTF_8)
  }
}

class PinManager(
  private val store: PinStateStore,
  private val secureRandom: SecureRandom = SecureRandom(),
  private val now: () -> Long = System::currentTimeMillis,
  private val iterations: Int = 210_000
) {
  fun hasPin(): Boolean = store.load() != null

  fun createPin(pin: String): SecretKeySpec {
    requireValidPin(pin)
    val salt = ByteArray(16).also(secureRandom::nextBytes)
    val masterKey = ByteArray(32).also(secureRandom::nextBytes)
    val kek = deriveKek(pin, salt, iterations)
    val wrapped = wrap(masterKey, kek)
    val verifier = hmac(masterKey, VERIFIER_LABEL)
    store.save(
      PinState(
        salt = encode(salt),
        verifier = encode(verifier),
        wrappedMasterKey = encode(wrapped),
        iterations = iterations
      )
    )
    return SecretKeySpec(masterKey, "AES")
  }

  fun verifyPin(pin: String): Result<SecretKeySpec> {
    val state = store.load() ?: return Result.failure(IllegalStateException("PIN is not configured"))
    if (isLocked(state)) return Result.failure(IllegalStateException("PIN is locked"))
    val kek = deriveKek(pin, decode(state.salt), state.iterations)
    val masterKey = try {
      unwrap(decode(state.wrappedMasterKey), kek)
    } catch (_: Exception) {
      recordFailure(state)
      return Result.failure(IllegalArgumentException("Invalid PIN"))
    }
    val verifier = hmac(masterKey, VERIFIER_LABEL)
    if (!MessageDigest.isEqual(verifier, decode(state.verifier))) {
      recordFailure(state)
      return Result.failure(IllegalArgumentException("Invalid PIN"))
    }
    store.save(state.copy(failedAttempts = 0, lastFailureAt = 0L, lockedUntil = 0L))
    return Result.success(SecretKeySpec(masterKey, "AES"))
  }

  fun isLocked(): Boolean = store.load()?.let(::isLocked) ?: false

  fun isLocked(state: PinState): Boolean {
    if (state.lockedUntil > now()) return true
    return state.failedAttempts >= MAX_ATTEMPTS
  }

  fun remainingDelaySeconds(): Long = store.load()?.let(::remainingDelaySeconds) ?: 0L

  fun remainingDelaySeconds(state: PinState): Long {
    val lockedFor = state.lockedUntil - now()
    return if (lockedFor > 0) TimeUnit.MILLISECONDS.toSeconds(lockedFor) + 1 else 0L
  }

  fun clear() = store.clear()

  private fun recordFailure(state: PinState) {
    val attempts = state.failedAttempts + 1
    val delaySeconds = when {
      attempts >= MAX_ATTEMPTS -> LOCKOUT_SECONDS
      attempts < 4 -> 1L shl (attempts - 1)
      else -> 30L
    }
    store.save(
      state.copy(
        failedAttempts = attempts,
        lastFailureAt = now(),
        lockedUntil = now() + TimeUnit.SECONDS.toMillis(delaySeconds)
      )
    )
  }

  private fun requireValidPin(pin: String) {
    require(pin.length in 4..12 && pin.all(Char::isDigit)) { "PIN must contain 4 to 12 digits" }
  }

  private fun deriveKek(pin: String, salt: ByteArray, iterations: Int): SecretKeySpec {
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSha256")
    val key = factory.generateSecret(PBEKeySpec(pin.toCharArray(), salt, iterations, 256))
    return SecretKeySpec(key.encoded, "AES")
  }

  private fun wrap(key: ByteArray, kek: SecretKeySpec): ByteArray {
    val cipher = Cipher.getInstance(AES_TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, kek)
    cipher.updateAAD(AAD)
    val iv = cipher.iv
    val encrypted = cipher.doFinal(key)
    return byteArrayOf(iv.size.toByte()) + iv + encrypted
  }

  private fun unwrap(record: ByteArray, kek: SecretKeySpec): ByteArray {
    val ivLength = record[0].toInt()
    require(ivLength == 12) { "Invalid IV length" }
    val cipher = Cipher.getInstance(AES_TRANSFORMATION)
    cipher.init(Cipher.DECRYPT_MODE, kek, GCMParameterSpec(128, record.copyOfRange(1, 1 + ivLength)))
    cipher.updateAAD(AAD)
    return cipher.doFinal(record.copyOfRange(1 + ivLength, record.size))
  }

  private fun hmac(key: ByteArray, label: String): ByteArray {
    val mac = javax.crypto.Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(key, "HmacSHA256"))
    return mac.doFinal(label.toByteArray(Charsets.UTF_8))
  }

  private fun encode(value: ByteArray): String = Base64.encodeToString(value, Base64.NO_WRAP)

  private fun decode(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)

  private companion object {
    const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
    const val MAX_ATTEMPTS = 10
    const val LOCKOUT_SECONDS = 300L
    val AAD = "chatwave-pin-wrap-v1".toByteArray(Charsets.UTF_8)
    const val VERIFIER_LABEL = "chatwave-pin-verifier-v1"
  }
}
