@file:JvmName("Version4")

package wishKnish.knishIO.client.versions

import kotlinx.serialization.Serializable
import wishKnish.knishIO.client.Atom
import wishKnish.knishIO.client.data.MetaData

@Serializable class Version4(
  @JvmField var position: String? = null,
  @JvmField var walletAddress: String? = null,
  @JvmField var isotope: Char? = null,
  @JvmField var token: String? = null,
  @JvmField var value: String? = null,
  @JvmField var batchId: String? = null,
  @JvmField var metaType: String? = null,
  @JvmField var metaId: String? = null,
  @JvmField var meta: List<MetaData>? = null,
  @JvmField var index: Int? = null,
  @JvmField val createdAt: String? = null
): HashAtom() {
  companion object {
    @JvmStatic
    fun create(atom: Atom): Version4 = create {
      Version4(
        position = atom.position,
        walletAddress = atom.walletAddress,
        isotope = atom.isotope,
        token = atom.token,
        value = atom.value,
        batchId = atom.batchId,
        metaType = atom.metaType,
        metaId = atom.metaId,
        meta = atom.meta,
        index = atom.index,
        createdAt = atom.createdAt
      )
    }
  }
}
