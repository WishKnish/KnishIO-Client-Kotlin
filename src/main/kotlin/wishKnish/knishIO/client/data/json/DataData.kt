package wishKnish.knishIO.client.data.json

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class DataData(@JvmField var ProposeMolecule: ProposeMoleculeData? = null) {
    companion object {
        private val jsonFormat: Json
            get() =  Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
                coerceInputValues = true
            }

        @JvmStatic
        fun create(ProposeMolecule: ProposeMoleculeData? = null): DataData {
            return DataData(ProposeMolecule)
        }

        @JvmStatic
        fun jsonToObject(json: String): DataData {
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