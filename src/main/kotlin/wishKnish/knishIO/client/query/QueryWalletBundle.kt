@file:JvmName("QueryWalletBundle")
package wishKnish.knishIO.client.query

import wishKnish.knishIO.client.data.json.query.WalletBundle
import wishKnish.knishIO.client.data.json.variables.IVariable
import wishKnish.knishIO.client.data.json.variables.WalletBundleVariable
import wishKnish.knishIO.client.httpClient.HttpClient
import wishKnish.knishIO.client.response.IResponse


class QueryWalletBundle(client: HttpClient): Query(client) {
  override fun createResponse(json: String): IResponse {
    TODO("Not yet implemented")
  }

  override fun getQuery(variables: IVariable): WalletBundle {
    return WalletBundle(variables as WalletBundleVariable)
  }
}
