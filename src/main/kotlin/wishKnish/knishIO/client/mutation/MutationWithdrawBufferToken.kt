@file:JvmName("MutationWithdrawBufferToken")

package wishKnish.knishIO.client.mutation

import wishKnish.knishIO.client.Molecule
import wishKnish.knishIO.client.httpClient.HttpClient

class MutationWithdrawBufferToken @JvmOverloads constructor(
  httpClient: HttpClient,
  override val molecule: Molecule? = null
) : MutationProposeMolecule(httpClient, molecule) {
  fun fillMolecule(recipients: Map<String, Number>) {
    molecule?.apply {
      initWithdrawBuffer(recipients)
      sign()
      check(sourceWallet)
    }
  }
}
