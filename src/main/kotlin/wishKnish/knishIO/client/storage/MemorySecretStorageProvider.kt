@file:JvmName("MemorySecretStorageProvider")

package wishKnish.knishIO.client.storage

import wishKnish.knishIO.client.exception.SecretStorageException
import wishKnish.knishIO.client.libraries.SecureMemory
import java.util.concurrent.ConcurrentHashMap

data class MemorySecretEntry(
  val secret: String,
  val metadata: SecretStorageMetadata
)

/**
 * Thread-safe in-memory secret storage provider
 * Used for testing, headless runners, and backward-compatible fallback
 */
class MemorySecretStorageProvider @JvmOverloads constructor(
  private val backend: StorageBackend = MemoryStorageBackend()
) : SecretStorageProvider {
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

    if (options.recoveryPassphrase != null) {
      val recoveryMetadata = metadata.copy(
        providerType = "aes-gcm",
        hardwareBacked = false
      )
      val recoveryPayload = SecretEnvelope.seal(secret, options.recoveryPassphrase, recoveryMetadata)
      backend.setItem("$RECOVERY_KEY_PREFIX$bundleHash", SecretEnvelope.encode(recoveryPayload))
    }
  }

  override fun retrieveSecret(
    bundleHash: String,
    options: StorageOptions
  ): String? {
    return secrets[bundleHash]?.secret
  }

  override fun deleteSecret(bundleHash: String): Boolean {
    backend.removeItem("$RECOVERY_KEY_PREFIX$bundleHash")
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

  override fun recoverSecret(
    bundleHash: String,
    recoveryPassphrase: String,
    options: StorageOptions
  ) {
    if (bundleHash.isEmpty()) {
      throw SecretStorageException("Bundle hash cannot be empty")
    }
    if (recoveryPassphrase.isEmpty()) {
      throw SecretStorageException("Recovery passphrase cannot be empty")
    }

    val raw = backend.getItem("$RECOVERY_KEY_PREFIX$bundleHash")
      ?: throw SecretStorageException.notFound(bundleHash)

    val payload = try {
      SecretEnvelope.decode(raw)
    } catch (e: Exception) {
      throw SecretStorageException.decryptionFailed("Corrupted recovery payload format", e)
    }

    val decryptedBytes = try {
      SecretEnvelope.open(payload, recoveryPassphrase)
    } catch (e: Exception) {
      throw SecretStorageException.decryptionFailed(e.message ?: "Authentication failed", e)
    }

    val secretStr = SecureMemory.withSecureBytes(decryptedBytes) { bytes ->
      String(bytes, Charsets.UTF_8)
    }

    val reEnrollOptions = if (options.recoveryPassphrase == null) {
      options.copy(recoveryPassphrase = recoveryPassphrase)
    } else {
      options
    }

    storeSecret(bundleHash, secretStr, reEnrollOptions)
  }

  fun clear() {
    secrets.clear()
    (backend as? MemoryStorageBackend)?.clear()
  }
}
