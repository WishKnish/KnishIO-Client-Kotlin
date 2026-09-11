@file:JvmName("SecretStorageProvider")

package wishKnish.knishIO.client.storage

import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap

/**
 * Metadata associated with an encrypted secret in storage
 */
@Serializable
data class SecretStorageMetadata(
  val bundleHash: String,
  val label: String? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val hardwareBacked: Boolean = false,
  val providerType: String
)

/**
 * Versioned envelope encryption payload
 */
@Serializable
data class EncryptedSecretPayload(
  val version: Int = 1,
  val ciphertext: String, // Base64 encoded
  val iv: String,         // Base64 encoded
  val salt: String,       // Base64 encoded
  val algorithm: String = "AES-GCM",
  val iterations: Int = 100000,
  val metadata: SecretStorageMetadata
)

/**
 * Storage options for operations like passphrase and custom label
 */
data class StorageOptions @JvmOverloads constructor(
  val label: String? = null,
  val passphrase: String? = null
)

/**
 * Pluggable key-value storage backend adapter (Memory, SharedPreferences, etc.)
 */
interface StorageBackend {
  fun getItem(key: String): String?
  fun setItem(key: String, value: String)
  fun removeItem(key: String): Boolean
  fun keys(): List<String>
}

/**
 * Default in-memory thread-safe storage backend
 */
class MemoryStorageBackend : StorageBackend {
  private val store = ConcurrentHashMap<String, String>()

  override fun getItem(key: String): String? {
    return store[key]
  }

  override fun setItem(key: String, value: String) {
    store[key] = value
  }

  override fun removeItem(key: String): Boolean {
    return store.remove(key) != null
  }

  override fun keys(): List<String> {
    return store.keys().toList()
  }

  fun clear() {
    store.clear()
  }
}

/**
 * Contract for hardware-compatible envelope encryption secret storage providers
 */
interface SecretStorageProvider {
  /**
   * Unique identifier of this provider implementation
   */
  val providerType: String

  /**
   * True only when this provider holds a non-exportable key inside platform-secure
   * hardware (Android TEE/StrongBox, Secure Enclave, TPM) and learned that from the
   * platform itself — never from a caller argument. Software envelope providers
   * return false. The value is persisted as `metadata.hardwareBacked` in every
   * envelope this provider writes.
   */
  fun isHardwareBacked(): Boolean

  /**
   * Whether the storage backend is available in this runtime
   */
  fun isAvailable(): Boolean

  /**
   * Store and encrypt a master secret for the given bundle hash
   */
  fun storeSecret(
    bundleHash: String,
    secret: String,
    options: StorageOptions = StorageOptions()
  )

  /**
   * Retrieve and decrypt the master secret for the given bundle hash
   */
  fun retrieveSecret(
    bundleHash: String,
    options: StorageOptions = StorageOptions()
  ): String?

  /**
   * Delete a stored secret
   */
  fun deleteSecret(bundleHash: String): Boolean

  /**
   * Check if a secret exists for the given bundle hash
   */
  fun hasSecret(bundleHash: String): Boolean

  /**
   * List all stored secret metadata without exposing plaintext secrets
   */
  fun listSecrets(): List<SecretStorageMetadata>

  /**
   * Execute a block with the unwrapped secret and zeroize memory buffers upon completion
   */
  fun <T> withSecret(
    bundleHash: String,
    options: StorageOptions = StorageOptions(),
    block: (String) -> T
  ): T
}
