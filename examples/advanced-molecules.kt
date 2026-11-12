@file:JvmName("AdvancedMolecules")

package examples.advancedmolecules

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import wishKnish.knishIO.client.Atom
import wishKnish.knishIO.client.KnishIOClient
import wishKnish.knishIO.client.Molecule
import wishKnish.knishIO.client.TransferRecipient
import wishKnish.knishIO.client.data.MetaData
import wishKnish.knishIO.client.data.json.variables.MoleculeMutationVariable
import wishKnish.knishIO.client.exception.BaseException
import wishKnish.knishIO.client.exception.UnauthenticatedException
import wishKnish.knishIO.client.libraries.Crypto
import wishKnish.knishIO.client.mutation.MutationProposeMolecule
import wishKnish.knishIO.client.response.ResponseProposeMolecule
import java.net.URI

/**
 * Molecules built by hand: compose atoms with the Molecule builders (or by hand), sign, verify
 * locally, and submit. Also the client operations that put several transfers or a buffer
 * deposit/withdrawal into one molecule.
 *
 * Environment: KNISHIO_NODE_URI, KNISHIO_SECRET, KNISHIO_TOKEN (a token this identity holds).
 */
fun main() {
    val nodeUri = System.getenv("KNISHIO_NODE_URI") ?: "http://localhost:8000/graphql"
    val secret = System.getenv("KNISHIO_SECRET") ?: Crypto.generateSecret()
    val token = System.getenv("KNISHIO_TOKEN") ?: "DEMO"

    KnishIOClient(uris = listOf(URI(nodeUri))).use { client ->
        try {
            client.requestAuthToken(secret = secret)
        } catch (e: UnauthenticatedException) {
            println("Login rejected: ${e.message}")
            return
        }

        // createMolecule() signs with the identity's ContinuID head wallet and adds a USER
        // remainder wallet that becomes the next head
        submit(client, "meta", metaMolecule(client.createMolecule()))
        submit(client, "rule", ruleMolecule(client.createMolecule()))
        submit(client, "two metadata atoms", handBuiltMetaMolecule(client.createMolecule()))

        // Several recipients in one molecule: one V atom per recipient plus the remainder
        val recipients = listOf(
            TransferRecipient(bundleHash = Crypto.generateBundleHash(Crypto.generateSecret()), amount = 100),
            TransferRecipient(bundleHash = Crypto.generateBundleHash(Crypto.generateSecret()), amount = 200)
        )
        report("transferTokens", client.transferTokens(token = token, recipients = recipients))

        // Move tokens into this identity's buffer (B isotope) with trade rates, then back out
        report("depositBufferToken", client.depositBufferToken(token = token, amount = 50, tradeRates = mapOf("USER" to 1)))
        report("withdrawBufferToken", client.withdrawBufferToken(token = token, amount = 20))
    }
}

/** An M atom with the asset's metadata, then the USER remainder (I) atom. */
fun metaMolecule(molecule: Molecule): Molecule = molecule.initMeta(
    meta = mutableListOf(MetaData("theme", "dark"), MetaData("language", "en")),
    metaType = "AppSettings",
    metaId = "settings-1"
)

/** An R atom: reject writes to the asset while its `status` meta is `locked`. */
fun ruleMolecule(molecule: Molecule): Molecule = molecule.createRule(
    metaType = "AppSettings",
    metaId = "settings-1",
    rule = Json.parseToJsonElement(
        """[{"condition":[{"key":"status","value":"locked","comparison":"=="}],"callback":[{"action":"reject"}]}]"""
    ).jsonArray
)

/** Two M atoms written by hand, signed by the source wallet, then the remainder atom. */
fun handBuiltMetaMolecule(molecule: Molecule): Molecule {
    val source = molecule.sourceWallet
    listOf("AppSettings" to "settings-2", "AppSettings" to "settings-3").forEach { (metaType, metaId) ->
        molecule.addAtom(
            Atom(
                position = source.position!!,
                walletAddress = source.address!!,
                isotope = 'M',
                token = source.token,
                metaType = metaType,
                metaId = metaId,
                meta = molecule.finalMetas(mutableListOf(MetaData("theme", "light")), source),
                index = molecule.generateIndex()
            )
        )
    }
    return molecule.addUserRemainderAtom()
}

/** Signs the molecule and runs every local check; throws the specific exception on a failure. */
fun signAndVerify(molecule: Molecule): Molecule {
    molecule.sign()
    Molecule.verify(molecule, molecule.sourceWallet)
    println("Signed ${molecule.atoms.map { it.isotope }}: ${molecule.molecularHash}")
    println("Hash recomputed from the atoms matches: ${Atom.hashAtoms(molecule.atoms) == molecule.molecularHash}")
    return molecule
}

/** Signs, verifies locally, and sends the molecule as it is. */
fun submit(client: KnishIOClient, label: String, molecule: Molecule) {
    try {
        signAndVerify(molecule)
        val mutation = client.createMoleculeMutation(MutationProposeMolecule::class, molecule)
        report(label, mutation.execute(MoleculeMutationVariable(molecule)) as ResponseProposeMolecule)
    } catch (e: BaseException) {
        println("$label: ${e.message}")
    }
}

private fun report(label: String, response: ResponseProposeMolecule) {
    println("$label: ${if (response.success()) "accepted" else "${response.status()} - ${response.reason()}"}")
}
