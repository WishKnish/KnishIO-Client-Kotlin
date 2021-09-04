@file:JvmName("WalletBundle")

package wishKnish.knishIO.client.data.json.query

import kotlinx.serialization.Serializable
import wishKnish.knishIO.client.data.json.variables.WalletBundleVariable

@Serializable data class WalletBundle(@JvmField val variables: WalletBundleVariable) : QueryInterface {
  override val query = """
    query( ${'$'}bundleHash: String, ${'$'}bundleHashes: [ String! ], ${'$'}key: String, ${'$'}keys: [ String! ], ${'$'}value: String, ${'$'}values: [ String! ], ${'$'}keys_values: [ MetaInput ], ${'$'}latest: Boolean, ${'$'}limit: Int, ${'$'}order: String ) {
      WalletBundle( bundleHash: ${'$'}bundleHash, bundleHashes: ${'$'}bundleHashes, key: ${'$'}key, keys: ${'$'}keys, value: ${'$'}value, values: ${'$'}values, keys_values: ${'$'}keys_values, latest: ${'$'}latest, limit: ${'$'}limit, order: ${'$'}order ) {
        bundleHash,
        metas {
          molecularHash,
          position,
          key,
          value,
          createdAt
        },
        createdAt
      }
    }
  """.trimIndent()
}
