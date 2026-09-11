@file:JvmName("SecretStorageFactory")

package wishKnish.knishIO.client.storage

/**
 * Factory object for instantiating SecretStorageProvider implementations
 */
object SecretStorageFactory {

  @JvmStatic
  @JvmOverloads
  fun createDefault(
    type: String = "aes-gcm",
    defaultPassphrase: String? = null,
    backend: StorageBackend? = null
  ): SecretStorageProvider {
    return when (type.lowercase()) {
      "memory" -> MemorySecretStorageProvider()
      else -> AesGcmSecretStorageProvider(
        backend = backend ?: MemoryStorageBackend(),
        defaultPassphrase = defaultPassphrase,
      )
    }
  }
}
