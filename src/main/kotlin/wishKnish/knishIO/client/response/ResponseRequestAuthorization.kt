@file:JvmName("ResponseRequestAuthorization")

package wishKnish.knishIO.client.response

import wishKnish.knishIO.client.Wallet
import wishKnish.knishIO.client.data.graphql.types.AccessToken
import wishKnish.knishIO.client.exception.InvalidResponseException
import wishKnish.knishIO.client.mutation.MutationRequestAuthorization

class ResponseRequestAuthorization(
  query: MutationRequestAuthorization,
  json: String,
) : ResponseProposeMolecule(query, json), IResponseRequestAuthorization {
  var payload: AccessToken? = null

  init {
    initialization()
  }

  override fun wallet(): Wallet {
    return clientMolecule() !!.sourceWallet
  }

  override fun initialization() {
    val data = data()

    payload = data?.payload?.let {
      AccessToken.jsonToObject(it)
    }
  }

  override fun payload(): AccessToken? {
    return payload
  }

  override fun success(): Boolean {
    return payload() != null
  }

  override fun token(): String {
    return payload?.token ?: throw InvalidResponseException("ResponseRequestAuthorization::token() - response missing payload")
  }

  override fun time(): Int {
    return payload?.time ?: throw InvalidResponseException("ResponseRequestAuthorization::token() - response missing time")
  }

  override fun encrypt(): Boolean {
    return payload?.encrypt ?: throw InvalidResponseException("ResponseRequestAuthorization::token() - response missing encrypt")
  }

  override fun pubKey(): String {
    return payload?.key ?: throw InvalidResponseException("ResponseRequestAuthorization::token() - response missing key")
  }
}
