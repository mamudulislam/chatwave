package com.example.chatwave.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeyStoreManager(context: Context) {
  private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
  private val appContext = context.applicationContext

  fun hasKey(alias: String): Boolean = keyStore.containsAlias(alias)

  fun deleteKey(alias: String) {
    if (hasKey(alias)) keyStore.deleteEntry(alias)
  }

  fun aliases(): List<String> = keyStore.aliases().toList().filter { it.startsWith(KEY_PREFIX) }

  fun getOrCreateAesKey(alias: String): SecretKey {
    if (hasKey(alias)) return keyStore.getKey(alias, null) as SecretKey
    val spec = KeyGenParameterSpec.Builder(
      alias,
      KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    )
      .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
      .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
      .setKeySize(256)
      .setRandomizedEncryptionRequired(true)
      .build()
    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
    generator.init(spec)
    return generator.generateKey()
  }

  fun getOrCreateIdentityKeyPair(alias: String, requireAuthentication: Boolean): KeyPair {
    if (hasKey(alias)) {
      val privateKey = keyStore.getKey(alias, null) as? PrivateKey
        ?: throw IllegalStateException("Identity key alias does not contain a private key")
      val publicKey = keyStore.getCertificate(alias)?.publicKey
        ?: throw IllegalStateException("Identity key alias does not contain a certificate")
      return KeyPair(publicKey, privateKey)
    }
    val builder = KeyGenParameterSpec.Builder(
      alias,
      KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
    )
      .setDigests(KeyProperties.DIGEST_SHA256)
      .setUserAuthenticationRequired(requireAuthentication)
      .setUserAuthenticationValidityDurationSeconds(if (requireAuthentication) 300 else -1)
    if (requireAuthentication && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      builder.setInvalidatedByBiometricEnrollment(true)
    }
    val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE)
    generator.initialize(builder.build())
    return generator.generateKeyPair()
  }

  fun getIdentityPrivateKey(alias: String): PrivateKey =
    keyStore.getKey(alias, null) as? PrivateKey
      ?: throw IllegalStateException("Identity key is not available")

  fun getPublicKey(alias: String): PublicKey =
    keyStore.getCertificate(alias)?.publicKey
      ?: throw IllegalStateException("Identity key is not available")

  fun encrypt(alias: String, plaintext: ByteArray, associatedData: ByteArray? = null): String {
    val cipher = Cipher.getInstance("${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}")
    cipher.init(Cipher.ENCRYPT_MODE, getOrCreateAesKey(alias))
    associatedData?.let(cipher::updateAAD)
    val iv = cipher.iv
    val ciphertext = cipher.doFinal(plaintext)
    val record = ByteArray(1 + iv.size + ciphertext.size)
    record[0] = iv.size.toByte()
    System.arraycopy(iv, 0, record, 1, iv.size)
    System.arraycopy(ciphertext, 0, record, 1 + iv.size, ciphertext.size)
    return Base64.encodeToString(record, Base64.NO_WRAP)
  }

  fun decrypt(alias: String, encoded: String, associatedData: ByteArray? = null): ByteArray {
    val record = Base64.decode(encoded, Base64.NO_WRAP)
    require(record.size > 17) { "Encrypted record is too short" }
    val ivLength = record[0].toInt()
    require(ivLength == 12) { "Unsupported IV length" }
    val iv = record.copyOfRange(1, 1 + ivLength)
    val ciphertext = record.copyOfRange(1 + ivLength, record.size)
    val cipher = Cipher.getInstance("${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}")
    cipher.init(Cipher.DECRYPT_MODE, getOrCreateAesKey(alias), GCMParameterSpec(128, iv))
    associatedData?.let(cipher::updateAAD)
    return cipher.doFinal(ciphertext)
  }

  fun encryptString(alias: String, value: String, associatedData: ByteArray? = null): String =
    encrypt(alias, value.toByteArray(Charsets.UTF_8), associatedData)

  fun decryptString(alias: String, value: String, associatedData: ByteArray? = null): String =
    String(decrypt(alias, value, associatedData), Charsets.UTF_8)

  private companion object {
    const val ANDROID_KEYSTORE = "AndroidKeyStore"
    const val KEY_PREFIX = "cw_"
  }
}
