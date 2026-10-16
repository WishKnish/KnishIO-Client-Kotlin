/*
 * Served-molecule verification — checks CheckMolecule.verifyFromServerData against the
 * shared served-molecules.json (molecules exactly as a validator's GraphQL API serves
 * them) and Atom.version4ViewJson / Atom.hashAtoms against version4-hash-vectors.json.
 *
 * Both fixtures are vendored byte-for-byte from sdks/shared-test-results/ and shared
 * with the other SDKs' served-molecule tests.
 */

package wishKnish.knishIO.client

import kotlinx.serialization.json.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import wishKnish.knishIO.client.data.MetaData
import wishKnish.knishIO.client.libraries.CheckMolecule

@DisplayName("Served molecules (shared served-molecules.json, version4-hash-vectors.json)")
class ServedMoleculeTest {

    private val mismatch = "The molecular hash does not match"

    private fun resource(name: String): String {
        val stream = javaClass.classLoader.getResourceAsStream(name)
            ?: throw IllegalStateException("$name not found in test resources")
        return stream.bufferedReader().use { it.readText() }
    }

    private val served: JsonObject by lazy {
        Json.parseToJsonElement(resource("served-molecules.json")).jsonObject
    }

    private val vectors: JsonObject by lazy {
        Json.parseToJsonElement(resource("version4-hash-vectors.json")).jsonObject
    }

    private fun molecule(key: String): JsonObject = served[key]!!.jsonObject

    /** Returns [data] with every atom passed through [transform]. */
    private fun mapAtoms(data: JsonObject, transform: (Int, JsonObject) -> JsonObject): JsonObject {
        val atoms = data["atoms"]!!.jsonArray.mapIndexed { i, atom -> transform(i, atom.jsonObject) }
        return JsonObject(data + ("atoms" to JsonArray(atoms)))
    }

    private fun withVersion(data: JsonObject, version: String?): JsonObject =
        mapAtoms(data) { _, atom -> JsonObject(atom + ("version" to JsonPrimitive(version))) }

    private fun tampered(data: JsonObject): JsonObject =
        mapAtoms(data) { i, atom ->
            if (i == 0) JsonObject(atom + ("metaId" to JsonPrimitive("rust-pre-tampered"))) else atom
        }

    private fun assertVerified(data: JsonObject) {
        val result = CheckMolecule.verifyFromServerData(data.toString())
        assertNull(result.error, "unexpected error")
        assertTrue(result.verified)
        assertEquals(data["molecularHash"]!!.jsonPrimitive.content, result.molecularHash)
    }

    private fun assertMismatch(data: JsonObject) {
        val result = CheckMolecule.verifyFromServerData(data.toString())
        assertFalse(result.verified)
        assertEquals(mismatch, result.error)
    }

    @Test
    @DisplayName("1. v1 and v2 verify")
    fun v1AndV2Verify() {
        assertVerified(molecule("v1"))
        assertVerified(molecule("v2"))
    }

    @Test
    @DisplayName("2. v2ts verifies through the \"4\" retry")
    fun v2tsVerifiesThroughRetry() {
        assertTrue(molecule("v2ts")["atoms"]!!.jsonArray.all { it.jsonObject["version"] == null })
        assertVerified(molecule("v2ts"))
    }

    @Test
    @DisplayName("3. v2ts served with version \"4\" verifies directly")
    fun v2tsWithVersionVerifies() {
        assertVerified(withVersion(molecule("v2ts"), "4"))
    }

    @Test
    @DisplayName("4. v3rust (served with \"3\") verifies")
    fun v3rustVerifies() {
        assertTrue(molecule("v3rust")["atoms"]!!.jsonArray.all {
            it.jsonObject["version"]?.jsonPrimitive?.content == "3"
        })
        assertVerified(molecule("v3rust"))
    }

    @Test
    @DisplayName("5. v3rust without versions verifies through the \"3\" retry")
    fun v3rustWithoutVersionsVerifies() {
        assertVerified(withVersion(molecule("v3rust"), null))
    }

    @Test
    @DisplayName("6. tampered v3rust fails with a hash mismatch, with and without versions")
    fun tamperedV3rustFails() {
        assertMismatch(tampered(molecule("v3rust")))
        assertMismatch(tampered(withVersion(molecule("v3rust"), null)))
    }

    @Test
    @DisplayName("7. v1 served with version \"4\" fails (a served version disables the retry)")
    fun v1WithVersionFails() {
        assertMismatch(withVersion(molecule("v1"), "4"))
    }

    @Test
    @DisplayName("8. createdAt normalization")
    fun createdAtNormalization() {
        assertEquals("1792008157000", CheckMolecule.toSignedTimestamp("2026-10-14T20:02:37+00:00"))
        assertEquals("1792008157110", CheckMolecule.toSignedTimestamp("2026-10-14T20:02:37.110Z"))
        assertEquals("1792008157110", CheckMolecule.toSignedTimestamp(1792008157110L))
        assertEquals("1792008157110", CheckMolecule.toSignedTimestamp("1792008157110"))
        assertEquals("not-a-date", CheckMolecule.toSignedTimestamp("not-a-date"))
        assertNull(CheckMolecule.toSignedTimestamp(""))
        assertNull(CheckMolecule.toSignedTimestamp(null))
    }

    @TestFactory
    @DisplayName("9. Version4 hash vectors")
    fun version4HashVectors(): List<DynamicTest> {
        val cases = vectors["cases"]!!.jsonArray
        assertEquals(
            listOf(
                "v4-basic", "v3-basic", "slash-unicode", "control-quote", "line-separators",
                "nulls", "empty-strings", "empty-version", "index-order"
            ),
            cases.map { it.jsonObject["name"]!!.jsonPrimitive.content }
        )
        return cases.map { element ->
            val case = element.jsonObject
            val name = case["name"]!!.jsonPrimitive.content
            DynamicTest.dynamicTest("Version4: $name") {
                val atoms = case["atoms"]!!.jsonArray.map { Atom.fromJSON(it.toString()) }
                assertEquals(case["viewJson"]!!.jsonPrimitive.content, Atom.version4ViewJson(atoms), "viewJson: $name")
                assertEquals(case["expectedHash"]!!.jsonPrimitive.content, Atom.hashAtoms(atoms), "hash: $name")
            }
        }
    }

    @TestFactory
    @DisplayName("9b. Legacy (unversioned) hash vectors")
    fun legacyHashVectors(): List<DynamicTest> {
        val cases = vectors["legacyCases"]!!.jsonArray
        assertEquals(
            listOf("legacy-empty-strings", "legacy-nulls"),
            cases.map { it.jsonObject["name"]!!.jsonPrimitive.content }
        )
        return cases.map { element ->
            val case = element.jsonObject
            val name = case["name"]!!.jsonPrimitive.content
            DynamicTest.dynamicTest("Legacy: $name") {
                val atoms = case["atoms"]!!.jsonArray.map { Atom.fromJSON(it.toString()) }
                assertTrue(atoms.all { it.version == null }, "legacy atoms carry no version: $name")
                assertEquals(case["expectedHash"]!!.jsonPrimitive.content, Atom.hashAtoms(atoms), "hash: $name")
            }
        }
    }

    @Test
    @DisplayName("Malformed served data is reported, not thrown")
    fun malformedInput() {
        for (input in listOf("[]", "{}", "not json", "{\"molecularHash\":\"x\",\"atoms\":7}")) {
            val result = CheckMolecule.verifyFromServerData(input)
            assertFalse(result.verified, input)
            assertNotNull(result.error, input)
        }
    }

    @Test
    @DisplayName("Atom JSON carries version only when it is set")
    fun versionOnlySerializedWhenSet() {
        val atom = Atom(
            position = "p", walletAddress = "w", isotope = 'M', token = "USER",
            meta = listOf(MetaData("k", "v")), createdAt = "1792008157000"
        )
        val plain = Json.parseToJsonElement(atom.toJson()).jsonObject
        assertFalse(plain.containsKey("version"))
        assertFalse(atom.toJSON().containsKey("version"))
        assertNull(Atom.fromJSON(atom.toJson()).version)

        atom.version = "4"
        val versioned = Json.parseToJsonElement(atom.toJson()).jsonObject
        assertEquals("4", versioned["version"]!!.jsonPrimitive.content)
        assertEquals("4", atom.toJSON()["version"])
        assertEquals("4", Atom.fromJSON(atom.toJson()).version)
        assertEquals("4", Atom.fromJSON(atom.toJson().replace("\"version\":\"4\"", "\"version\":4")).version)
    }
}
