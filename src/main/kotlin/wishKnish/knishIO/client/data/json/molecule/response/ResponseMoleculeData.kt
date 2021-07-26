@file:JvmName("ProposeMoleculeData")

package wishKnish.knishIO.client.data.json.molecule.response

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable class ResponseMoleculeData(@JvmField var data: DataData? = null) {
  companion object {
    private val jsonFormat: Json
      get() = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        coerceInputValues = true
      }

    @JvmStatic
    fun create(data: DataData? = null): ResponseMoleculeData {
      return ResponseMoleculeData(data)
    }

    @JvmStatic
    fun jsonToObject(json: String): ResponseMoleculeData {
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