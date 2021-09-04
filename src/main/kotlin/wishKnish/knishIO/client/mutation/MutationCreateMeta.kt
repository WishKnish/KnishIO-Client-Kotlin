@file:JvmName("MutationCreateMeta")

package wishKnish.knishIO.client.mutation

import wishKnish.knishIO.client.Molecule
import wishKnish.knishIO.client.data.MetaData
import wishKnish.knishIO.client.httpClient.HttpClient

class MutationCreateMeta @JvmOverloads constructor(
  httpClient: HttpClient,
  override val molecule: Molecule? = null
) : MutationProposeMolecule(httpClient, molecule) {

  @JvmOverloads
  fun fillMolecule(
    metaType: String,
    metaId: String,
    meta: MutableList<MetaData> = mutableListOf()
  ) {
    molecule?.apply {
      initMeta(meta, metaType, metaId)
      sign()
      check()
    }
  }
}
