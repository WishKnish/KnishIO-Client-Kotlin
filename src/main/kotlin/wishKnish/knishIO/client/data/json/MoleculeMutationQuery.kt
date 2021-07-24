@file:JvmName("MoleculeMutationQuery")

package wishKnish.knishIO.client.data.json

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import wishKnish.knishIO.client.Molecule

@Serializable
data class MoleculeMutationQuery(@Transient val molecule: Molecule? = null) {
  @JvmField
  val query = """
        mutation(${'$'}molecule: MoleculeInput! ) {
          ProposeMolecule( molecule: ${'$'}molecule ) {
            molecularHash,
            height,
            depth,
            status,
            reason,
            payload,
            createdAt,
            receivedAt,
            processedAt,
            broadcastedAt
          }
        }
    """.trimIndent()

  @JvmField
  val variables: MutableMap<String, Molecule?> = mutableMapOf("molecule" to null)

  init {
    requireNotNull(molecule) { "Molecule is a required parameter" }
    requireNotNull(molecule.molecularHash) { "The molecule must be signed" }
    variables["molecule"] = molecule
  }
}
