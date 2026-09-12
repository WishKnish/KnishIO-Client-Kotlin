@file:JvmName("AesGcmSecretStorageProvider")

package wishKnish.knishIO.client.storage

import wishKnish.knishIO.client.exception.SecretStorageException
import wishKnish.knishIO.client.libraries.SecureMemory

/**
 * Standard JCA AES-GCM envelope encryption provider
 * Compatible with JVM and Android platforms.
 * Software provider: never hardware-backed. Hardware custody is `AndroidKeystoreSecretStorageProvider` in `android/`.
 */
class AesGcmSecretStorageProvider @JvmOverloads constructor(
  private val backend: StorageBackend = MemoryStorageBackend(),
  private val defaultPassphrase: String? = null
) : SecretStorageProvider {

  override val providerType: String = "aes-gcm"

  companion object {
    private const val KEY_PREFIX = "knishio:secret:"
  }

  /**
   * True only when this provider holds a non-exportable key inside platform-secure
   * hardware (Android TEE/StrongBox, Secure Enclave, TPM) and learned that from the
   * platform itself — never from a caller argument. Software envelope providers
   * return false. The value is persisted as `metadata.hardwareBacked` in every
   * envelope this provider writes.
   */
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

    val passphrase = options.passphrase ?: defaultPassphrase
      ?: throw SecretStorageException("Passphrase required for envelope encryption")

    val metadata = SecretStorageMetadata(
      bundleHash = bundleHash,
      label = options.label,
      createdAt = System.currentTimeMillis(),
      hardwareBacked = false,
      providerType = providerType
    )

    try {
      val payload = SecretEnvelope.seal(secret, passphrase, metadata)
      backend.setItem("$KEY_PREFIX$bundleHash", SecretEnvelope.encode(payload))

      if (options.recoveryPassphrase != null) {
        val recoveryMetadata = metadata.copy(
          providerType = "aes-gcm",
          hardwareBacked = false
        )
        val recoveryPayload = SecretEnvelope.seal(secret, options.recoveryPassphrase, recoveryMetadata)
        backend.setItem("$RECOVERY_KEY_PREFIX$bundleHash", SecretEnvelope.encode(recoveryPayload))
      }
    } catch (e: Exception) {
      throw SecretStorageException("Encryption failed: ${e.message}", e)
    }
  }

  override fun retrieveSecret(
    bundleHash: String,
    options: StorageOptions
  ): String? {
    val raw = backend.getItem("$KEY_PREFIX$bundleHash") ?: return null

    val payload = try {
      SecretEnvelope.decode(raw)
    } catch (e: Exception) {
      throw SecretStorageException.decryptionFailed("Corrupted payload format", e)
    }

    val passphrase = options.passphrase ?: defaultPassphrase
      ?: throw SecretStorageException("Passphrase required for secret decryption")

    val decryptedBytes = try {
      SecretEnvelope.open(payload, passphrase)
    } catch (e: Exception) {
      throw SecretStorageException.decryptionFailed(e.message ?: "Authentication failed", e)
    }

    return SecureMemory.withSecureBytes(decryptedBytes) { bytes ->
      String(bytes, Charsets.UTF_8)
    }
  }

  override fun deleteSecret(bundleHash: String): Boolean {
    backend.removeItem("$RECOVERY_KEY_PREFIX$bundleHash")
    return backend.removeItem("$KEY_PREFIX$bundleHash")
  }

  override fun hasSecret(bundleHash: String): Boolean {
    return backend.getItem("$KEY_PREFIX$bundleHash") != null
  }

  override fun listSecrets(): List<SecretStorageMetadata> {
    return backend.keys()
      .filter { it.startsWith(KEY_PREFIX) && !it.startsWith(RECOVERY_KEY_PREFIX) }
      .mapNotNull { key ->
        backend.getItem(key)?.let { raw ->
          try {
            SecretEnvelope.decode(raw).metadata
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
      SecretEnvelope.decode(raw)
    } catch (e: Exception) {
      throw SecretStorageException.decryptionFailed("Corrupted payload format", e)
    }

    val passphrase = options.passphrase ?: defaultPassphrase
      ?: throw SecretStorageException("Passphrase required for secret decryption")

    // Only the SDK's own decryption is wrapped as decryptionFailed. `block` is caller code and
    // runs OUTSIDE the try: wrapping it mislabelled every caller exception as a decryption
    // failure, and needed an `is SecretStorageException` guard that detekt's
    // InstanceOfCheckForException rejects. Rust's with_secret draws the same boundary
    // (`let res = f(&secret_guard); … res`).
    val decryptedBytes = try {
      SecretEnvelope.open(payload, passphrase)
    } catch (e: Exception) {
      throw SecretStorageException.decryptionFailed(e.message ?: "Authentication failed", e)
    }

    return SecureMemory.withSecureBytes(decryptedBytes) { bytes ->
      block(String(bytes, Charsets.UTF_8))
    }
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
      throw SecretStorageException.decryptionFailed("Corrupted payload format", e)
    }

    val decryptedBytes = try {
      SecretEnvelope.open(payload, recoveryPassphrase)
    } catch (e: Exception) {
      throw SecretStorageException.decryptionFailed(e.message ?: "Authentication failed", e)
    }

    val secretStr = SecureMemory.withSecureBytes(decryptedBytes) { bytes ->
      String(bytes, Charsets.UTF_8)
    }

    val effectivePassphrase = options.passphrase ?: defaultPassphrase ?: recoveryPassphrase
    val reEnrollOptions = options.copy(
      passphrase = effectivePassphrase,
      recoveryPassphrase = options.recoveryPassphrase ?: recoveryPassphrase
    )

    storeSecret(bundleHash, secretStr, reEnrollOptions)
  }
}
