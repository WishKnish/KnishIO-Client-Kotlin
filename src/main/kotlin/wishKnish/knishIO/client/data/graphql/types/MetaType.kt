@file:JvmName("MetaType")

package wishKnish.knishIO.client.data.graphql.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable data class MetaType @JvmOverloads constructor(
  @JvmField val metaType: String? = null,
  @JvmField val createdAt: String? = null,
  @JvmField val instances: List<MetaInstance>? = null,
  @JvmField val instanceCount: List<KeyValueInt>? = null,
  @JvmField val metas: List<Meta>? = null,
  @JvmField val paginatorInfo: Paginator? = null
) : IGraphql {
  companion object {
    private val jsonFormat: Json
      get() = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
      }

    @JvmStatic
    fun jsonToObject(json: String): MetaType {
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
