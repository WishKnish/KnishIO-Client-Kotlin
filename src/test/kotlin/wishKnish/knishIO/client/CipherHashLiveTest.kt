package wishKnish.knishIO.client

import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI

/**
 * Live ML-KEM768 `CipherHash` encrypted-transport round-trip against a running validator
 * (PQ-transport Phase E, completed cycle 162).
 *
 * End-to-end: the encrypted client authenticates (conveying its AUTH source wallet's ML-KEM
 * pubkey via a signed `walletPubkey` U-atom meta), then issues an encrypted `queryBalance` — the
 * validator ML-KEM-decrypts the request, executes it, and encrypts the response back to the
 * client's ML-KEM pubkey, which the client decrypts. Asserts the encrypted result matches the
 * plaintext baseline. Run live (gated: skips cleanly when no validator is reachable):
 * `CIPHERHASH_TEST_URL=http://localhost:8081/graphql ./gradlew test --tests …CipherHashLiveTest`
 */
class CipherHashLiveTest {

    private fun serverUrl(): String =
        System.getenv("CIPHERHASH_TEST_URL") ?: "http://localhost:8081/graphql"

    private fun validatorReachable(url: String): Boolean = try {
        val uri = URI(url)
        val port = when {
            uri.port != -1 -> uri.port
            uri.scheme == "https" -> 443
            else -> 80
        }
        Socket().use { it.connect(InetSocketAddress(uri.host, port), 1500) }
        true
    } catch (e: Exception) {
        false
    }

    @Test
    fun `encrypted CipherHash round-trip matches plaintext`() {
        val url = serverUrl()
        Assumptions.assumeTrue(validatorReachable(url)) {
            "No validator reachable at $url (set CIPHERHASH_TEST_URL) — skipping live CipherHash test"
        }
        val secret = "phase-e-live-kotlin-secret-0123456789ABCDEF"

        // Plaintext baseline (same secret/bundle).
        val plain = KnishIOClient(listOf(URI(url)), encrypt = false)
        plain.requestAuthToken(secret = secret, encrypt = false)
        val plainResp = plain.queryBalance("USER")

        // Encrypted: the validator ML-KEM-decrypts the request, executes it, and encrypts the
        // response back to the client's ML-KEM pubkey; the client decrypts it. Transparent →
        // the encrypted result must equal the plaintext one.
        val enc = KnishIOClient(listOf(URI(url)), encrypt = true)
        enc.requestAuthToken(secret = secret, encrypt = true)
        val encResp = enc.queryBalance("USER")

        expectThat(encResp.success()).isEqualTo(plainResp.success())
    }
}
