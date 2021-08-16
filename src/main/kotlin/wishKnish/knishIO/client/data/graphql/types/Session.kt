@file:JvmName("Session")
package wishKnish.knishIO.client.data.graphql.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable data class Session @JvmOverloads constructor(
  @JvmField val bundleHash: String? = null,
  @JvmField val metaType: String? = null,
  @JvmField val metaId: String? = null,
  @JvmField val jsonData: String? = null,
  @JvmField val createdAt: String? = null,
  @JvmField val updatedAt: String? = null
): IGraphql {
  companion object {
    private val jsonFormat: Json
      get() = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
      }

    @JvmStatic
    fun jsonToObject(json: String): Session {
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
