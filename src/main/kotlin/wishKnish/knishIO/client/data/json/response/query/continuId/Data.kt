@file:JvmName("Data")

package wishKnish.knishIO.client.data.json.response.query.continuId

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import wishKnish.knishIO.client.data.graphql.types.Wallet


// Property named `ContinuId` to match BOTH the wire field (data.ContinuId) for kotlinx
// deserialization AND the abstract Response's dataKey reflection-navigation by property name
// (mirrors the working balance Data, whose property is `Balance`). The prior `continuIdWallet`
// name matched neither -> the response neither deserialized nor navigated (threw / returned null).
@Serializable data class Data @JvmOverloads constructor(@JvmField var ContinuId: Wallet? = null) {
  companion object {
    private val jsonFormat: Json
      get() = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        coerceInputValues = true
      }

    @JvmStatic
    @JvmOverloads
    fun create(continuId: Wallet? = null): Data {
      return Data(continuId)
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
