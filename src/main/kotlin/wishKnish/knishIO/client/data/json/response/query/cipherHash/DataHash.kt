@file:JvmName("DataHash")

package wishKnish.knishIO.client.data.json.response.query.cipherHash

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable data class DataHash @JvmOverloads constructor(@JvmField val hash: String? = null) {
  companion object {
    private val jsonFormat: Json
      get() = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        coerceInputValues = true
      }

    @JvmStatic
    @JvmOverloads
    fun create(data: String? = null): DataHash {
      return DataHash(data)
    }

    @JvmStatic
    fun jsonToObject(json: String): DataHash {
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
