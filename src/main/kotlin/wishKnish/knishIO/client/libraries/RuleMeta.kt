@file:JvmName("RuleMeta")

package wishKnish.knishIO.client.libraries

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import wishKnish.knishIO.client.exception.CodeException
import wishKnish.knishIO.client.exception.MetaMissingException
import wishKnish.knishIO.client.exception.RuleArgumentException

/**
 * The `rule` and `policy` meta of R isotope atoms, built and checked as the JS reference does
 * (instance/Rules/{Rule,Condition,Callback,Meta}.js and PolicyMeta.js). Every value is the JSON
 * the JS objects serialize to, so both metas hash identically across SDKs.
 */
object RuleMeta {

  private val ARRAY_INDEX = Regex("^(0|[1-9][0-9]*)$")

  /** JS truthiness of a JSON value (`undefined`/`null`/`false`/`0`/`""` are falsy). */
  private fun truthy(element: JsonElement?): Boolean = when (element) {
    null, JsonNull -> false
    is JsonPrimitive -> when {
      element.isString -> element.content.isNotEmpty()
      element.booleanOrNull != null -> element.booleanOrNull!!
      else -> element.doubleOrNull?.let { it != 0.0 && !it.isNaN() } ?: true
    }
    else -> true
  }

  private fun isArrayIndex(key: String): Boolean = ARRAY_INDEX.matches(key) && key.toLong() < 4294967295L

  /**
   * [element] with every object's keys in the order `JSON.stringify` writes them: integer-like keys
   * ascending first, then the others in insertion order.
   */
  @JvmStatic
  fun jsOrdered(element: JsonElement): JsonElement = when (element) {
    is JsonObject -> {
      val (indices, names) = element.keys.partition { isArrayIndex(it) }
      JsonObject((indices.sortedBy { it.toLong() } + names).associateWith { jsOrdered(element.getValue(it)) })
    }
    is JsonArray -> JsonArray(element.map { jsOrdered(it) })
    else -> element
  }

  /** JS `Condition.toObject(object).toJSON()`. */
  private fun condition(element: JsonElement): JsonObject {
    val fields = listOf("key", "value", "comparison").map { (element as? JsonObject)?.get(it) }
    if (fields.any { !truthy(it) }) {
      throw RuleArgumentException("Condition::constructor( { key, value, comparison } ) - not all class parameters are initialised!")
    }
    return JsonObject(mapOf("key" to jsOrdered(fields[0]!!), "value" to jsOrdered(fields[1]!!), "comparison" to jsOrdered(fields[2]!!)))
  }

  /** JS `Callback.toObject(object).toJSON()`. */
  private fun callback(element: JsonElement): JsonObject {
    val source = element as? JsonObject ?: JsonObject(emptyMap())
    val action = source["action"]
    if (!truthy(action)) {
      throw RuleArgumentException("Callback structure violated, missing mandatory \"action\" parameter.")
    }
    val amount = source["amount"]
    if (truthy(amount) && !isNumeric(amount!!)) {
      throw CodeException("Parameter amount should be a string containing numbers")
    }
    val result = linkedMapOf("action" to jsOrdered(action!!))
    listOf("metaType", "metaId", "meta", "address", "token", "amount", "comparison").forEach { key ->
      source[key]?.takeIf { truthy(it) }?.let { result[key] = jsOrdered(it) }
    }
    return JsonObject(result)
  }

  /** JS `isNumeric`: a number, or a non-blank string that parses as one. */
  private fun isNumeric(element: JsonElement): Boolean {
    val primitive = element as? JsonPrimitive ?: return false
    if (!primitive.isString) return primitive.doubleOrNull != null
    val text = primitive.content.trim()
    return text.isNotEmpty() && text.toDoubleOrNull() != null
  }

  /**
   * One rule as JS `Rule.toObject(object).toJSON()` writes it: `{"condition": [...], "callback": [...]}`.
   *
   * @throws MetaMissingException when the rule has no condition or no callback field
   * @throws RuleArgumentException when a condition or callback is incomplete
   */
  @JvmStatic
  @Throws(MetaMissingException::class, RuleArgumentException::class, CodeException::class)
  fun rule(element: JsonElement): JsonObject {
    val source = element as? JsonObject
    val conditions = source?.get("condition")
    val callbacks = source?.get("callback")
    if (!truthy(conditions)) {
      throw MetaMissingException("Rule::toObject() - Incorrect rule format! There is no condition field.")
    }
    if (!truthy(callbacks)) {
      throw MetaMissingException("Rule::toObject() - Incorrect rule format! There is no callback field.")
    }
    val conditionList = conditions as? JsonArray ?: throw RuleArgumentException("Rule::toObject() - condition must be a list")
    val callbackList = callbacks as? JsonArray ?: throw RuleArgumentException("Rule::toObject() - callback must be a list")
    return JsonObject(
      mapOf(
        "condition" to JsonArray(conditionList.map { condition(it) }),
        "callback" to JsonArray(callbackList.map { callback(it) })
      )
    )
  }

  /**
   * The `policy` meta JS `AtomMeta.addPolicy(policy)` writes for an atom whose meta has
   * [metaKeys] (JS passes the meta array's indexes): the caller's `read`/`write` entries, then
   * `["all"]` read and `["self"]` write defaults for every key the caller left out.
   */
  @JvmStatic
  fun policy(policy: JsonObject, metaKeys: List<String>): String {
    val result = linkedMapOf<String, MutableMap<String, JsonElement>>()
    policy.forEach { (type, value) ->
      if (value !is JsonNull && type in listOf("read", "write")) {
        result[type] = linkedMapOf<String, JsonElement>().apply { (value as? JsonObject)?.let { putAll(it) } }
      }
    }
    listOf("read", "write").forEach { type ->
      val entries = result.getOrPut(type) { linkedMapOf() }
      metaKeys.forEach { key ->
        if (!truthy(entries[key])) {
          val holder = if (type == "write" && key !in listOf("characters", "pubkey")) "self" else "all"
          entries[key] = JsonArray(listOf(JsonPrimitive(holder)))
        }
      }
    }
    return jsOrdered(JsonObject(result.mapValues { JsonObject(it.value) })).toString()
  }
}
