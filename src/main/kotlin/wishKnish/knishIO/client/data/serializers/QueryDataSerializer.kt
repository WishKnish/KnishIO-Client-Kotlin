package wishKnish.knishIO.client.data.serializers

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import wishKnish.knishIO.client.data.QueryData
import wishKnish.knishIO.client.Molecule

class QueryDataSerializer : KSerializer<QueryData> {

  override val descriptor: SerialDescriptor = buildClassSerialDescriptor("QueryData") {
    element<String>("query")
    element<String>("variables")
  }

  override fun deserialize(decoder: Decoder): QueryData {
    // Cast to JSON-specific interface
    val jsonInput = decoder as? JsonDecoder ?: error("Can be deserialized only by JSON")
    // Read the whole content as JSON
    val json = jsonInput.decodeJsonElement().jsonObject
    // Extract and remove name property
    val query = json.getValue("query").jsonPrimitive.content

    val details = json.toMutableMap()

    val variables = details["variables"]
    var molecule: Molecule? = null

    variables?.let {
      if (it is JsonObject && it.containsKey("molecule")) {
        molecule = Molecule.jsonToObject(it["molecule"].toString())
      }
    }

    val queryData = QueryData(query, variables?.toString())

    queryData.molecule = molecule

    return queryData
  }

  override fun serialize(
    encoder: Encoder,
    value: QueryData
  ) {
    // Emit the GraphQL request body { "query": ..., "variables": {...} }. `variables`
    // is a PRE-ENCODED JSON string (HttpClient encodes each *Variable with its own
    // serializer), so it must be inlined as raw JSON — not re-quoted as a string.
    // Previously this threw, so no live mutation/query could be sent.
    val jsonOutput = encoder as? JsonEncoder ?: error("Can be serialized only by JSON")
    val obj = buildJsonObject {
      put("query", value.query)
      value.variables?.let { put("variables", jsonOutput.json.parseToJsonElement(it)) }
    }
    jsonOutput.encodeJsonElement(obj)
  }
}
