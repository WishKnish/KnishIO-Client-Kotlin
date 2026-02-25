package wishKnish.knishIO.client

import org.junit.jupiter.api.Test
import wishKnish.knishIO.client.libraries.Shake256
import wishKnish.knishIO.client.libraries.Strings

/**
 * JavaScript Hash Reverse Engineering
 * 
 * Attempts to find the input combination that would produce
 * the expected JavaScript molecular hash.
 */
class JavaScriptHashReverse {
    
    @Test
    fun reverseEngineerExpectedHash() {
        println("=".repeat(80))
        println("JAVASCRIPT HASH REVERSE ENGINEERING")
        println("=".repeat(80))
        
        val expectedBase17 = "0457b43d5b0f313bc30b2d931ba39f5badg4a88ca81515e2ag80e0970c3567gb"
        
        // Convert expected base17 back to hex
        val expectedHex = try {
            Strings.charsetBaseConvert(
                expectedBase17, 17, 16, "0123456789abcdefg", "0123456789abcdef"
            )
        } catch (e: Exception) {
            "conversion_failed"
        }
        
        println("\n1. EXPECTED VALUES:")
        println("   Expected base17: $expectedBase17")
        println("   Expected hex: $expectedHex")
        
        // Test various field combinations that might match JavaScript behavior
        val testCombinations = mapOf(
            "Current Kotlin" to listOf(
                "1", 
                "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                "f4d24186c3d02c880f6864dc568623d48919747ebccc4018bb7b64cef5abcc78",
                "C",
                "USER", 
                "1000000",
                "characters",
                "BASE64",
                "1754540720045"
            ),
            
            "Without numberOfAtoms" to listOf(
                "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                "f4d24186c3d02c880f6864dc568623d48919747ebccc4018bb7b64cef5abcc78",
                "C",
                "USER", 
                "1000000",
                "characters",
                "BASE64",
                "1754540720045"
            ),
            
            "Without createdAt" to listOf(
                "1",
                "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                "f4d24186c3d02c880f6864dc568623d48919747ebccc4018bb7b64cef5abcc78",
                "C",
                "USER",
                "1000000",
                "characters",
                "BASE64"
            ),
            
            "Without both" to listOf(
                "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                "f4d24186c3d02c880f6864dc568623d48919747ebccc4018bb7b64cef5abcc78",
                "C",
                "USER",
                "1000000",
                "characters",
                "BASE64"
            ),
            
            "Different value format (int)" to listOf(
                "1",
                "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                "f4d24186c3d02c880f6864dc568623d48919747ebccc4018bb7b64cef5abcc78",
                "C",
                "USER",
                "1000000", // JavaScript might format this as integer
                "characters",
                "BASE64",
                "1754540720045"
            ),
            
            "No metadata at all" to listOf(
                "1",
                "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                "f4d24186c3d02c880f6864dc568623d48919747ebccc4018bb7b64cef5abcc78",
                "C",
                "USER",
                "1000000",
                "1754540720045"
            ),
            
            "Only core fields" to listOf(
                "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                "f4d24186c3d02c880f6864dc568623d48919747ebccc4018bb7b64cef5abcc78",
                "C",
                "USER",
                "1000000"
            ),
            
            "Meta reversed order" to listOf(
                "1",
                "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                "f4d24186c3d02c880f6864dc568623d48919747ebccc4018bb7b64cef5abcc78",
                "C",
                "USER",
                "1000000",
                "BASE64",
                "characters",
                "1754540720045"
            )
        )
        
        println("\n2. TESTING COMBINATIONS:")
        
        testCombinations.forEach { (name, inputs) ->
            val sponge = Shake256.create()
            inputs.forEach { input ->
                sponge.absorb(input)
            }
            val hash = sponge.hexString(32)
            val base17 = Strings.charsetBaseConvert(
                hash, 16, 17, "0123456789abcdef", "0123456789abcdefg"
            ).padStart(64, '0')
            
            val matches = base17 == expectedBase17
            val prefix = if (matches) "*** MATCH *** " else "              "
            
            println("   ${prefix}$name:")
            println("              Inputs: ${inputs.joinToString(", ")}")
            println("              Hash: $hash")
            println("              Base17: $base17")
            if (matches) {
                println("              *** THIS IS THE CORRECT COMBINATION! ***")
            }
            println()
        }
        
        println("\n3. TRYING MORE VARIATIONS:")
        
        // Test with different isotope values (maybe it's not 'C')
        listOf('C', 'V', 'M', 'U', 'I').forEach { isotope ->
            val inputs = listOf(
                "1",
                "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                "f4d24186c3d02c880f6864dc568623d48919747ebccc4018bb7b64cef5abcc78",
                isotope.toString(),
                "USER",
                "1000000",
                "characters",
                "BASE64",
                "1754540720045"
            )
            
            val sponge = Shake256.create()
            inputs.forEach { input ->
                sponge.absorb(input)
            }
            val hash = sponge.hexString(32)
            val base17 = Strings.charsetBaseConvert(
                hash, 16, 17, "0123456789abcdef", "0123456789abcdefg"
            ).padStart(64, '0')
            
            if (base17 == expectedBase17) {
                println("   *** ISOTOPE MATCH *** Isotope '$isotope' produces correct hash!")
            }
        }
        
        println("=".repeat(80))
    }
    
    @Test
    fun testHashFromExpectedBase17() {
        println("=".repeat(80))
        println("BASE17 TO HEX CONVERSION TEST")
        println("=".repeat(80))
        
        val expectedBase17 = "0457b43d5b0f313bc30b2d931ba39f5badg4a88ca81515e2ag80e0970c3567gb"
        
        try {
            // Remove leading zeros for conversion
            val trimmedBase17 = expectedBase17.trimStart('0')
            val hexResult = Strings.charsetBaseConvert(
                trimmedBase17, 17, 16, "0123456789abcdefg", "0123456789abcdef"
            )
            
            println("   Original base17: $expectedBase17")
            println("   Trimmed base17: $trimmedBase17")
            println("   Converted to hex: $hexResult")
            
            // Pad to 64 characters if needed
            val paddedHex = hexResult.padStart(64, '0')
            println("   Padded hex (64 chars): $paddedHex")
            
            // Convert back to verify
            val backToBase17 = Strings.charsetBaseConvert(
                paddedHex, 16, 17, "0123456789abcdef", "0123456789abcdefg"
            ).padStart(64, '0')
            
            println("   Back to base17: $backToBase17")
            println("   Round-trip match: ${backToBase17 == expectedBase17}")
            
        } catch (e: Exception) {
            println("   Conversion error: ${e.message}")
        }
        
        println("=".repeat(80))
    }
}