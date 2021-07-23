@file:JvmName("ProposeMoleculeData")
package wishKnish.knishIO.client.data.json

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ProposeMoleculeData(
    @JvmField var molecularHash: String = "",
    @JvmField var height: Int = 0,
    @JvmField var depth: Int = 0,
    @JvmField var status: String = "",
    @JvmField var reason: String = "",
    @JvmField var payload: String = "",
    @JvmField var createdAt: String = "",
    @JvmField var receivedAt: String = "",
    @JvmField var processedAt: String = "",
    @JvmField var broadcastedAt: String = ""
) {
    companion object {
        private val jsonFormat: Json
            get() =  Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
                coerceInputValues = true
            }

        @JvmStatic
        fun create(
            molecularHash: String = "",
            height: Int = 0,
            depth: Int = 0,
            status: String = "",
            reason: String = "",
            payload: String = "",
            createdAt: String = "",
            receivedAt: String = "",
            processedAt: String = "",
            broadcastedAt: String = ""
        ): ProposeMoleculeData {
            return ProposeMoleculeData(
                molecularHash,
                height,
                depth,
                status,
                reason,
                payload,
                createdAt,
                receivedAt,
                processedAt,
                broadcastedAt
            )
        }

        @JvmStatic
        fun jsonToObject(json: String): ProposeMoleculeData {
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