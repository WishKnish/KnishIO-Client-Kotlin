@file:JvmName("BatchIdException")

package wishKnish.knishIO.client.exception

class BatchIdException : BaseException {
  constructor(message: String = "Incorrect BatchId") : super(message)
  constructor(
    message: String = "Incorrect BatchId",
    cause: Throwable
  ) : super(message, cause)

  constructor(cause: Throwable) : super(cause)
}
