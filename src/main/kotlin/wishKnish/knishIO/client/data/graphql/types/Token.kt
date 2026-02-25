@file:JvmName("Token")

package wishKnish.knishIO.client.data.graphql.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable data class Token @JvmOverloads constructor(
  @JvmField val slug: String? = null,
  @JvmField val name: String? = null,
  @JvmField val fungibility: String? = null,
  @JvmField val supply: String? = null,
  @JvmField val decimals: String? = null,
  @JvmField val amount: String? = null,
  @JvmField val icon: String? = null,
  @JvmField val metas: List<Meta> = listOf(),
  @JvmField val atoms: List<Atom> = listOf(),
  @JvmField val wallets: List<Wallet> = listOf(),
  @JvmField val createdAt: String? = null
) : IGraphql {
  companion object {
    private val jsonFormat: Json
      get() = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
      }

    @JvmStatic
    fun jsonToObject(json: String): Token {
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