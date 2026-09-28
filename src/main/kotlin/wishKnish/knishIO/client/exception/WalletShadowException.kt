@file:JvmName("WalletShadowException")

package wishKnish.knishIO.client.exception

class WalletShadowException : BaseException {
  constructor(message: String = "The shadow wallet does not exist") : super(message)
  constructor(
    message: String = "The shadow wallet does not exist",
    cause: Throwable
  ) : super(message, cause)

  constructor(cause: Throwable) : super(cause)
}
