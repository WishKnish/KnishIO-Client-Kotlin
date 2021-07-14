package wishKnish.knishIO.client.exception

class BalanceInsufficientException: BaseException {
    constructor(message: String = "Insufficient balance for requested transfer") : super(message)
    constructor(message: String = "Insufficient balance for requested transfer", cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}
