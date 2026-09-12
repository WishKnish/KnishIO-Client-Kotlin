@file:JvmName("SecretEnvelope")

package wishKnish.knishIO.client.storage

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import wishKnish.knishIO.client.libraries.SecureMemory
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * The cross-SDK at-rest envelope: PBKDF2-HMAC-SHA256 ×100000 (16-byte salt) → AES-256-GCM
 * (12-byte IV, 128-bit tag appended), standard padded base64, camelCase metadata.
 * Custody-agnostic — the caller supplies the metadata, including `hardwareBacked`/`providerType`;
 * this object never decides custody.
 */
object SecretEnvelope {
  const val ALGORITHM = "AES-GCM"
  const val DEFAULT_ITERATIONS = 100000
  private const val GCM_TAG_LENGTH = 128
  private const val GCM_IV_LENGTH = 12
  private const val SALT_LENGTH = 16

  @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
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

  fun seal(secret: String, passphrase: String, metadata: SecretStorageMetadata): EncryptedSecretPayload {
    val random = SecureRandom()
    val salt = ByteArray(SALT_LENGTH)
    val iv = ByteArray(GCM_IV_LENGTH)
    random.nextBytes(salt)
    random.nextBytes(iv)

    val secretBytes = secret.toByteArray(Charsets.UTF_8)
    return try {
      val secretKey = deriveKey(passphrase, salt, DEFAULT_ITERATIONS)
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))

      val ciphertext = cipher.doFinal(secretBytes)
      EncryptedSecretPayload(
        version = 1,
        ciphertext = Base64.getEncoder().encodeToString(ciphertext),
        iv = Base64.getEncoder().encodeToString(iv),
        salt = Base64.getEncoder().encodeToString(salt),
        algorithm = ALGORITHM,
        iterations = DEFAULT_ITERATIONS,
        metadata = metadata
      )
    } finally {
      SecureMemory.zeroize(secretBytes)
    }
  }

  fun open(payload: EncryptedSecretPayload, passphrase: String): ByteArray {
    val salt = Base64.getDecoder().decode(payload.salt)
    val iv = Base64.getDecoder().decode(payload.iv)
    val ciphertext = Base64.getDecoder().decode(payload.ciphertext)

    val secretKey = deriveKey(passphrase, salt, payload.iterations)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
    return cipher.doFinal(ciphertext)
  }

  fun encode(payload: EncryptedSecretPayload): String = json.encodeToString(payload)

  fun decode(raw: String): EncryptedSecretPayload = json.decodeFromString(raw)
}
