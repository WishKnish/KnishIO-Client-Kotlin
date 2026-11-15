@file:JvmName("Errors")

package wishKnish.knishIO.client.data.json.errors

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable data class Errors @JvmOverloads constructor(
  @JvmField val debugMessage: String? = null,
  @JvmField val message: String? = null,
  @JvmField val extensions: Extensions? = null,
  @JvmField val locations: List<Locations> = listOf(),
  // A GraphQL path segment is a field name or a list index: ["MetaType", 0, "instances"].
  @JvmField val path: List<JsonPrimitive> = listOf(),
  @JvmField val trace: List<Trace> = listOf()
)
