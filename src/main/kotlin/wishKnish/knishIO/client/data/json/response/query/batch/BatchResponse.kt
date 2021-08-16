@file:JvmName("BatchResponse")
package wishKnish.knishIO.client.data.json.response.query.batch

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import wishKnish.knishIO.client.data.json.response.IResponse


@Serializable data class BatchResponse(@JvmField var data: Data? = null): IResponse {
  companion object {
    private val jsonFormat: Json
      get() = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        coerceInputValues = true
      }

    @JvmStatic
    fun create(data: Data? = null): BatchResponse {
      return BatchResponse(data)
    }

    @JvmStatic
    fun jsonToObject(json: String): BatchResponse {
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