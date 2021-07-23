@file:JvmName("ProposeMoleculeData")
package wishKnish.knishIO.client.data.json

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class ResponseData(@JvmField var data: DataData? = null) {
    companion object {
        private val jsonFormat: Json
            get() =  Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
                coerceInputValues = true
            }

        @JvmStatic
        fun create(data: DataData? = null): ResponseData {
            return ResponseData(data)
        }

        @JvmStatic
        fun jsonToObject(json: String): ResponseData {
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