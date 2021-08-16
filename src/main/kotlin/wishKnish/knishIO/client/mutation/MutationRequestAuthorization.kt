@file:JvmName("MutationRequestAuthorization")
package wishKnish.knishIO.client.mutation

import wishKnish.knishIO.client.Molecule
import wishKnish.knishIO.client.data.MetaData
import wishKnish.knishIO.client.httpClient.HttpClient
import wishKnish.knishIO.client.response.ResponseRequestAuthorization

class MutationRequestAuthorization @JvmOverloads constructor(
  httpClient: HttpClient,
  override val molecule: Molecule? = null
): MutationProposeMolecule(httpClient, molecule) {
  fun fillMolecule(meta: List<MetaData>) {
    molecule()?.run {
      initAuthorization(meta.toMutableList())
      sign()
      check()
    }
  }

  override fun createResponse(json: String): ResponseRequestAuthorization {
    return ResponseRequestAuthorization(this, json)
  }
}
