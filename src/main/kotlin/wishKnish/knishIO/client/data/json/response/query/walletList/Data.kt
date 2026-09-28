@file:JvmName("Data")

package wishKnish.knishIO.client.data.json.response.query.walletList

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import wishKnish.knishIO.client.data.graphql.types.Wallet


// Property MUST be named `Wallet` — it is both the JSON key the validator returns (data.Wallet)
// and the segment Response.data() navigates by reflection (dataKey "data.Wallet"), as for the
// Balance response. The former name `wallets` left it null on deserialize, so every
// queryWallets/queryShadowWallets threw "Response does not match the key".
@Serializable data class Data @JvmOverloads constructor(@JvmField var Wallet: List<Wallet>? = null) {
  companion object {
    private val jsonFormat: Json
      get() = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        coerceInputValues = true
      }

    @JvmStatic
    @JvmOverloads
    fun create(data: List<Wallet>? = null): Data {
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
