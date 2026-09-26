/*
 * signingWallet forgery regression (WOTS+ mitigation plan §8 Phase 0.2).
 *
 * signing-wallet-forgery.json is the cross-SDK fixture built with the published
 * @wishknish/knishio-client-js 1.2.1 (byte-identical copy in every SDK). `forged` claims the
 * victim's address in atoms[0].walletAddress but carries the attacker's OTS signature, plus an
 * atoms[0] `signingWallet` meta naming the attacker. JS 1.2.1 check() accepts both molecules;
 * the verifier must bind the signature to atoms[0].walletAddress only, so `forged` is rejected.
 */

package wishKnish.knishIO.client

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import wishKnish.knishIO.client.exception.SignatureMismatchException

@DisplayName("signingWallet forgery fixture (signing-wallet-forgery.json)")
class SigningWalletForgeryTest {

    private val fixture: JsonObject by lazy {
        val stream = javaClass.classLoader.getResourceAsStream("signing-wallet-forgery.json")
            ?: throw IllegalStateException("signing-wallet-forgery.json not found in test resources")
        Json.parseToJsonElement(stream.bufferedReader().use { it.readText() }).jsonObject
    }

    private fun JsonObject.str(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    /**
     * Rebuilds a secretless molecule from JS `Molecule.toJSON()` output by verbatim field copy
     * (atoms via [Atom.fromJSON]), as CrossPlatformVectorsTest does. [Molecule.fromJSON] cannot
     * load it: it constructs `Molecule(secret = null)` with a default position-less `Wallet()`,
     * which the initialiser rejects ("SourceWallet parameter not initialized by valid wallet").
     * The fixture carries no sourceWallet, so the validation context is a secretless wallet at
     * atoms[0]'s token and position; the M and I checks do not read it.
     */
    private fun loadMolecule(data: JsonObject): Molecule {
        val atoms = data["atoms"]!!.jsonArray.map { Atom.fromJSON(it.toString()) }
        val first = atoms.first()
        val sourceWallet = Wallet(secret = null, token = first.token, position = first.position)

        return Molecule(secret = null, sourceWallet = sourceWallet, cellSlug = data.str("cellSlug")).apply {
            molecularHash = data.str("molecularHash")
            bundle = data.str("bundle")
            createdAt = data.str("createdAt")!!
            status = data.str("status")
            this.atoms.clear()
            this.atoms.addAll(atoms)
        }
    }

    @Test
    @DisplayName("genuine molecule (signed by its own wallet) loads and passes check()")
    fun genuinePasses() {
        val genuine = loadMolecule(fixture["genuine"]!!.jsonObject)

        assertEquals(fixture.str("attackerAddress"), genuine.atoms.first().walletAddress)
        assertTrue(genuine.check(genuine.sourceWallet), "genuine molecule must validate")
        assertTrue(Molecule.verify(genuine, genuine.sourceWallet))
    }

    @Test
    @DisplayName("forged molecule (victim address, attacker OTS, signingWallet meta) fails with SignatureMismatchException")
    fun forgedRejected() {
        val forgedJson = fixture["forged"]!!.jsonObject
        val forged = loadMolecule(forgedJson)

        assertEquals(fixture.str("victimAddress"), forgedJson["atoms"]!!.jsonArray.first().jsonObject.str("walletAddress"))
        assertEquals(fixture.str("victimAddress"), forged.atoms.first().walletAddress)
        assertTrue(
            forged.atoms.first().meta.any { it.key == "signingWallet" },
            "fixture must carry the atoms[0] signingWallet meta the verifier has to ignore"
        )

        assertFalse(forged.check(forged.sourceWallet), "forged molecule must not validate")
        assertThrows<SignatureMismatchException> { Molecule.verify(forged, forged.sourceWallet) }
    }
}
