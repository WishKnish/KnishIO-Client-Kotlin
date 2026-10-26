/*
 * Offline fail-closed checks for the encrypted (CipherHash) transport: with encryption on, a
 * normal operation must be refused before any connection when the authorized wallet or the
 * validator public key is missing (null or empty). The client points at 127.0.0.1:1, so a
 * request that did reach the network would surface as a connection error, not a CodeException.
 */

package wishKnish.knishIO.client

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import wishKnish.knishIO.client.data.json.query.Balance
import wishKnish.knishIO.client.data.json.query.ContinuId
import wishKnish.knishIO.client.data.json.variables.BalanceVariable
import wishKnish.knishIO.client.data.json.variables.ContinuIdVariable
import wishKnish.knishIO.client.exception.CodeException
import wishKnish.knishIO.client.httpClient.HttpClient
import java.net.URI

class EncryptedTransportFailClosedTest {

  private val missingKeyMessages = setOf("Authorized wallet missing.", "Server public key missing.")

  private fun client(wallet: Wallet?, pubkey: String?): HttpClient =
    HttpClient(URI.create("http://127.0.0.1:1/graphql"), encrypt = true).also {
      it.wallet = wallet
      it.pubkey = pubkey
    }

  private fun authWallet() = Wallet("a1b2c3d4e5f6".repeat(8), "AUTH")

  /** Runs the call and returns the root cause (query() wraps failures in an ExecutionException). */
  private fun rootCause(block: () -> Unit): Throwable {
    val thrown = assertThrows(Throwable::class.java) { block() }
    var cause: Throwable = thrown
    while (cause !is CodeException && cause.cause != null && cause.cause !== cause) {
      cause = cause.cause!!
    }
    return cause
  }

  private fun assertRefused(client: HttpClient, message: String) {
    val cause = rootCause { client.query(Balance(BalanceVariable(token = "USER"))) }
    assertEquals(CodeException::class.java, cause::class.java, "expected a pre-send refusal, got $cause")
    assertEquals(message, cause.message)
  }

  @Test
  fun `refuses a normal operation without a wallet`() {
    assertRefused(client(null, null), "Authorized wallet missing.")
  }

  @Test
  fun `refuses a normal operation when the validator pubkey is null`() {
    assertRefused(client(authWallet(), null), "Server public key missing.")
  }

  @Test
  fun `refuses a normal operation when the validator pubkey is empty`() {
    assertRefused(client(authWallet(), ""), "Server public key missing.")
  }

  @Test
  fun `wallet and pubkey accessors fail closed`() {
    assertEquals("Authorized wallet missing.", assertThrows(CodeException::class.java) { client(null, null).wallet() }.message)
    assertEquals("Server public key missing.", assertThrows(CodeException::class.java) { client(null, null).pubkey() }.message)
    assertEquals("Server public key missing.", assertThrows(CodeException::class.java) { client(null, "").pubkey() }.message)
  }

  @Test
  fun `bypassed ContinuId query is not rejected by the key checks`() {
    val cause = rootCause { client(null, null).query(ContinuId(ContinuIdVariable(bundle = "0".repeat(64)))) }
    assertFalse(
      cause is CodeException && cause.message in missingKeyMessages,
      "bypassed operation was refused by the key check: $cause"
    )
  }
}
