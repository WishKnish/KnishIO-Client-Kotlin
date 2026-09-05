@file:JvmName("AesGcmSecretStorageProvider")

package wishKnish.knishIO.client.storage

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import wishKnish.knishIO.client.exception.SecretStorageException
import wishKnish.knishIO.client.libraries.SecureMemory
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Standard JCA AES-GCM envelope encryption provider
 * Compatible with JVM and Android platforms
 */
class AesGcmSecretStorageProvider @JvmOverloads constructor(
  private val backend: StorageBackend = MemoryStorageBackend(),
  private val defaultPassphrase: String? = null,
  private val hardwareBacked: Boolean = false
) : SecretStorageProvider {

  override val providerType: String = if (hardwareBacked) "android-keystore-strongbox" else "aes-gcm"

  companion object {
    private const val KEY_PREFIX = "knishio:secret:"
    private const val DEFAULT_ITERATIONS = 100000
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12
    private const val SALT_LENGTH = 16

    private val json = Json {
      ignoreUnknownKeys = true
      encodeDefaults = true
    }
  }

  override fun isHardwareBacked(): Boolean {
    return hardwareBacked
  }

  override fun isAvailable(): Boolean {
    return true
  }

  private fun deriveKey(passphrase: String, salt: ByteArray, iterations: Int = DEFAULT_ITERATIONS): SecretKeySpec {
    val charArray = passphrase.toCharArray()
    return try {
      val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
      val spec = PBEKeySpec(charArray, salt, iterations, 256)
      val tmp = factory.generateSecret(spec)
      val keyBytes = tmp.encoded
      try {
        SecretKeySpec(keyBytes, "AES")
      } finally {
        SecureMemory.zeroize(keyBytes)
      }
    } finally {
      SecureMemory.zeroize(charArray)
    }
  }

  override fun storeSecret(
    bundleHash: String,
    secret: String,
    options: StorageOptions
  ) {
    if (bundleHash.isEmpty()) {
      throw SecretStorageException("Bundle hash cannot be empty")
    }
    if (secret.isEmpty()) {
      throw SecretStorageException("Secret cannot be empty")
    }

    val passphrase = options.passphrase ?: defaultPassphrase
      ?: throw SecretStorageException("Passphrase required for envelope encryption")

    val random = SecureRandom()
    val salt = ByteArray(SALT_LENGTH)
    val iv = ByteArray(GCM_IV_LENGTH)
    random.nextBytes(salt)
    random.nextBytes(iv)

    val secretBytes = secret.toByteArray(Charsets.UTF_8)
    try {
      val secretKey = deriveKey(passphrase, salt, DEFAULT_ITERATIONS)
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))

      val ciphertext = cipher.doFinal(secretBytes)

      val metadata = SecretStorageMetadata(
        bundleHash = bundleHash,
        label = options.label,
        createdAt = System.currentTimeMillis(),
        hardwareBacked = hardwareBacked,
        providerType = providerType
      )

      val payload = EncryptedSecretPayload(
        version = 1,
        ciphertext = Base64.getEncoder().encodeToString(ciphertext),
        iv = Base64.getEncoder().encodeToString(iv),
        salt = Base64.getEncoder().encodeToString(salt),
        algorithm = "AES-GCM",
        iterations = DEFAULT_ITERATIONS,
        metadata = metadata
      )

      backend.setItem("$KEY_PREFIX$bundleHash", json.encodeToString(payload))
    } catch (e: Exception) {
      throw SecretStorageException("Encryption failed: ${e.message}", e)
    } finally {
      SecureMemory.zeroize(secretBytes)
    }
  }

  override fun retrieveSecret(
    bundleHash: String,
    options: StorageOptions
  ): String? {
    val raw = backend.getItem("$KEY_PREFIX$bundleHash") ?: return null

    val payload = try {
      json.decodeFromString<EncryptedSecretPayload>(raw)
    } catch (e: Exception) {
      throw SecretStorageException.decryptionFailed("Corrupted payload format", e)
    }

    val passphrase = options.passphrase ?: defaultPassphrase
      ?: throw SecretStorageException("Passphrase required for secret decryption")

    val salt = Base64.getDecoder().decode(payload.salt)
    val iv = Base64.getDecoder().decode(payload.iv)
    val ciphertext = Base64.getDecoder().decode(payload.ciphertext)

    return try {
      val secretKey = deriveKey(passphrase, salt, payload.iterations)
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))

      val decryptedBytes = cipher.doFinal(ciphertext)
      SecureMemory.withSecureBytes(decryptedBytes) { bytes ->
        String(bytes, Charsets.UTF_8)
      }
    } catch (e: Exception) {
      throw SecretStorageException.decryptionFailed(e.message ?: "Authentication failed", e)
    }
  }

  override fun deleteSecret(bundleHash: String): Boolean {
    return backend.removeItem("$KEY_PREFIX$bundleHash")
  }

  override fun hasSecret(bundleHash: String): Boolean {
    return backend.getItem("$KEY_PREFIX$bundleHash") != null
  }

  override fun listSecrets(): List<SecretStorageMetadata> {
    return backend.keys()
      .filter { it.startsWith(KEY_PREFIX) }
      .mapNotNull { key ->
        backend.getItem(key)?.let { raw ->
          try {
            json.decodeFromString<EncryptedSecretPayload>(raw).metadata
          } catch (e: Exception) {
            null
          }
        }
      }
  }

  override fun <T> withSecret(
    bundleHash: String,
    options: StorageOptions,
    block: (String) -> T
  ): T {
    val raw = backend.getItem("$KEY_PREFIX$bundleHash")
      ?: throw SecretStorageException.notFound(bundleHash)

    val payload = try {
      json.decodeFromString<EncryptedSecretPayload>(raw)
    } catch (e: Exception) {
      throw SecretStorageException.decryptionFailed("Corrupted payload format", e)
    }

    val passphrase = options.passphrase ?: defaultPassphrase
      ?: throw SecretStorageException("Passphrase required for secret decryption")

    val salt = Base64.getDecoder().decode(payload.salt)
    val iv = Base64.getDecoder().decode(payload.iv)
    val ciphertext = Base64.getDecoder().decode(payload.ciphertext)

    // Only the SDK's own decryption is wrapped as decryptionFailed. `block` is caller code and
    // runs OUTSIDE the try: wrapping it mislabelled every caller exception as a decryption
    // failure, and needed an `is SecretStorageException` guard that detekt's
    // InstanceOfCheckForException rejects. Rust's with_secret draws the same boundary
    // (`let res = f(&secret_guard); … res`).
    val decryptedBytes = try {
      val secretKey = deriveKey(passphrase, salt, payload.iterations)
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
      cipher.doFinal(ciphertext)
    } catch (e: Exception) {
      throw SecretStorageException.decryptionFailed(e.message ?: "Authentication failed", e)
    }

    return SecureMemory.withSecureBytes(decryptedBytes) { bytes ->
      block(String(bytes, Charsets.UTF_8))
    }
  }
}
