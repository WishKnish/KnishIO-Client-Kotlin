@file:JvmName("Atom")

package wishKnish.knishIO.client.data.graphql.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable data class Atom @JvmOverloads constructor(
  @JvmField val molecule: Molecule,
  @JvmField val molecularHash: String,
  @JvmField val position: String? = null,
  @JvmField val isotope: String,
  @JvmField val walletAddress: String? = null,
  @JvmField val tokenSlug: String? = null,
  @JvmField val token: Token? = null,
  @JvmField val batchId: String? = null,
  @JvmField val value: String? = null,
  @JvmField val index: Int? = null,
  @JvmField val metaType: String? = null,
  @JvmField val metaId: String? = null,
  @JvmField val metasJson: String? = null,
  @JvmField val metas: List<Meta> = listOf(),
  @JvmField val otsFragment: String,
  @JvmField val createdAt: String
) : IGraphql {
  companion object {
    private val jsonFormat: Json
      get() = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
      }

    @JvmStatic
    fun jsonToObject(json: String): Atom {
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
