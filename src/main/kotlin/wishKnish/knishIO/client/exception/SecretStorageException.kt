@file:JvmName("SecretStorageException")

package wishKnish.knishIO.client.exception

/**
 * Exception thrown when a secret storage or hardware envelope encryption operation fails
 */
class SecretStorageException : BaseException {
  constructor() : super("Secret storage operation failed")
  constructor(message: String) : super(message)
  constructor(message: String, cause: Throwable) : super(message, cause)
  constructor(cause: Throwable) : super(cause)

  companion object {
    @JvmStatic
    fun notFound(bundleHash: String): SecretStorageException {
      return SecretStorageException("Secret not found for bundle: $bundleHash")
    }

    @JvmStatic
    @JvmOverloads
    fun decryptionFailed(reason: String, cause: Throwable? = null): SecretStorageException {
      return if (cause != null) {
        SecretStorageException("Failed to decrypt master secret: $reason", cause)
      } else {
        SecretStorageException("Failed to decrypt master secret: $reason")
      }
    }

    @JvmStatic
    fun unavailable(provider: String, reason: String): SecretStorageException {
      return SecretStorageException("Secret storage provider '$provider' is unavailable: $reason")
    }

    @JvmStatic
    fun validationError(message: String): SecretStorageException {
      return SecretStorageException(message)
    }
  }
}
