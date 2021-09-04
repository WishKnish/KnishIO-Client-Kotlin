@file:JvmName("WalletBundle")

package wishKnish.knishIO.client.data.graphql.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable data class WalletBundle @JvmOverloads constructor(
  @JvmField val bundleHash: String? = null,
  @JvmField val slug: String? = null,
  @JvmField val metas: List<Meta> = listOf(),
  @JvmField val molecules: List<Molecule> = listOf(),
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
    fun jsonToObject(json: String): WalletBundle {
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