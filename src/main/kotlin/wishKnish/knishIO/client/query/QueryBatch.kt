@file:JvmName("QueryBatch")

package wishKnish.knishIO.client.query

import wishKnish.knishIO.client.data.json.query.Batch
import wishKnish.knishIO.client.data.json.variables.BatchVariable
import wishKnish.knishIO.client.data.json.variables.IVariable
import wishKnish.knishIO.client.httpClient.HttpClient
import wishKnish.knishIO.client.response.ResponseBatch

class QueryBatch(client: HttpClient) : Query(client) {
  override fun getQuery(variables: IVariable): Batch {
    return Batch(variables as BatchVariable)
  }

  override fun createResponse(json: String): ResponseBatch {
    return ResponseBatch(this, json)
  }
}
