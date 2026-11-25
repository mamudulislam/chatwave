package com.example.chatwave.crypto

import android.util.Base64
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

class SignatureManager(private val canonicalJson: CanonicalJson = CanonicalJson) {
  fun signingInput(unsigned: Map<String, Any?>): ByteArray =
    canonicalJson.canonicalize(unsigned).toByteArray(Charsets.UTF_8)

  fun sign(unsigned: Map<String, Any?>, privateKey: PrivateKey): String {
    val signer = Signature.getInstance("SHA256withECDSA")
    signer.initSign(privateKey)
    signer.update(signingInput(unsigned))
    return Base64.encodeToString(signer.sign(), Base64.NO_WRAP)
  }

  fun verify(unsigned: Map<String, Any?>, signature: String, publicKey: PublicKey): Boolean = try {
    val verifier = Signature.getInstance("SHA256withECDSA")
    verifier.initVerify(publicKey)
    verifier.update(signingInput(unsigned))
    verifier.verify(Base64.decode(signature, Base64.DEFAULT))
  } catch (_: Exception) {
    false
  }

  fun verify(unsigned: Map<String, Any?>, signature: String, publicKeyDer: ByteArray): Boolean =
    verify(unsigned, signature, publicKeyFromDer(publicKeyDer))

  fun publicKeyFromDer(der: ByteArray): PublicKey =
    KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(der))
}
