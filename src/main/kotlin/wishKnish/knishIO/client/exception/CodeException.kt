@file:JvmName("CodeException")
package wishKnish.knishIO.client.exception

class CodeException: BaseException {
  constructor(message: String = "Code exception") : super(message)
  constructor(
    message: String = "Code exception",
    cause: Throwable
  ) : super(message, cause)

  constructor(cause: Throwable) : super(cause)
}