@file:JvmName("ResponseBatch")
package wishKnish.knishIO.client.response

import wishKnish.knishIO.client.data.graphql.types.BatchInstance
import wishKnish.knishIO.client.data.json.response.query.batch.BatchResponse
import wishKnish.knishIO.client.query.QueryBatch

class ResponseBatch(
  query: QueryBatch,
  json: String,
): Response(query, json, "data.Batch") {

  override fun data(): List<BatchInstance>? {
    @Suppress("UNCHECKED_CAST") return super.data() as? List<BatchInstance>?
  }

  override fun mapping(response: String): BatchResponse {
    return BatchResponse.jsonToObject(response)
  }
}
