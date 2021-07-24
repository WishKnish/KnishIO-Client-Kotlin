@file:JvmName("MetaMissingException")

package wishKnish.knishIO.client.exception

class MetaMissingException : BaseException {
  constructor(message: String = "Empty meta data.") : super(message)
  constructor(
    message: String = "Empty meta data.",
    cause: Throwable
  ) : super(message, cause)

  constructor(cause: Throwable) : super(cause)
}
