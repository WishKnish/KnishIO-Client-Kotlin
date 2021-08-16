@file:JvmName("Crypto")

package wishKnish.knishIO.client.libraries

import kotlinx.serialization.SerializationException
import java.security.GeneralSecurityException
import kotlin.jvm.Throws

class Crypto {
  companion object {

    @JvmStatic
    @Throws(IllegalArgumentException::class)
    fun hashShare(
      key: String,
      characters: String = "GMP"
    ): String {
      return Soda(characters).shortHash(key)
    }

    @JvmStatic
    @Throws(IllegalArgumentException::class, NumberFormatException::class)
    fun generateEncPublicKey(
      privateKey: String,
      characters: String = "GMP"
    ): String {
      return Soda(characters).generatePublicKey(privateKey)
    }

    @JvmStatic
    @Throws(IllegalArgumentException::class)
    fun generateEncPrivateKey(
      key: String,
      characters: String = "GMP"
    ): String {
      return Soda(characters).generatePrivateKey(key)
    }

    @JvmStatic
    @Throws(IllegalArgumentException::class, GeneralSecurityException::class)
    fun <T: Collection<*>>encryptMessage(
      message: T,
      recipientPublicKey: String,
      characters: String = "GMP"
    ): String {
      return Soda(characters).encrypt(message, recipientPublicKey)
    }

    @JvmStatic
    @Throws(IllegalArgumentException::class, GeneralSecurityException::class)
    fun encryptMessage(
      message: String,
      recipientPublicKey: String,
      characters: String = "GMP"
    ): String {
      return Soda(characters).encrypt(message, recipientPublicKey)
    }

    @JvmStatic
    @Throws(
      IllegalArgumentException::class, GeneralSecurityException::class, SerializationException::class
    )
    fun decryptMessage(
      message: String,
      privateKey: String,
      publicKey: String,
      characters: String = "GMP"
    ): Any? {
      return Soda(characters).decrypt(message, privateKey, publicKey)
    }

    @JvmStatic
    fun generateBundleHash(secret: String): String {
      return Shake256.hash(secret, 32)
    }

    @JvmStatic
    @Throws(NoSuchElementException::class)
    fun generateSecret(
      seed: String? = null,
      length: Int = 2048
    ): String {
      return when (seed) {
        null -> Strings.randomString(length)
        else -> Shake256.hash(seed, length / 4)
      }
    }

    @JvmStatic
    @Throws(NoSuchElementException::class)
    fun generateBatchId(
      molecularHash: String? = null,
      index: Int? = null
    ): String {
      if (molecularHash != null && index != null) {
        return this.generateBundleHash("$molecularHash$index")
      }

      return Strings.randomString(64)
    }

    @JvmStatic
    @Throws(NoSuchElementException::class)
    fun generateWalletPosition(saltLength: Int = 64): String {
      return Strings.randomString(saltLength)
    }
  }
}
