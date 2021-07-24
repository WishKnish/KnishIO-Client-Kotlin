@file:JvmName("NegativeAmountException")

package wishKnish.knishIO.client.exception

class NegativeAmountException : BaseException {
  constructor(message: String = "Amount cannot be negative!") : super(message)
  constructor(
    message: String = "Amount cannot be negative!",
    cause: Throwable
  ) : super(message, cause)

  constructor(cause: Throwable) : super(cause)
}
