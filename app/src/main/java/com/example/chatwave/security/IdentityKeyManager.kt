package com.example.chatwave.security

import com.example.chatwave.crypto.SafetyCode
import com.example.chatwave.crypto.SignatureManager
import java.security.KeyPair

data class IdentityPublicKey(val version: Int, val der: ByteArray, val fingerprint: String)

class IdentityKeyManager(
  private val keyStoreManager: KeyStoreManager,
  private val signatureManager: SignatureManager
) {
  fun initialize(version: Int, requireAuthentication: Boolean): IdentityPublicKey {
    require(version > 0) { "Key version must be positive" }
    val alias = aliasFor(version)
    if (!keyStoreManager.hasKey(alias)) {
      keyStoreManager.getOrCreateIdentityKeyPair(alias, requireAuthentication)
    }
    return publicKey(version)
  }

  fun publicKey(version: Int): IdentityPublicKey {
    val der = keyStoreManager.getPublicKey(aliasFor(version)).encoded
    return IdentityPublicKey(version, der, SafetyCode.fingerprint(der))
  }

  fun sign(unsigned: Map<String, Any?>, version: Int): String =
    signatureManager.sign(unsigned, keyStoreManager.getIdentityPrivateKey(aliasFor(version)))

  fun signingKeyAvailable(version: Int): Boolean = keyStoreManager.hasKey(aliasFor(version))

  fun allAliases(): List<String> = keyStoreManager.aliases().filter { it.startsWith(IDENTITY_PREFIX) }

  fun deleteIdentityKeys() = allAliases().forEach(keyStoreManager::deleteKey)

  private fun aliasFor(version: Int): String = "$IDENTITY_PREFIX$version"

  private companion object {
    const val IDENTITY_PREFIX = "cw_identity_v"
  }
}
