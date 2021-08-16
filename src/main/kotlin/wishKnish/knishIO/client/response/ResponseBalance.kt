@file:JvmName("ResponseBalance")
package wishKnish.knishIO.client.response

import wishKnish.knishIO.client.Wallet
import wishKnish.knishIO.client.data.json.response.query.balance.BalanceResponse
import wishKnish.knishIO.client.query.QueryBalance
import wishKnish.knishIO.client.data.graphql.types.Wallet as GraphqlWallet

class ResponseBalance(
  query: QueryBalance,
  json: String,
): Response(query, json, "data.Balance") {

  override fun data(): GraphqlWallet? {
    return super.data() as GraphqlWallet?
  }

  override fun mapping(response: String): BalanceResponse {
    return BalanceResponse.jsonToObject(response)
  }

  override fun payload(): Wallet? {
    val walletData = data()

    return when {
      walletData == null -> null
      walletData.bundleHash == null -> null
      walletData.tokenSlug == null -> null
      else -> ResponseWalletList.toClientWallet(walletData)
    }
  }
}
