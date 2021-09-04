@file:JvmName("StackableUnitAmountException")

package wishKnish.knishIO.client.exception

class StackableUnitAmountException : BaseException {
  constructor(message: String = "Stackable tokens with unit IDs cannot have decimal places!") : super(message)
  constructor(
    message: String = "Stackable tokens with unit IDs cannot have decimal places!",
    cause: Throwable
  ) : super(message, cause)

  constructor(cause: Throwable) : super(cause)
}
