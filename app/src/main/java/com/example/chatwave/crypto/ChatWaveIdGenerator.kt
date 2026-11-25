package com.example.chatwave.crypto

import java.security.SecureRandom
import java.util.Random

class ChatWaveIdGenerator(private val random: Random = SecureRandom()) {
  fun generate(): String {
    val suffix = (0 until 8).joinToString("") { ALPHABET[random.nextInt(ALPHABET.length)].toString() }
    return "CW-$suffix"
  }

  fun generateUnique(existing: Set<String>): String {
    var candidate = generate()
    while (candidate in existing) candidate = generate()
    return candidate
  }

  private companion object {
    const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
  }
}
