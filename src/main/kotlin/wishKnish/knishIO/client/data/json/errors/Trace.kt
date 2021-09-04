@file:JvmName("Trace")

package wishKnish.knishIO.client.data.json.errors

import kotlinx.serialization.Serializable

@Serializable data class Trace @JvmOverloads constructor(
  @JvmField val file: String? = null,
  @JvmField val line: Int? = null,
  @JvmField val call: String? = null
)
