@file:JvmName("TransferBalanceException")

package wishKnish.knishIO.client.exception

class TransferBalanceException : BaseException {
  constructor(message: String = "Insufficient balance to make transfer") : super(message)
  constructor(
    message: String = "Insufficient balance to make transfer",
    cause: Throwable
  ) : super(message, cause)

  constructor(cause: Throwable) : super(cause)
}