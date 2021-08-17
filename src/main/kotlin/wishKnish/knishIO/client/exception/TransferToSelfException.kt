@file:JvmName("TransferToSelfException")

package wishKnish.knishIO.client.exception

class TransferToSelfException : BaseException {
  constructor(message: String = "Sender and recipient(s) cannot be the same") : super(message)
  constructor(
    message: String = "Sender and recipient(s) cannot be the same",
    cause: Throwable
  ) : super(
    message, cause
  )

  constructor(cause: Throwable) : super(cause)
}
