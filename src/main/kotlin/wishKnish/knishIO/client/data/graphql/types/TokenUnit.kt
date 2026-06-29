@file:JvmName("TokenUnit")

package wishKnish.knishIO.client.data.graphql.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonPrimitive


/**
 * The validator returns a wallet's tokenUnits with `metas` as a single GraphQL String (a
 * serialized-JSON blob, or null), but the SDK models a unit's metas as List<String> (its internal
 * array form for the [[id, name, …metas], …] write shape). Tolerate the wire shape on READ
 * (String / null / object -> emptyList; an actual array decodes normally) while preserving the
 * array encode the write path relies on (createToken's encodeToString, tokenUnitsJson). Without
 * this, deserializing any stackable Balance response throws — kotlinx is strict on type mismatches.
 */
object TokenUnitMetasSerializer : KSerializer<List<String>> {
  private val delegate = ListSerializer(String.serializer())
  override val descriptor: SerialDescriptor = delegate.descriptor

  override fun deserialize(decoder: Decoder): List<String> {
    val jsonDecoder = decoder as? JsonDecoder ?: return delegate.deserialize(decoder)
    val element = jsonDecoder.decodeJsonElement()
    return if (element is JsonArray) element.map { it.jsonPrimitive.content } else emptyList()
  }

  override fun serialize(encoder: Encoder, value: List<String>) {
    delegate.serialize(encoder, value)
  }
}


@Serializable data class TokenUnit(
  @JvmField val id: String,
  @JvmField val name: String,
  @JvmField @Serializable(with = TokenUnitMetasSerializer::class) val metas: List<String>
) : IGraphql {
  companion object {
    private val jsonFormat: Json
      get() = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
      }

    @JvmStatic
    fun create(
      id: String,
      name: String,
      metas: List<String>
    ): TokenUnit {
      return TokenUnit(id, name, metas)
    }

    @JvmStatic
    fun jsonToObject(json: String): TokenUnit {
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
