@file:JvmName("Extensions")
package wishKnish.knishIO.client.data.json.errors

import kotlinx.serialization.Serializable

@Serializable data class Extensions(
  @JvmField val category: String? = null
)
