@file:JvmName("Meta")
package wishKnish.knishIO.client.data.graphql.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable data class Meta @JvmOverloads constructor(
  @JvmField val molecularHash: String,
  @JvmField val position: String,
  @JvmField val metaType: String?  = null,
  @JvmField val metaId: String?  = null,
  @JvmField val key: String,
  @JvmField val value: String? = null,
  @JvmField val createdAt: String? = null,
  @JvmField val molecule: Molecule? = null,
  @JvmField val atoms: List<Atom> = listOf(),
  @JvmField val atom: Atom? = null
): IGraphql {
  companion object {
    private val jsonFormat: Json
      get() = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
      }

    @JvmStatic
    fun jsonToObject(json: String): Meta {
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