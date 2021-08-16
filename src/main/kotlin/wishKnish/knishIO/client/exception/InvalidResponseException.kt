@file:JvmName("InvalidResponseException")
package wishKnish.knishIO.client.exception

class InvalidResponseException: BaseException {
  constructor(message: String = "GraphQL did not provide a valid response.") : super(message)
  constructor(
    message: String = "GraphQL did not provide a valid response.",
    cause: Throwable
  ) : super(message, cause)

  constructor(cause: Throwable) : super(cause)
}