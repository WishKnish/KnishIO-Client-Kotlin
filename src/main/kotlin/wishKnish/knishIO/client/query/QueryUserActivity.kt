@file:JvmName("QueryUserActivity")

package wishKnish.knishIO.client.query

import wishKnish.knishIO.client.data.json.query.UserActivity
import wishKnish.knishIO.client.data.json.variables.IVariable
import wishKnish.knishIO.client.data.json.variables.UserActivityVariable
import wishKnish.knishIO.client.httpClient.HttpClient
import wishKnish.knishIO.client.response.ResponseUserActivity


class QueryUserActivity(client: HttpClient) : Query(client) {
  override fun createResponse(json: String): ResponseUserActivity {
    return ResponseUserActivity(this, json)
  }

  override fun getQuery(variables: IVariable): UserActivity {
    return UserActivity(variables as UserActivityVariable)
  }
}
