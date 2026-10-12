/*
 * OTS v2 cross-SDK vectors: key derivation, address, the normalized hash, ML-KEM identities
 * of v2 wallets, molecule verdicts and the malformed declared-hash check.
 *
 * Reads the shared master ../shared-test-results/ots-v2-vectors.json (relative to the SDK root).
 * A standalone checkout has no master; the vector suite then registers one visible skip.
 * The v2 sign/verify round trip below always runs.
 */

package wishKnish.knishIO.client

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import wishKnish.knishIO.client.data.MetaData
import wishKnish.knishIO.client.exception.SignatureMalformedException
import wishKnish.knishIO.client.exception.SignatureMismatchException
import wishKnish.knishIO.client.libraries.CheckMolecule
import java.io.File

@DisplayName("OTS v2")
class OtsV2VectorsTest {

  private val json = Json { ignoreUnknownKeys = true }

  private val vectorsFile = File("../shared-test-results/ots-v2-vectors.json")

  private val exceptions = mapOf(
    "SignatureMismatch" to SignatureMismatchException::class.java,
    "SignatureMalformed" to SignatureMalformedException::class.java
  )

  private fun JsonObject.str(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

  private fun tests(vectors: JsonObject, family: String) =
    vectors[family]!!.jsonObject["tests"]!!.jsonArray.map { it.jsonObject }

  /** Rebuilds a molecule verbatim from vector JSON; the source wallet comes from the signing atom. */
  private fun reconstruct(data: JsonObject, secret: String? = null, sourceWallet: Wallet? = null): Molecule {
    val atoms = data["atoms"]!!.jsonArray.map { Atom.fromJSON(it.toString()) }
    val first = atoms.first()
    val source = sourceWallet ?: Wallet(secret = null, token = first.token, position = first.position).apply {
      address = first.walletAddress
    }
    return Molecule(secret = secret, sourceWallet = source, cellSlug = data.str("cellSlug")).apply {
      molecularHash = data.str("molecularHash")
      bundle = data.str("bundle")
      data.str("createdAt")?.let { createdAt = it }
      this.atoms.clear()
      this.atoms.addAll(atoms)
    }
  }

  @TestFactory
  @DisplayName("shared ots-v2-vectors.json")
  fun vectors(): List<DynamicNode> {
    assumeTrue(vectorsFile.isFile, "skipped: ../shared-test-results/ots-v2-vectors.json not found (standalone checkout)")
    val vectors = json.parseToJsonElement(vectorsFile.readText()).jsonObject["vectors"]!!.jsonObject
    val keyDerivation = tests(vectors, "key_derivation")
    val keyFrom = { name: String -> keyDerivation.first { it.str("name") == name } }

    val keys = tests(vectors, "key_derivation").map { v ->
      DynamicTest.dynamicTest(v.str("name")!!) {
        val secret = v.str("secret")!!
        val token = v.str("token")!!
        val position = v.str("position")!!
        assertEquals(v.str("expectedKeyV1"), Wallet.generatePrivateKey(secret, token, position))
        assertEquals(v.str("expectedKeyV2"), Wallet.generatePrivateKey(secret, token, position, 2))
        assertEquals(v.str("expectedAddressV1"), Wallet.generatePublicKey(v.str("expectedKeyV1")!!))
        assertEquals(v.str("expectedAddressV2"), Wallet.generatePublicKey(v.str("expectedKeyV2")!!))
        assertEquals(v.str("expectedAddressV2"), Wallet(secret, token, position, otsVersion = 2).address)
      }
    } + DynamicTest.dynamicTest("an unsupported OTS version is refused") {
      val error = assertThrows(IllegalArgumentException::class.java) { Wallet("x", otsVersion = 3) }
      assertEquals("KnishIO: unsupported OTS version 3; expected 1 or 2.", error.message)
    }

    val signature = tests(vectors, "signature").map { v ->
      DynamicTest.dynamicTest(v.str("name")!!) {
        val normalized = CheckMolecule.normalizedHash(v.str("molecularHash")!!)
        val expected = v["expectedNormalized"]!!.jsonArray.map { it.jsonPrimitive.int }
        assertEquals(expected, expected.indices.map { normalized[it] })
      }
    }

    val mlkem = tests(vectors, "mlkem_identity").map { v ->
      DynamicTest.dynamicTest(v.str("name")!!) {
        val k = keyFrom(v.str("keyFrom")!!)
        val pubkey = { set: Int ->
          Wallet(k.str("secret"), k.str("token")!!, k.str("position"), mlkemParameterSet = set, otsVersion = 2).pubkey
        }
        assertEquals(v.str("expectedMlKem1024Pubkey"), pubkey(1024))
        assertEquals(v.str("expectedMlKem768Pubkey"), pubkey(768))
      }
    }

    val moleculeTests = tests(vectors, "molecules")
    val verdicts = moleculeTests.map { v ->
      DynamicTest.dynamicTest("${v.str("name")} → ${v.str("expected")}") {
        val molecule = reconstruct(v["molecule"]!!.jsonObject)
        val expected = v.str("expected")!!
        if (expected == "verified") {
          assertTrue(Molecule.verify(molecule, molecule.sourceWallet))
        } else {
          assertThrows(exceptions.getValue(expected)) { Molecule.verify(molecule, molecule.sourceWallet) }
        }
      }
    }
    val resign = listOf("genuine_v2", "genuine_v1_control").map { name ->
      DynamicTest.dynamicTest("re-signing $name reproduces every otsFragment") {
        val v = moleculeTests.first { it.str("name") == name }
        val user = keyFrom("hex_secret_user")
        val secret = user.str("secret")!!
        val source = Wallet(secret, "USER", user.str("position"), otsVersion = v["otsVersion"]!!.jsonPrimitive.int)
        val molecule = reconstruct(v["molecule"]!!.jsonObject, secret, source)
        molecule.sign()
        val expected = v["molecule"]!!.jsonObject["atoms"]!!.jsonArray.map { it.jsonObject.str("otsFragment") }
        assertEquals(expected, molecule.atoms.map { it.otsFragment })
      }
    }

    val malformed = tests(vectors, "malformed_hash").map { v ->
      DynamicTest.dynamicTest("${v.str("name")} → ${v.str("expected")}") {
        val molecule = reconstruct(v["molecule"]!!.jsonObject)
        assertThrows(exceptions.getValue(v.str("expected")!!)) { CheckMolecule.ots(molecule) }
      }
    }

    return listOf(
      DynamicContainer.dynamicContainer("key_derivation", keys),
      DynamicContainer.dynamicContainer("signature", signature),
      DynamicContainer.dynamicContainer("mlkem_identity", mlkem),
      DynamicContainer.dynamicContainer("molecules", verdicts + resign),
      DynamicContainer.dynamicContainer("malformed_hash", malformed)
    )
  }

  @Test
  @DisplayName("v2 round trip: a 64-chain signature verifies and a changed fragment does not")
  fun v2RoundTrip() {
    val secret = "d".repeat(2048)
    val source = Wallet(secret, "USER", "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef", otsVersion = 2)
    val molecule = Molecule(secret, source)
    assertEquals(2, molecule.signingOtsVersion())
    assertEquals(2, molecule.remainderWallet!!.otsVersion)

    molecule.initMeta(mutableListOf(MetaData("role", "admin")), "AppRole", "role-1")
    molecule.sign(compressed = true)
    assertEquals(5464, molecule.atoms.joinToString("") { it.otsFragment ?: "" }.length)
    assertTrue(Molecule.verify(molecule, source))

    val fragment = molecule.atoms[0].otsFragment!!
    val idx = fragment.indexOfFirst { it == 'A' || it == 'B' }
    val flipped = if (fragment[idx] == 'A') 'B' else 'A'
    molecule.atoms[0].otsFragment = fragment.substring(0, idx) + flipped + fragment.substring(idx + 1)
    assertThrows(SignatureMismatchException::class.java) { Molecule.verify(molecule, source) }
  }
}
