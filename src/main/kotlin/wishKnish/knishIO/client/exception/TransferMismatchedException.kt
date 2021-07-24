@file:JvmName("TransferMismatchedException")

package wishKnish.knishIO.client.exception

class TransferMismatchedException : BaseException {
  constructor(message: String = "Token slugs for wallets in transfer do not match!") : super(message)
  constructor(
    message: String = "Token slugs for wallets in transfer do not match!", cause: Throwable
  ) : super(message, cause)

  constructor(cause: Throwable) : super(cause)
}