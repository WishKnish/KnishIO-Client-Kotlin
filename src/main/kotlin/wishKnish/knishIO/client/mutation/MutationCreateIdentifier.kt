@file:JvmName("MutationCreateIdentifier")
package wishKnish.knishIO.client.mutation

import wishKnish.knishIO.client.Molecule
import wishKnish.knishIO.client.httpClient.HttpClient

class MutationCreateIdentifier @JvmOverloads constructor(
  httpClient: HttpClient,
  override val molecule: Molecule? = null
): MutationProposeMolecule(httpClient, molecule) {

  fun fillMolecule(
    type: String,
    contact: String,
    code: String
  ) {
    molecule?.apply {
      initIdentifierCreation(type, contact, code)
      sign()
      check()
    }
  }
}
