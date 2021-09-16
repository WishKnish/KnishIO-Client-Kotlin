@file:JvmName("ResponseRequestAuthorizationGuest")

package wishKnish.knishIO.client.response

import wishKnish.knishIO.client.data.graphql.types.AccessToken
import wishKnish.knishIO.client.data.json.response.mutation.accessToken.AccessTokenResponse
import wishKnish.knishIO.client.exception.InvalidResponseException
import wishKnish.knishIO.client.mutation.MutationRequestAuthorizationGuest
import kotlin.jvm.Throws

class ResponseRequestAuthorizationGuest(
  query: MutationRequestAuthorizationGuest,
  json: String,
) : Response(query, json, "data.AccessToken"), IResponseRequestAuthorization {

  override fun reason(): String {
    return "Invalid response from server"
  }

  override fun mapping(response: String): AccessTokenResponse {
    return AccessTokenResponse.jsonToObject(response)
  }

  override fun success(): Boolean {
    return payload() != null
  }

  override fun payload(): AccessToken? {
    return data() as? AccessToken
  }

  @Throws(InvalidResponseException::class)
  override fun token(): String {
    return payload()?.token ?: throw InvalidResponseException("ResponseAuthorizationGuest::token() - 'token' key is not found in the payload!")
  }

  @Throws(InvalidResponseException::class)
  override fun time(): Int {
    return payload()?.time ?: throw InvalidResponseException("ResponseAuthorizationGuest::time() - 'time' key is not found in the payload!")
  }

  @Throws(InvalidResponseException::class)
  override fun pubKey(): String {
    return payload()?.key ?: throw InvalidResponseException("ResponseAuthorizationGuest::pubKey() - 'pubKey' key is not found in the payload!")
  }

  override fun encrypt(): Boolean {
    return payload()?.encrypt ?: throw InvalidResponseException("ResponseAuthorizationGuest::encrypt() - 'pubKey' key is not found in the encrypt!")
  }
}
