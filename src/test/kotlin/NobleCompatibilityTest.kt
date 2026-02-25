package wishKnish.knishIO.client

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import wishKnish.knishIO.client.libraries.PostQuantumCrypto
import wishKnish.knishIO.client.libraries.Shake256
import org.bouncycastle.util.encoders.Hex
import org.bouncycastle.util.encoders.Base64

/**
 * Test cross-platform compatibility with JavaScript @noble/post-quantum
 */
class NobleCompatibilityTest {
    
    @Test
    fun testMLKEMKeyGenerationCompatibility() {
        // Test data that matches JavaScript test
        val secret = "d".repeat(2048)
        val token = "USER"
        
        println("=".repeat(80))
        println("ML-KEM CROSS-PLATFORM COMPATIBILITY TEST")
        println("=".repeat(80))
        
        // Generate ML-KEM seed the same way as JavaScript
        val pqSeedHex = Shake256.hash(secret, 64) // 64 bytes = 128 hex chars
        println("\n1. ML-KEM Seed Generation:")
        println("   Seed (hex): $pqSeedHex")
        println("   Seed length: ${pqSeedHex.length} chars (${pqSeedHex.length / 2} bytes)")
        
        // Generate ML-KEM key pair using NobleMLKEMBridge
        val pqSeed = Hex.decode(pqSeedHex)
        val keyPair = PostQuantumCrypto.generateMLKEMKeyPairFromSeed(pqSeed)
        
        println("\n2. ML-KEM Key Generation:")
        println("   Public key size: ${keyPair.public.encoded.size} bytes")
        println("   Private key size: ${keyPair.private.encoded.size} bytes")
        
        // Extract raw public key for comparison
        val rawPublicKey = PostQuantumCrypto.extractRawMLKEMPublicKey(keyPair.public)
        println("   Raw public key size: ${rawPublicKey.size} bytes")
        
        // Convert to base64 for comparison with JavaScript
        val publicKeyBase64 = Base64.toBase64String(rawPublicKey)
        println("\n3. Public Key (Base64):")
        println("   First 50 chars: ${publicKeyBase64.take(50)}...")
        println("   Last 50 chars: ...${publicKeyBase64.takeLast(50)}")
        
        // Expected values from JavaScript (you would get these from running the JS test)
        // For now, we just verify the key sizes are correct
        assertEquals(1184, rawPublicKey.size, "Raw ML-KEM768 public key should be 1184 bytes")
        
        // Test encapsulation/decapsulation roundtrip
        println("\n4. Testing Encapsulation/Decapsulation:")
        val testMessage = "Test message for encryption"
        val encryptedMessage = PostQuantumCrypto.encryptMessage(testMessage, keyPair.public)
        val decryptedMessage = PostQuantumCrypto.decryptMessage(encryptedMessage, keyPair.private)
        
        assertEquals(testMessage, decryptedMessage, "Decrypted message should match original")
        println("   ✅ Encryption/decryption roundtrip successful")
        
        println("\n" + "=".repeat(80))
        println("SUMMARY:")
        println("  ✅ ML-KEM key generation working with NobleMLKEMBridge")
        println("  ✅ Correct key sizes (1184 bytes public, as per FIPS-203)")
        println("  ✅ Encryption/decryption working correctly")
        println("  🔍 To verify full compatibility, compare public key with JavaScript output")
        println("=".repeat(80))
    }
    
    @Test
    fun testWalletWithNobleMLKEM() {
        // Test wallet generation with Noble ML-KEM keys
        val secret = "d".repeat(2048)
        val token = "USER"
        val position = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"
        
        println("\n" + "=".repeat(80))
        println("WALLET WITH NOBLE ML-KEM TEST")
        println("=".repeat(80))
        
        val wallet = Wallet(
            secret = secret,
            token = token,
            position = position
        )
        
        println("\n1. Wallet Details:")
        println("   Address: ${wallet.address}")
        println("   Bundle: ${wallet.bundle}")
        println("   Position: ${wallet.position}")
        
        // Check if pubkey is set (should be base64 encoded ML-KEM public key)
        assertNotNull(wallet.pubkey, "Wallet should have ML-KEM public key")
        println("\n2. ML-KEM Public Key:")
        println("   Pubkey (first 50 chars): ${wallet.pubkey?.take(50)}...")
        println("   Pubkey length: ${wallet.pubkey?.length} chars")
        
        // Decode and check size
        val pubkeyBytes = Base64.decode(wallet.pubkey)
        assertEquals(1184, pubkeyBytes.size, "Decoded pubkey should be 1184 bytes")
        println("   Decoded size: ${pubkeyBytes.size} bytes ✅")
        
        println("\n" + "=".repeat(80))
    }
}