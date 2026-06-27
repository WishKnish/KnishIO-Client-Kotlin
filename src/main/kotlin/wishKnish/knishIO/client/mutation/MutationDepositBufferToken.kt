@file:JvmName("MutationDepositBufferToken")

package wishKnish.knishIO.client.mutation

import wishKnish.knishIO.client.Molecule
import wishKnish.knishIO.client.httpClient.HttpClient

class MutationDepositBufferToken @JvmOverloads constructor(
  httpClient: HttpClient,
  override val molecule: Molecule? = null
) : MutationProposeMolecule(httpClient, molecule) {
  @JvmOverloads
  fun fillMolecule(
    amount: Number,
    tradeRates: Map<String, Any> = emptyMap()
  ) {
    molecule?.apply {
      initDepositBuffer(amount, tradeRates)
      sign()
      check(sourceWallet)
    }
  }
}
