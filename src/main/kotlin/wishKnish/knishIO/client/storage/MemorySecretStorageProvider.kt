@file:JvmName("MemorySecretStorageProvider")

package wishKnish.knishIO.client.storage

import wishKnish.knishIO.client.exception.SecretStorageException
import java.util.concurrent.ConcurrentHashMap

data class MemorySecretEntry(
  val secret: String,
  val metadata: SecretStorageMetadata
)

/**
 * Thread-safe in-memory secret storage provider
 * Used for testing, headless runners, and backward-compatible fallback
 */
class MemorySecretStorageProvider : SecretStorageProvider {
  override val providerType: String = "memory"
  private val secrets = ConcurrentHashMap<String, MemorySecretEntry>()

  override fun isHardwareBacked(): Boolean {
    return false
  }

  override fun isAvailable(): Boolean {
    return true
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

    val metadata = SecretStorageMetadata(
      bundleHash = bundleHash,
      label = options.label,
      createdAt = System.currentTimeMillis(),
      hardwareBacked = false,
      providerType = providerType
    )

    secrets[bundleHash] = MemorySecretEntry(secret, metadata)
  }

  override fun retrieveSecret(
    bundleHash: String,
    options: StorageOptions
  ): String? {
    return secrets[bundleHash]?.secret
  }

  override fun deleteSecret(bundleHash: String): Boolean {
    return secrets.remove(bundleHash) != null
  }

  override fun hasSecret(bundleHash: String): Boolean {
    return secrets.containsKey(bundleHash)
  }

  override fun listSecrets(): List<SecretStorageMetadata> {
    return secrets.values.map { it.metadata }
  }

  override fun <T> withSecret(
    bundleHash: String,
    options: StorageOptions,
    block: (String) -> T
  ): T {
    val entry = secrets[bundleHash] ?: throw SecretStorageException.notFound(bundleHash)
    return block(entry.secret)
  }

  fun clear() {
    secrets.clear()
  }
}
