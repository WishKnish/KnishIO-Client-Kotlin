@file:JvmName("ResponseBatchHistory")
package wishKnish.knishIO.client.response

import wishKnish.knishIO.client.data.graphql.types.BatchInstance
import wishKnish.knishIO.client.data.json.response.query.batchHistory.BatchHistoryResponse
import wishKnish.knishIO.client.query.QueryBatchHistory

class ResponseBatchHistory(
  query: QueryBatchHistory,
  json: String,
): Response(query, json, "data.BatchHistory") {
  override fun data(): List<BatchInstance>? {
    @Suppress("UNCHECKED_CAST") return super.data() as? List<BatchInstance>?
  }

  override fun mapping(response: String): BatchHistoryResponse {
    return BatchHistoryResponse.jsonToObject(response)
  }
}