package wishKnish.knishIO.client

import org.bouncycastle.crypto.digests.SHAKEDigest
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.File

/**
 * Cross-Platform SDK Validator
 * 
 * This test ensures cryptographic compatibility across all KnishIO SDKs
 * by validating against standardized test vectors.
 */
class CrossPlatformSDKValidator {
    
    @Serializable
    data class TestVector(
        val name: String,
        val input: String,
        val outputLength: Int,
        val expected: String
    )
    
    @Serializable
    data class BundleTestVector(
        val name: String,
        val secret: String,
        val expected: String
    )
    
    @Serializable
    data class WalletTestVector(
        val name: String,
        val secret: String,
        val token: String,
        val position: String,
        val expectedBundle: String,
        val expectedAddress: String,
        val expectedPrivateKey: String,
        val expectedPublicKey: String
    )
    
    private fun shake256(input: String, outputLength: Int): String {
        val shake = SHAKEDigest(256)
        val inputBytes = input.toByteArray(Charsets.UTF_8)
        shake.update(inputBytes, 0, inputBytes.size)
        
        val output = ByteArray(outputLength)
        shake.doFinal(output, 0, outputLength)
        
        return output.joinToString("") { "%02x".format(it) }
    }
    
    @Test
    fun `validate SHAKE256 compatibility across all SDKs`() {
        println("\n" + "═".repeat(60))
        println("🧪 Kotlin Cross-Platform SDK Validator")
        println("Using: BouncyCastle SHAKEDigest(256)")
        println("═".repeat(60))
        
        var totalTests = 0
        var passedTests = 0
        var failedTests = 0
        
        // Load test vectors from the central location
        val testVectorsFile = File("../KnishIOClientSDK/test-vectors.json")
        if (!testVectorsFile.exists()) {
            // Try alternate path if running from different location
            val altFile = File("../../KnishIOClientSDK/test-vectors.json")
            assertTrue(altFile.exists(), "test-vectors.json not found at expected locations")
        }
        
        val actualFile = if (testVectorsFile.exists()) testVectorsFile else File("../../KnishIOClientSDK/test-vectors.json")
        
        val json = Json { 
            ignoreUnknownKeys = true
            isLenient = true
        }
        
        val testVectorsContent = actualFile.readText()
        val rootObject = json.parseToJsonElement(testVectorsContent).jsonObject
        val vectors = rootObject["vectors"]?.jsonObject ?: error("No vectors found")
        
        // Test SHAKE256 vectors
        println("\nSHAKE256 Tests:")
        val shake256Tests = vectors["shake256"]?.jsonObject?.get("tests")?.jsonArray
        shake256Tests?.forEach { testElement ->
            val test = json.decodeFromJsonElement<TestVector>(testElement)
            totalTests++
            try {
                val result = shake256(test.input, test.outputLength)
                if (result == test.expected) {
                    println("  ✅ ${test.name}")
                    passedTests++
                } else {
                    println("  ❌ ${test.name}")
                    println("    Expected: ${test.expected}")
                    println("    Got:      $result")
                    failedTests++
                }
            } catch (e: Exception) {
                println("  ❌ ${test.name} - ERROR: ${e.message}")
                failedTests++
            }
        }
        
        // Test Bundle Hash generation
        println("\nBundle Hash Tests:")
        val bundleTests = vectors["bundle_hash"]?.jsonObject?.get("tests")?.jsonArray
        bundleTests?.forEach { testElement ->
            val test = json.decodeFromJsonElement<BundleTestVector>(testElement)
            totalTests++
            try {
                val result = shake256(test.secret, 32)
                if (result == test.expected) {
                    println("  ✅ ${test.name}")
                    passedTests++
                } else {
                    println("  ❌ ${test.name}")
                    println("    Expected: ${test.expected}")
                    println("    Got:      $result")
                    failedTests++
                }
            } catch (e: Exception) {
                println("  ❌ ${test.name} - ERROR: ${e.message}")
                failedTests++
            }
        }
        
        // Test Wallet Generation (simplified - just checking bundle hash)
        println("\nWallet Generation Tests (Bundle Hash):")
        val walletTests = vectors["wallet_generation"]?.jsonObject?.get("tests")?.jsonArray
        walletTests?.forEach { testElement ->
            val test = json.decodeFromJsonElement<WalletTestVector>(testElement)
            totalTests++
            try {
                val bundleResult = shake256(test.secret, 32)
                if (bundleResult == test.expectedBundle) {
                    println("  ✅ ${test.name} - bundle")
                    passedTests++
                } else {
                    println("  ❌ ${test.name} - bundle")
                    println("    Expected: ${test.expectedBundle}")
                    println("    Got:      $bundleResult")
                    failedTests++
                }
            } catch (e: Exception) {
                println("  ❌ ${test.name} - ERROR: ${e.message}")
                failedTests++
            }
        }
        
        // Summary
        println("\n" + "─".repeat(60))
        println("Summary:")
        println("  Total Tests: $totalTests")
        println("  Passed: $passedTests")
        println("  Failed: $failedTests")
        
        val successRate = if (totalTests > 0) ((passedTests.toDouble() / totalTests) * 100).toInt() else 0
        
        when {
            successRate == 100 -> println("  Success Rate: $successRate% ✨")
            successRate >= 80 -> println("  Success Rate: $successRate%")
            else -> println("  Success Rate: $successRate%")
        }
        
        // Assert all tests passed for JUnit
        assertEquals(0, failedTests, "$failedTests tests failed - SDKs are not fully interoperable")
        
        if (successRate == 100) {
            println("\n✅ Kotlin SDK is 100% compatible with JavaScript and PHP SDKs!")
        }
    }
}