/*
Test Vector Generator for Cross-Platform Compatibility Testing
Generates deterministic test vectors that can be used by both Kotlin and JavaScript implementations

This utility populates the test-vectors.json file with actual computed values
to enable precise cross-platform validation.

License: https://github.com/WishKnish/KnishIO-Client-Kotlin/blob/master/LICENSE
*/

package wishKnish.knishIO.client

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import strikt.api.*
import strikt.assertions.*
import wishKnish.knishIO.client.libraries.*
import wishKnish.knishIO.client.data.MetaData
import java.io.File
import java.io.FileWriter
import com.google.gson.GsonBuilder

/**
 * Test Vector Generator
 * 
 * This class generates deterministic test vectors for cross-platform compatibility testing.
 * It computes actual values for all cryptographic operations and data transformations
 * that need to be consistent between Kotlin and JavaScript implementations.
 */
@DisplayName("Test Vector Generator")
class TestVectorGenerator {

    companion object {
        // Fixed seeds for deterministic generation
        val TEST_SECRET_2048 = "d".repeat(2048)
        const val TEST_POSITION = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"
        const val TEST_TOKEN = "TEST"
        const val TEST_MESSAGE = "Hello, KnishIO Cross-Platform World!"
        const val FIXED_TIMESTAMP = "1640995200000" // Fixed timestamp for deterministic testing
    }

    @Test
    @DisplayName("Generate comprehensive test vectors for cross-platform validation")
    fun generateTestVectors() {
        println("Generating test vectors for cross-platform compatibility validation...")
        
        val testVectors = mutableMapOf<String, Any>()
        
        // Generate SHAKE256 test vectors
        testVectors["shake256"] = generateShake256Vectors()
        
        // Generate bundle hash test vectors  
        testVectors["bundle_hash"] = generateBundleHashVectors()
        
        // Generate wallet generation test vectors
        testVectors["wallet_generation"] = generateWalletVectors()
        
        // Generate hash normalization test vectors
        testVectors["hash_normalization"] = generateHashNormalizationVectors()
        
        // Generate Base58 encoding test vectors
        testVectors["base58_encoding"] = generateBase58Vectors()
        
        // Generate molecular hash test vectors
        testVectors["molecular_hash"] = generateMolecularHashVectors()
        
        // Generate signature test vectors (simplified for now)
        testVectors["wots_signature"] = generateSignatureVectors()
        
        // Generate edge case test vectors
        testVectors["edge_cases"] = generateEdgeCaseVectors()
        
        // Create final test vector structure
        val finalVectors = mapOf(
            "version" to "1.0.0",
            "description" to "Cross-platform compatibility test vectors for KnishIO Kotlin and JavaScript clients",
            "generated" to java.time.Instant.now().toString(),
            "vectors" to testVectors,
            "implementation_notes" to mapOf(
                "kotlin" to mapOf(
                    "shake256" to "Uses BouncyCastle SHAKEDigest(256)",
                    "base58" to "Custom implementation with support for multiple character sets",
                    "wots" to "Custom WOTS+ implementation with hash normalization",
                    "encryption" to "Supports NaCl Box (TweetNacl) and ML-KEM768+AES-GCM (BouncyCastle)"
                ),
                "javascript" to mapOf(
                    "shake256" to "Should use compatible SHAKE256 implementation",
                    "base58" to "Should use same character sets and encoding logic", 
                    "wots" to "Must implement identical hash normalization algorithm",
                    "encryption" to "Should support same encryption modes and formats"
                )
            ),
            "validation_instructions" to mapOf(
                "setup" to listOf(
                    "1. Run this test vector generator in Kotlin to produce actual values",
                    "2. Use the generated test-vectors-generated.json file for validation",
                    "3. Run the JavaScript test suite with the same test vectors",
                    "4. Compare results to verify 100% compatibility"
                ),
                "continuous_validation" to listOf(
                    "1. Any changes to cryptographic functions should regenerate test vectors",
                    "2. Both platforms should run the same test vectors on each release",
                    "3. Failed compatibility tests should block releases until resolved"
                )
            )
        )
        
        // Write to file
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonString = gson.toJson(finalVectors)
        
        val outputFile = File("src/test/resources/cross-platform-test-vectors-generated.json")
        FileWriter(outputFile).use { writer ->
            writer.write(jsonString)
        }
        
        println("Test vectors generated successfully: ${outputFile.absolutePath}")
        println("Total test cases: ${countTestCases(testVectors)}")
        
        // Validate that we can read the generated file
        expectThat(outputFile.exists()).isTrue()
        expectThat(outputFile.readText()).isNotEmpty()
    }

    private fun generateShake256Vectors(): Map<String, Any> {
        val tests = listOf(
            Triple("empty_string_32_bytes", "", 32),
            Triple("single_a_32_bytes", "a", 32),
            Triple("abc_32_bytes", "abc", 32),
            Triple("test_message_32_bytes", TEST_MESSAGE, 32),
            Triple("unicode_message_32_bytes", "🌟🔐💎", 32),
            Triple("long_string_32_bytes", "a".repeat(256), 32),
            Triple("hex_pattern_64_bytes", "0123456789abcdef", 64)
        ).map { (name, input, outputLength) ->
            mapOf(
                "name" to name,
                "input" to input,
                "outputLength" to outputLength,
                "expected" to Shake256.hash(input, outputLength)
            )
        }
        
        return mapOf(
            "description" to "SHAKE256 hash function test vectors with various inputs and output lengths",
            "tests" to tests
        )
    }

    private fun generateBundleHashVectors(): Map<String, Any> {
        val tests = listOf(
            "empty_secret" to "",
            "test_secret" to "test-secret",
            "long_secret_2048" to TEST_SECRET_2048
        ).map { (name, secret) ->
            mapOf(
                "name" to name,
                "secret" to secret,
                "expected" to Crypto.generateBundleHash(secret)
            )
        }
        
        return mapOf(
            "description" to "Bundle hash generation test vectors using SHAKE256",
            "tests" to tests
        )
    }

    private fun generateWalletVectors(): Map<String, Any> {
        val walletTestCases = listOf(
            Triple("standard_wallet", TEST_SECRET_2048, "TEST"),
            Triple("user_wallet", "test-user-secret", "USER"),
            Triple("bitcoin_wallet", "btc-wallet-secret", "BTC")
        )
        
        val tests = walletTestCases.map { (name, secret, token) ->
            val wallet = Wallet(secret, token, TEST_POSITION)
            val privateKey = Wallet.generatePrivateKey(wallet.bundle!!, token, TEST_POSITION)
            
            mapOf(
                "name" to name,
                "secret" to secret,
                "token" to token,
                "position" to TEST_POSITION,
                "expectedBundle" to wallet.bundle,
                "expectedAddress" to wallet.address,
                "expectedPrivateKey" to privateKey,
                "expectedPublicKey" to wallet.pubkey
            )
        }
        
        return mapOf(
            "description" to "Wallet address and key generation test vectors",
            "tests" to tests
        )
    }

    private fun generateHashNormalizationVectors(): Map<String, Any> {
        val hashTestCases = listOf(
            "all_zeros" to "0".repeat(64),
            "all_f" to "f".repeat(64),
            "hex_pattern" to "0123456789abcdef".repeat(4),
            "mixed_pattern" to "deadbeef12345678cafebabe87654321feedface98765432abcdef0123456789".take(64)
        )
        
        val tests = hashTestCases.map { (name, hash) ->
            val enumerated = CheckMolecule.enumerate(hash)
            val normalized = CheckMolecule.normalize(enumerated)
            
            mapOf(
                "name" to name,
                "hash" to hash,
                "expectedEnumerated" to enumerated,
                "expectedNormalized" to normalized
            )
        }
        
        return mapOf(
            "description" to "WOTS+ hash normalization test vectors",
            "tests" to tests
        )
    }

    private fun generateBase58Vectors(): Map<String, Any> {
        val charsets = listOf("GMP", "BITCOIN", "IPFS", "RIPPLE", "FLICKR")
        val testInputs = listOf(
            "" to "",
            "00" to "00",
            "0001" to "0001", 
            "48656c6c6f20576f726c64" to "48656c6c6f20576f726c64", // "Hello World" in hex
            "deadbeef" to "deadbeef",
            "00000000" to "00000000"
        )
        
        val tests = charsets.map { charset ->
            val base58 = Base58(charset)
            val cases = testInputs.map { (name, hexInput) ->
                val bytes = if (hexInput.isEmpty()) ByteArray(0) else hexStringToByteArray(hexInput)
                mapOf(
                    "input" to hexInput,
                    "expected" to base58.encode(bytes)
                )
            }
            
            mapOf(
                "charset" to charset,
                "cases" to cases
            )
        }
        
        return mapOf(
            "description" to "Base58 encoding/decoding test vectors for all character sets",
            "tests" to tests
        )
    }

    private fun generateMolecularHashVectors(): Map<String, Any> {
        val wallet = Wallet(TEST_SECRET_2048, TEST_TOKEN, TEST_POSITION)
        
        // Single atom molecule
        val singleAtom = Atom(
            position = TEST_POSITION,
            walletAddress = wallet.address!!,
            isotope = 'M',
            token = TEST_TOKEN,
            metaType = "test",
            metaId = "test-123",
            meta = listOf(MetaData("test-key", "test-value")),
            index = 0,
            createdAt = FIXED_TIMESTAMP
        )
        
        val singleAtomHash = Atom.hashAtoms(listOf(singleAtom))
        
        // Multi atom molecule
        val wallet2 = Wallet("recipient-secret", TEST_TOKEN, "fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321")
        val multiAtoms = listOf(
            Atom(
                position = TEST_POSITION,
                walletAddress = wallet.address!!,
                isotope = 'V',
                token = TEST_TOKEN,
                value = "-100.0",
                index = 0,
                createdAt = FIXED_TIMESTAMP
            ),
            Atom(
                position = wallet2.position!!,
                walletAddress = wallet2.address!!,
                isotope = 'V',
                token = TEST_TOKEN,
                value = "100.0",
                index = 1,
                createdAt = FIXED_TIMESTAMP
            )
        )
        
        val multiAtomHash = Atom.hashAtoms(multiAtoms)
        
        val tests = listOf(
            mapOf(
                "name" to "single_atom_molecule",
                "atoms" to listOf(atomToMap(singleAtom)),
                "expectedHash" to singleAtomHash
            ),
            mapOf(
                "name" to "multi_atom_molecule",
                "atoms" to multiAtoms.map { atomToMap(it) },
                "expectedHash" to multiAtomHash
            )
        )
        
        return mapOf(
            "description" to "Molecular hash calculation test vectors",
            "tests" to tests
        )
    }

    private fun generateSignatureVectors(): Map<String, Any> {
        val wallet = Wallet(TEST_SECRET_2048, TEST_TOKEN, TEST_POSITION)
        val molecule = Molecule(TEST_SECRET_2048, wallet).apply {
            addAtom(Atom(
                position = wallet.position!!,
                walletAddress = wallet.address!!,
                isotope = 'M',
                token = TEST_TOKEN,
                metaType = "test",
                metaId = "test-123",
                meta = listOf(MetaData("test", "value"))
            ))
            molecularHash = Atom.hashAtoms(atoms)
        }
        
        val privateKey = Wallet.generatePrivateKey(TEST_SECRET_2048, TEST_TOKEN, TEST_POSITION)
        val signatureFragments = molecule.signatureFragments(privateKey, false)
        val compressedSignature = molecule.signatureFragments(privateKey, true)
        
        val tests = listOf(
            mapOf(
                "name" to "standard_signature",
                "privateKey" to privateKey,
                "molecularHash" to molecule.molecularHash,
                "expectedSignatureFragments" to signatureFragments,
                "expectedCompressedSignature" to compressedSignature
            )
        )
        
        return mapOf(
            "description" to "WOTS+ signature generation and verification test vectors",
            "tests" to tests
        )
    }

    private fun generateEdgeCaseVectors(): Map<String, Any> {
        val unicodeInputs = listOf("🌟🔐💎", "测试", "テスト", "тест", "🚀✨🌈🎯🔥")
        val unicodeHashes = unicodeInputs.map { Shake256.hash(it, 32) }
        
        val largeInput = "a".repeat(10000)
        val largeHash = Shake256.hash(largeInput, 32)
        
        val boundaryCases = listOf(
            Triple("Single character", "a", Shake256.hash("a", 32)),
            Triple("64 characters", "a".repeat(64), Shake256.hash("a".repeat(64), 32)),
            Triple("2048 characters", "a".repeat(2048), Shake256.hash("a".repeat(2048), 32))
        )
        
        val tests = mapOf(
            "unicode_handling" to mapOf(
                "inputs" to unicodeInputs,
                "expectedHashes" to unicodeHashes
            ),
            "large_inputs" to mapOf(
                "input" to largeInput.take(100) + "...[${largeInput.length} total chars]",
                "inputLength" to largeInput.length,
                "expectedHash" to largeHash
            ),
            "boundary_values" to mapOf(
                "cases" to boundaryCases.map { (desc, input, hash) ->
                    mapOf(
                        "description" to desc,
                        "input" to input,
                        "inputLength" to input.length,
                        "expectedHash" to hash
                    )
                }
            )
        )
        
        return mapOf(
            "description" to "Edge case test vectors",
            "tests" to tests
        )
    }

    // Helper functions
    private fun hexStringToByteArray(hex: String): ByteArray {
        if (hex.isEmpty()) return ByteArray(0)
        val len = hex.length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
        }
        return data
    }

    private fun atomToMap(atom: Atom): Map<String, Any?> {
        return mapOf(
            "position" to atom.position,
            "walletAddress" to atom.walletAddress,
            "isotope" to atom.isotope.toString(),
            "token" to atom.token,
            "value" to atom.value,
            "batchId" to atom.batchId,
            "metaType" to atom.metaType,
            "metaId" to atom.metaId,
            "meta" to atom.meta.map { mapOf("key" to it.key, "value" to it.value) },
            "index" to atom.index,
            "createdAt" to atom.createdAt
        )
    }

    private fun countTestCases(testVectors: Map<String, Any>): Int {
        var count = 0
        
        fun countRecursively(obj: Any) {
            when (obj) {
                is Map<*, *> -> {
                    if (obj.containsKey("tests")) {
                        val tests = obj["tests"]
                        if (tests is List<*>) {
                            count += tests.size
                        }
                    }
                    obj.values.forEach { countRecursively(it ?: return@forEach) }
                }
                is List<*> -> obj.forEach { countRecursively(it ?: return@forEach) }
            }
        }
        
        countRecursively(testVectors)
        return count
    }
}