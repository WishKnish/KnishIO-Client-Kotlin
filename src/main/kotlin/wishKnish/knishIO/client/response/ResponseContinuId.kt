@file:JvmName("ResponseContinuId")

package wishKnish.knishIO.client.response

import wishKnish.knishIO.client.Wallet
import wishKnish.knishIO.client.data.json.response.query.continuId.ContinuIdResponse
import wishKnish.knishIO.client.query.QueryContinuId
import wishKnish.knishIO.client.data.graphql.types.Wallet as GraphqlWallet

class ResponseContinuId(
  query: QueryContinuId,
  json: String,
) : Response(query, json, "data.ContinuId") {

  override fun mapping(response: String): ContinuIdResponse {
    return ContinuIdResponse.jsonToObject(response)
  }

  override fun data(): GraphqlWallet? {
    return super.data() as? GraphqlWallet
  }

  override fun payload(): Wallet? {
    return data()?.let {
      Wallet(token = it.tokenSlug as String).apply {
        address = it.address
        position = it.position
        bundle = it.bundleHash
        batchId = it.batchId
        characters = it.characters
        pubkey = it.pubkey
        balance = it.amount !!.toDouble()
      }
    }
  }
}