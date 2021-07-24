@file:JvmName("MolecularHashMissingException")

package wishKnish.knishIO.client.exception

class MolecularHashMissingException : BaseException {
  constructor(message: String = "The molecular hash is missing") : super(message)
  constructor(
    message: String = "The molecular hash is missing",
    cause: Throwable
  ) : super(message, cause)

  constructor(cause: Throwable) : super(cause)
}