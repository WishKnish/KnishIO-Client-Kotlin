@file:JvmName("UnitData")

package wishKnish.knishIO.client.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


@Serializable data class UnitData(
  @JvmField val id: String,
  @JvmField val name: String,
  @JvmField val metas: List<String>
) {
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
    ): UnitData {
      return UnitData(id, name, metas)
    }

    @JvmStatic
    fun jsonToObject(json: String): UnitData {
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
