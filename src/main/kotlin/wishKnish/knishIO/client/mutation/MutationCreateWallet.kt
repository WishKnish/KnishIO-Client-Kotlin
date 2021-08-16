@file:JvmName("MutationCreateWallet")
package wishKnish.knishIO.client.mutation

import wishKnish.knishIO.client.Molecule
import wishKnish.knishIO.client.Wallet
import wishKnish.knishIO.client.httpClient.HttpClient

class MutationCreateWallet @JvmOverloads constructor(
  httpClient: HttpClient,
  override val molecule: Molecule? = null
): MutationProposeMolecule(httpClient, molecule) {

  fun fillMolecule(newWallet: Wallet) {
    molecule?.apply {
      initWalletCreation(newWallet)
      sign()
      check()
    }
  }
}
