@file:JvmName("AccessToken")
package wishKnish.knishIO.client.data.graphql.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable data class AccessToken @JvmOverloads constructor(
  @JvmField val token: String,
  @JvmField val time: Int,
  @JvmField val key: String,
  @JvmField val encrypt: Boolean? = null,
  @JvmField val expiresAt: Int
): IGraphql {
  companion object {
    private val jsonFormat: Json
      get() = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
      }

    @JvmStatic
    fun jsonToObject(json: String): AccessToken {
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
