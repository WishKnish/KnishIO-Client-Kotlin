/*
 * Cross-platform vectors — verifies the Kotlin SDK against the shared
 * cross-platform-test-vectors.json (SHAKE256 / bundle_hash / wallet_generation /
 * molecular_hash / wots_signature, the Rust reference implementation).
 *
 * Sibling of the other SDKs' cross-platform tests (PHP CrossPlatformVectorsTest,
 * Rust cross_platform_vectors.rs, JS cross-platform-canonical.test.js) — one of
 * the two unified cross-SDK vector tests every package shares (the other is
 * PatentVectorValidationTest over canonical-patent-vectors.json).
 */

package wishKnish.knishIO.client

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Assertions.*
import wishKnish.knishIO.client.libraries.Crypto
import wishKnish.knishIO.client.libraries.Shake256

@DisplayName("Cross-Platform Vectors (shared cross-platform-test-vectors.json)")
class CrossPlatformVectorsTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val vectors: JsonObject by lazy {
        val stream = javaClass.classLoader.getResourceAsStream("cross-platform-test-vectors.json")
            ?: throw IllegalStateException("cross-platform-test-vectors.json not found in test resources")
        val content = stream.bufferedReader().use { it.readText() }
        json.parseToJsonElement(content).jsonObject["vectors"]!!.jsonObject
    }

    @TestFactory
    @DisplayName("SHAKE256 Vectors")
    fun shake256Vectors(): List<DynamicTest> {
        val tests = vectors["shake256"]!!.jsonObject["tests"]!!.jsonArray
        return tests.map { element ->
            val test = element.jsonObject
            val name = test["name"]!!.jsonPrimitive.content
            val input = test["input"]!!.jsonPrimitive.content
            val outputLength = test["outputLength"]!!.jsonPrimitive.int
            val expected = test["expected"]!!.jsonPrimitive.content

            DynamicTest.dynamicTest("SHAKE256: $name") {
                val result = Shake256.hash(input, outputLength)
                assertEquals(expected, result, "SHAKE256 mismatch for vector: $name")
            }
        }
    }

    @TestFactory
    @DisplayName("Bundle Hash Vectors")
    fun bundleHashVectors(): List<DynamicTest> {
        val tests = vectors["bundle_hash"]!!.jsonObject["tests"]!!.jsonArray
        return tests.map { element ->
            val test = element.jsonObject
            val name = test["name"]!!.jsonPrimitive.content
            val secret = test["secret"]!!.jsonPrimitive.content
            val expected = test["expected"]!!.jsonPrimitive.content

            DynamicTest.dynamicTest("Bundle hash: $name") {
                val result = Crypto.generateBundleHash(secret)
                assertEquals(expected, result, "Bundle hash mismatch for vector: $name")
            }
        }
    }

    @TestFactory
    @DisplayName("Wallet Address Vectors")
    fun walletAddressVectors(): List<DynamicTest> {
        val tests = vectors["wallet_generation"]!!.jsonObject["tests"]!!.jsonArray
        return tests.map { element ->
            val test = element.jsonObject
            val name = test["name"]!!.jsonPrimitive.content
            val secret = test["secret"]!!.jsonPrimitive.content
            val token = test["token"]!!.jsonPrimitive.content
            val position = test["position"]!!.jsonPrimitive.content
            val expectedBundle = test["expectedBundle"]!!.jsonPrimitive.content
            val expectedAddress = test["expectedAddress"]!!.jsonPrimitive.content

            DynamicTest.dynamicTest("Wallet: $name") {
                // Bundle hash must match
                val bundle = Crypto.generateBundleHash(secret)
                assertEquals(expectedBundle, bundle, "Bundle hash mismatch for wallet: $name")

                // Create wallet with explicit position and check address
                val wallet = Wallet(secret = secret, token = token, position = position)
                assertEquals(expectedAddress, wallet.address, "Wallet address mismatch for wallet: $name")
            }
        }
    }
}
