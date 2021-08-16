@file:JvmName("QueryContinuId")
package wishKnish.knishIO.client.query

import wishKnish.knishIO.client.data.json.query.ContinuId
import wishKnish.knishIO.client.data.json.query.QueryInterface
import wishKnish.knishIO.client.data.json.variables.ContinuIdVariable
import wishKnish.knishIO.client.data.json.variables.IVariable
import wishKnish.knishIO.client.httpClient.HttpClient
import wishKnish.knishIO.client.response.ResponseContinuId


class QueryContinuId(client: HttpClient): Query(client) {
  override fun getQuery(variables: IVariable): QueryInterface {
    return ContinuId(variables as ContinuIdVariable)
  }

  override fun createResponse(json: String): ResponseContinuId {
    return ResponseContinuId(this, json)
  }
}
