@file:JvmName("MoleculeResponse")

package wishKnish.knishIO.client.data.json.response.mutation.molecule

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import wishKnish.knishIO.client.data.json.errors.Errors
import wishKnish.knishIO.client.data.json.response.IResponse


@Serializable data class MoleculeResponse @JvmOverloads constructor(@JvmField var data: Data? = null) : IResponse {
  override val errors: List<Errors> = listOf()
  override val message: String? = null
  override val exception: Boolean? = null

  companion object {
    private val jsonFormat: Json
      get() = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        coerceInputValues = true
      }

    @JvmStatic
    @JvmOverloads
    fun create(data: Data? = null): MoleculeResponse {
      return MoleculeResponse(data)
    }

    @JvmStatic
    fun jsonToObject(json: String): MoleculeResponse {
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
