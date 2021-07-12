package wishKnish.knishIO.client.libraries

import kotlinx.serialization.json.*


private fun JsonObject.toMap(): Map<String, *> = keys.asSequence().associateWith { it ->
    when (val value = this[it]) {
        is JsonArray -> {
            val map = (0 until value.size).associate {
                Pair(it.toString(), value[it])
            }
            JsonObject(map).toMap().values.toList()
        }
        is JsonObject -> value.toMap()
        is JsonNull -> null
        else -> value
    }
}


private fun JsonArray.toList(): List<Any?> = map {
    when (val value = it) {
        is JsonArray -> {
            val map = (0 until value.size).associate { _ ->
                Pair(it.toString(), it)
            }
            JsonObject(map).toMap().values.toList()
        }
        is JsonObject -> value.toMap()
        is JsonNull -> null
        else -> value
    }
}


fun JsonElement.decode(): Any {
    return when (this) {
        is JsonArray -> this.toList()
        is JsonObject -> this.toMap()
        else -> this
    }
}

fun Map<*, *>.toJsonElement(): JsonElement {

    val map: MutableMap<String, JsonElement> = mutableMapOf()

    this.forEach {
        val key = it.key as? String ?: return@forEach
        val value = it.value ?: return@forEach

        when(value) {
            is Map<*, *> -> map[key] = (value).toJsonElement()
            is List<*> -> map[key] = value.toJsonElement()
            else -> map[key] = JsonPrimitive(value.toString())
        }
    }

    return JsonObject(map)
}

fun List<*>.toJsonElement(): JsonElement {
    val list: MutableList<JsonElement> = mutableListOf()

    this.forEach {
        val value = it as? Any ?: return@forEach

        when(value) {
            is Map<*, *> -> list.add((value).toJsonElement())
            is List<*> -> list.add(value.toJsonElement())
            else -> list.add(JsonPrimitive(value.toString()))
        }
    }

    return JsonArray(list)
}
