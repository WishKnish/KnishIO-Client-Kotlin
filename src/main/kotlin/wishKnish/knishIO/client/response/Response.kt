@file:JvmName("Response")
package wishKnish.knishIO.client.response

import wishKnish.knishIO.client.data.json.response.IResponse as JsonIResponse
import wishKnish.knishIO.client.data.json.response.Response as JsonResponse
import wishKnish.knishIO.client.exception.InvalidResponseException
import wishKnish.knishIO.client.query.Query
import kotlin.jvm.Throws
import kotlin.reflect.full.memberProperties


abstract class Response(@JvmField val query: Query, json: String, @JvmField val dataKey: String): IResponse {
  @JvmField var errorKey = "exception"
  @JvmField var response: JsonIResponse
  @JvmField var originResponse = ""

  init {
    originResponse = json
    response = mapping(json)
  }

  @Throws(InvalidResponseException::class)
  override fun data(): Any? {
    if (dataKey.isEmpty()) {
      return response
    }

    var data:Any? = response

    for (key in dataKey.split(".")) {
      data = getResponseData(data, key)
    }

    return data
  }

  @Throws(InvalidResponseException::class)
  protected fun getResponseData(data: Any?, key: String): Any? {
    if (data == null) {
      throw InvalidResponseException("Response does not match the key.")
    }

    val property = data::class.memberProperties.find { it.name == key }

    return when {
      property != null -> property.call(data)
      data is Map<*,*> && data.containsKey(key) -> data[key]
      data is List<*> && key.all { it in '0'..'9' }  -> data[key.toInt()]
      data is Set<*> && key.all { it in '0'..'9' } -> data.toList()[key.toInt()]
      else -> throw InvalidResponseException("Response does not match the key.")
    }
  }

  fun query() = query
  fun response() = response

  override fun mapping(response: String): JsonIResponse {
    return JsonResponse.jsonToObject(response)
  }

  override fun status(): Any? {
    return null
  }

  override fun initialization() {
  }

  override fun payload(): Any? {
    return null
  }

  override fun success(): Boolean {
    return true
  }
}
