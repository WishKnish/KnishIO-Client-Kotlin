@file:JvmName("Balance")

package wishKnish.knishIO.client.data.json.query

import kotlinx.serialization.Serializable
import wishKnish.knishIO.client.data.json.variables.BalanceVariable

@Serializable data class Balance(@JvmField val variables: BalanceVariable) : QueryInterface {
  override val query = """
    query( ${'$'}address: String, ${'$'}bundleHash: String, ${'$'}token: String, ${'$'}position: String ) {
      Balance( address: ${'$'}address, bundleHash: ${'$'}bundleHash, token: ${'$'}token, position: ${'$'}position ) {
        address,
        bundleHash,
        tokenSlug,
        batchId,
        position,
        amount,
        characters,
        pubkey,
        createdAt,
        tokenUnits {
          id,
          name,
          metas
        }
      }
    }
    """.trimIndent()
}
