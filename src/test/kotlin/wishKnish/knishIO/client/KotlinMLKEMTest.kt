/**
 * Kotlin-to-Kotlin ML-KEM Test
 * 
 * Tests ML-KEM768 encryption/decryption within Kotlin to isolate 
 * whether the issue is internal to Kotlin or cross-platform
 */

import org.junit.jupiter.api.Test
import wishKnish.knishIO.client.*
import wishKnish.knishIO.client.libraries.PostQuantumCrypto
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KotlinMLKEMTest {

    @Test
    fun encryptMessageRejectsNon1184Key() {
        // PQ-transport hardening: a stale/non-PQ validator advertises a ~48-byte `key`; encryptMessage
        // must fail with an actionable error, not a cryptic bridge crash.
        val wallet = Wallet(
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            "USER",
            "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"
        )
        val shortKey = java.util.Base64.getEncoder().encodeToString(ByteArray(48))
        val ex = assertFailsWith<IllegalArgumentException> {
            wallet.encryptMessage("{\"q\":1}", shortKey)
        }
        assertTrue(ex.message!!.contains("expected 1184 (ML-KEM-768)"))
    }

    @Test
    @Suppress("DEPRECATION") // exercises the non-canonical PostQuantumCrypto envelope on purpose
    fun testKotlinToKotlinEncryption() {
        println("🔐 Testing Kotlin-to-Kotlin ML-KEM encryption...")
        
        // Create sender and recipient wallets
        val senderSecret = "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd"
        val senderPosition = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"
        
        val recipientSecret = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
        val recipientPosition = "fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321"
        
        val senderWallet = Wallet(senderSecret, "USER", senderPosition)
        val recipientWallet = Wallet(recipientSecret, "USER", recipientPosition)
        
        println("📨 Sender address: ${senderWallet.address}")
        println("📨 Recipient address: ${recipientWallet.address}")
        println("🔐 Sender has PQ keys: ${senderWallet.pqPrivateKey != null}")
        println("🔐 Recipient has PQ keys: ${recipientWallet.pqPrivateKey != null}")
        
        val message = "Hello, KnishIO Kotlin-to-Kotlin World!"
        
        try {
            // Encrypt with recipient's public key
            println("🔒 Encrypting message: \"$message\"")
            val encrypted = PostQuantumCrypto.encryptMessage(
                message = message,
                recipientPublicKey = recipientWallet.pqPublicKey!!
            )
            
            println("✅ Encryption successful!")
            println("   Encapsulation length: ${encrypted.encapsulation.length}")
            println("   IV length: ${encrypted.iv.length}")
            println("   Ciphertext length: ${encrypted.ciphertext.length}")
            
            // Decrypt with recipient's private key
            println("🔓 Decrypting message...")
            val decrypted = PostQuantumCrypto.decryptMessage(
                encryptedMessage = encrypted,
                privateKey = recipientWallet.pqPrivateKey!!
            )
            
            println("✅ Decryption successful!")
            println("   Decrypted message: \"$decrypted\"")
            
            // Verify messages match
            assertEquals(message, decrypted, "Encrypted and decrypted messages must match")
            
            println("🎉 Kotlin-to-Kotlin ML-KEM encryption test PASSED!")
            
        } catch (e: Exception) {
            println("❌ Test failed: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}