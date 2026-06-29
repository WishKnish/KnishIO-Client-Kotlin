@file:JvmName("Data")

package wishKnish.knishIO.client.data.json.response.query.cipherHash

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


// The validator returns the envelope under the PascalCase GraphQL field name `CipherHash`
// (`{"data":{"CipherHash":{"hash":...}}}`). Without @SerialName, kotlinx matches keys
// case-sensitively and (with ignoreUnknownKeys=true) silently drops it → cipherHash stays null →
// the encrypted response decrypts to nothing. @SerialName binds the camelCase field to that key.
@Serializable data class Data @JvmOverloads constructor(@JvmField @SerialName("CipherHash") var cipherHash: DataHash? = null) {
  companion object {
    private val jsonFormat: Json
      get() = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        coerceInputValues = true
      }

    @JvmStatic
    @JvmOverloads
    fun create(data: DataHash? = null): Data {
      return Data(data)
    }

    @JvmStatic
    fun jsonToObject(json: String): Data {
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
