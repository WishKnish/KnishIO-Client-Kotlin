@file:JvmName("QueryMetaType")

package wishKnish.knishIO.client.query

import wishKnish.knishIO.client.data.json.query.MetaType
import wishKnish.knishIO.client.data.json.query.QueryInterface
import wishKnish.knishIO.client.data.json.variables.MetaTypeVariable
import wishKnish.knishIO.client.data.json.variables.IVariable
import wishKnish.knishIO.client.httpClient.HttpClient
import wishKnish.knishIO.client.response.ResponseMetaType

class QueryMetaType(client: HttpClient) : Query(client) {
  override fun getQuery(variables: IVariable): QueryInterface {
    return MetaType(variables as MetaTypeVariable)
  }

  override fun createResponse(json: String): ResponseMetaType {
    return ResponseMetaType(this, json)
  }
}
