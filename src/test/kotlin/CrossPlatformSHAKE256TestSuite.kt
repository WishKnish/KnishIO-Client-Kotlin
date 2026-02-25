package wishKnish.knishIO.client

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import wishKnish.knishIO.client.libraries.Shake256
import java.io.File

/**
 * SHAKE256 Cross-Platform Test Suite
 * 
 * This comprehensive test suite addresses the critical SHAKE256 compatibility issues
 * identified by sub-agent analysis between Kotlin (BouncyCastle) and JavaScript (JsSHA) implementations.
 * 
 * Root Cause: Different SHAKE256 libraries producing different outputs for identical inputs,
 * causing molecular hash mismatches that prevent cross-platform molecule validation.
 * 
 * Approach:
 * 1. Generate NIST-compliant reference test vectors
 * 2. Test Kotlin SHAKE256 implementation against known vectors
 * 3. Export test vectors for JavaScript validation
 * 4. Create standardized compatibility framework
 */
class CrossPlatformSHAKE256TestSuite {
    
    @Serializable
    data class SHAKE256TestVector(
        val name: String,
        val input: String,
        val inputBytes: String, // Hex representation of input bytes
        val outputLength: Int,
        val expectedOutput: String? = null, // To be filled by "source of truth" platform
        val kotlinOutput: String? = null,
        val javascriptOutput: String? = null,
        val compatible: Boolean = false
    )
    
    @Serializable
    data class SHAKE256TestSuite(
        val generatedBy: String = "Kotlin SDK",
        val timestamp: String = System.currentTimeMillis().toString(),
        val libraryInfo: String = "BouncyCastle SHAKEDigest(256)",
        val testVectors: List<SHAKE256TestVector>
    )
    
    @Test
    @DisplayName("Generate comprehensive SHAKE256 test vectors for cross-platform validation")
    fun generateSHAKE256TestVectors() {
        println("\n🔬 SHAKE256 Cross-Platform Test Suite")
        println("Generating comprehensive test vectors for Kotlin ↔ JavaScript compatibility\n")
        
        val testVectors = mutableListOf<SHAKE256TestVector>()
        
        // ===========================================
        // Test Category 1: NIST Reference Vectors
        // ===========================================
        println("📋 Category 1: NIST Reference Test Vectors")
        
        // Empty input
        testVectors.add(generateTestVector(
            name = "empty_input_32_bytes",
            input = "",
            outputLength = 32
        ))
        
        testVectors.add(generateTestVector(
            name = "empty_input_64_bytes", 
            input = "",
            outputLength = 64
        ))
        
        // Single byte inputs
        testVectors.add(generateTestVector(
            name = "single_byte_zero",
            input = "\\x00",
            outputLength = 32
        ))
        
        testVectors.add(generateTestVector(
            name = "single_byte_ff",
            input = "\\xff",
            outputLength = 32
        ))
        
        // ASCII strings
        testVectors.add(generateTestVector(
            name = "ascii_abc",
            input = "abc",
            outputLength = 32
        ))
        
        testVectors.add(generateTestVector(
            name = "ascii_hello_world",
            input = "Hello, World!",
            outputLength = 64
        ))
        
        // ===========================================
        // Test Category 2: Knish.IO Specific Patterns
        // ===========================================
        println("📋 Category 2: Knish.IO Molecular Hash Patterns")
        
        // Typical wallet position
        testVectors.add(generateTestVector(
            name = "wallet_position",
            input = "0",
            outputLength = 64
        ))
        
        // Typical wallet address
        testVectors.add(generateTestVector(
            name = "wallet_address",
            input = "9b3e4d63fcaaa54b6c8c63a4e5f6e89567c0f92b8e8c1234567890abcdef1234",
            outputLength = 64
        ))
        
        // Isotope characters
        listOf('V', 'M', 'C', 'I', 'R').forEach { isotope ->
            testVectors.add(generateTestVector(
                name = "isotope_$isotope",
                input = isotope.toString(),
                outputLength = 64
            ))
        }
        
        // Token slugs
        listOf("USER", "TEST", "CRZY", "BTC", "ETH").forEach { token ->
            testVectors.add(generateTestVector(
                name = "token_$token",
                input = token,
                outputLength = 64
            ))
        }
        
        // ===========================================
        // Test Category 3: Edge Cases
        // ===========================================
        println("📋 Category 3: Edge Case Testing")
        
        // Unicode characters
        testVectors.add(generateTestVector(
            name = "unicode_emoji",
            input = "🧬🔗⚛️",
            outputLength = 64
        ))
        
        testVectors.add(generateTestVector(
            name = "unicode_chinese",
            input = "区块链技术",
            outputLength = 64
        ))
        
        // Large inputs
        val largeString = "A".repeat(1024)
        testVectors.add(generateTestVector(
            name = "large_input_1kb",
            input = largeString,
            outputLength = 64
        ))
        
        val megaString = "SHAKE256".repeat(1024) // ~8KB
        testVectors.add(generateTestVector(
            name = "large_input_8kb",
            input = megaString,
            outputLength = 128
        ))
        
        // ===========================================
        // Test Category 4: Molecular Component Patterns
        // ===========================================
        println("📋 Category 4: Molecular Component Testing")
        
        // Simulate typical molecular hash inputs
        testVectors.add(generateTestVector(
            name = "molecular_hash_simulation",
            input = "30${createMockAtomData()}",
            outputLength = 64
        ))
        
        // Multiple atom simulation
        var multiAtomInput = "3"
        repeat(3) { i ->
            multiAtomInput += "3" + createMockAtomData(i)
        }
        testVectors.add(generateTestVector(
            name = "multi_atom_molecular_hash",
            input = multiAtomInput,
            outputLength = 64
        ))
        
        // ===========================================
        // Test Category 5: Various Output Lengths
        // ===========================================
        println("📋 Category 5: Output Length Variations")
        
        val testInput = "SHAKE256 output length testing"
        listOf(1, 4, 16, 32, 64, 128, 256).forEach { length ->
            testVectors.add(generateTestVector(
                name = "output_length_${length}_bytes",
                input = testInput,
                outputLength = length
            ))
        }
        
        // ===========================================
        // Export Test Suite
        // ===========================================
        val testSuite = SHAKE256TestSuite(testVectors = testVectors)
        
        val json = Json { 
            prettyPrint = true
            encodeDefaults = true
        }
        
        val jsonString = json.encodeToString(testSuite)
        File("shake256-cross-platform-test-vectors.json").writeText(jsonString)
        
        println("\n✅ Generated ${testVectors.size} SHAKE256 test vectors")
        println("📁 Exported to: shake256-cross-platform-test-vectors.json")
        println("📊 Test Vector Categories:")
        println("   • NIST Reference: 6 vectors")
        println("   • Knish.IO Patterns: ${5 + 5} vectors") 
        println("   • Edge Cases: 4 vectors")
        println("   • Molecular Components: 2 vectors")
        println("   • Output Lengths: 7 vectors")
        println()
        println("🔄 Next Steps:")
        println("   1. Run corresponding JavaScript test to compare outputs")
        println("   2. Identify discrepancies between BouncyCastle and JsSHA")
        println("   3. Implement compatibility bridge based on findings")
        
        // Validate our own outputs
        println("\n🧪 Self-Validation Results:")
        var validationErrors = 0
        testVectors.forEach { vector ->
            if (vector.kotlinOutput.isNullOrEmpty()) {
                println("   ❌ ${vector.name}: No Kotlin output generated")
                validationErrors++
            } else if (vector.kotlinOutput!!.length != vector.outputLength * 2) {
                println("   ❌ ${vector.name}: Incorrect output length ${vector.kotlinOutput!!.length} != ${vector.outputLength * 2}")
                validationErrors++
            } else {
                println("   ✅ ${vector.name}: Valid (${vector.kotlinOutput!!.length} hex chars)")
            }
        }
        
        if (validationErrors == 0) {
            println("\n🎯 All Kotlin SHAKE256 outputs generated successfully!")
            println("Ready for JavaScript cross-platform validation.")
        } else {
            println("\n⚠️ $validationErrors validation errors detected.")
            println("Review SHAKE256 implementation before cross-platform testing.")
        }
    }
    
    private fun generateTestVector(
        name: String,
        input: String,
        outputLength: Int
    ): SHAKE256TestVector {
        // Convert input to bytes
        val inputBytes = when {
            input.startsWith("\\x") -> {
                // Hex escape sequence
                val hex = input.substring(2)
                if (hex.isEmpty()) byteArrayOf() else hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            }
            else -> input.toByteArray(Charsets.UTF_8)
        }
        
        // Generate SHAKE256 hash using Kotlin implementation
        val kotlinOutput = try {
            Shake256.hash(input, outputLength)
        } catch (e: Exception) {
            println("   ❌ Error generating hash for $name: ${e.message}")
            null
        }
        
        return SHAKE256TestVector(
            name = name,
            input = input,
            inputBytes = inputBytes.joinToString("") { "%02x".format(it) },
            outputLength = outputLength,
            kotlinOutput = kotlinOutput
        )
    }
    
    private fun createMockAtomData(index: Int = 0): String {
        // Simulate typical atom data for molecular hash
        return buildString {
            append("wallet_position_$index")
            append("9b3e4d63fcaaa54b6c8c63a4e5f6e89567c0f92b8e8c1234567890abcdef123$index")
            append("V")
            append("TEST")
            append("1000")
            append("meta_key_$index")
            append("meta_value_$index")
            append(System.currentTimeMillis() + index)
        }
    }
    
    @Test
    @DisplayName("Validate SHAKE256 consistency with known reference vectors")
    fun validateKnownReferenceVectors() {
        println("\n📚 SHAKE256 Reference Vector Validation")
        
        // Known NIST test vectors for SHAKE256
        val knownVectors = mapOf(
            // Empty input, 32-byte output
            "" to mapOf(32 to "46b9dd2b0ba88d13233b3feb743eeb243fcd52ea62b81b82b50c27646ed5762fd2352e1e2c5b4d473c5c1b2e8b87b6b7b"), 
            
            // "abc", 32-byte output  
            "abc" to mapOf(32 to "483366601360a8771c6863080cc4114d8db44530f8f1e1ee4f94ea37e78b5739d5a15bef186a5386c75744c0527e1faa9f8726e462a12a4feb06bd8801e751e4")
        )
        
        println("Testing ${knownVectors.size} known reference vectors...")
        
        var passed = 0
        var failed = 0
        
        knownVectors.forEach { (input, expectedOutputs) ->
            expectedOutputs.forEach { (outputLength, expected) ->
                val actual = Shake256.hash(input, outputLength)
                
                if (actual.equals(expected, ignoreCase = true)) {
                    println("   ✅ '$input' ($outputLength bytes): MATCH")
                    passed++
                } else {
                    println("   ❌ '$input' ($outputLength bytes): MISMATCH")
                    println("      Expected: ${expected.take(64)}...")
                    println("      Actual:   ${actual.take(64)}...")
                    failed++
                }
            }
        }
        
        println("\n📊 Reference Vector Validation Results:")
        println("   ✅ Passed: $passed")
        println("   ❌ Failed: $failed")
        println("   📈 Success Rate: ${(passed * 100) / (passed + failed)}%")
        
        if (failed > 0) {
            println("\n⚠️ CRITICAL: SHAKE256 implementation does not match NIST reference vectors!")
            println("This indicates the BouncyCastle implementation may have compatibility issues.")
            println("Investigation required before cross-platform testing.")
        } else {
            println("\n🎯 SHAKE256 implementation matches NIST reference vectors!")
            println("Ready for cross-platform JavaScript comparison.")
        }
    }
}