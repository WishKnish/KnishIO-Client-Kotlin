@file:JvmName("ResponseUserActivity")
package wishKnish.knishIO.client.response

import wishKnish.knishIO.client.data.graphql.types.UserActivityMetaType
import wishKnish.knishIO.client.data.json.response.query.userActivity.UserActivityResponse
import wishKnish.knishIO.client.query.QueryUserActivity

class ResponseUserActivity(
  query: QueryUserActivity,
  json: String,
): Response(query, json, "data.UserActivity") {
  override fun data(): UserActivityMetaType? {
    return super.data() as? UserActivityMetaType
  }

  override fun mapping(response: String): UserActivityResponse {
    return UserActivityResponse.jsonToObject(response)
  }

  override fun payload(): UserActivityMetaType? {
    return data()
  }
}
