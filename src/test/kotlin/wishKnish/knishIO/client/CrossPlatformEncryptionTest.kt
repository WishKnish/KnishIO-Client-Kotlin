/**
 * Cross-Platform Wallet Generation Test
 * 
 * Tests that wallets generated in Kotlin match expected values from test vectors
 */

import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeAll
import wishKnish.knishIO.client.*
import kotlin.test.assertTrue

class CrossPlatformEncryptionTest {
    
    companion object {
        private lateinit var testData: JsonObject
        
        @BeforeAll
        @JvmStatic
        fun setupTestData() {
            println("🔐 Starting Cross-Platform Wallet Test...")
            
            // Load test data from resources
            val resourceStream = CrossPlatformEncryptionTest::class.java.classLoader
                .getResourceAsStream("cross-platform-test-vectors-generated.json")
                ?: throw RuntimeException("❌ Test vectors file not found in resources")
            
            val testDataJson = resourceStream.bufferedReader().use { it.readText() }
            testData = Json.parseToJsonElement(testDataJson).jsonObject
            
            println("📊 Test Data Overview:")
            println("   Version: ${testData["version"]?.jsonPrimitive?.content}")
            println("   Description: ${testData["description"]?.jsonPrimitive?.content}")
        }
    }
    
    @Test
    fun testCrossPlatformWalletGeneration() {
        val vectors = testData["vectors"]?.jsonObject
        val walletTests = vectors?.get("wallet_generation")?.jsonObject?.get("tests")?.jsonArray
            ?: JsonArray(emptyList())
        
        if (walletTests.isEmpty()) {
            println("⚠️ No wallet generation tests found")
            return
        }
        
        walletTests.forEach { walletTestJson ->
            val walletTest = walletTestJson.jsonObject
            val testName = walletTest["name"]?.jsonPrimitive?.content ?: "unknown"
            val secret = walletTest["secret"]?.jsonPrimitive?.content ?: ""
            val token = walletTest["token"]?.jsonPrimitive?.content ?: "USER"
            val position = walletTest["position"]?.jsonPrimitive?.content ?: ""
            val expectedBundle = walletTest["expectedBundle"]?.jsonPrimitive?.content
            val expectedAddress = walletTest["expectedAddress"]?.jsonPrimitive?.content
            
            println("\n🧪 Testing wallet: $testName")
            println("   Token: $token")
            println("   Position: ${position.take(16)}...")
            
            // Create Kotlin wallet with same parameters
            val kotlinWallet = Wallet(secret, token, position)
            
            println("   👛 Kotlin wallet created")
            println("      Address: ${kotlinWallet.address}")
            println("      Bundle: ${kotlinWallet.bundle}")
            
            // Validate results
            if (expectedBundle != null) {
                val bundleMatch = kotlinWallet.bundle == expectedBundle
                println("   📊 Bundle validation: ${if (bundleMatch) "✅ PASS" else "❌ FAIL"}")
                assertTrue(bundleMatch, "Bundle should match expected value")
            }
            
            if (expectedAddress != null) {
                val addressMatch = kotlinWallet.address == expectedAddress
                println("   📊 Address validation: ${if (addressMatch) "✅ PASS" else "❌ FAIL"}")
                assertTrue(addressMatch, "Address should match expected value")
            }
        }
        
        println("\n" + "=".repeat(60))
        println("🎉 CROSS-PLATFORM WALLET TEST COMPLETE")
        println("=".repeat(60))
    }
}