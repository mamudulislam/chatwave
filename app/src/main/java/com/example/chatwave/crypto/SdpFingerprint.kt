package com.example.chatwave.crypto

object SdpFingerprint {
  fun parse(sdp: String): String? {
    val match = Regex("a=fingerprint:sha-256\\s+([0-9A-Fa-f:]+)", RegexOption.IGNORE_CASE)
      .find(sdp) ?: return null
    val fingerprint = match.groupValues[1].trim().uppercase()
    return fingerprint.takeIf(SafetyCode::isValidFingerprint)
  }
}
