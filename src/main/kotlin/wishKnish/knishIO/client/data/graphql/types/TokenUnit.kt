@file:JvmName("TokenUnit")

package wishKnish.knishIO.client.data.graphql.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive


/**
 * A unit's `metas` as a JSON object. The validator serves them as a single GraphQL String (a
 * serialized JSON object, or null); JS `TokenUnit.createFromGraphQL` parses that string, so this
 * does the same. An object is kept as it is; null, an empty string or anything that is not a JSON
 * object reads as `{}`.
 */
object TokenUnitMetasSerializer : KSerializer<JsonObject> {
  private val delegate = JsonObject.serializer()
  override val descriptor: SerialDescriptor = delegate.descriptor

  override fun deserialize(decoder: Decoder): JsonObject {
    val jsonDecoder = decoder as? JsonDecoder ?: return delegate.deserialize(decoder)
    return when (val element = jsonDecoder.decodeJsonElement()) {
      is JsonObject -> element
      is JsonPrimitive -> element.takeIf { it.isString && it.content.isNotEmpty() }?.let { parseObject(it.content) }
      else -> null
    } ?: JsonObject(emptyMap())
  }

  private fun parseObject(text: String): JsonObject? = try {
    Json.parseToJsonElement(text) as? JsonObject
  } catch (e: SerializationException) {
    null
  }

  override fun serialize(encoder: Encoder, value: JsonObject) {
    delegate.serialize(encoder, value)
  }
}


@Serializable data class TokenUnit @JvmOverloads constructor(
  @JvmField val id: String,
  @JvmField val name: String,
  @JvmField @Serializable(with = TokenUnitMetasSerializer::class) val metas: JsonObject = JsonObject(emptyMap())
) : IGraphql {
  companion object {
    private val jsonFormat: Json
      get() = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
      }

    @JvmStatic
    @JvmOverloads
    fun create(
      id: String,
      name: String,
      metas: JsonObject = JsonObject(emptyMap())
    ): TokenUnit {
      return TokenUnit(id, name, metas)
    }

    @JvmStatic
    fun jsonToObject(json: String): TokenUnit {
      return jsonFormat.decodeFromString(json)
    }
  }

  /**
   * The unit as the JS reference writes it into a `tokenUnits` meta (`TokenUnit.toData()`):
   * `[id, name, metas]`. [extraMetas] are added after the unit's own metas (JS `fuseToken` adds
   * `fusedTokenUnits` this way).
   */
  @JvmOverloads
  fun toTriple(extraMetas: JsonObject = JsonObject(emptyMap())): JsonArray {
    return JsonArray(listOf(JsonPrimitive(id), JsonPrimitive(name), JsonObject(metas + extraMetas)))
  }

  private fun toJson(): String {
    return jsonFormat.encodeToString(this)
  }

  override fun toString(): String {
    return toJson()
  }
}
