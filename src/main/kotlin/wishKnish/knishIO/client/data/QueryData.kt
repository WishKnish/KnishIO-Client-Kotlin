package wishKnish.knishIO.client.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import wishKnish.knishIO.client.Molecule
import wishKnish.knishIO.client.data.serializers.QueryDataSerializer


@Serializable(with = QueryDataSerializer::class) data class QueryData @JvmOverloads constructor(
  @JvmField val query: String,
  @JvmField val variables: String? = null,
) {
  @Transient var molecule: Molecule? = null

  companion object {
    private val jsonFormat: Json
      get() = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
      }

    @JvmStatic
    fun jsonToObject(json: String): QueryData {
      return jsonFormat.decodeFromString(json)
    }
  }
}
