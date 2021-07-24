@file:JvmName("SignatureMismatchException")

package wishKnish.knishIO.client.exception

class SignatureMismatchException : BaseException {
  constructor(message: String = "One-time signature (OTS) does not match!") : super(message)
  constructor(
    message: String = "One-time signature (OTS) does not match!",
    cause: Throwable
  ) : super(message, cause)

  constructor(cause: Throwable) : super(cause)
}