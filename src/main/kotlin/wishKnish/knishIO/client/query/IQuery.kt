@file:JvmName("IQuery")

package wishKnish.knishIO.client.query

import wishKnish.knishIO.client.data.json.query.QueryInterface
import wishKnish.knishIO.client.data.json.variables.IVariable
import wishKnish.knishIO.client.response.IResponse

interface IQuery {
  fun createResponse(json: String): IResponse
  fun getQuery(variables: IVariable): QueryInterface
}
