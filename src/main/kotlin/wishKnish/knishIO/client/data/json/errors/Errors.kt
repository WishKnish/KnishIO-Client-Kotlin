@file:JvmName("Errors")
package wishKnish.knishIO.client.data.json.errors

import kotlinx.serialization.Serializable

@Serializable data class Errors @JvmOverloads constructor(
  @JvmField val debugMessage: String? = null,
  @JvmField val message: String? = null,
  @JvmField val extensions: Extensions? = null,
  @JvmField val locations: List<Locations> = listOf(),
  @JvmField val path: List<String> = listOf(),
  @JvmField val trace: List<Trace> = listOf()
)
