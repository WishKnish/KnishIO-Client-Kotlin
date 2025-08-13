package wishKnish.knishIO.client

import org.junit.jupiter.api.Test
import wishKnish.knishIO.client.libraries.Shake256

class GetActualKotlinValues {
    
    @Test
    fun printActualWalletValues() {
        val secret = "d".repeat(2048)
        val position = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"
        val token = "USER"
        
        val wallet = Wallet(secret, token, position)
        
        println("=== ACTUAL KOTLIN SDK VALUES ===")
        println("Secret: $secret")
        println("Position: $position")
        println("Token: $token")
        println("Address: ${wallet.address}")
        println("Bundle: ${wallet.bundle}")
        println("Pubkey: ${wallet.pubkey}")
        println("Key (first 100 chars): ${wallet.key?.take(100)}")
        
        println("\n=== SHAKE256 HASH VALUES ===")
        println("Empty string (32): ${Shake256.hash("", 32)}")
        println("'a' (32): ${Shake256.hash("a", 32)}")
        println("'abc' (32): ${Shake256.hash("abc", 32)}")
    }
}