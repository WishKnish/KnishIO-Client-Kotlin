@file:JvmName("Meta")

package wishKnish.knishIO.client

import kotlinx.serialization.Serializable
import wishKnish.knishIO.client.data.MetaData
import wishKnish.knishIO.client.libraries.Strings

@Serializable data class Meta @JvmOverloads constructor(
  @JvmField var modelType: String,
  @JvmField var modelId: String,
  @JvmField var meta: List<MetaData> = mutableListOf(),
  @JvmField var snapshotMolecule: String? = null,
  @JvmField val createdAt: String = Strings.currentTimeMillis()
)
