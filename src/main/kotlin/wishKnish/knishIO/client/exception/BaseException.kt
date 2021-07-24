@file:JvmName("BaseException")

package wishKnish.knishIO.client.exception

open class BaseException : RuntimeException {
  constructor() : super()
  constructor(message: String) : super(message)
  constructor(message: String, cause: Throwable) : super(message, cause)
  constructor(cause: Throwable) : super(cause)
}
