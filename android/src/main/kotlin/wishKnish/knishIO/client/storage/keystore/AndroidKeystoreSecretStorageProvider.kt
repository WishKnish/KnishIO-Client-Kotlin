@file:JvmName("AndroidKeystoreSecretStorageProvider")

package wishKnish.knishIO.client.storage.keystore

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import org.json.JSONObject
import wishKnish.knishIO.client.exception.SecretStorageException
import wishKnish.knishIO.client.libraries.SecureMemory
import wishKnish.knishIO.client.storage.SecretEnvelope
import wishKnish.knishIO.client.storage.SecretStorageMetadata
import wishKnish.knishIO.client.storage.SecretStorageProvider
import wishKnish.knishIO.client.storage.StorageBackend
import wishKnish.knishIO.client.storage.StorageOptions
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed envelope encryption secret storage provider for Android.
 *
 * Wraps a random device passphrase under a non-exportable AES-256-GCM Key-Encryption-Key (KEK)
 * managed by AndroidKeyStore. The master secret is stored in the standard cross-SDK envelope format
 * under that passphrase.
 *
 * The providerType and hardwareBacked values are derived directly from the platform via KeyInfo,
 * never from caller claims.
 */
class AndroidKeystoreSecretStorageProvider @JvmOverloads constructor(
  private val backend: StorageBackend,
  private val keyAlias: String = DEFAULT_KEY_ALIAS,
  requireStrongBox: Boolean = false
) : SecretStorageProvider {

  companion object {
    const val DEFAULT_KEY_ALIAS = "knishio.secret-storage.kek"
    const val PROVIDER_TYPE_TEE = "android-keystore-tee"
    const val PROVIDER_TYPE_STRONGBOX = "android-keystore-strongbox"
    private const val PROVIDER_NAME = "android-keystore"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_PREFIX = "knishio:secret:"
    private const val PASSPHRASE_PREFIX = "knishio:kek:"
    private const val PASSPHRASE_BYTES = 32
    private const val GCM_TAG_LENGTH = 128
  }

  private data class WrappedPassphrase(
    val version: Int = 1,
    val alias: String,
    val iv: String,
    val ciphertext: String
  ) {
    fun toJson(): String = JSONObject().apply {
      put("version", version)
      put("alias", alias)
      put("iv", iv)
      put("ciphertext", ciphertext)
    }.toString()

    companion object {
      fun fromJson(raw: String): WrappedPassphrase {
        val obj = JSONObject(raw)
        return WrappedPassphrase(
          version = obj.getInt("version"),
          alias = obj.getString("alias"),
          iv = obj.getString("iv"),
          ciphertext = obj.getString("ciphertext")
        )
      }
    }
  }

  private val kek: SecretKey
  override val providerType: String

  init {
    val keyStore = try {
      KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    } catch (e: Exception) {
      throw SecretStorageException.unavailable(
        PROVIDER_NAME,
        "AndroidKeyStore is not present in this runtime: ${e.message}"
      )
    }

    val existing = try {
      keyStore.getKey(keyAlias, null) as? SecretKey
    } catch (e: Exception) {
      null
    }

    var wasGenerated = false
    val key: SecretKey = if (existing != null) {
      existing
    } else {
      try {
        val specBuilder = KeyGenParameterSpec.Builder(
          keyAlias,
          KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
          .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
          .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
          .setKeySize(256)

        if (requireStrongBox) {
          specBuilder.setIsStrongBoxBacked(true)
        }

        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGen.init(specBuilder.build())
        val generated = keyGen.generateKey()
        wasGenerated = true
        generated
      } catch (e: StrongBoxUnavailableException) {
        throw SecretStorageException.unavailable(
          PROVIDER_NAME,
          "StrongBox requested but not present on this device"
        )
      } catch (e: Exception) {
        throw SecretStorageException.unavailable(
          PROVIDER_NAME,
          "KEK generation failed: ${e.message}"
        )
      }
    }

    val info = try {
      val factory = SecretKeyFactory.getInstance(key.algorithm, ANDROID_KEYSTORE)
      factory.getKeySpec(key, KeyInfo::class.java) as KeyInfo
    } catch (e: Exception) {
      if (wasGenerated) {
        try { keyStore.deleteEntry(keyAlias) } catch (_: Exception) {}
      }
      throw SecretStorageException.unavailable(
        PROVIDER_NAME,
        "Failed to inspect key security level: ${e.message}"
      )
    }

    val securityLevel = info.securityLevel
    val resolvedType = when (securityLevel) {
      KeyProperties.SECURITY_LEVEL_STRONGBOX -> PROVIDER_TYPE_STRONGBOX
      KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT,
      KeyProperties.SECURITY_LEVEL_UNKNOWN_SECURE -> PROVIDER_TYPE_TEE
      else -> {
        if (wasGenerated) {
          try { keyStore.deleteEntry(keyAlias) } catch (_: Exception) {}
        }
        throw SecretStorageException.unavailable(
          PROVIDER_NAME,
          "key security level $securityLevel is not hardware-backed"
        )
      }
    }

    if (requireStrongBox && resolvedType != PROVIDER_TYPE_STRONGBOX) {
      if (wasGenerated) {
        try { keyStore.deleteEntry(keyAlias) } catch (_: Exception) {}
      }
      throw SecretStorageException.unavailable(
        PROVIDER_NAME,
        "StrongBox required but key is at level $securityLevel"
      )
    }

    this.kek = key
    this.providerType = resolvedType
  }

  override fun isHardwareBacked(): Boolean = true

  override fun isAvailable(): Boolean = true

  private fun devicePassphrase(): String {
    val raw = backend.getItem("$PASSPHRASE_PREFIX$keyAlias")
    return if (raw != null) {
      val record = try {
        WrappedPassphrase.fromJson(raw)
      } catch (e: Exception) {
        throw SecretStorageException.decryptionFailed("Corrupted device passphrase record: ${e.message}", e)
      }
      val iv = Base64.getDecoder().decode(record.iv)
      val ciphertext = Base64.getDecoder().decode(record.ciphertext)
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      val decryptedBytes = try {
        cipher.init(Cipher.DECRYPT_MODE, kek, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        cipher.doFinal(ciphertext)
      } catch (e: Exception) {
        throw SecretStorageException.decryptionFailed(
          "wrapped device passphrase failed authentication under KEK '$keyAlias'",
          e
        )
      }
      try {
        Base64.getEncoder().encodeToString(decryptedBytes)
      } finally {
        SecureMemory.zeroize(decryptedBytes)
      }
    } else {
      val randomBytes = ByteArray(PASSPHRASE_BYTES)
      SecureRandom().nextBytes(randomBytes)
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      val ciphertext = try {
        cipher.init(Cipher.ENCRYPT_MODE, kek)
        cipher.doFinal(randomBytes)
      } catch (e: Exception) {
        throw SecretStorageException("Failed to wrap device passphrase: ${e.message}", e)
      }
      val record = WrappedPassphrase(
        version = 1,
        alias = keyAlias,
        iv = Base64.getEncoder().encodeToString(cipher.iv),
        ciphertext = Base64.getEncoder().encodeToString(ciphertext)
      )
      backend.setItem("$PASSPHRASE_PREFIX$keyAlias", record.toJson())
      try {
        Base64.getEncoder().encodeToString(randomBytes)
      } finally {
        SecureMemory.zeroize(randomBytes)
      }
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
    if (options.passphrase != null) {
      throw SecretStorageException(
        "AndroidKeystoreSecretStorageProvider derives its passphrase from the Keystore-protected device key; StorageOptions.passphrase is not accepted"
      )
    }

    val passphrase = devicePassphrase()
    val metadata = SecretStorageMetadata(
      bundleHash = bundleHash,
      label = options.label,
      createdAt = System.currentTimeMillis(),
      hardwareBacked = true,
      providerType = providerType
    )

    try {
      val payload = SecretEnvelope.seal(secret, passphrase, metadata)
      backend.setItem("$KEY_PREFIX$bundleHash", SecretEnvelope.encode(payload))
    } catch (e: Exception) {
      throw SecretStorageException("Encryption failed: ${e.message}", e)
    }
  }

  override fun retrieveSecret(
    bundleHash: String,
    options: StorageOptions
  ): String? {
    if (options.passphrase != null) {
      throw SecretStorageException(
        "AndroidKeystoreSecretStorageProvider derives its passphrase from the Keystore-protected device key; StorageOptions.passphrase is not accepted"
      )
    }

    val raw = backend.getItem("$KEY_PREFIX$bundleHash") ?: return null

    val payload = try {
      SecretEnvelope.decode(raw)
    } catch (e: Exception) {
      throw SecretStorageException.decryptionFailed("Corrupted payload format", e)
    }

    val passphrase = devicePassphrase()
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
    if (options.passphrase != null) {
      throw SecretStorageException(
        "AndroidKeystoreSecretStorageProvider derives its passphrase from the Keystore-protected device key; StorageOptions.passphrase is not accepted"
      )
    }

    val raw = backend.getItem("$KEY_PREFIX$bundleHash")
      ?: throw SecretStorageException.notFound(bundleHash)

    val payload = try {
      SecretEnvelope.decode(raw)
    } catch (e: Exception) {
      throw SecretStorageException.decryptionFailed("Corrupted payload format", e)
    }

    val passphrase = devicePassphrase()
    val decryptedBytes = try {
      SecretEnvelope.open(payload, passphrase)
    } catch (e: Exception) {
      throw SecretStorageException.decryptionFailed(e.message ?: "Authentication failed", e)
    }

    return SecureMemory.withSecureBytes(decryptedBytes) { bytes ->
      block(String(bytes, Charsets.UTF_8))
    }
  }
}
