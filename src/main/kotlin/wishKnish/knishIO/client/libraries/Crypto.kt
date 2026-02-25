@file:JvmName("Crypto")

package wishKnish.knishIO.client.libraries

import kotlinx.serialization.SerializationException
import java.security.GeneralSecurityException
import kotlin.jvm.Throws

class Crypto {
  companion object {

    @JvmStatic
    @JvmOverloads
    @Throws(IllegalArgumentException::class)
    fun hashShare(
      key: String,
      characters: String = "BASE64"
    ): String {
      return Soda(characters).shortHash(key)
    }

    @JvmStatic
    @JvmOverloads
    @Throws(IllegalArgumentException::class, NumberFormatException::class)
    fun generateEncPublicKey(
      privateKey: String,
      characters: String = "BASE64"
    ): String {
      return Soda(characters).generatePublicKey(privateKey)
    }

    @JvmStatic
    @JvmOverloads
    @Throws(IllegalArgumentException::class)
    fun generateEncPrivateKey(
      key: String,
      characters: String = "BASE64"
    ): String {
      return Soda(characters).generatePrivateKey(key)
    }

    @JvmStatic
    @JvmOverloads
    @Throws(IllegalArgumentException::class, GeneralSecurityException::class)
    fun <T : Collection<*>> encryptMessage(
      message: T,
      recipientPublicKey: String,
      characters: String = "BASE64"
    ): String {
      return Soda(characters).encrypt(message, recipientPublicKey)
    }

    @JvmStatic
    @JvmOverloads
    @Throws(IllegalArgumentException::class, GeneralSecurityException::class)
    fun encryptMessage(
      message: String,
      recipientPublicKey: String,
      characters: String = "BASE64"
    ): String {
      return Soda(characters).encrypt(message, recipientPublicKey)
    }

    @JvmStatic
    @JvmOverloads
    @Throws(
      IllegalArgumentException::class, GeneralSecurityException::class, SerializationException::class
    )
    fun decryptMessage(
      message: String,
      privateKey: String,
      publicKey: String,
      characters: String = "BASE64"
    ): Any? {
      return Soda(characters).decrypt(message, privateKey, publicKey)
    }

    @JvmStatic
    fun generateBundleHash(secret: String): String {
      return Shake256.hash(secret, 32)
    }

    @JvmStatic
    @JvmOverloads
    @Throws(NoSuchElementException::class)
    fun generateSecret(
      seed: String? = null,
      length: Int = 2048
    ): String {
      return when (seed) {
        null -> Strings.randomString(length)
        else -> Shake256.hash(seed, length / 2)
      }
    }

    @JvmStatic
    @JvmOverloads
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
    @JvmOverloads
    @Throws(NoSuchElementException::class)
    fun generateWalletPosition(
      saltLength: Int = 64,
      secret: String? = null,
      index: Int = 0
    ): String {
      return if (secret != null) {
        // Deterministic position generation for cross-SDK compatibility
        val sponge = Shake256.create()
        sponge.absorb(secret)
        sponge.absorb("WalletPosition")
        sponge.absorb(index.toString())
        sponge.hexString(32) // 32 bytes = 64 hex characters
      } else {
        // Fallback to random for backward compatibility
        Strings.randomString(saltLength, "abcdef0123456789")
      }
    }

    /**
     * SHAKE256 hash function
     * 
     * @param input The input string to hash
     * @param outputLength The desired output length in bits
     * @return The hex-encoded hash
     */
    @JvmStatic
    fun shake256(input: String, outputLength: Int): String {
      // Convert bits to bytes (outputLength is in bits, Shake256.hash expects bytes)
      return Shake256.hash(input, outputLength / 8)
    }
  }
}
