/*
Hybrid Cryptography implementation for KnishIO Client
Supports both post-quantum (ML-KEM768+AES-GCM) and classical (NaCl Box) encryption
Provides automatic fallback and version negotiation for compatibility

License: https://github.com/WishKnish/KnishIO-Client-Kotlin/blob/master/LICENSE
*/

package wishKnish.knishIO.client.libraries

import java.security.*
import kotlin.jvm.Throws

/**
 * Hybrid cryptography system supporting both post-quantum and classical encryption
 * Provides compatibility with both old and new KnishIO client versions
 */
class HybridCrypto {
    
    companion object {
        // Version constants for encryption method identification
        const val VERSION_NACL_BOX = 1
        const val VERSION_POST_QUANTUM = 2
        const val VERSION_HYBRID = 3
        
        /**
         * Encrypt message using automatic encryption method selection
         * Prefers post-quantum encryption but falls back to classical if needed
         */
        @Throws(Exception::class)
        fun encryptMessage(
            message: String,
            recipientPublicKey: String,
            encryptionMode: EncryptionMode = EncryptionMode.HYBRID,
            senderPrivateKey: String? = null
        ): Map<String, String> {
            return when (encryptionMode) {
                EncryptionMode.POST_QUANTUM_ONLY -> {
                    encryptPostQuantum(message, recipientPublicKey)
                }
                EncryptionMode.CLASSICAL_ONLY -> {
                    encryptClassical(message, recipientPublicKey, senderPrivateKey)
                }
                EncryptionMode.HYBRID -> {
                    // Try post-quantum first, fallback to classical
                    try {
                        encryptPostQuantum(message, recipientPublicKey)
                    } catch (e: Exception) {
                        encryptClassical(message, recipientPublicKey, senderPrivateKey)
                    }
                }
            }
        }
        
        /**
         * Decrypt message using automatic method detection
         */
        @Throws(Exception::class)
        fun decryptMessage(
            encryptedMessage: Map<String, String>,
            recipientPrivateKey: String,
            postQuantumPrivateKey: PrivateKey? = null
        ): String {
            val version = encryptedMessage["version"]?.toIntOrNull() ?: VERSION_NACL_BOX
            
            return when (version) {
                VERSION_POST_QUANTUM -> {
                    if (postQuantumPrivateKey == null) {
                        throw IllegalArgumentException("Post-quantum private key required for decryption")
                    }
                    decryptPostQuantum(encryptedMessage, postQuantumPrivateKey)
                }
                VERSION_NACL_BOX -> {
                    decryptClassical(encryptedMessage, recipientPrivateKey)
                }
                else -> {
                    // Try both methods for maximum compatibility
                    try {
                        if (postQuantumPrivateKey != null) {
                            decryptPostQuantum(encryptedMessage, postQuantumPrivateKey)
                        } else {
                            decryptClassical(encryptedMessage, recipientPrivateKey)
                        }
                    } catch (e: Exception) {
                        decryptClassical(encryptedMessage, recipientPrivateKey)
                    }
                }
            }
        }
        
        /**
         * Encrypt using post-quantum ML-KEM768 + AES-GCM
         */
        @Throws(Exception::class)
        private fun encryptPostQuantum(
            message: String,
            recipientPublicKeyHex: String
        ): Map<String, String> {
            val recipientPublicKey = PostQuantumCrypto.publicKeyFromHex(recipientPublicKeyHex)
            val encrypted = PostQuantumCrypto.encryptMessage(message, recipientPublicKey)
            return encrypted.toMap()
        }
        
        /**
         * Encrypt using classical NaCl Box
         */
        @Throws(Exception::class)
        private fun encryptClassical(
            message: String,
            recipientPublicKey: String,
            senderPrivateKey: String?
        ): Map<String, String> {
            // Use existing Soda class for classical encryption
            val soda = Soda()
            
            // Soda class currently only supports sealed box (anonymous encryption)
            val encryptedData = soda.encrypt(message, recipientPublicKey)
            
            return mapOf(
                "version" to VERSION_NACL_BOX.toString(),
                "algorithm" to "NaCl-SealedBox",
                "data" to encryptedData
            )
        }
        
        /**
         * Decrypt post-quantum encrypted message
         */
        @Throws(Exception::class)
        private fun decryptPostQuantum(
            encryptedMessage: Map<String, String>,
            privateKey: PrivateKey
        ): String {
            val pqMessage = PostQuantumEncryptedMessage.fromMap(encryptedMessage)
            return PostQuantumCrypto.decryptMessage(pqMessage, privateKey)
        }
        
        /**
         * Decrypt classical NaCl Box encrypted message
         */
        @Throws(Exception::class)
        private fun decryptClassical(
            encryptedMessage: Map<String, String>,
            privateKey: String
        ): String {
            val soda = Soda()
            
            // Get the encrypted data from the message
            val encryptedData = encryptedMessage["data"] ?: throw IllegalArgumentException("Missing encrypted data")
            
            // Generate public key from private key for NaCl sealed box decryption
            val publicKey = soda.generatePublicKey(privateKey)
            
            // Decrypt using Soda's decrypt method
            return when (val decrypted = soda.decrypt(encryptedData, privateKey, publicKey)) {
                is String -> decrypted
                else -> decrypted.toString()
            }
        }
        
        /**
         * Detect encryption method from encrypted message
         */
        fun detectEncryptionMethod(encryptedMessage: Map<String, String>): EncryptionMethod {
            return when {
                encryptedMessage.containsKey("encapsulation") -> EncryptionMethod.POST_QUANTUM
                encryptedMessage.containsKey("publicKey") -> EncryptionMethod.NACL_BOX
                encryptedMessage.containsKey("data") -> EncryptionMethod.NACL_SEALED_BOX
                else -> EncryptionMethod.UNKNOWN
            }
        }
        
        /**
         * Negotiate encryption method based on capabilities
         */
        fun negotiateEncryptionMethod(
            clientCapabilities: Set<EncryptionMethod>,
            serverCapabilities: Set<EncryptionMethod>
        ): EncryptionMethod {
            // Prefer post-quantum for future security
            return when {
                clientCapabilities.contains(EncryptionMethod.POST_QUANTUM) &&
                serverCapabilities.contains(EncryptionMethod.POST_QUANTUM) -> 
                    EncryptionMethod.POST_QUANTUM
                    
                clientCapabilities.contains(EncryptionMethod.NACL_BOX) &&
                serverCapabilities.contains(EncryptionMethod.NACL_BOX) -> 
                    EncryptionMethod.NACL_BOX
                    
                clientCapabilities.contains(EncryptionMethod.NACL_SEALED_BOX) &&
                serverCapabilities.contains(EncryptionMethod.NACL_SEALED_BOX) -> 
                    EncryptionMethod.NACL_SEALED_BOX
                    
                else -> EncryptionMethod.UNKNOWN
            }
        }
    }
}

/**
 * Encryption mode for method selection
 */
enum class EncryptionMode {
    POST_QUANTUM_ONLY,  // Use only ML-KEM768 + AES-GCM
    CLASSICAL_ONLY,     // Use only NaCl Box/Sealed Box
    HYBRID              // Try post-quantum first, fallback to classical
}

/**
 * Encryption method identification
 */
enum class EncryptionMethod {
    POST_QUANTUM,       // ML-KEM768 + AES-GCM
    NACL_BOX,          // NaCl Box (authenticated)
    NACL_SEALED_BOX,   // NaCl Sealed Box (anonymous)
    UNKNOWN
}

/**
 * Enhanced wallet key pair supporting both classical and post-quantum keys
 */
data class HybridKeyPair(
    // Classical keys (existing)
    val classicalPrivateKey: String,
    val classicalPublicKey: String,
    
    // Post-quantum keys (new)
    val postQuantumPrivateKey: PrivateKey?,
    val postQuantumPublicKey: PublicKey?,
    
    // Metadata
    val supportedMethods: Set<EncryptionMethod>
) {
    /**
     * Get public key for specified encryption method
     */
    fun getPublicKey(method: EncryptionMethod): String? {
        return when (method) {
            EncryptionMethod.POST_QUANTUM -> 
                postQuantumPublicKey?.let { PostQuantumCrypto.publicKeyToHex(it) }
            EncryptionMethod.NACL_BOX, EncryptionMethod.NACL_SEALED_BOX -> 
                classicalPublicKey
            else -> null
        }
    }
    
    /**
     * Check if encryption method is supported
     */
    fun supportsMethod(method: EncryptionMethod): Boolean {
        return supportedMethods.contains(method)
    }
}