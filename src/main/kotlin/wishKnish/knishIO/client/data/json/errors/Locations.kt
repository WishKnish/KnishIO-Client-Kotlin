@file:JvmName("Locations")

package wishKnish.knishIO.client.data.json.errors

import kotlinx.serialization.Serializable

@Serializable data class Locations @JvmOverloads constructor(
  @JvmField val line: Int? = null,
  @JvmField val column: Int? = null
)
