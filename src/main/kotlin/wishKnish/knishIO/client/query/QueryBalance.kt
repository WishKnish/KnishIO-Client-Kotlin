@file:JvmName("QueryBalance")
package wishKnish.knishIO.client.query

import wishKnish.knishIO.client.data.json.query.Balance
import wishKnish.knishIO.client.data.json.query.QueryInterface
import wishKnish.knishIO.client.data.json.variables.BalanceVariable
import wishKnish.knishIO.client.data.json.variables.IVariable
import wishKnish.knishIO.client.httpClient.HttpClient
import wishKnish.knishIO.client.response.ResponseBalance


class QueryBalance(client: HttpClient): Query(client) {
  override fun createResponse(json: String): ResponseBalance {
    return ResponseBalance(this, json)
  }

  override fun getQuery(variables: IVariable): Balance {
    return Balance(variables as BalanceVariable)
  }
}
