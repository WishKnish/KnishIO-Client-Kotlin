package wishKnish.knishIO.client

import org.junit.jupiter.api.Test
import wishKnish.knishIO.client.libraries.Shake256
import wishKnish.knishIO.client.libraries.Strings
import org.bouncycastle.crypto.digests.SHAKEDigest
import org.bouncycastle.util.encoders.Hex

/**
 * SHAKE256 Compatibility Test
 * 
 * This test isolates SHAKE256 and Base17 encoding to identify
 * the source of cross-platform hash differences.
 */
class Shake256CompatibilityTest {
    
    @Test
    fun testShake256Absorption() {
        println("=".repeat(80))
        println("SHAKE256 COMPATIBILITY TEST")
        println("=".repeat(80))
        
        println("\n1. TESTING INDIVIDUAL FIELD ABSORPTION:")
        
        // Test each field individually
        val testInputs = listOf(
            "1" to "numberOfAtoms",
            "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef" to "position",
            "f4d24186c3d02c880f6864dc568623d48919747ebccc4018bb7b64cef5abcc78" to "walletAddress",
            "C" to "isotope",
            "USER" to "token",
            "1000000" to "value",
            "characters" to "meta.key",
            "BASE64" to "meta.value",
            "1754540720045" to "createdAt"
        )
        
        testInputs.forEach { (input, description) ->
            val sponge = Shake256.create()
            sponge.absorb(input)
            val hash = sponge.hexString(32)
            val base17 = Strings.charsetBaseConvert(
                hash, 16, 17, "0123456789abcdef", "0123456789abcdefg"
            )
            println("   $description: '$input' -> $hash -> $base17")
        }
        
        println("\n2. TESTING COMBINED ABSORPTION (KOTLIN ORDER):")
        
        // Test with the exact sequence from our debug log
        val combinedSponge = Shake256.create()
        testInputs.forEach { (input, _) ->
            combinedSponge.absorb(input)
        }
        val combinedHash = combinedSponge.hexString(32)
        val combinedBase17 = Strings.charsetBaseConvert(
            combinedHash, 16, 17, "0123456789abcdef", "0123456789abcdefg"
        ).padStart(64, '0')
        
        println("   Combined hash (hex): $combinedHash")
        println("   Combined hash (base17): $combinedBase17")
        println("   Expected (JS): 0457b43d5b0f313bc30b2d931ba39f5badg4a88ca81515e2ag80e0970c3567gb")
        
        println("\n3. TESTING ALTERNATIVE FIELD ORDERS:")
        
        // Test without createdAt (in case JS doesn't include it)
        val noCreatedAtSponge = Shake256.create()
        testInputs.dropLast(1).forEach { (input, _) ->
            noCreatedAtSponge.absorb(input)
        }
        val noCreatedAtHash = noCreatedAtSponge.hexString(32)
        val noCreatedAtBase17 = Strings.charsetBaseConvert(
            noCreatedAtHash, 16, 17, "0123456789abcdef", "0123456789abcdefg"
        ).padStart(64, '0')
        
        println("   Without createdAt (hex): $noCreatedAtHash")
        println("   Without createdAt (base17): $noCreatedAtBase17")
        
        // Test with different metadata order (key after value)
        val altMetaOrder = listOf(
            "1" to "numberOfAtoms",
            "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef" to "position",
            "f4d24186c3d02c880f6864dc568623d48919747ebccc4018bb7b64cef5abcc78" to "walletAddress",
            "C" to "isotope",
            "USER" to "token",
            "1000000" to "value",
            "BASE64" to "meta.value", // Value first
            "characters" to "meta.key", // Key second
            "1754540720045" to "createdAt"
        )
        
        val altMetaSponge = Shake256.create()
        altMetaOrder.forEach { (input, _) ->
            altMetaSponge.absorb(input)
        }
        val altMetaHash = altMetaSponge.hexString(32)
        val altMetaBase17 = Strings.charsetBaseConvert(
            altMetaHash, 16, 17, "0123456789abcdef", "0123456789abcdefg"
        ).padStart(64, '0')
        
        println("   Alt meta order (hex): $altMetaHash")
        println("   Alt meta order (base17): $altMetaBase17")
        
        println("\n4. TESTING BASE17 ENCODING:")
        
        // Test Base17 encoding with known hex values
        val testHex = "c2eaff746c71e132c9db3f7dd0873639df4c9adb43a2d2d87eacd3f1cec0aabc"
        val base17Result = Strings.charsetBaseConvert(
            testHex, 16, 17, "0123456789abcdef", "0123456789abcdefg"
        ).padStart(64, '0')
        
        println("   Test hex: $testHex")
        println("   Base17: $base17Result")
        
        println("\n5. TESTING DIFFERENT ABSORPTION PATTERNS:")
        
        // Test absorbing as single concatenated string
        val concatenated = testInputs.joinToString("") { it.first }
        val concatSponge = Shake256.create()
        concatSponge.absorb(concatenated)
        val concatHash = concatSponge.hexString(32)
        val concatBase17 = Strings.charsetBaseConvert(
            concatHash, 16, 17, "0123456789abcdef", "0123456789abcdefg"
        ).padStart(64, '0')
        
        println("   Concatenated: '$concatenated'")
        println("   Concat hash (hex): $concatHash")
        println("   Concat hash (base17): $concatBase17")
        
        println("=".repeat(80))
    }
    
    @Test
    fun testDirectBouncecastleShake256() {
        println("=".repeat(80))
        println("DIRECT BOUNCYCASTLE SHAKE256 TEST")
        println("=".repeat(80))
        
        // Test with raw BouncyCastle to ensure our implementation is correct
        val shake = SHAKEDigest(256)
        
        val inputs = listOf(
            "1",
            "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
            "f4d24186c3d02c880f6864dc568623d48919747ebccc4018bb7b64cef5abcc78",
            "C",
            "USER",
            "1000000",
            "characters",
            "BASE64",
            "1754540720045"
        )
        
        inputs.forEach { input ->
            val bytes = input.toByteArray()
            shake.update(bytes, 0, bytes.size)
        }
        
        val output = ByteArray(32)
        shake.doFinal(output, 0, 32)
        val hexResult = Hex.toHexString(output)
        
        val base17Result = Strings.charsetBaseConvert(
            hexResult, 16, 17, "0123456789abcdef", "0123456789abcdefg"
        ).padStart(64, '0')
        
        println("   Raw BouncyCastle (hex): $hexResult")
        println("   Raw BouncyCastle (base17): $base17Result")
        println("   Expected (JS): 0457b43d5b0f313bc30b2d931ba39f5badg4a88ca81515e2ag80e0970c3567gb")
        
        println("=".repeat(80))
    }
}