@file:JvmName("TransferMalformedException")

package wishKnish.knishIO.client.exception

class TransferMalformedException : BaseException {
  constructor(message: String = "Token transfer atoms are malformed") : super(message)
  constructor(message: String = "Token transfer atoms are malformed", cause: Throwable) : super(message, cause)
  constructor(cause: Throwable) : super(cause)
}