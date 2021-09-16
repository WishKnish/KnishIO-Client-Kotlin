@file:JvmName("IResponseRequestAuthorization")

package wishKnish.knishIO.client.response

interface IResponseRequestAuthorization : IResponse {
  fun token(): String
  fun time(): Int
  fun pubKey(): String
  fun encrypt(): Boolean
  fun reason(): String?
}
