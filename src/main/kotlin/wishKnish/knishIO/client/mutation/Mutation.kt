@file:JvmName("Mutation")
package wishKnish.knishIO.client.mutation

import wishKnish.knishIO.client.data.json.query.QueryInterface
import wishKnish.knishIO.client.httpClient.HttpClient
import wishKnish.knishIO.client.query.Query
import wishKnish.knishIO.client.data.json.mutation.Mutation
import wishKnish.knishIO.client.data.json.variables.IVariable
import wishKnish.knishIO.client.response.IResponse


abstract class Mutation(httpClient: HttpClient): Query(httpClient) {
  override fun getQuery(variables: IVariable): QueryInterface {
    return Mutation(variables)
  }

  override suspend fun execute(variables: IVariable): IResponse {
    request = createQuery(variables)

    val resp = client.mutate(request!!)

    response = createResponseRaw(resp)

    return response!!
  }
}
