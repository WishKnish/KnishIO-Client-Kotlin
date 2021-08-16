@file:JvmName("Wallet")
package wishKnish.knishIO.client.data.graphql.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable data class Wallet @JvmOverloads constructor(
  @JvmField val address: String? = null,
  @JvmField val bundleHash: String? = null,
  @JvmField val walletBundle: WalletBundle? = null,
  @JvmField val tokenSlug: String? = null,
  @JvmField val token: Token? = null,
  @JvmField val batchId: String? = null,
  @JvmField val position: String? = null,
  @JvmField val characters: String? = null,
  @JvmField val pubkey: String? = null,
  @JvmField val amount: String? = null,
  @JvmField val metas: List<Meta> = listOf(),
  @JvmField val atoms: List<Atom> = listOf(),
  @JvmField val molecules: List<Molecule> = listOf(),
  @JvmField val tokenUnits: List<TokenUnit> = listOf(),
  @JvmField val createdAt: String? = null
): IGraphql {
  companion object {
    private val jsonFormat: Json
      get() = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
      }

    @JvmStatic
    fun jsonToObject(json: String): Wallet {
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