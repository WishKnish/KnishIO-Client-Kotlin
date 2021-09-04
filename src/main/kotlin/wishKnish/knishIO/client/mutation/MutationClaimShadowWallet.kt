@file:JvmName("MutationClaimShadowWallet")

package wishKnish.knishIO.client.mutation

import wishKnish.knishIO.client.Molecule
import wishKnish.knishIO.client.Wallet
import wishKnish.knishIO.client.httpClient.HttpClient

class MutationClaimShadowWallet @JvmOverloads constructor(
  httpClient: HttpClient,
  override val molecule: Molecule? = null
) : MutationProposeMolecule(httpClient, molecule) {

  @JvmOverloads
  fun fillMolecule(
    token: String,
    batchId: String? = null
  ) {
    val wallet = Wallet.create(molecule() !!.secret, token, batchId)
    molecule?.apply {
      initShadowWalletClaim(token, wallet)
      sign()
      check()
    }
  }
}
