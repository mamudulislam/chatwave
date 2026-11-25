package com.example.chatwave.crypto

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class SessionKeyManager(private val secureRandom: SecureRandom = SecureRandom()) {
  fun generateEphemeralKeyPair(): KeyPair {
    val generator = KeyPairGenerator.getInstance("EC")
    generator.initialize(ECGenParameterSpec("secp256r1"), secureRandom)
    return generator.generateKeyPair()
  }

  fun publicKeyDer(publicKey: PublicKey): ByteArray = publicKey.encoded

  fun publicKeyFromDer(der: ByteArray): PublicKey =
    KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(der))

  fun deriveSessionKey(privateKey: PrivateKey, peerPublicKeyDer: ByteArray, info: String): ByteArray {
    val agreement = KeyAgreement.getInstance("ECDH")
    agreement.init(privateKey)
    agreement.doPhase(publicKeyFromDer(peerPublicKeyDer), true)
    val sharedSecret = agreement.generateSecret()
    return hkdf(sharedSecret, HKDF_SALT, info.toByteArray(Charsets.UTF_8), 32)
  }

  fun hkdf(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
    require(length in 1..255) { "Invalid derived key length" }
    val prk = hmac(salt, ikm)
    val output = ByteArrayOutputStreamCompat()
    var previous = ByteArray(0)
    var counter = 1
    while (output.size() < length) {
      previous = hmac(prk, previous + info + counter.toByte())
      output.write(previous)
      counter++
    }
    return output.toByteArray().copyOf(length)
  }

  private fun hmac(key: ByteArray, data: ByteArray): ByteArray {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(key, "HmacSHA256"))
    return mac.doFinal(data)
  }

  private class ByteArrayOutputStreamCompat {
    private val bytes = mutableListOf<Byte>()
    fun write(value: ByteArray) = bytes.addAll(value.asList())
    fun size(): Int = bytes.size
    fun toByteArray(): ByteArray = bytes.toByteArray()
  }

  private companion object {
    val HKDF_SALT = "chatwave-session-key-v1".toByteArray(Charsets.UTF_8)
  }
}

fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
