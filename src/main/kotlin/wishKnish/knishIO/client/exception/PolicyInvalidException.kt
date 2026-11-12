@file:JvmName("PolicyInvalidException")

package wishKnish.knishIO.client.exception

class PolicyInvalidException : BaseException {
  constructor(message: String = "Invalid policy") : super(message)
  constructor(
    message: String = "Invalid policy",
    cause: Throwable
  ) : super(message, cause)

  constructor(cause: Throwable) : super(cause)
}
