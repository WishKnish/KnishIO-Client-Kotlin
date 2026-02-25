import org.junit.jupiter.api.Test
import wishKnish.knishIO.client.Wallet

class AddressDebugTest {
    
    @Test
    fun testSpecificAddressGeneration() {
        // Test specific case mentioned in the issue
        val secret = "d".repeat(2048)
        val token = "USER"
        val position = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"
        
        println("=".repeat(80))
        println("ADDRESS GENERATION DEBUG")
        println("=".repeat(80))
        println("Secret: ${secret.take(20)}... (${secret.length} chars)")
        println("Token: $token")  
        println("Position: $position")
        
        val wallet = Wallet(
            secret = secret,
            token = token,
            position = position
        )
        
        println("\nGenerated values:")
        println("Bundle: ${wallet.bundle}")
        println("Address: ${wallet.address}")
        println("Key: ${wallet.key}")
        
        println("\nExpected values from JavaScript:")
        println("Address: 3aa12567bd4db7b2576e83a3f5f80c7b159b5ac3c58b04fdc4623d75a1406ed8")
        println("Current:  ${wallet.address}")
        
        if (wallet.address == "3aa12567bd4db7b2576e83a3f5f80c7b159b5ac3c58b04fdc4623d75a1406ed8") {
            println("✅ ADDRESSES MATCH!")
        } else {
            println("❌ ADDRESS MISMATCH!")
        }
        
        println("=".repeat(80))
    }
}