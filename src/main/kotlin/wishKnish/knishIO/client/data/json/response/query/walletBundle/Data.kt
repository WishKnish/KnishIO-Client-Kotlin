@file:JvmName("Data")
package wishKnish.knishIO.client.data.json.response.query.walletBundle

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import wishKnish.knishIO.client.data.graphql.types.WalletBundle


@Serializable data class Data @JvmOverloads constructor(@JvmField var WalletBundle: List<WalletBundle>? = null) {
  companion object {
    private val jsonFormat: Json
      get() = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        coerceInputValues = true
      }

    @JvmStatic
    @JvmOverloads
    fun create(data: List<WalletBundle>? = null): Data {
      return Data(data)
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
