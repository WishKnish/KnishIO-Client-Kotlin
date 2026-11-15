@file:JvmName("KeyValueInt")

package wishKnish.knishIO.client.data.graphql.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// MetaType instanceCount entries: the validator's InstanceCount.value is an integer.
@Serializable data class KeyValueInt @JvmOverloads constructor(
  @JvmField val key: String? = null,
  @JvmField val value: Int? = null
) : IGraphql {
  companion object {
    private val jsonFormat: Json
      get() = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
      }

    @JvmStatic
    fun jsonToObject(json: String): KeyValueInt {
      return jsonFormat.decodeFromString(json)
    }
  }

  private fun toJson(): String {
    return jsonFormat.encodeToString(this)
  }

  override fun toString(): String {
    return toJson()
  }
}
