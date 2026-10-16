package wishKnish.knishIO.client.libraries

import kotlinx.serialization.json.*
import wishKnish.knishIO.client.Atom
import wishKnish.knishIO.client.Molecule
import wishKnish.knishIO.client.Wallet
import wishKnish.knishIO.client.exception.AtomsMissingException
import wishKnish.knishIO.client.exception.MolecularHashMismatchException
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * Result of [CheckMolecule.verifyFromServerData].
 *
 * @property molecularHash the served molecular hash, or null when absent
 * @property verified true when the molecule passed the full molecule check
 * @property error the check's error message, or null when verified
 */
data class ServedVerification(
  val molecularHash: String?,
  val verified: Boolean,
  val error: String?
)

/**
 * Maps molecules as a validator's GraphQL API serves them (`bundleHash`, `tokenSlug`,
 * `metasJson`, RFC 3339 timestamps) onto the client molecule format.
 */
internal object ServedMolecule {

  /** Versions tried, in order, for a molecule served without atom versions. */
  private val UNSERVED_ATOM_VERSIONS = listOf("4", "3")

  private val RFC3339 = Regex("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?(Z|[+-]\d{2}:\d{2})$""")
  private val DIGITS = Regex("""^\d+$""")
  private val INTEGER = Regex("""^-?\d+$""")

  private const val JS_EXPONENT_MIN = -7
  private const val JS_EXPONENT_MAX = 21

  private val json = Json { ignoreUnknownKeys = true }

  fun toSignedTimestamp(value: Any?): String? = when (value) {
    null, JsonNull -> null
    is JsonPrimitive -> if (value.isString) timestampText(value.content) else timestampNumber(value.content)
    is String -> timestampText(value)
    is Number -> timestampNumber(value.toString())
    else -> value.toString()
  }

  private fun timestampNumber(content: String): String =
    if (INTEGER.matches(content)) content else jsNumber(content)

  private fun timestampText(text: String): String? = when {
    text.isEmpty() -> null
    DIGITS.matches(text) -> text
    RFC3339.matches(text) -> try {
      OffsetDateTime.parse(text).toInstant().toEpochMilli().toString()
    } catch (e: DateTimeParseException) {
      text
    }
    else -> text
  }

  /** The decimal string JavaScript's `String(n)` gives for the number literal [content]. */
  private fun jsNumber(content: String): String {
    val number = content.toDoubleOrNull()?.takeIf { it.isFinite() } ?: return content
    val decimal = BigDecimal(number.toString()).stripTrailingZeros()
    val exponent = decimal.precision() - decimal.scale() - 1
    if (exponent in JS_EXPONENT_MIN until JS_EXPONENT_MAX) {
      return if (decimal.signum() == 0) "0" else decimal.toPlainString()
    }
    val mantissa = decimal.movePointLeft(exponent).toPlainString()
    return mantissa + "e" + (if (exponent > 0) "+" else "") + exponent
  }

  private fun string(element: JsonElement?): String? =
    (element as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content

  /** A served scalar as the client string: null stays null, numbers as JS `String(n)`, booleans as text. */
  private fun scalar(element: JsonElement?): JsonElement = when {
    element == null || element is JsonNull -> JsonNull
    element is JsonPrimitive && element.isString -> element
    element is JsonPrimitive && element.booleanOrNull != null -> JsonPrimitive(element.content)
    element is JsonPrimitive -> JsonPrimitive(jsNumber(element.content))
    else -> JsonPrimitive(element.toString())
  }

  private fun meta(metasJson: JsonElement?): JsonArray {
    val text = (metasJson as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return JsonArray(emptyList())
    val parsed = try {
      json.parseToJsonElement(text)
    } catch (e: IllegalArgumentException) {
      return JsonArray(emptyList())
    }
    val pairs = when (parsed) {
      is JsonArray -> parsed.mapNotNull { item ->
        val entry = item as? JsonObject ?: return@mapNotNull null
        string(entry["key"])?.let { it to entry["value"] }
      }
      is JsonObject -> parsed.entries.map { (key, value) -> key to value }
      else -> emptyList()
    }
    return JsonArray(pairs.map { (key, value) ->
      JsonObject(mapOf("key" to JsonPrimitive(key), "value" to scalar(value)))
    })
  }

  private fun version(element: JsonElement?): JsonElement = string(element)?.let { JsonPrimitive(it) } ?: JsonNull

  private fun atom(served: JsonObject): JsonObject = buildJsonObject {
    put("position", string(served["position"]))
    put("walletAddress", string(served["walletAddress"]))
    put("isotope", string(served["isotope"]))
    put("token", string(served["tokenSlug"]) ?: string(served["token"]))
    put("value", scalar(served["value"]))
    put("batchId", string(served["batchId"]))
    put("metaType", string(served["metaType"]))
    put("metaId", string(served["metaId"]))
    put("meta", meta(served["metasJson"]))
    served["index"]?.let { if (it !is JsonNull) put("index", it) }
    put("otsFragment", string(served["otsFragment"]))
    toSignedTimestamp(served["createdAt"])?.let { put("createdAt", it) }
    put("version", version(served["version"]))
  }

  private fun parse(servedJson: String): JsonObject {
    val data = json.parseToJsonElement(servedJson) as? JsonObject
      ?: throw IllegalArgumentException("Served molecule data must be a JSON object")
    if (data["atoms"] !is JsonArray) {
      throw IllegalArgumentException("Served molecule data has no atoms array")
    }
    return data
  }

  fun fromServerData(servedJson: String): Molecule = fromServerData(parse(servedJson))

  /**
   * Builds the molecule from client-format atoms (parsed by [Atom.fromJSON]) the way a peer molecule
   * is reconstructed for verification: no secret, and a source wallet that only names the signer.
   */
  private fun fromServerData(data: JsonObject): Molecule {
    val atoms = data["atoms"]!!.jsonArray.map { Atom.fromJSON(atom(it.jsonObject).toString()) }
    val signer = atoms.firstOrNull() ?: throw AtomsMissingException()
    val source = Wallet(secret = null, token = signer.token, position = signer.position).apply {
      address = signer.walletAddress
    }
    return Molecule(secret = null, sourceWallet = source, cellSlug = string(data["cellSlug"])).apply {
      molecularHash = string(data["molecularHash"])
      bundle = string(data["bundleHash"])
      status = string(data["status"])
      toSignedTimestamp(data["createdAt"])?.let { createdAt = it }
      this.atoms.clear()
      this.atoms.addAll(atoms)
    }
  }

  private class Attempt(val result: ServedVerification, val hashMismatch: Boolean)

  @Suppress("TooGenericExceptionCaught")  // any check failure is reported as the verdict, never thrown
  private fun attempt(data: JsonObject, molecularHash: String?): Attempt = try {
    if (Molecule.verify(fromServerData(data))) {
      Attempt(ServedVerification(molecularHash, true, null), false)
    } else {
      Attempt(ServedVerification(molecularHash, false, "Molecule verification failed"), false)
    }
  } catch (e: MolecularHashMismatchException) {
    Attempt(ServedVerification(molecularHash, false, e.message ?: e.toString()), true)
  } catch (e: Exception) {
    Attempt(ServedVerification(molecularHash, false, e.message ?: e.toString()), false)
  }

  private fun withVersion(data: JsonObject, version: String): JsonObject {
    val atoms = data["atoms"]!!.jsonArray.map { atom ->
      (atom as? JsonObject)?.let { JsonObject(it + ("version" to JsonPrimitive(version))) } ?: atom
    }
    return JsonObject(data + ("atoms" to JsonArray(atoms)))
  }

  @Suppress("TooGenericExceptionCaught")  // malformed input is reported as the verdict, never thrown
  fun verifyFromServerData(servedJson: String): ServedVerification {
    val data = try {
      parse(servedJson)
    } catch (e: Exception) {
      return ServedVerification(null, false, e.message ?: e.toString())
    }
    val molecularHash = string(data["molecularHash"])

    val first = attempt(data, molecularHash)
    val servedVersion = data["atoms"]!!.jsonArray.any { string((it as? JsonObject)?.get("version")) != null }
    if (first.result.verified || !first.hashMismatch || servedVersion) {
      return first.result
    }

    // Molecules served without their atom versions only match their hash when rebuilt with that version.
    var retried = first.result
    for (version in UNSERVED_ATOM_VERSIONS) {
      retried = attempt(withVersion(data, version), molecularHash).result
      if (retried.verified) {
        break
      }
    }
    return retried
  }
}
