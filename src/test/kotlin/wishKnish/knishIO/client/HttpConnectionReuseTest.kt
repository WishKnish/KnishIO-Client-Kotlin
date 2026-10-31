package wishKnish.knishIO.client

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import wishKnish.knishIO.client.data.json.query.Balance
import wishKnish.knishIO.client.data.json.variables.BalanceVariable
import wishKnish.knishIO.client.httpClient.HttpClient
import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * The HTTP client reuses its TCP connection across requests.
 *
 * Up to 1.5.1 HttpClient.ktorClient was a custom getter that built a new Ktor/OkHttp client on
 * every access and closed it via use {} at the end of query(), opening a new TCP connection per
 * request. These cases count the connections a keep-alive TCP server on 127.0.0.1 accepts.
 */
class HttpConnectionReuseTest {

  private class KeepAliveServer(
    private val expectedRequests: Int,
    private val closeAfterResponse: Boolean
  ) : AutoCloseable {
    private val serverSocket = ServerSocket(0, 8, InetAddress.getByName("127.0.0.1")).apply {
      soTimeout = 5000
    }
    val port: Int = serverSocket.localPort
    val accepts = AtomicInteger(0)
    val requests = AtomicInteger(0)
    private val worker: Thread

    init {
      worker = thread(start = true, isDaemon = true, name = "test-keepalive-server") {
        serve()
      }
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
      val body = """{"data":{"Balance":null}}"""
      val responseBytes = (
        "HTTP/1.1 200 OK\r\n" +
          "Content-Type: application/json\r\n" +
          "Content-Length: ${body.length}\r\n" +
          "Connection: ${if (closeAfterResponse) "close" else "keep-alive"}\r\n" +
          "\r\n" +
          body
        ).toByteArray(StandardCharsets.UTF_8)

      var conn: Socket? = null
      try {
        while (requests.get() < expectedRequests) {
          if (conn == null) {
            conn = serverSocket.accept().apply { soTimeout = 5000 }
            accepts.incrementAndGet()
          }
          val sock = conn
          if (!readRequest(sock.getInputStream())) {
            sock.close()
            conn = null
            continue
          }
          sock.getOutputStream().write(responseBytes)
          sock.getOutputStream().flush()
          requests.incrementAndGet()
          if (closeAfterResponse) {
            sock.close()
            conn = null
          }
        }
      } catch (_: Exception) {
        // Timeout or socket closed on teardown; assertions below check exact counts.
      } finally {
        runCatching { conn?.close() }
      }
    }

    fun awaitAndClose() {
      worker.join(5000)
      runCatching { serverSocket.close() }
    }

    override fun close() {
      awaitAndClose()
    }
  }

  @Test
  fun `sequential queries on one HttpClient share one TCP connection`() {
    KeepAliveServer(expectedRequests = 5, closeAfterResponse = false).use { server ->
      HttpClient(URI.create("http://127.0.0.1:${server.port}/graphql")).use { client ->
        repeat(5) {
          val body = client.query(Balance(BalanceVariable(token = "USER")))
          assertEquals("""{"data":{"Balance":null}}""", body)
        }
      }
      server.awaitAndClose()
      assertEquals(5, server.requests.get())
      assertEquals(1, server.accepts.get())
    }
  }

  @Test
  fun `a connection closed by the server after answering is replaced on the next query`() {
    KeepAliveServer(expectedRequests = 2, closeAfterResponse = true).use { server ->
      HttpClient(URI.create("http://127.0.0.1:${server.port}/graphql")).use { client ->
        assertEquals("""{"data":{"Balance":null}}""", client.query(Balance(BalanceVariable(token = "USER"))))
        Thread.sleep(200)
        assertEquals("""{"data":{"Balance":null}}""", client.query(Balance(BalanceVariable(token = "USER"))))
      }
      server.awaitAndClose()
      assertEquals(2, server.requests.get())
      assertEquals(2, server.accepts.get())
    }
  }
}
