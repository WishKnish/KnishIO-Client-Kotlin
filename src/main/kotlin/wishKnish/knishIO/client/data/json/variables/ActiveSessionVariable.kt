@file:JvmName("ActiveSessionVariable")

package wishKnish.knishIO.client.data.json.variables

import kotlinx.serialization.Serializable

@Serializable data class ActiveSessionVariable @JvmOverloads constructor(
  @JvmField val bundleHash: String? = null,
  @JvmField val metaType: String? = null,
  @JvmField val metaId: String? = null
) : IVariable
