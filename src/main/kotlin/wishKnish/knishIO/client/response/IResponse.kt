@file:JvmName("IResponse")
package wishKnish.knishIO.client.response

import wishKnish.knishIO.client.data.json.response.IResponse as JsonIResponse

interface IResponse {
  fun data(): Any?
  fun mapping(response: String): JsonIResponse
  fun status(): Any?
  fun initialization()
  fun payload(): Any?
  fun success(): Boolean
}
