package com.example.chatwave.crypto

import java.security.MessageDigest

object SafetyCode {
  private val FINGERPRINT_REGEX = Regex("^([0-9A-F]{2}:){31}[0-9A-F]{2}$")

  fun fingerprint(publicKeyDer: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(publicKeyDer).toHexUpper().replace(Regex("(..)(?=.)"), "$1:")

  fun isValidFingerprint(value: String): Boolean = FINGERPRINT_REGEX.matches(value)

  fun compute(
    localId: String,
    localFingerprint: String,
    remoteId: String,
    remoteFingerprint: String
  ): String {
    val pair = listOf(localId to localFingerprint, remoteId to remoteFingerprint)
      .sortedBy { "${it.first}|${it.second}" }
    val material = "${pair[0].first}:${pair[0].second}|${pair[1].first}:${pair[1].second}"
    val digest = MessageDigest.getInstance("SHA-256").digest(material.toByteArray(Charsets.UTF_8))
    return Base32.encode(digest).take(20).replace(Regex("(.{4})(?=.)"), "$1 ")
  }

  private fun ByteArray.toHexUpper(): String = joinToString("") { "%02X".format(it) }
}
