@file:JvmName("MutationWithdrawBufferToken")

package wishKnish.knishIO.client.mutation

import wishKnish.knishIO.client.Molecule
import wishKnish.knishIO.client.Wallet
import wishKnish.knishIO.client.httpClient.HttpClient

class MutationWithdrawBufferToken @JvmOverloads constructor(
  httpClient: HttpClient,
  override val molecule: Molecule? = null
) : MutationProposeMolecule(httpClient, molecule) {
  @JvmOverloads
  fun fillMolecule(
    recipients: Map<String, Number>,
    signingWallet: Wallet? = null
  ) {
    molecule?.apply {
      initWithdrawBuffer(recipients, signingWallet)
      sign()
      check(sourceWallet)
    }
  }
}
