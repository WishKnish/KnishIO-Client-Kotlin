package wishKnish.knishIO.client.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


@Serializable
data class UnitData(val id:String, val name: String, val metas: List<String>) {
    companion object {

        @JvmStatic
        fun create(id: String, name: String, metas: List<String>): UnitData {
            return UnitData(id, name, metas)
        }

        @JvmStatic
        fun jsonToObject(json: String): UnitData {
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