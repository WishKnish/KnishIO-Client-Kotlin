@file:JvmName("QueryActiveSession")
package wishKnish.knishIO.client.query

import wishKnish.knishIO.client.data.json.query.ActiveSession
import wishKnish.knishIO.client.data.json.variables.ActiveSessionVariable
import wishKnish.knishIO.client.data.json.variables.IVariable
import wishKnish.knishIO.client.httpClient.HttpClient
import wishKnish.knishIO.client.response.ResponseActiveSession

  class QueryActiveSession(client: HttpClient): Query(client) {
  override fun createResponse(json: String): ResponseActiveSession {
    return ResponseActiveSession(this, json)
  }

  override fun getQuery(variables: IVariable): ActiveSession {
    return ActiveSession(variables as ActiveSessionVariable)
  }
}
