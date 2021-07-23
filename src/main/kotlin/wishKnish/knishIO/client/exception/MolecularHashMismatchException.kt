@file:JvmName("MolecularHashMismatchException")
package wishKnish.knishIO.client.exception

class MolecularHashMismatchException: BaseException {
    constructor(message: String = "The molecular hash does not match") : super(message)
    constructor(message: String = "The molecular hash does not match", cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}