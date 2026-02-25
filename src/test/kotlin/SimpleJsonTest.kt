package wishKnish.knishIO.client

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Simple test to debug JSON parsing issues
 */
class SimpleJsonTest {

    @Test
    fun testJsonParsing() {
        println("🔄 Testing JSON Parsing...")
        
        val testVectorsFile = File("/tmp/cross-platform-tests/enhanced-js-test-vectors.json")
        println("File exists: ${testVectorsFile.exists()}")
        println("File size: ${testVectorsFile.length()} bytes")
        
        try {
            val jsonContent = testVectorsFile.readText()
            println("JSON content length: ${jsonContent.length}")
            
            // Try to parse just the basic structure
            val json = Json { 
                ignoreUnknownKeys = true 
                isLenient = true
            }
            
            // Try parsing as JsonElement first
            val jsonElement = json.parseToJsonElement(jsonContent)
            println("✅ JSON parsing as JsonElement successful")
            
        } catch (e: Exception) {
            println("❌ JSON parsing failed: ${e.message}")
            e.printStackTrace()
        }
    }
}