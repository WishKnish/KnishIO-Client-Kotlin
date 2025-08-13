/*
Post-Quantum Cryptography implementation for KnishIO Client
Provides ML-KEM768 + AES-GCM encryption compatibility with JavaScript client

License: https://github.com/WishKnish/KnishIO-Client-Kotlin/blob/master/LICENSE
*/

package wishKnish.knishIO.client.libraries

import org.bouncycastle.jcajce.spec.MLKEMParameterSpec
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.encoders.Hex
import java.security.*
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.jvm.Throws

/**
 * Post-Quantum Cryptography implementation using ML-KEM768 (ML-Kyber) + AES-GCM
 * Compatible with the JavaScript KnishIO client's post-quantum encryption
 */
class PostQuantumCrypto {
    
    companion object {
        private const val ML_KEM_768_KEYSIZE = 768
        private const val AES_KEY_SIZE = 256
        private const val AES_GCM_IV_LENGTH = 12
        private const val AES_GCM_TAG_LENGTH = 16
        
        init {
            // Register BouncyCastle provider (ML-KEM is included in BC 1.79+)
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(BouncyCastleProvider())
            }
        }
        
        /**
         * Generate ML-KEM768 key pair for post-quantum key encapsulation
         */
        @Throws(Exception::class)
        fun generateMLKEMKeyPair(): KeyPair {
            val keyPairGenerator = KeyPairGenerator.getInstance("ML-KEM", "BC")
            keyPairGenerator.initialize(MLKEMParameterSpec.ml_kem_768)
            return keyPairGenerator.generateKeyPair()
        }
        
        /**
         * Generate ML-KEM768 key pair from seed for deterministic key generation
         * Uses native BouncyCastle implementation for reliability (KISS & YAGNI)
         */
        @Throws(Exception::class)
        fun generateMLKEMKeyPairFromSeed(seed: ByteArray): KeyPair {
            // Use native BouncyCastle for simplicity and reliability
            val secureRandom = java.security.SecureRandom.getInstance("SHA1PRNG")
            secureRandom.setSeed(seed)
            
            val keyPairGenerator = KeyPairGenerator.getInstance("ML-KEM", "BC")
            keyPairGenerator.initialize(MLKEMParameterSpec.ml_kem_768, secureRandom)
            return keyPairGenerator.generateKeyPair()
        }
        
        /**
         * Encrypt message using ML-KEM768 + AES-GCM (compatible with JavaScript client)
         */
        @Throws(Exception::class)
        fun encryptMessage(
            message: String,
            recipientPublicKey: PublicKey
        ): PostQuantumEncryptedMessage {
            // Step 1: Generate shared secret using NobleMLKEMBridge for compatibility
            val (sharedSecret, encapsulation) = NobleMLKEMBridge.encapsulate(recipientPublicKey)
            
            // Step 2: Use shared secret directly as AES key (JavaScript compatibility)
            val aesKey = SecretKeySpec(sharedSecret, "AES")
            
            // Step 3: Encrypt message with AES-GCM
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = ByteArray(AES_GCM_IV_LENGTH)
            SecureRandom().nextBytes(iv)
            
            val gcmSpec = GCMParameterSpec(AES_GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec)
            
            val ciphertext = cipher.doFinal(message.toByteArray(Charsets.UTF_8))
            
            return PostQuantumEncryptedMessage(
                version = 2, // Post-quantum version
                encapsulation = Hex.toHexString(encapsulation),
                iv = Hex.toHexString(iv),
                ciphertext = Hex.toHexString(ciphertext),
                algorithm = "ML-KEM768+AES-GCM"
            )
        }
        
        /**
         * Decrypt message using ML-KEM768 + AES-GCM
         */
        @Throws(Exception::class)
        fun decryptMessage(
            encryptedMessage: PostQuantumEncryptedMessage,
            privateKey: PrivateKey
        ): String {
            // Step 1: Extract shared secret using NobleMLKEMBridge for compatibility
            val encapsulation = Hex.decode(encryptedMessage.encapsulation)
            val sharedSecret = NobleMLKEMBridge.decapsulate(encapsulation, privateKey)
            
            // Step 2: Use shared secret directly as AES key (JavaScript compatibility)
            val aesKey = SecretKeySpec(sharedSecret, "AES")
            
            // Step 3: Decrypt message with AES-GCM
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = Hex.decode(encryptedMessage.iv)
            val ciphertext = Hex.decode(encryptedMessage.ciphertext)
            
            val gcmSpec = GCMParameterSpec(AES_GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec)
            
            val decryptedBytes = cipher.doFinal(ciphertext)
            return String(decryptedBytes, Charsets.UTF_8)
        }
        
        /**
         * Convert public key to hex string for storage/transmission
         */
        fun publicKeyToHex(publicKey: PublicKey): String {
            return Hex.toHexString(publicKey.encoded)
        }
        
        /**
         * Convert private key to hex string for storage (use with caution)
         */
        fun privateKeyToHex(privateKey: PrivateKey): String {
            return Hex.toHexString(privateKey.encoded)
        }
        
        /**
         * Reconstruct public key from hex string
         */
        @Throws(Exception::class)
        fun publicKeyFromHex(hexKey: String): PublicKey {
            val keyBytes = Hex.decode(hexKey)
            // Use KeyFactory to reconstruct the public key
            val keyFactory = java.security.KeyFactory.getInstance("ML-KEM", "BC")
            val keySpec = java.security.spec.X509EncodedKeySpec(keyBytes)
            return keyFactory.generatePublic(keySpec)
        }
        
        /**
         * Extract raw ML-KEM768 public key bytes (1184 bytes) from KeyPair
         * Compatible with JavaScript @noble/post-quantum library format
         * 
         * For NobleMLKEMBridge keys, returns the raw bytes directly.
         * For BouncyCastle keys, uses reflection to access internal fields.
         */
        fun extractRawMLKEMPublicKey(publicKey: PublicKey): ByteArray {
            // Check if this is a NobleMLKEMBridge key
            if (publicKey is NobleMLKEMBridge.Companion.MLKEMPublicKey) {
                return publicKey.bytes
            }
            
            // First try the direct approach using getPublicData() if available
            try {
                val getPublicDataMethod = publicKey.javaClass.getMethod("getPublicData")
                val result = getPublicDataMethod.invoke(publicKey)
                if (result is ByteArray && result.size == 1184) {
                    return result
                }
            } catch (e: NoSuchMethodException) {
                // Method doesn't exist, try reflection approach
            } catch (e: Exception) {
                // Method call failed, try reflection approach  
            }
            
            // Try to access the internal params field via reflection
            try {
                val paramsField = publicKey.javaClass.getDeclaredField("params")
                paramsField.isAccessible = true
                val paramsValue = paramsField.get(publicKey)
                
                // The params object should have a getEncoded() method that returns the raw key
                if (paramsValue != null) {
                    val getEncodedMethod = paramsValue.javaClass.getMethod("getEncoded")
                    val rawKey = getEncodedMethod.invoke(paramsValue)
                    if (rawKey is ByteArray && rawKey.size == 1184) {
                        return rawKey
                    }
                }
            } catch (e: Exception) {
                // Reflection failed, fall back to ASN.1 extraction
            }
            
            // Fallback: Extract from X.509 encoded bytes
            // Get the X.509 encoded key bytes
            val encodedKey = publicKey.encoded
            
            // X.509 SubjectPublicKeyInfo format:
            // SEQUENCE {
            //   algorithm AlgorithmIdentifier,
            //   subjectPublicKey BIT STRING
            // }
            // The raw ML-KEM768 key is in the BIT STRING
            
            val rawKeySize = 1184
            
            // Try to find the raw key by looking for the BIT STRING containing it
            // In ASN.1, a BIT STRING starts with 0x03, followed by length encoding
            
            // Look for patterns that indicate the start of the raw key data
            // ML-KEM768 keys typically have high entropy throughout
            for (i in 0 until encodedKey.size - rawKeySize) {
                // Check if this could be the start of a BIT STRING containing our key
                if (i > 0 && encodedKey[i - 1] == 0x03.toByte()) {
                    // This might be a BIT STRING, check the length
                    val lengthByte = encodedKey[i]
                    
                    // For 1184 bytes, we expect either:
                    // - 0x82 0x04 0xA0 (long form: 1184 = 0x04A0)
                    // - Direct offset after algorithm identifier
                    
                    // Try extracting from after potential length encoding
                    var offset = i + 1
                    
                    // Handle long form length encoding
                    if ((lengthByte.toInt() and 0x80) != 0) {
                        val numLengthBytes = lengthByte.toInt() and 0x7F
                        offset = i + 1 + numLengthBytes
                    }
                    
                    // Skip the unused bits byte in BIT STRING
                    if (offset < encodedKey.size && encodedKey[offset] == 0x00.toByte()) {
                        offset++
                    }
                    
                    // Check if we have enough bytes for the raw key
                    if (offset + rawKeySize <= encodedKey.size) {
                        val candidate = encodedKey.sliceArray(offset until offset + rawKeySize)
                        
                        // Validate this looks like an ML-KEM key
                        if (hasValidMLKEMEntropy(candidate)) {
                            return candidate
                        }
                    }
                }
            }
            
            // Simpler approach: The raw key is often at the very end after all ASN.1 structure
            // Try the last 1184 bytes
            if (encodedKey.size >= rawKeySize) {
                val candidate = encodedKey.sliceArray(encodedKey.size - rawKeySize until encodedKey.size)
                if (hasValidMLKEMEntropy(candidate)) {
                    return candidate
                }
            }
            
            throw IllegalArgumentException(
                "Unable to extract ${rawKeySize}-byte ML-KEM768 raw key from ${encodedKey.size}-byte encoded key"
            )
        }
        
        /**
         * Simple entropy validation for ML-KEM768 keys
         * This is a heuristic check - proper validation would require more analysis
         */
        private fun hasValidMLKEMEntropy(keyBytes: ByteArray): Boolean {
            if (keyBytes.size != 1184) return false
            
            // Check for reasonable entropy distribution
            val byteFrequency = IntArray(256)
            keyBytes.forEach { byte ->
                byteFrequency[byte.toUByte().toInt()]++
            }
            
            // ML-KEM keys should have reasonably distributed byte values
            // Check that no single byte value dominates (> 10% of total)
            val maxFrequency = byteFrequency.maxOrNull() ?: 0
            return maxFrequency < (keyBytes.size * 0.1)
        }

        /**
         * Convert raw ML-KEM768 public key to base64 (JavaScript compatible)
         */
        fun rawMLKEMPublicKeyToBase64(publicKey: PublicKey): String {
            val rawKeyBytes = extractRawMLKEMPublicKey(publicKey)
            return java.util.Base64.getEncoder().encodeToString(rawKeyBytes)
        }
        
        /**
         * Reconstruct private key from hex string
         */
        @Throws(Exception::class)
        fun privateKeyFromHex(hexKey: String): PrivateKey {
            val keyBytes = Hex.decode(hexKey)
            // Use KeyFactory to reconstruct the private key
            val keyFactory = java.security.KeyFactory.getInstance("ML-KEM", "BC")
            val keySpec = java.security.spec.PKCS8EncodedKeySpec(keyBytes)
            return keyFactory.generatePrivate(keySpec)
        }
        
        /**
         * Convert public key to base64 string (JavaScript compatible)
         */
        fun publicKeyToBase64(publicKey: PublicKey): String {
            return Base64.getEncoder().encodeToString(publicKey.encoded)
        }
        
        /**
         * Convert private key to base64 string (JavaScript compatible)
         */
        fun privateKeyToBase64(privateKey: PrivateKey): String {
            return Base64.getEncoder().encodeToString(privateKey.encoded)
        }
        
        /**
         * Reconstruct public key from base64 string (JavaScript compatible)
         */
        @Throws(Exception::class)
        fun publicKeyFromBase64(base64Key: String): PublicKey {
            val keyBytes = Base64.getDecoder().decode(base64Key)
            val keyFactory = java.security.KeyFactory.getInstance("ML-KEM", "BC")
            val keySpec = java.security.spec.X509EncodedKeySpec(keyBytes)
            return keyFactory.generatePublic(keySpec)
        }
        
        /**
         * Reconstruct private key from base64 string (JavaScript compatible)
         */
        @Throws(Exception::class)
        fun privateKeyFromBase64(base64Key: String): PrivateKey {
            val keyBytes = Base64.getDecoder().decode(base64Key)
            val keyFactory = java.security.KeyFactory.getInstance("ML-KEM", "BC")
            val keySpec = java.security.spec.PKCS8EncodedKeySpec(keyBytes)
            return keyFactory.generatePrivate(keySpec)
        }
        
        /**
         * Convert between JavaScript and Kotlin encryption formats
         */
        fun convertJSEncryptedDataToKotlin(
            cipherText: String, // Base64 from JS
            encryptedMessage: String // Base64 from JS
        ): PostQuantumEncryptedMessage {
            // JavaScript uses base64, we need to convert to hex for our format
            val cipherTextBytes = Base64.getDecoder().decode(cipherText)
            val encryptedMessageBytes = Base64.getDecoder().decode(encryptedMessage)
            
            // Extract IV (first 12 bytes) and ciphertext from encryptedMessage
            val iv = encryptedMessageBytes.sliceArray(0 until AES_GCM_IV_LENGTH)
            val actualCiphertext = encryptedMessageBytes.sliceArray(AES_GCM_IV_LENGTH until encryptedMessageBytes.size)
            
            return PostQuantumEncryptedMessage(
                version = 2,
                encapsulation = Hex.toHexString(cipherTextBytes),
                iv = Hex.toHexString(iv),
                ciphertext = Hex.toHexString(actualCiphertext),
                algorithm = "ML-KEM768+AES-GCM"
            )
        }
    }
}

/**
 * Type alias for standard KeyPair used in ML-KEM operations
 */
typealias MLKEMKeyPair = KeyPair

/**
 * Data class representing a post-quantum encrypted message
 * Compatible with JavaScript client format
 */
data class PostQuantumEncryptedMessage(
    val version: Int,
    val encapsulation: String, // Hex-encoded ML-KEM768 encapsulation
    val iv: String,           // Hex-encoded AES-GCM IV
    val ciphertext: String,   // Hex-encoded AES-GCM ciphertext (includes auth tag)
    val algorithm: String
) {
    /**
     * Convert to map format compatible with existing encryption interface
     */
    fun toMap(): Map<String, String> {
        return mapOf(
            "version" to version.toString(),
            "algorithm" to algorithm,
            "encapsulation" to encapsulation,
            "iv" to iv,
            "data" to ciphertext
        )
    }
    
    companion object {
        /**
         * Create from map format (for compatibility)
         */
        fun fromMap(map: Map<String, String>): PostQuantumEncryptedMessage {
            return PostQuantumEncryptedMessage(
                version = map["version"]?.toIntOrNull() ?: 2,
                algorithm = map["algorithm"] ?: "ML-KEM768+AES-GCM",
                encapsulation = map["encapsulation"] ?: "",
                iv = map["iv"] ?: "",
                ciphertext = map["data"] ?: ""
            )
        }
    }
}


