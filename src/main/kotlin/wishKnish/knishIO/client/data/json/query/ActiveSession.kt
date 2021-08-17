@file:JvmName("ActiveSession")
package wishKnish.knishIO.client.data.json.query

import kotlinx.serialization.Serializable
import wishKnish.knishIO.client.data.json.variables.ActiveSessionVariable


@Serializable data class ActiveSession(@JvmField val variables: ActiveSessionVariable): QueryInterface {
  override val query = """
    query ActiveUserQuery (${'$'}bundleHash:String, ${'$'}metaType: String, ${'$'}metaId: String) {
      ActiveUser (bundleHash: ${'$'}bundleHash, metaType: ${'$'}metaType, metaId: ${'$'}metaId) {
        bundleHash,
        metaType,
        metaId,
        jsonData,
        createdAt,
        updatedAt
      }
    }
  """.trimIndent()
}
