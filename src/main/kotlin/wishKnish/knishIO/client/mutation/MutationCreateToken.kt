@file:JvmName("MutationCreateToken")

package wishKnish.knishIO.client.mutation

import wishKnish.knishIO.client.Molecule
import wishKnish.knishIO.client.Wallet
import wishKnish.knishIO.client.data.MetaData
import wishKnish.knishIO.client.httpClient.HttpClient

class MutationCreateToken @JvmOverloads constructor(
  httpClient: HttpClient,
  override val molecule: Molecule? = null
) : MutationProposeMolecule(httpClient, molecule) {

  fun fillMolecule(
    recipientWallet: Wallet,
    amount: Number,
    meta: MutableList<MetaData>
  ) {
    molecule?.apply {
      initTokenCreation(recipientWallet, amount, meta)
      sign()
      check()
    }
  }
}
