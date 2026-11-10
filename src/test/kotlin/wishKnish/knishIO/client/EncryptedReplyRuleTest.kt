/*
 * The reply to a request the client wrapped in the CipherHash envelope is accepted only as a
 * decryptable envelope or an errors-only reply (`data` missing, null or `{}`). A plaintext `data`
 * reply, or an envelope that does not decrypt, is refused. Bootstrap operations the client sends
 * unwrapped (here ContinuId) are not subject to the rule. Each case answers one request from a
 * loopback HTTP stub with a fixed body.
 */

package wishKnish.knishIO.client

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import wishKnish.knishIO.client.data.json.query.Balance
import wishKnish.knishIO.client.data.json.query.ContinuId
import wishKnish.knishIO.client.data.json.query.QueryInterface
import wishKnish.knishIO.client.data.json.variables.BalanceVariable
import wishKnish.knishIO.client.data.json.variables.ContinuIdVariable
import wishKnish.knishIO.client.exception.CodeException
import wishKnish.knishIO.client.httpClient.HttpClient
import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URI
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread

class EncryptedReplyRuleTest {

  /** Answers exactly one HTTP request on 127.0.0.1 with [body], then closes. */
  private class OneReplyServer(private val body: String) : AutoCloseable {
    private val serverSocket = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).apply {
      soTimeout = 30000
    }
    val port: Int = serverSocket.localPort
    private val worker: Thread = thread(start = true, isDaemon = true, name = "test-one-reply-server") {
      serve()
    }

    private fun readRequest(input: InputStream): Boolean {
      val headerBytes = ArrayList<Byte>(512)
      var match = 0
      while (match < 4) {
        val b = input.read()
        if (b < 0) return false
        headerBytes.add(b.toByte())
        match = when {
          (match == 0 || match == 2) && b == '\r'.code -> match + 1
          (match == 1 || match == 3) && b == '\n'.code -> match + 1
          b == '\r'.code -> 1
          else -> 0
        }
      }
      val headers = ByteArray(headerBytes.size) { headerBytes[it] }.toString(StandardCharsets.ISO_8859_1)
      val contentLength = headers
        .lineSequence()
        .firstOrNull { it.startsWith("Content-Length:", ignoreCase = true) }
        ?.substringAfter(':')
        ?.trim()
        ?.toIntOrNull() ?: 0
      var remaining = contentLength
      val buf = ByteArray(4096)
      while (remaining > 0) {
        val n = input.read(buf, 0, minOf(buf.size, remaining))
        if (n <= 0) return false
        remaining -= n
      }
      return true
    }

    private fun serve() {
      val bodyBytes = body.toByteArray(StandardCharsets.UTF_8)
      val head = (
        "HTTP/1.1 200 OK\r\n" +
          "Content-Type: application/json\r\n" +
          "Content-Length: ${bodyBytes.size}\r\n" +
          "Connection: close\r\n" +
          "\r\n"
        ).toByteArray(StandardCharsets.UTF_8)
      try {
        serverSocket.accept().use { sock ->
          sock.soTimeout = 5000
          if (readRequest(sock.getInputStream())) {
            sock.getOutputStream().write(head + bodyBytes)
            sock.getOutputStream().flush()
          }
        }
      } catch (_: Exception) {
        // Timeout or socket closed on teardown; each test asserts on what the client returned.
      }
    }

    override fun close() {
      worker.join(5000)
      runCatching { serverSocket.close() }
    }
  }

  /** Runs the call and returns the root cause (query() wraps failures in an ExecutionException). */
  private fun rootCause(block: () -> Unit): Throwable {
    val thrown = assertThrows(Throwable::class.java) { block() }
    var cause: Throwable = thrown
    while (cause !is CodeException && cause.cause != null && cause.cause !== cause) {
      cause = cause.cause!!
    }
    return cause
  }

  private fun keyedQuery(reply: String, request: QueryInterface): String {
    // Build the wallet (ML-KEM keygen, slow on a cold JVM) before the stub starts waiting.
    val wallet = Wallet("a1b2c3d4e5f6".repeat(8), "AUTH")
    return OneReplyServer(reply).use { server ->
      HttpClient(URI.create("http://127.0.0.1:${server.port}/graphql"), encrypt = true).also {
        it.wallet = wallet
        it.pubkey = wallet.pubkey
      }.use { client -> client.query(request) }
    }
  }

  private fun balance() = Balance(BalanceVariable(token = "USER"))

  private fun assertRefused(reply: String, message: String) {
    val cause = rootCause { keyedQuery(reply, balance()) }
    assertEquals(CodeException::class.java, cause::class.java, "expected a refusal, got $cause")
    assertEquals(message, cause.message)
  }

  @Test
  fun `plaintext data reply to an encrypted request is rejected`() {
    assertRefused(
      """{"data":{"Balance":{"address":"x"}}}""",
      "CipherHash: plaintext reply to an encrypted request"
    )
  }

  @Test
  fun `errors-only reply passes through`() {
    val reply = """{"data":null,"errors":[{"message":"CipherHash: no envelope addressed to the validator"}]}"""
    assertEquals(reply, keyedQuery(reply, balance()))
  }

  @Test
  fun `undecryptable envelope is rejected`() {
    assertRefused("""{"data":{"CipherHash":{"hash":"{}"}}}""", "CipherHash: reply could not be decrypted")
  }

  @Test
  fun `plaintext reply to a bypassed ContinuId query is returned unchanged`() {
    val reply = """{"data":{"ContinuId":{"address":"x","bundleHash":"y"}}}"""
    assertEquals(reply, keyedQuery(reply, ContinuId(ContinuIdVariable(bundle = "0".repeat(64)))))
  }
}
