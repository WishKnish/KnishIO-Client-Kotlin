@file:JvmName("TransferRemainderException")

package wishKnish.knishIO.client.exception

class TransferRemainderException : BaseException {
  constructor(message: String = "Invalid remainder provided") : super(message)
  constructor(
    message: String = "Invalid remainder provided",
    cause: Throwable
  ) : super(message, cause)

  constructor(cause: Throwable) : super(cause)
}
