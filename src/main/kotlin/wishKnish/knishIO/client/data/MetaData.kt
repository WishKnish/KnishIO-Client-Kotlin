package wishKnish.knishIO.client.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


@Serializable
data class MetaData(val key: String, val value: String? = null) {
    companion object {

        @JvmStatic
        fun create(key: String, value: String? = null): MetaData {
            return MetaData(key, value)
        }

        @JvmStatic
        fun jsonToObject(json: String): MetaData {
            return Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
            }.decodeFromString(json)
        }
    }

    private fun toJson(): String {
        return Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }.encodeToString(this)
    }

    override fun toString(): String {
        return toJson()
    }
}
