package com.example.chatwave.security

import com.example.chatwave.crypto.Hash

class RecoveryManager {
  fun generateToken(): String = Hash.recoveryToken()

  fun hash(token: String): String = Hash.sha256Hex(token)

  fun formatForDisplay(token: String): String = token.chunked(4).joinToString(" ")
}
