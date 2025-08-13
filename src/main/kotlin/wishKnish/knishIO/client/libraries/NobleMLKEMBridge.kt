package wishKnish.knishIO.client.libraries

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Value
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.bouncycastle.util.encoders.Hex
import org.bouncycastle.util.encoders.Base64

/**
 * JavaScript bridge for ML-KEM768 cryptography using noble-post-quantum
 * Provides 100% compatibility with JavaScript implementation
 */
class NobleMLKEMBridge {
    
    companion object {
        private val context: Context by lazy {
            Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup { true }
                .build()
        }
        
        private val nobleMLKEM: Value by lazy {
            // Load the bundled JavaScript library
            val jsCode = this::class.java.getResourceAsStream("/js/noble-ml-kem-bundle.js")
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: throw RuntimeException("Failed to load noble-ml-kem-bundle.js")
            
            // Execute JavaScript and get the NobleMLKEM object
            context.eval("js", jsCode)
            context.eval("js", "NobleMLKEM")
        }
        
        // Custom key classes for ML-KEM
        class MLKEMPublicKey(val bytes: ByteArray) : PublicKey {
            override fun getAlgorithm(): String = "ML-KEM768"
            override fun getFormat(): String = "RAW"
            override fun getEncoded(): ByteArray = bytes
        }
        
        class MLKEMPrivateKey(val bytes: ByteArray) : PrivateKey {
            override fun getAlgorithm(): String = "ML-KEM768"
            override fun getFormat(): String = "RAW"
            override fun getEncoded(): ByteArray = bytes
        }
        
        /**
         * Generate ML-KEM768 key pair from seed using noble JavaScript implementation
         */
        fun generateMLKEMKeyPairFromSeed(seed: ByteArray): KeyPair {
            val seedHex = Hex.toHexString(seed)
            
            // Call JavaScript function
            val result = nobleMLKEM.getMember("generateKeyPairFromSeed")
                .execute(seedHex)
            
            val publicKeyHex = result.getMember("publicKey").asString()
            val secretKeyHex = result.getMember("secretKey").asString()
            
            val publicKey = MLKEMPublicKey(Hex.decode(publicKeyHex))
            val privateKey = MLKEMPrivateKey(Hex.decode(secretKeyHex))
            
            return KeyPair(publicKey, privateKey)
        }
        
        /**
         * Encapsulate to generate shared secret and ciphertext
         */
        fun encapsulate(publicKey: PublicKey): Pair<ByteArray, ByteArray> {
            val publicKeyHex = Hex.toHexString(publicKey.encoded)
            
            val result = nobleMLKEM.getMember("encapsulate")
                .execute(publicKeyHex)
            
            val sharedSecretHex = result.getMember("sharedSecret").asString()
            val cipherTextHex = result.getMember("cipherText").asString()
            
            return Pair(
                Hex.decode(sharedSecretHex),
                Hex.decode(cipherTextHex)
            )
        }
        
        /**
         * Decapsulate to recover shared secret from ciphertext
         */
        fun decapsulate(cipherText: ByteArray, privateKey: PrivateKey): ByteArray {
            val cipherTextHex = Hex.toHexString(cipherText)
            val secretKeyHex = Hex.toHexString(privateKey.encoded)
            
            val sharedSecretHex = nobleMLKEM.getMember("decapsulate")
                .execute(cipherTextHex, secretKeyHex)
                .asString()
            
            return Hex.decode(sharedSecretHex)
        }
        
        /**
         * Encrypt message using ML-KEM + native AES-GCM
         */
        fun encryptMessage(publicKey: PublicKey, message: String): String {
            // Use JavaScript for ML-KEM encapsulation only
            val (sharedSecret, mlKemCipherText) = encapsulate(publicKey)
            
            // Use native Kotlin for AES-GCM
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val keySpec = SecretKeySpec(sharedSecret, "AES")
            
            // Generate random IV
            val iv = ByteArray(12)
            java.security.SecureRandom().nextBytes(iv)
            val gcmSpec = GCMParameterSpec(128, iv)
            
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
            // In Java's AES-GCM, doFinal() returns ciphertext with the tag appended
            val encryptedWithTag = cipher.doFinal(message.toByteArray())
            
            // Combine all components in hex format
            // Note: encryptedWithTag already includes the authentication tag
            val combinedHex = "${Hex.toHexString(mlKemCipherText)}:${Hex.toHexString(encryptedWithTag)}:${Hex.toHexString(iv)}"
            return Base64.toBase64String(combinedHex.toByteArray())
        }
        
        /**
         * Decrypt message using ML-KEM + native AES-GCM
         */
        fun decryptMessage(privateKey: PrivateKey, encryptedData: String): String {
            // Decode from base64
            val combined = String(Base64.decode(encryptedData))
            val parts = combined.split(":")
            
            if (parts.size != 3) {
                throw IllegalArgumentException("Invalid encrypted data format")
            }
            
            val mlKemCipherText = Hex.decode(parts[0])
            val encryptedWithTag = Hex.decode(parts[1])
            val iv = Hex.decode(parts[2])
            
            // Use JavaScript for ML-KEM decapsulation only
            val sharedSecret = decapsulate(mlKemCipherText, privateKey)
            
            // Use native Kotlin for AES-GCM
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val keySpec = SecretKeySpec(sharedSecret, "AES")
            val gcmSpec = GCMParameterSpec(128, iv)
            
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
            
            // Java's AES-GCM expects ciphertext with tag appended
            val decrypted = cipher.doFinal(encryptedWithTag)
            return String(decrypted)
        }
        
        /**
         * Test method: Decrypt JavaScript-encrypted message using shared secret approach
         */
        fun decryptJavaScriptMessage(
            privateKey: PrivateKey,
            mlKemCipherTextHex: String,
            encryptedMessageHex: String,
            tagHex: String,
            ivHex: String
        ): String {
            // Get shared secret from JavaScript decapsulation
            val mlKemCipherText = Hex.decode(mlKemCipherTextHex)
            val sharedSecret = decapsulate(mlKemCipherText, privateKey)
            
            // Use native Kotlin AES-GCM with the shared secret
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val keySpec = SecretKeySpec(sharedSecret, "AES")
            val gcmSpec = GCMParameterSpec(128, Hex.decode(ivHex))
            
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
            
            // Combine ciphertext and tag for decryption
            val ciphertext = Hex.decode(encryptedMessageHex)
            val tag = Hex.decode(tagHex)
            val combined = ByteArray(ciphertext.size + tag.size)
            System.arraycopy(ciphertext, 0, combined, 0, ciphertext.size)
            System.arraycopy(tag, 0, combined, ciphertext.size, tag.size)
            
            val decrypted = cipher.doFinal(combined)
            return String(decrypted)
        }
    }
}