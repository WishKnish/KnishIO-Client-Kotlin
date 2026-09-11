package wishKnish.knishIO.client.storage.keystore

import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import wishKnish.knishIO.client.exception.SecretStorageException
import wishKnish.knishIO.client.storage.MemoryStorageBackend

class AndroidKeystoreSecretStorageProviderJvmTest {

  @Test
  fun `construction fails closed on a JVM without AndroidKeyStore`() {
    val exception = assertThrows(SecretStorageException::class.java) {
      AndroidKeystoreSecretStorageProvider(MemoryStorageBackend())
    }
    assertTrue(
      "Expected exception message to contain 'android-keystore', got: ${exception.message}",
      exception.message?.contains("android-keystore") == true
    )
  }
}
