@file:JvmName("ResponseWalletBundle")
package wishKnish.knishIO.client.response

import wishKnish.knishIO.client.data.graphql.types.WalletBundle
import wishKnish.knishIO.client.data.json.response.query.walletBundle.WalletBundleResponse
import wishKnish.knishIO.client.query.QueryWalletBundle


class ResponseWalletBundle(
  query: QueryWalletBundle,
  json: String,
): Response(query, json, "data.WalletBundle") {
  override fun mapping(response: String): WalletBundleResponse {
    return WalletBundleResponse.jsonToObject(response)
  }

  override fun data(): List<WalletBundle>? {
    @Suppress("UNCHECKED_CAST") return super.data() as? List<WalletBundle>
  }

  override fun payload(): Map<String, WalletBundle> {
    val bundleData = data()
    val aggregate = mutableMapOf<String, WalletBundle>()

    bundleData?.let { 
      it.forEach { walletBundle ->
        aggregate[walletBundle.bundleHash!!] = walletBundle
      }
    }

    return aggregate.toMap()
  }
}
