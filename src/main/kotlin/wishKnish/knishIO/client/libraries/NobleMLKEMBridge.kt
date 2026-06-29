package wishKnish.knishIO.client.libraries

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Value
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import org.bouncycastle.util.encoders.Hex

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
         * Generate seed using Noble SHAKE256 (identical to JavaScript SDK generateSecret)
         */
        fun generateSeed(input: String, lengthInBits: Int): String {
            // Call JavaScript generateSecret function for deterministic compatibility
            return nobleMLKEM.getMember("generateSecret")
                .execute(input, lengthInBits)
                .asString()
        }
    }
}