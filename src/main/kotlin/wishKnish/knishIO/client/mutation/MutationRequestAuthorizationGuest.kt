@file:JvmName("MutationRequestAuthorizationGuest")
package wishKnish.knishIO.client.mutation

import wishKnish.knishIO.client.Wallet
import wishKnish.knishIO.client.data.json.mutation.AccessTokenMutation
import wishKnish.knishIO.client.data.json.query.QueryInterface
import wishKnish.knishIO.client.data.json.variables.AccessTokenMutationVariable
import wishKnish.knishIO.client.data.json.variables.IVariable
import wishKnish.knishIO.client.httpClient.HttpClient
import wishKnish.knishIO.client.response.ResponseRequestAuthorizationGuest


class MutationRequestAuthorizationGuest(httpClient: HttpClient): Mutation(httpClient) {
  var wallet: Wallet? = null

  override fun getQuery(variables: IVariable): QueryInterface {
    return AccessTokenMutation(variables as AccessTokenMutationVariable)
  }

  fun setAuthorizationWallet ( wallet: Wallet ) {
    this.wallet = wallet;
  }

  override fun createResponse(json: String): ResponseRequestAuthorizationGuest {
    return ResponseRequestAuthorizationGuest(this, json)
  }
}
