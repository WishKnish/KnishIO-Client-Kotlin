@file:JvmName("SignatureMalformedException")
package wishKnish.knishIO.client.exception

class SignatureMalformedException: BaseException {
    constructor(message: String = "OTS malformed") : super(message)
    constructor(message: String = "OTS malformed", cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}