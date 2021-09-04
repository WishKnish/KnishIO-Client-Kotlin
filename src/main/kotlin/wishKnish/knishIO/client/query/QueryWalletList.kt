@file:JvmName("QueryWalletList")

package wishKnish.knishIO.client.query

import wishKnish.knishIO.client.data.json.query.WalletList
import wishKnish.knishIO.client.data.json.variables.IVariable
import wishKnish.knishIO.client.data.json.variables.WalletListVariable
import wishKnish.knishIO.client.httpClient.HttpClient
import wishKnish.knishIO.client.response.ResponseWalletList

class QueryWalletList(client: HttpClient) : Query(client) {
  override fun createResponse(json: String): ResponseWalletList {
    return ResponseWalletList(this, json)
  }

  override fun getQuery(variables: IVariable): WalletList {
    return WalletList(variables as WalletListVariable)
  }
}
