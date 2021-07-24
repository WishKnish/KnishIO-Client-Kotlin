@file:JvmName("AtomsMissingException")

package wishKnish.knishIO.client.exception

class AtomsMissingException : BaseException {
  constructor(message: String = "The molecule does not contain atoms") : super(message)
  constructor(message: String = "The molecule does not contain atoms", cause: Throwable) : super(message, cause)
  constructor(cause: Throwable) : super(cause)
}