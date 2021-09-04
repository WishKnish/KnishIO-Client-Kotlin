@file:JvmName("WalletList")

package wishKnish.knishIO.client.data.json.query

import kotlinx.serialization.Serializable
import wishKnish.knishIO.client.data.json.variables.WalletListVariable

@Serializable data class WalletList(@JvmField val variables: WalletListVariable) : QueryInterface {
  override val query = """
    query( ${'$'}address: String, ${'$'}bundleHash: String, ${'$'}token: String, ${'$'}position: String, ${'$'}unspent: Boolean ) {
      Wallet( address: ${'$'}address, bundleHash: ${'$'}bundleHash, token: ${'$'}token, position: ${'$'}position, unspent: ${'$'}unspent ) {
        address,
        bundleHash,
        token {
          name,
          amount,
          fungibility,
          supply
        },
        molecules {
          molecularHash,
          createdAt
        }
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
