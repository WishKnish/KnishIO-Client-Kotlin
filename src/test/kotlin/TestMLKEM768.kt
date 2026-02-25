import wishKnish.knishIO.client.*
import wishKnish.knishIO.client.libraries.Crypto
import org.junit.jupiter.api.Test

class TestMLKEM768 {
    
    @Test
    fun testMLKEMKeyGeneration() {
        println("Testing ML-KEM768 key generation...")
        
        // Generate deterministic secret
        val aliceSecret = Crypto.generateSecret("alice-test-seed-2025")
        println("Generated secret: ${aliceSecret.substring(0, 20)}...")
        
        // Create wallet
        val aliceWallet = Wallet(aliceSecret, "TEST")
        
        // Check wallet properties
        println("Wallet key: ${aliceWallet.key?.substring(0, 20) ?: "null"}...")
        println("Wallet address: ${aliceWallet.address?.substring(0, 20) ?: "null"}...")
        println("Wallet bundle: ${aliceWallet.bundle?.substring(0, 20) ?: "null"}...")
        
        // Check post-quantum keys
        println("PQ private key: ${if (aliceWallet.pqPrivateKey != null) "present" else "null"}")
        println("PQ public key: ${if (aliceWallet.pqPublicKey != null) "present" else "null"}")
        println("Wallet pubkey: ${aliceWallet.pubkey?.substring(0, 20) ?: "null"}...")
        
        // Check supported methods
        println("Supported encryption methods: ${aliceWallet.supportedMethods}")
    }
}