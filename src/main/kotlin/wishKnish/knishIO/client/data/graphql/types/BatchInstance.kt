@file:JvmName("BatchInstance")
package wishKnish.knishIO.client.data.graphql.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable data class BatchInstance @JvmOverloads constructor(
  @JvmField val batchId: String? = null,
  @JvmField val molecularHash: String? = null,
  @JvmField val type: String? = null,
  @JvmField val status: String? = null,
  @JvmField val createdAt: String? = null,

  @JvmField val wallet: Wallet? = null,
  @JvmField val fromWallet: Wallet? = null,
  @JvmField val toWallet: Wallet? = null,

  @JvmField val children: List<BatchInstance> = listOf(),
  @JvmField val sourceTokenUnits: List<TokenUnit> = listOf(),
  @JvmField val transferTokenUnits: List<TokenUnit> = listOf(),

  @JvmField val metas: List<Meta> = listOf(),
  @JvmField val throughMetas: List<Meta> = listOf()
): IGraphql {
  companion object {
    private val jsonFormat: Json
      get() = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
      }

    @JvmStatic
    fun jsonToObject(json: String): BatchInstance {
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