@file:JvmName("MetaData")

package wishKnish.knishIO.client.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


@Serializable data class MetaData @JvmOverloads constructor(
  @JvmField val key: String,
  @JvmField var value: String? = null
) {
  companion object {
    private val jsonFormat: Json
      get() = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
      }

    @JvmStatic
    @JvmOverloads
    fun create(
      key: String,
      value: String? = null
    ): MetaData {
      return MetaData(key, value)
    }

    @JvmStatic
    fun jsonToObject(json: String): MetaData {
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
