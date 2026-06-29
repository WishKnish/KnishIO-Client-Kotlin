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

        // ONE authenticated session (encrypt=true → the proven cycle-162 path: conveys the AUTH
        // wallet's ML-KEM pubkey as a signed walletPubkey U-atom meta, so the validator can encrypt
        // responses back to it). We then vary ONLY the transport on this SAME session — the queried
        // balance wallet stays fixed. (A fresh second auth would rotate the USER remainder via
        // ContinuID → a different address/position/pubkey, which is correct protocol behaviour, not
        // a transport bug — so it must NOT be the variable under test.)
        val client = KnishIOClient(listOf(URI(url)), encrypt = true)
        client.requestAuthToken(secret = secret, encrypt = true)

        // Encrypted round-trip: the validator ML-KEM-decrypts the request, executes it, and
        // encrypts the response back to the client's ML-KEM pubkey; the client decrypts it.
        val encResp = client.queryBalance("USER")

        // Plaintext baseline of the SAME wallet on the SAME authed session — only the transport
        // differs.
        client.disableEncryption()
        val plainResp = client.queryBalance("USER")

        // The PQ transport must be transparent: not just a non-error response, but the SAME data.
        // Same authed session → identical balance wallet → its deterministic identity fields match.
        expectThat(encResp.success()).isEqualTo(plainResp.success())
        expectThat(encResp.payload()?.address).isEqualTo(plainResp.payload()?.address)
        expectThat(encResp.payload()?.position).isEqualTo(plainResp.payload()?.position)
        expectThat(encResp.payload()?.pubkey).isEqualTo(plainResp.payload()?.pubkey)
        expectThat(encResp.payload()?.token).isEqualTo(plainResp.payload()?.token)
        expectThat(encResp.payload()?.bundle).isEqualTo(plainResp.payload()?.bundle)
    }
}
