package com.example.chatwave.crypto

import java.security.SecureRandom
import java.util.Random

class InvitationCodeGenerator(private val random: Random = SecureRandom()) {
  fun generate(): String = (0 until 9).joinToString("") { random.nextInt(10).toString() }

  fun normalize(input: String): String = input.filter(Char::isDigit)

  fun format(code: String): String {
    val normalized = normalize(code)
    if (normalized.length != 9) return normalized
    return "${normalized.substring(0, 3)} ${normalized.substring(3, 6)} ${normalized.substring(6, 9)}"
  }
}
