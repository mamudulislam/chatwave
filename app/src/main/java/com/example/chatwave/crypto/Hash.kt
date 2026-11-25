package com.example.chatwave.crypto

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

object Hash {
  fun sha256(input: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(input)

  fun sha256Hex(input: String): String = sha256(input.toByteArray(Charsets.UTF_8)).toHex()

  fun sha256Hex(input: ByteArray): String = sha256(input).toHex()

  fun randomToken(bytes: Int = 32, random: SecureRandom = SecureRandom()): String {
    val value = ByteArray(bytes)
    random.nextBytes(value)
    return Base64.encodeToString(value, Base64.URL_SAFE or Base64.NO_WRAP)
  }

  fun recoveryToken(random: SecureRandom = SecureRandom()): String = randomToken(32, random)
}
