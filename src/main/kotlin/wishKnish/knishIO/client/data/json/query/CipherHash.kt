package wishKnish.knishIO.client.data.json.query

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

import wishKnish.knishIO.client.data.json.variables.CipherHashVariable

@Serializable data class CipherHash(@JvmField val variables: CipherHashVariable) : QueryInterface {
  override val query = $$"""query ( $Hash: String! ) { CipherHash ( Hash: $Hash ) { hash } }""".trimIndent()

  companion object {
    private val jsonFormat = Json {
      encodeDefaults = true
      ignoreUnknownKeys = true
    }
  }

  fun toJson(): String {
    return jsonFormat.encodeToString(this)
  }
}
