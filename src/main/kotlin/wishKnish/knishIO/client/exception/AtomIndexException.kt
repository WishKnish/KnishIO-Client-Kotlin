@file:JvmName("AtomIndexException")

package wishKnish.knishIO.client.exception

class AtomIndexException : BaseException {
  constructor(message: String = "There is an atom without an index") : super(message)
  constructor(
    message: String = "There is an atom without an index",
    cause: Throwable
  ) : super(message, cause)

  constructor(cause: Throwable) : super(cause)
}
