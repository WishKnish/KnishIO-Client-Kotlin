@file:JvmName("BatchHistory")

package wishKnish.knishIO.client.data.json.query

import kotlinx.serialization.Serializable
import wishKnish.knishIO.client.data.json.variables.BatchHistoryVariable


@Serializable data class BatchHistory(@JvmField val variables: BatchHistoryVariable) : QueryInterface {
  override val query = $$"""
    query( $batchId: String ) {
      BatchHistory( batchId: $batchId ) {
        $${Batch.getFields()}
      }
    }
  """.trimIndent()
}
