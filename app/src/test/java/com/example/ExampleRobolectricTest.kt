package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ChatWave", appName)
  }

  @Test
  fun `verify encrypted repository initialization and encryption`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.chatwave.data.security.EncryptedDataRepository(context)
    val plain = "Secret ChatWave Message"
    val encrypted = repo.encryptString(plain)
    val decrypted = repo.decryptString(encrypted)
    assertEquals(plain, decrypted)

    val secondEncryption = repo.encryptString(plain)
    assertNotEquals("AES-GCM encryption must use a fresh nonce", encrypted, secondEncryption)

    val tampered = android.util.Base64.decode(encrypted, android.util.Base64.NO_WRAP)
    tampered[tampered.lastIndex] = (tampered.last().toInt() xor 1).toByte()
    assertThrows(java.security.GeneralSecurityException::class.java) {
      repo.decryptString(android.util.Base64.encodeToString(tampered, android.util.Base64.NO_WRAP))
    }
  }

  @Test
  fun `persisted repository snapshot is encrypted and can be restored`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = com.example.chatwave.data.security.EncryptedDataRepository(context)
    repository.updateCurrentUserProfile("Encrypted Snapshot Check", "Private profile", "+1 555 000 0000")
    Thread.sleep(1000)

    val snapshot = File(context.filesDir, "chatwave_local_store.enc")
    assertTrue("Encrypted snapshot should be written", snapshot.exists())
    assertTrue("Plaintext profile data must not be stored", !snapshot.readText().contains("Encrypted Snapshot Check"))

    val restoredRepository = com.example.chatwave.data.security.EncryptedDataRepository(context)
    assertEquals("Encrypted Snapshot Check", restoredRepository.currentUser.value?.name)
  }
}
