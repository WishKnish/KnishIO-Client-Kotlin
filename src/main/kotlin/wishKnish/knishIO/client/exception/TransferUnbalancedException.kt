@file:JvmName("TransferUnbalancedException")

package wishKnish.knishIO.client.exception

class TransferUnbalancedException : BaseException {
  constructor(message: String = "Token transfer atoms are unbalanced") : super(message)
  constructor(
    message: String = "Token transfer atoms are unbalanced",
    cause: Throwable
  ) : super(message, cause)

  constructor(cause: Throwable) : super(cause)
}