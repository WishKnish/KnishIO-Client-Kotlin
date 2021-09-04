@file:JvmName("MoleculeMutation")

package wishKnish.knishIO.client.data.json.mutation

import kotlinx.serialization.Serializable
import wishKnish.knishIO.client.data.json.query.QueryInterface
import wishKnish.knishIO.client.data.json.variables.MoleculeMutationVariable

@Serializable data class MoleculeMutation(@JvmField val variables: MoleculeMutationVariable) : QueryInterface {
  override val query = """
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
}
