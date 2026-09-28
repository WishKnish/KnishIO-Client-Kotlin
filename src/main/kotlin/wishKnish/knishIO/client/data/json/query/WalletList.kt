@file:JvmName("WalletList")

package wishKnish.knishIO.client.data.json.query

import kotlinx.serialization.Serializable
import wishKnish.knishIO.client.data.json.variables.WalletListVariable

// The validator's `Wallet` field takes bundleHash/token/unspent (no address or position
// arguments) and its Wallet type has no `molecules` field; either made every query a
// GraphQL validation error. Selection mirrors JS QueryWalletList.
@Serializable data class WalletList(@JvmField val variables: WalletListVariable) : QueryInterface {
  override val query = $$"""
    query( $bundleHash: String, $token: String, $unspent: Boolean ) {
      Wallet( bundleHash: $bundleHash, token: $token, unspent: $unspent ) {
        address,
        bundleHash,
        token {
          name,
          amount,
          fungibility,
          supply
        },
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
