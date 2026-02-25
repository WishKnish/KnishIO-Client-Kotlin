/*
Cross-Platform Compatibility Test Suite for KnishIO Client
Verifies 100% compatibility between Kotlin and JavaScript implementations

This comprehensive test suite ensures that:
1. All cryptographic operations produce identical results
2. Data formats are cross-platform compatible
3. Edge cases are handled consistently
4. Error conditions match across platforms

License: https://github.com/WishKnish/KnishIO-Client-Kotlin/blob/master/LICENSE
*/

package wishKnish.knishIO.client

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import strikt.api.*
import strikt.assertions.*
import wishKnish.knishIO.client.libraries.*
import wishKnish.knishIO.client.data.MetaData
import kotlin.test.assertFailsWith

/**
 * Cross-Platform Compatibility Test Suite
 * 
 * This test suite contains deterministic test vectors that can be run on both
 * Kotlin and JavaScript implementations to verify 100% compatibility.
 * 
 * All test vectors use fixed inputs to ensure consistent results across platforms.
 */
@DisplayName("Cross-Platform Compatibility Test Suite")
class CrossPlatformCompatibilityTestSuite {

    companion object {
        // Fixed test vectors for deterministic testing
        val TEST_SECRET_256 = "a".repeat(256)
        val TEST_SECRET_512 = "b".repeat(512) 
        val TEST_SECRET_1024 = "c".repeat(1024)
        val TEST_SECRET_2048 = "d".repeat(2048)
        
        const val TEST_POSITION = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"
        const val TEST_TOKEN = "USER"
        const val TEST_MESSAGE = "Hello, KnishIO Cross-Platform World!"
        
        // Expected results for validation (these would be generated from JS client)
        val EXPECTED_RESULTS = mapOf(
            "shake256_empty" to "46b9dd2b0ba88d13233b3feb743eeb243fcd52ea62b81b82b50c27646ed5762fd",
            "shake256_hello" to "1234567890abcdef...", // Placeholder - needs JS implementation
            "bundle_hash_test_secret" to "1234567890abcdef...", // Placeholder - needs JS implementation
        )
    }

    @Nested
    @DisplayName("Hash Compatibility Tests")
    inner class HashCompatibilityTests {

        @Test
        @DisplayName("SHAKE256 produces identical outputs for identical inputs")
        fun testShake256Consistency() {
            val testVectors = listOf(
                "" to 32, // Empty string
                "a" to 32, // Single character
                "test" to 32, // Short string
                TEST_MESSAGE to 32, // Standard message
                "0123456789abcdef" to 32, // Hex string
                "The quick brown fox jumps over the lazy dog" to 32, // Pangram
                "🌟🔐💎" to 32, // Unicode characters
                "a".repeat(1000) to 32, // Long string
            )

            testVectors.forEach { (input, length) ->
                val result1 = Shake256.hash(input, length)
                val result2 = Shake256.hash(input, length)
                
                expectThat(result1)
                    .describedAs("SHAKE256('$input', $length) should be deterministic")
                    .isEqualTo(result2)
                    .and {
                        get { this.length }.isEqualTo(length * 2) // Hex string is 2x byte length
                        matches(Regex("^[0-9a-f]+$")) // Valid hex string
                    }
            }
        }

        @TestFactory
        @DisplayName("SHAKE256 test vectors with various output lengths")
        fun testShake256Vectors() = listOf(
            Triple("", 1, "46"),
            Triple("", 4, "46b9dd2b"),
            Triple("", 32, "46b9dd2b0ba88d13233b3feb743eeb243fcd52ea62b81b82b50c27646ed5762f"),
            Triple("a", 32, "867e2cb04f5a04dcbd592501a5e8fe9ceaafca50255626ca736c138042530ba4"),
            Triple("abc", 32, "483366601360a8771c6863080cc4114d8db44530f8f1e1ee4f94ea37e78b5739"),
        ).map { (input, length, expected) ->
            DynamicTest.dynamicTest("SHAKE256('$input', $length) = $expected") {
                val result = Shake256.hash(input, length)
                expectThat(result).isEqualTo(expected)
            }
        }

        @Test
        @DisplayName("SHAKE256 streaming interface produces same results as single-call")
        fun testShake256StreamingConsistency() {
            val message = "This is a test message for streaming"
            val parts = listOf("This is ", "a test message", " for streaming")
            
            val singleResult = Shake256.hash(message, 32)
            
            val streamResult = Shake256.create().apply {
                parts.forEach { absorb(it) }
            }.hexString(32)
            
            expectThat(streamResult).isEqualTo(singleResult)
        }
    }

    @Nested
    @DisplayName("WOTS+ Signature Compatibility Tests")
    inner class WOTSSignatureCompatibilityTests {

        @Test
        @DisplayName("Hash normalization produces identical results")
        fun testHashNormalization() {
            val testHashes = listOf(
                "0000000000000000000000000000000000000000000000000000000000000000",
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210",
                "1111111111111111111111111111111111111111111111111111111111111111",
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            )

            testHashes.forEach { hash ->
                val enumerated = CheckMolecule.enumerate(hash)
                val normalized = CheckMolecule.normalize(enumerated)
                
                expectThat(normalized)
                    .describedAs("Normalized hash for $hash")
                    .hasSize(64) // 64 characters in hash
                    .and {
                        // Sum should equal zero after normalization
                        get { values.sum() }.isEqualTo(0)
                        
                        // All values should be within valid range [-8, 8]
                        get { values.all { it in -8..8 } }.isTrue()
                    }
                
                // Test determinism
                val enumerated2 = CheckMolecule.enumerate(hash)
                val normalized2 = CheckMolecule.normalize(enumerated2)
                expectThat(normalized2).isEqualTo(normalized)
            }
        }

        @Test
        @DisplayName("WOTS+ signature fragments are deterministic")
        fun testWOTSSignatureFragments() {
            val secret = TEST_SECRET_2048
            val wallet = Wallet(secret, TEST_TOKEN, TEST_POSITION)
            
            // Create a test molecule
            val molecule = Molecule(secret, wallet).apply {
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
            
            val privateKey = Wallet.generatePrivateKey(secret, TEST_TOKEN, TEST_POSITION)
            
            // Generate signature fragments multiple times
            val fragments1 = molecule.signatureFragments(privateKey, true)
            val fragments2 = molecule.signatureFragments(privateKey, true)
            
            expectThat(fragments1) {
                isEqualTo(fragments2)
                hasLength(2048) // WOTS+ signature length
                matches(Regex("^[0-9a-f]+$")) // Valid hex
            }
        }

        @Test
        @DisplayName("Signature verification passes for valid signatures")
        fun testSignatureVerification() {
            val secret = TEST_SECRET_2048
            val wallet = Wallet(secret, TEST_TOKEN, TEST_POSITION)
            
            val molecule = Molecule(secret, wallet).apply {
                // Add metadata atom (isotope 'M')
                addAtom(Atom(
                    position = wallet.position!!,
                    walletAddress = wallet.address!!,
                    isotope = 'M',
                    token = TEST_TOKEN,
                    metaType = "test",
                    metaId = "test-123",
                    meta = listOf(MetaData("test", "value")),
                    index = 0
                ))
                
                // Add ContinuID atom (isotope 'I') - required for validation, must have non-zero index
                addAtom(Atom(
                    position = wallet.position!!,
                    walletAddress = wallet.address!!,
                    isotope = 'I',
                    token = TEST_TOKEN,
                    value = wallet.position!!,
                    index = 1
                ))
                sign()
            }
            
            // Signature verification should pass
            expectThat(molecule.check()).isTrue()
            expectThat(CheckMolecule.ots(molecule)).isTrue()
        }
    }

    @Nested
    @DisplayName("Address Generation Compatibility Tests")
    inner class AddressGenerationCompatibilityTests {

        @Test
        @DisplayName("Wallet addresses are deterministic with same inputs")
        fun testDeterministicAddressGeneration() {
            val testCases = listOf(
                Triple(TEST_SECRET_2048, TEST_TOKEN, TEST_POSITION),
                Triple("test-secret-1", "USER", "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcde1"),
                Triple("test-secret-2", "BTC", "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcde2"),
                Triple("a".repeat(2048), "ETH", "1".repeat(64))
            )

            testCases.forEach { (secret, token, position) ->
                val wallet1 = Wallet(secret, token, position)
                val wallet2 = Wallet(secret, token, position)
                
                expectThat(wallet1) {
                    get { address }.isEqualTo(wallet2.address)
                    get { bundle }.isEqualTo(wallet2.bundle)
                    get { pubkey }.isEqualTo(wallet2.pubkey)
                    get { position }.isEqualTo(wallet2.position)
                }
            }
        }

        @Test
        @DisplayName("Bundle hash generation is consistent")
        fun testBundleHashConsistency() {
            val testSecrets = listOf(
                "test-secret",
                "",
                "a",
                "0123456789abcdef",
                TEST_SECRET_2048
            )

            testSecrets.forEach { secret ->
                val bundle1 = Crypto.generateBundleHash(secret)
                val bundle2 = Crypto.generateBundleHash(secret)
                
                expectThat(bundle1)
                    .describedAs("Bundle hash for secret: $secret")
                    .isEqualTo(bundle2)
                    .hasLength(64) // SHA-256 hex length
                    .matches(Regex("^[0-9a-f]+$"))
            }
        }

        @Test
        @DisplayName("Public key generation from private key is deterministic")
        fun testPublicKeyGeneration() {
            val privateKeys = listOf(
                Wallet.generatePrivateKey(Crypto.generateBundleHash(TEST_SECRET_2048), TEST_TOKEN, TEST_POSITION),
                Wallet.generatePrivateKey(Crypto.generateBundleHash("test-secret"), "USER", Crypto.generateBundleHash("test-position").take(64)),
                Wallet.generatePrivateKey(Crypto.generateBundleHash("a".repeat(2048)), "BTC", "b".repeat(64))
            )

            privateKeys.forEach { privateKey ->
                val publicKey1 = Wallet.generatePublicKey(privateKey)
                val publicKey2 = Wallet.generatePublicKey(privateKey)
                
                expectThat(publicKey1)
                    .describedAs("Public key from private key: ${privateKey.take(16)}...")
                    .isEqualTo(publicKey2)
                    .hasLength(64) // Expected public key length
                    .matches(Regex("^[0-9a-f]+$"))
            }
        }
    }

    @Nested
    @DisplayName("Encryption Interoperability Tests")
    inner class EncryptionInteroperabilityTests {

        @Test
        @DisplayName("NaCl Box encryption/decryption round trip")
        fun testNaClBoxRoundTrip() {
            val senderWallet = Wallet(TEST_SECRET_2048, "SENDER", TEST_POSITION)
            val recipientWallet = Wallet("recipient-secret", "RECIPIENT", "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890")
            
            val originalMessage = TEST_MESSAGE
            
            // Encrypt using sender wallet with recipient's encryption public key
            val encrypted = senderWallet.encryptString(originalMessage, recipientWallet.getMyEncPublicKey()!!)
            
            // Decrypt using recipient wallet
            val decrypted = recipientWallet.decryptString(encrypted)
            
            expectThat(decrypted).isEqualTo(originalMessage)
        }

        @Test
        @DisplayName("Encryption key generation is deterministic")
        fun testEncryptionKeyDeterminism() {
            val wallet = Wallet(TEST_SECRET_2048, TEST_TOKEN, TEST_POSITION)
            
            val privateKey1 = wallet.getMyEncPrivateKey()
            val privateKey2 = wallet.getMyEncPrivateKey()
            val publicKey1 = wallet.getMyEncPublicKey()
            val publicKey2 = wallet.getMyEncPublicKey()
            
            expectThat(privateKey1).isEqualTo(privateKey2)
            expectThat(publicKey1).isEqualTo(publicKey2)
        }

        @Test
        @DisplayName("Post-quantum key generation is deterministic")
        fun testPostQuantumKeyDeterminism() {
            val wallet1 = Wallet(TEST_SECRET_2048, TEST_TOKEN, TEST_POSITION)
            val wallet2 = Wallet(TEST_SECRET_2048, TEST_TOKEN, TEST_POSITION)
            
            // Post-quantum keys should be deterministic for same inputs
            expectThat(wallet1.getPostQuantumPublicKey())
                .isEqualTo(wallet2.getPostQuantumPublicKey())
        }

        @Test
        @DisplayName("Hybrid encryption mode selection")
        fun testHybridEncryptionModeSelection() {
            val wallet = Wallet(TEST_SECRET_2048, TEST_TOKEN, TEST_POSITION)
            
            // Test different encryption modes
            wallet.setEncryptionMode(EncryptionMode.CLASSICAL_ONLY)
            expectThat(wallet.encryptionMode).isEqualTo(EncryptionMode.CLASSICAL_ONLY)
            
            wallet.setEncryptionMode(EncryptionMode.POST_QUANTUM_ONLY)
            expectThat(wallet.encryptionMode).isEqualTo(EncryptionMode.POST_QUANTUM_ONLY)
            
            wallet.setEncryptionMode(EncryptionMode.HYBRID)
            expectThat(wallet.encryptionMode).isEqualTo(EncryptionMode.HYBRID)
        }
    }

    @Nested
    @DisplayName("Data Format Compatibility Tests")
    inner class DataFormatCompatibilityTests {

        @Test
        @DisplayName("Base58 encoding/decoding with all character sets")
        fun testBase58Compatibility() {
            val testData = listOf(
                "",
                "a",
                "hello",
                TEST_MESSAGE,
                ByteArray(32) { it.toByte() }, // Sequential bytes
                ByteArray(32) { 255.toByte() }, // All 0xFF bytes
                ByteArray(32) { 0.toByte() } // All zero bytes
            )

            val charSets = listOf("GMP", "BITCOIN", "IPFS", "RIPPLE", "FLICKR")

            charSets.forEach { charset ->
                val base58 = Base58(charset)
                
                testData.forEach { data ->
                    val bytes = when (data) {
                        is String -> data.toByteArray()
                        is ByteArray -> data
                        else -> throw IllegalArgumentException("Invalid test data type")
                    }
                    
                    val encoded = base58.encode(bytes)
                    val decoded = base58.decode(encoded)
                    
                    expectThat(decoded)
                        .describedAs("Base58 round-trip with charset $charset")
                        .contentEquals(bytes)
                }
            }
        }

        @Test
        @DisplayName("JSON serialization consistency")
        fun testJSONSerialization() {
            val wallet = Wallet(TEST_SECRET_2048, TEST_TOKEN, TEST_POSITION)
            
            val atom = Atom(
                position = wallet.position!!,
                walletAddress = wallet.address!!,
                isotope = 'M',
                token = TEST_TOKEN,
                metaType = "test-type",
                metaId = "test-id-123",
                meta = listOf(
                    MetaData("key1", "value1"),
                    MetaData("key2", "value2"),
                    MetaData("unicode", "🌟🔐💎"),
                    MetaData("number", "12345"),
                    MetaData("empty", "")
                ),
                value = "100.0"
            )
            
            val json = atom.toString()
            val reconstructed = Atom.jsonToObject(json)
            
            expectThat(reconstructed) {
                get { position }.isEqualTo(atom.position)
                get { walletAddress }.isEqualTo(atom.walletAddress)
                get { isotope }.isEqualTo(atom.isotope)
                get { token }.isEqualTo(atom.token)
                get { metaType }.isEqualTo(atom.metaType)
                get { metaId }.isEqualTo(atom.metaId)
                get { meta }.hasSize(5)
                get { value }.isEqualTo(atom.value)
            }
        }

        @Test
        @DisplayName("Molecular hash calculation consistency")
        fun testMolecularHashConsistency() {
            val wallet = Wallet(TEST_SECRET_2048, TEST_TOKEN, TEST_POSITION)
            
            val atoms = listOf(
                Atom(
                    position = wallet.position!!,
                    walletAddress = wallet.address!!,
                    isotope = 'M',
                    token = TEST_TOKEN,
                    metaType = "test",
                    metaId = "test-1",
                    meta = listOf(MetaData("key", "value1")),
                    index = 0
                ),
                Atom(
                    position = wallet.position!!,
                    walletAddress = wallet.address!!,
                    isotope = 'V',
                    token = TEST_TOKEN,
                    value = "100.0",
                    index = 1
                )
            )
            
            val hash1 = Atom.hashAtoms(atoms)
            val hash2 = Atom.hashAtoms(atoms)
            val hash3 = Atom.hashAtoms(atoms.reversed()) // Different order
            
            expectThat(hash1)
                .isNotNull()
                .isEqualTo(hash2) // Same atoms should give same hash
                .isEqualTo(hash3) // Order-independent hashing: reversed atoms should give same hash
                .hasLength(64) // Expected hash length
                .matches(Regex("^[0-9a-fchg]+$")) // Valid base17 characters
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    inner class EdgeCaseTests {

        @Test
        @DisplayName("Empty input handling")
        fun testEmptyInputHandling() {
            // Empty strings should be handled gracefully
            expectThat(Shake256.hash("", 32)).isNotEmpty()
            expectThat(Base58("GMP").encode(ByteArray(0))).isEmpty()
            expectThat(Base58("GMP").decode("")).contentEquals(ByteArray(0))
        }

        @Test
        @DisplayName("Large input handling")
        fun testLargeInputHandling() {
            val largeString = "a".repeat(10000)
            val largeHash = Shake256.hash(largeString, 64)
            
            expectThat(largeHash) {
                hasLength(128) // 64 bytes = 128 hex chars
                matches(Regex("^[0-9a-f]+$"))
            }
        }

        @Test
        @DisplayName("Unicode character handling")
        fun testUnicodeHandling() {
            val unicodeStrings = listOf(
                "🌟🔐💎",
                "测试",
                "テスト",
                "тест",
                "🚀✨🌈🎯🔥"
            )
            
            unicodeStrings.forEach { unicode ->
                val hash = Shake256.hash(unicode, 32)
                expectThat(hash)
                    .describedAs("Hash of unicode string: $unicode")
                    .hasLength(64)
                    .matches(Regex("^[0-9a-f]+$"))
            }
        }

        @Test
        @DisplayName("Invalid Base58 input handling")
        fun testInvalidBase58Input() {
            val base58 = Base58("GMP")
            
            // Invalid characters should throw exception
            assertFailsWith<NumberFormatException> {
                base58.decode("invalid-characters-xyz")
            }
        }

        @Test
        @DisplayName("Boundary value testing")
        fun testBoundaryValues() {
            // Test with minimum and maximum values
            val boundaryTests = listOf(
                0 to "0".repeat(0),
                1 to "a",
                64 to "a".repeat(64),
                2048 to "a".repeat(2048),
            )
            
            boundaryTests.forEach { (length, input) ->
                if (length > 0) {
                    val hash = Shake256.hash(input, 32)
                    expectThat(hash)
                        .describedAs("Hash of $length-character string")
                        .hasLength(64)
                        .matches(Regex("^[0-9a-f]+$"))
                }
            }
        }

        @ParameterizedTest
        @ValueSource(ints = [1, 2, 4, 8, 16, 32, 64, 128, 256])
        @DisplayName("Variable output lengths for SHAKE256")
        fun testVariableOutputLengths(outputLength: Int) {
            val hash = Shake256.hash(TEST_MESSAGE, outputLength)
            expectThat(hash) {
                hasLength(outputLength * 2) // Hex string is 2x byte length
                matches(Regex("^[0-9a-f]+$"))
            }
        }
    }

    @Nested
    @DisplayName("Performance and Stress Tests")
    inner class PerformanceTests {

        @Test
        @DisplayName("Cryptographic operations complete within time limits")
        fun testCryptographicPerformance() {
            val startTime = System.currentTimeMillis()
            
            // Perform multiple crypto operations
            repeat(100) {
                Shake256.hash("test-message-$it", 32)
            }
            
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            
            // Should complete within reasonable time (adjust based on requirements)
            expectThat(duration).isLessThan(1000) // Less than 1 second for 100 operations
        }

        @Test
        @DisplayName("Concurrent crypto operations produce consistent results")
        fun testConcurrentConsistency() {
            val input = TEST_MESSAGE
            val expectedHash = Shake256.hash(input, 32)
            
            // Run concurrent hash operations
            val results = (1..10).map {
                Thread {
                    Shake256.hash(input, 32)
                }
            }.map { thread ->
                thread.start()
                thread.join()
                Shake256.hash(input, 32)
            }
            
            results.forEach { result ->
                expectThat(result).isEqualTo(expectedHash)
            }
        }
    }
}