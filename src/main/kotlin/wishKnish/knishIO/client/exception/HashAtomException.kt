@file:JvmName("HashAtomException")

package wishKnish.knishIO.client.exception

class HashAtomException : BaseException {
  constructor(message: String = "Incorrect HashAtom structure") : super(message)
  constructor(
    message: String = "Incorrect HashAtom structure",
    cause: Throwable
  ) : super(message, cause)

  constructor(cause: Throwable) : super(cause)
}
