/*
Post-Quantum Cryptography implementation for KnishIO Client
Provides ML-KEM768 + AES-GCM encryption compatibility with JavaScript client

License: https://github.com/WishKnish/KnishIO-Client-Kotlin/blob/master/LICENSE
*/

package wishKnish.knishIO.client.libraries

import org.bouncycastle.util.encoders.Hex
import java.security.*
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
        private const val AES_GCM_IV_LENGTH = 12
        private const val AES_GCM_TAG_LENGTH = 16
        
        
        /**
         * Encrypt message using ML-KEM768 + AES-GCM.
         *
         * NON-CANONICAL ENVELOPE: produces a hex-joined [PostQuantumEncryptedMessage]
         * (version/encapsulation/iv/ciphertext) that no other KnishIO SDK can decrypt. The
         * canonical cross-SDK ML-KEM768 envelope is [Wallet.encryptMessage]'s
         * `{ cipherText, encryptedMessage }` map (the one asserted by the cross-platform
         * vector layer + strong cross-validation). Use that for interop.
         */
        @Deprecated("Non-canonical hex-joined ML-KEM envelope; use Wallet.encryptMessage for the canonical cross-SDK { cipherText, encryptedMessage } envelope.")
        @Throws(Exception::class)
        fun encryptMessage(
            message: String,
            recipientPublicKey: PublicKey
        ): PostQuantumEncryptedMessage {
            // Step 1: Generate shared secret using NobleMLKEMBridge for compatibility
            val (sharedSecret, encapsulation) = MlKemBackend.instance.encapsulate(recipientPublicKey.encoded)
            
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
         * Decrypt message using ML-KEM768 + AES-GCM.
         *
         * NON-CANONICAL ENVELOPE: consumes the hex-joined [PostQuantumEncryptedMessage]. The
         * canonical cross-SDK ML-KEM768 envelope is [Wallet.decryptMessage]'s
         * `{ cipherText, encryptedMessage }` map. Use that for interop.
         */
        @Deprecated("Non-canonical hex-joined ML-KEM envelope; use Wallet.decryptMessage for the canonical cross-SDK { cipherText, encryptedMessage } envelope.")
        @Throws(Exception::class)
        fun decryptMessage(
            encryptedMessage: PostQuantumEncryptedMessage,
            privateKey: PrivateKey
        ): String {
            // Step 1: Extract shared secret using NobleMLKEMBridge for compatibility
            val encapsulation = Hex.decode(encryptedMessage.encapsulation)
            val sharedSecret = MlKemBackend.instance.decapsulate(encapsulation, privateKey.encoded)
            
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
            return MlKemRawPublicKey(Hex.decode(hexKey))
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


