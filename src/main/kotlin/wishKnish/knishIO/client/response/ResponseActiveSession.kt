@file:JvmName("ResponseActiveSession")

package wishKnish.knishIO.client.response

import wishKnish.knishIO.client.data.graphql.types.Session
import wishKnish.knishIO.client.data.json.response.query.activeSession.ActiveSessionResponse
import wishKnish.knishIO.client.query.QueryActiveSession

class ResponseActiveSession(
  query: QueryActiveSession,
  json: String,
) : Response(query, json, "data.ActiveUser") {

  override fun data(): List<Session>? {
    @Suppress("UNCHECKED_CAST") return super.data() as List<Session>?
  }

  override fun mapping(response: String): ActiveSessionResponse {
    return ActiveSessionResponse.jsonToObject(response)
  }
}
