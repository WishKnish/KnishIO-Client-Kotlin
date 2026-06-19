@file:JvmName("Data")

package wishKnish.knishIO.client.data.json.response.mutation.molecule

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import wishKnish.knishIO.client.data.graphql.types.Molecule

// Property MUST be named `ProposeMolecule` — it is both the JSON key the validator
// returns (data.ProposeMolecule) and the segment Response.data() navigates by reflection
// (dataKey "data.ProposeMolecule"). A mismatched name (was `proposedMolecule`) left it
// null on deserialize AND threw "Response does not match the key" on navigation.
@Serializable data class Data @JvmOverloads constructor(@JvmField var ProposeMolecule: Molecule? = null) {
  companion object {
    private val jsonFormat: Json
      get() = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        coerceInputValues = true
      }

    @JvmStatic
    @JvmOverloads
    fun create(proposedMolecule: Molecule? = null): Data {
      return Data(proposedMolecule)
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
