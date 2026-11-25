package com.example.chatwave.crypto

object Base32 {
  private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

  fun encode(bytes: ByteArray): String {
    val output = StringBuilder()
    var bits = 0
    var value = 0
    for (byte in bytes) {
      value = (value shl 8) or (byte.toInt() and 0xFF)
      bits += 8
      while (bits >= 5) {
        output.append(ALPHABET[(value ushr (bits - 5)) and 0x1F])
        bits -= 5
      }
    }
    if (bits > 0) output.append(ALPHABET[(value shl (5 - bits)) and 0x1F])
    return output.toString()
  }
}
