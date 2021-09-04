@file:JvmName("Extensions")

package wishKnish.knishIO.client.data.json.errors

import kotlinx.serialization.Serializable

@Serializable data class Extensions @JvmOverloads constructor(
  @JvmField val category: String? = null
)
