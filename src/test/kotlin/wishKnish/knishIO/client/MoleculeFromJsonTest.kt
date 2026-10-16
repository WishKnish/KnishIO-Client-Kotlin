/*
 * Molecule.fromJSON — reconstructs a molecule another SDK signed (no secret) so that
 * Molecule.verify can check it. Uses the shared cross-platform-test-vectors.json
 * wotsSignedMetadataMolecule (signed by the JS SDK) and legacyMlkem768AuthMolecule.
 */

package wishKnish.knishIO.client

import kotlinx.serialization.json.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Molecule.fromJSON (shared cross-platform-test-vectors.json)")
class MoleculeFromJsonTest {

    private val vectors: JsonObject by lazy {
        val stream = javaClass.classLoader.getResourceAsStream("cross-platform-test-vectors.json")
            ?: throw IllegalStateException("cross-platform-test-vectors.json not found in test resources")
        Json.parseToJsonElement(stream.bufferedReader().use { it.readText() }).jsonObject["vectors"]!!.jsonObject
    }

    private fun signed(name: String): JsonObject = vectors[name]!!.jsonObject["molecule"]!!.jsonObject

    @Test
    @DisplayName("a JS-signed molecule with its sourceWallet loads and verifies")
    fun withSourceWallet() {
        val json = signed("wotsSignedMetadataMolecule")
        val molecule = Molecule.fromJSON(json.toString())

        assertNull(molecule.secret)
        assertEquals(json["molecularHash"]!!.jsonPrimitive.content, molecule.molecularHash)
        assertEquals(json["createdAt"]!!.jsonPrimitive.content, molecule.createdAt)
        assertEquals(json["bundle"]!!.jsonPrimitive.content, molecule.bundle)
        assertEquals(json["atoms"]!!.jsonArray.size, molecule.atoms.size)
        assertEquals(
            json["sourceWallet"]!!.jsonObject["address"]!!.jsonPrimitive.content,
            molecule.sourceWallet.address
        )
        assertTrue(Molecule.verify(molecule))
        assertTrue(molecule.check(molecule.sourceWallet))
    }

    @Test
    @DisplayName("without a sourceWallet, the signer wallet comes from atom 0")
    fun withoutSourceWallet() {
        for (name in listOf("wotsSignedMetadataMolecule", "legacyMlkem768AuthMolecule")) {
            val json = JsonObject(signed(name) - "sourceWallet" - "remainderWallet")
            val molecule = Molecule.fromJSON(json.toString())
            val first = molecule.atoms.first()

            assertEquals(first.position, molecule.sourceWallet.position, name)
            assertEquals(first.walletAddress, molecule.sourceWallet.address, name)
            assertEquals(first.token, molecule.sourceWallet.token, name)
            assertTrue(Molecule.verify(molecule), name)
        }
    }

    @Test
    @DisplayName("a molecule with a changed atom fails verification")
    fun tamperedFails() {
        val json = signed("wotsSignedMetadataMolecule")
        val atoms = json["atoms"]!!.jsonArray.mapIndexed { i, atom ->
            if (i == 0) JsonObject(atom.jsonObject + ("metaId" to JsonPrimitive("tampered"))) else atom
        }
        val molecule = Molecule.fromJSON(JsonObject(json + ("atoms" to JsonArray(atoms))).toString())
        assertThrows(wishKnish.knishIO.client.exception.MolecularHashMismatchException::class.java) {
            Molecule.verify(molecule)
        }
    }
}
