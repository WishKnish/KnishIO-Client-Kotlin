@file:JvmName("Molecule")

package wishKnish.knishIO.client.data.graphql.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable data class Molecule @JvmOverloads constructor(
  @JvmField val molecularHash: String? = null,
  @JvmField val cellSlug: String? = null,
  @JvmField val counterparty: String? = null,
  @JvmField val bundleHash: String? = null,
  @JvmField val status: String? = null,
  @JvmField val local: Int? = null,
  @JvmField val height: Int? = null,
  @JvmField val depth: Int? = null,
  @JvmField val createdAt: String? = null,
  @JvmField val receivedAt: String? = null,
  @JvmField val processedAt: String? = null,
  @JvmField val broadcastedAt: String? = null,
  @JvmField val metas: List<Meta> = listOf(),
  @JvmField val atoms: List<Atom> = listOf(),
  @JvmField val bonds: List<Molecule> = listOf(),
  @JvmField val cascades: List<Molecule> = listOf(),
  @JvmField val reason: String? = null,
  @JvmField val reasonPayload: String? = null,
  @JvmField val payload: String? = null
) : IGraphql {
  companion object {
    private val jsonFormat: Json
      get() = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
      }

    @JvmStatic
    fun jsonToObject(json: String): Molecule {
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