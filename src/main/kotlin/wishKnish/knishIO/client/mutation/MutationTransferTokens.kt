@file:JvmName("MutationTransferTokens")

package wishKnish.knishIO.client.mutation

import wishKnish.knishIO.client.Molecule
import wishKnish.knishIO.client.Wallet
import wishKnish.knishIO.client.httpClient.HttpClient

class MutationTransferTokens @JvmOverloads constructor(
  httpClient: HttpClient,
  override val molecule: Molecule? = null
) : MutationProposeMolecule(httpClient, molecule) {
  fun fillMolecule(
    recipientWallet: Wallet,
    amount: Number
  ) {
    molecule?.apply {
      initValue(recipientWallet, amount)
      sign()
      check(sourceWallet)
    }
  }
}
