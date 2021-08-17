@file:JvmName("WrongTokenTypeException")

package wishKnish.knishIO.client.exception

class WrongTokenTypeException : BaseException {
  constructor(message: String = "Wrong type of token for this isotope") : super(message)
  constructor(
    message: String = "Wrong type of token for this isotope",
    cause: Throwable
  ) : super(message, cause)

  constructor(cause: Throwable) : super(cause)
}
