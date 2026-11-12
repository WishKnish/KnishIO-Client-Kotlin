@file:JvmName("RuleArgumentException")

package wishKnish.knishIO.client.exception

class RuleArgumentException : BaseException {
  constructor(message: String = "Invalid rule argument") : super(message)
  constructor(
    message: String = "Invalid rule argument",
    cause: Throwable
  ) : super(message, cause)

  constructor(cause: Throwable) : super(cause)
}
