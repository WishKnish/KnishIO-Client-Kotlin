@file:JvmName("IResponse")
package wishKnish.knishIO.client.data.json.response

import wishKnish.knishIO.client.data.json.errors.Errors

interface IResponse {
  val errors: List<Errors>
  val message: String?
  val exception: Boolean?
}
