/**
 * Cross-Platform Molecule Validator Test
 * 
 * This test validates molecular hash generation using test vectors
 */

import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeAll
import wishKnish.knishIO.client.*
import wishKnish.knishIO.client.libraries.Shake256
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class CrossPlatformMoleculeValidatorTest {
    
    companion object {
        private lateinit var testData: JsonObject
        
        @BeforeAll
        @JvmStatic
        fun setupTestData() {
            println("🔍 Starting Cross-Platform Molecule Validation...")
            
            // Load test data from resources
            val resourceStream = CrossPlatformMoleculeValidatorTest::class.java.classLoader
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
    fun testShake256Hashing() {
        val vectors = testData["vectors"]?.jsonObject
        val shakeTests = vectors?.get("shake256")?.jsonObject?.get("tests")?.jsonArray
            ?: JsonArray(emptyList())
        
        if (shakeTests.isEmpty()) {
            println("⚠️ No SHAKE256 tests found")
            return
        }
        
        shakeTests.forEach { shakeTestJson ->
            val shakeTest = shakeTestJson.jsonObject
            val testName = shakeTest["name"]?.jsonPrimitive?.content ?: "unknown"
            val input = shakeTest["input"]?.jsonPrimitive?.content ?: ""
            val outputLength = shakeTest["outputLength"]?.jsonPrimitive?.int ?: 32
            val expected = shakeTest["expected"]?.jsonPrimitive?.content
            
            println("\n🧪 Testing SHAKE256: $testName")
            println("   Input: \"$input\"")
            println("   Output length: $outputLength bytes")
            
            // Compute SHAKE256 hash
            val result = Shake256.hash(input, outputLength)
            
            println("   Result: $result")
            
            if (expected != null) {
                val match = result == expected
                println("   📊 Validation: ${if (match) "✅ PASS" else "❌ FAIL"}")
                if (!match) {
                    println("      Expected: $expected")
                    println("      Got:      $result")
                }
                assertEquals(expected, result, "SHAKE256 hash should match expected value")
            }
        }
    }
    
    @Test
    fun testBundleHashGeneration() {
        val vectors = testData["vectors"]?.jsonObject
        val bundleTests = vectors?.get("bundle_hash")?.jsonObject?.get("tests")?.jsonArray
            ?: JsonArray(emptyList())
        
        if (bundleTests.isEmpty()) {
            println("⚠️ No bundle hash tests found")
            return
        }
        
        bundleTests.forEach { bundleTestJson ->
            val bundleTest = bundleTestJson.jsonObject
            val testName = bundleTest["name"]?.jsonPrimitive?.content ?: "unknown"
            val secret = bundleTest["secret"]?.jsonPrimitive?.content ?: ""
            val expected = bundleTest["expected"]?.jsonPrimitive?.content
            
            println("\n🧪 Testing Bundle Hash: $testName")
            println("   Secret length: ${secret.length} chars")
            
            // Generate bundle hash
            val result = wishKnish.knishIO.client.libraries.Crypto.generateBundleHash(secret)
            
            println("   Result: $result")
            
            if (expected != null) {
                val match = result == expected
                println("   📊 Validation: ${if (match) "✅ PASS" else "❌ FAIL"}")
                assertEquals(expected, result, "Bundle hash should match expected value")
            }
        }
        
        println("\n" + "=".repeat(60))
        println("🎉 CROSS-PLATFORM MOLECULAR VALIDATION COMPLETE")
        println("=".repeat(60))
    }
}