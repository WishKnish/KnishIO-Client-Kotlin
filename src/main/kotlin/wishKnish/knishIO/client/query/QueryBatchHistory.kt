@file:JvmName("QueryBatchHistory")

package wishKnish.knishIO.client.query

import wishKnish.knishIO.client.data.json.query.BatchHistory
import wishKnish.knishIO.client.data.json.variables.BatchHistoryVariable
import wishKnish.knishIO.client.data.json.variables.IVariable
import wishKnish.knishIO.client.httpClient.HttpClient
import wishKnish.knishIO.client.response.IResponse
import wishKnish.knishIO.client.response.ResponseBatchHistory


class QueryBatchHistory(client: HttpClient) : Query(client) {
  override fun createResponse(json: String): IResponse {
    return ResponseBatchHistory(this, json)
  }

  override fun getQuery(variables: IVariable): BatchHistory {
    return BatchHistory(variables as BatchHistoryVariable)
  }
}
