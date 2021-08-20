@file:JvmName("BalanceResponse")
package wishKnish.knishIO.client.data.json.response.query.balance

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import wishKnish.knishIO.client.data.json.errors.Errors
import wishKnish.knishIO.client.data.json.response.IResponse


@Serializable data class BalanceResponse @JvmOverloads constructor(@JvmField var data: Data? = null): IResponse {
  override val errors: List<Errors> = listOf()
  override val message: String? = null
  override val exception: Boolean? = null

  companion object {
    private val jsonFormat: Json
      get() = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        coerceInputValues = true
      }

    @JvmStatic
    @JvmOverloads
    fun create(data: Data? = null): BalanceResponse {
      return BalanceResponse(data)
    }

    @JvmStatic
    fun jsonToObject(json: String): BalanceResponse {
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
