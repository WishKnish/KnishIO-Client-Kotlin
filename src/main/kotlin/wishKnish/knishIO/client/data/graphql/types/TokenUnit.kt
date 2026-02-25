@file:JvmName("TokenUnit")

package wishKnish.knishIO.client.data.graphql.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


@Serializable data class TokenUnit(
  @JvmField val id: String,
  @JvmField val name: String,
  @JvmField val metas: List<String>
) : IGraphql {
  companion object {
    private val jsonFormat: Json
      get() = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
      }

    @JvmStatic
    fun create(
      id: String,
      name: String,
      metas: List<String>
    ): TokenUnit {
      return TokenUnit(id, name, metas)
    }

    @JvmStatic
    fun jsonToObject(json: String): TokenUnit {
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
