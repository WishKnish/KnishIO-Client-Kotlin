@file:JvmName("Data")

package wishKnish.knishIO.client.data.json.response.query.metaType

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import wishKnish.knishIO.client.data.graphql.types.MetaType

// Property MUST be named `MetaType` — it is both the JSON key the validator returns
// (data.MetaType) and the segment Response.data() navigates by reflection (dataKey
// "data.MetaType"). A mismatched name (was `metaTypes`) left it null on deserialize
// AND threw "Response does not match the key" on navigation.
@Serializable data class Data @JvmOverloads constructor(@JvmField var MetaType: List<MetaType>? = null) {
  companion object {
    private val jsonFormat: Json
      get() = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        coerceInputValues = true
      }

    @JvmStatic
    @JvmOverloads
    fun create(metaType: List<MetaType>? = null): Data {
      return Data(metaType)
    }

    @JvmStatic
    fun jsonToObject(json: String): Data {
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
