@file:JvmName("AccessTokenResponse")
package wishKnish.knishIO.client.data.json.response.mutation.accessToken

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import wishKnish.knishIO.client.data.json.response.IResponse


@Serializable data class AccessTokenResponse(@JvmField var data: Data? = null): IResponse {
  companion object {
    private val jsonFormat: Json
      get() = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        coerceInputValues = true
      }

    @JvmStatic
    fun create(data: Data? = null): AccessTokenResponse {
      return AccessTokenResponse(data)
    }

    @JvmStatic
    fun jsonToObject(json: String): AccessTokenResponse {
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