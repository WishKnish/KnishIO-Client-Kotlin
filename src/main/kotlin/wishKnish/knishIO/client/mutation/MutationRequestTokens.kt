@file:JvmName("MutationRequestTokens")

package wishKnish.knishIO.client.mutation

import wishKnish.knishIO.client.Molecule
import wishKnish.knishIO.client.data.MetaData
import wishKnish.knishIO.client.httpClient.HttpClient

class MutationRequestTokens @JvmOverloads constructor(
  httpClient: HttpClient,
  override val molecule: Molecule? = null
) : MutationProposeMolecule(httpClient, molecule) {

  @JvmOverloads
  fun fillMolecule(
    token: String,
    amount: Number,
    metaType: String? = null,
    metaId: String? = null,
    meta: MutableList<MetaData> = mutableListOf(),
    batchId: String? = null
  ) {
    molecule?.apply {
      initTokenRequest(token, amount, metaType, metaId, meta, batchId)
      sign()
      check()
    }
  }
}
