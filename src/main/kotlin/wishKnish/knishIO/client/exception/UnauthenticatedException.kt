@file:JvmName("UnauthenticatedException")
package wishKnish.knishIO.client.exception

class UnauthenticatedException: BaseException {
  constructor(message: String = "Authorization token missing or invalid.") : super(message)
  constructor(
    message: String = "Authorization token missing or invalid.",
    cause: Throwable
  ) : super(message, cause)

  constructor(cause: Throwable) : super(cause)
}
