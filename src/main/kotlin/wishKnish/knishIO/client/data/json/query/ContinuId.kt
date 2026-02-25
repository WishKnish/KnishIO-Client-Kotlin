@file:JvmName("ContinuId")

package wishKnish.knishIO.client.data.json.query

import kotlinx.serialization.Serializable
import wishKnish.knishIO.client.data.json.variables.ContinuIdVariable

@Serializable data class ContinuId(@JvmField val variables: ContinuIdVariable) : QueryInterface {
  override val query = $$"""
    query ($bundle: String!) {
      ContinuId(bundle: $bundle) {
        address,
        bundleHash,
        tokenSlug,
        position,
        batchId,
        characters,
        pubkey,
        amount,
        createdAt
      }
    }
  """.trimIndent()
}
