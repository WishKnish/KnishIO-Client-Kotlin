package wishKnish.knishIO.client

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Minimal test to debug serialization step by step
 */
class MinimalCompatibilityTest {

    @Serializable
    data class MinimalTestVectors(
        val testTokens: List<String>,
        val testCellSlugs: List<String?>
    )

    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }

    @Test
    fun testMinimalSerialization() {
        println("🔄 Testing minimal serialization...")
        
        // Use existing test resources
        val resourceStream = javaClass.classLoader.getResourceAsStream("cross-platform-test-vectors.json")
            ?: throw IllegalStateException("Test vectors file not found in resources")
        val jsonContent = resourceStream.bufferedReader().use { it.readText() }
        
        try {
            // Test just the basic fields that should work
            val minimalVectors = json.decodeFromString<MinimalTestVectors>(jsonContent)
            println("✅ Basic fields parsing successful")
            println("Tokens: ${minimalVectors.testTokens}")
            println("Cell Slugs: ${minimalVectors.testCellSlugs}")
            
        } catch (e: Exception) {
            println("❌ Minimal parsing failed: ${e.message}")
            e.printStackTrace()
        }
    }
}