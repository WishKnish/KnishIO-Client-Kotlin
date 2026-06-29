@file:JvmName("Data")

package wishKnish.knishIO.client.data.json.response.query.balance

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import wishKnish.knishIO.client.data.graphql.types.Wallet

// Property MUST be named `Balance` — it is both the JSON key the validator returns
// (data.Balance) and the segment Response.data() navigates by reflection (dataKey
// "data.Balance"). A mismatched name (was `balanceWallet`) left it null on deserialize
// AND threw "Response does not match the key" on navigation.
@Serializable data class Data @JvmOverloads constructor(@JvmField var Balance: Wallet? = null) {
  companion object {
    private val jsonFormat: Json
      get() = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        coerceInputValues = true
      }

    @JvmStatic
    @JvmOverloads
    fun create(balanceWallet: Wallet? = null): Data {
      return Data(balanceWallet)
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
