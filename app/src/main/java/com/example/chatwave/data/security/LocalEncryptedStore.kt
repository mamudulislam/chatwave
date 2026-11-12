package com.example.chatwave.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class LocalEncryptedStore(context: Context) {
  private val file = AtomicFile(File(context.filesDir, FILE_NAME))

  fun readText(): String? {
    if (!file.baseFile.exists()) return null
    val encrypted = file.openRead().use { it.readBytes() }
    return String(decrypt(encrypted), Charsets.UTF_8)
  }

  fun writeText(value: String) {
    val encrypted = encrypt(value.toByteArray(Charsets.UTF_8))
    val output = file.startWrite()
    try {
      output.write(encrypted)
      file.finishWrite(output)
    } catch (error: IOException) {
      file.failWrite(output)
      throw error
    }
  }

  fun encryptToBase64(value: String): String =
    Base64.encodeToString(encrypt(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)

  fun decryptFromBase64(value: String): String =
    String(decrypt(Base64.decode(value, Base64.NO_WRAP)), Charsets.UTF_8)

  private fun encrypt(plaintext: ByteArray): ByteArray {
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
    val iv = cipher.iv
    if (iv.size != GCM_IV_LENGTH_BYTES) {
      throw GeneralSecurityException("Unexpected AES-GCM IV length: ${iv.size}")
    }

    return MAGIC + byteArrayOf(iv.size.toByte()) + iv + cipher.doFinal(plaintext)
  }

  private fun decrypt(encrypted: ByteArray): ByteArray {
    val headerLength = MAGIC.size + 1
    if (encrypted.size < headerLength + GCM_IV_LENGTH_BYTES + GCM_TAG_LENGTH_BITS / 8 ||
      !encrypted.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)
    ) {
      throw GeneralSecurityException("Encrypted local data has an invalid header")
    }

    val ivLength = encrypted[MAGIC.size].toInt() and 0xff
    if (ivLength != GCM_IV_LENGTH_BYTES) {
      throw GeneralSecurityException("Encrypted local data has an invalid IV length")
    }

    val ivStart = headerLength
    val cipherStart = ivStart + ivLength
    val iv = encrypted.copyOfRange(ivStart, cipherStart)
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
    return cipher.doFinal(encrypted, cipherStart, encrypted.size - cipherStart)
  }

  private fun getOrCreateKey(): SecretKey {
    val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
    (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

    val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
    keyGenerator.init(
      KeyGenParameterSpec.Builder(
        KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
      )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setRandomizedEncryptionRequired(true)
        .setKeySize(256)
        .build()
    )
    return keyGenerator.generateKey()
  }

  private companion object {
    const val FILE_NAME = "chatwave_local_store.enc"
    const val KEY_ALIAS = "chatwave_local_store_key_v1"
    const val ANDROID_KEY_STORE = "AndroidKeyStore"
    const val TRANSFORMATION = "AES/GCM/NoPadding"
    const val GCM_IV_LENGTH_BYTES = 12
    const val GCM_TAG_LENGTH_BITS = 128
    val MAGIC = byteArrayOf(0x43, 0x57, 0x01)
  }
}
