package wishKnish.knishIO.client

import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI

/**
 * Live ML-KEM768 `CipherHash` encrypted-transport round-trip against a running validator
 * (PQ-transport Phase E, cycle 161).
 *
 * The Kotlin transport CODE (NaCl→ML-KEM) is migrated and the REQUEST side is proven live: the
 * validator ML-KEM-decrypts the encrypted request and executes the inner query. But the full
 * round-trip is blocked on an **auth-pubkey conveyance gap** (validator + SDK): the validator's
 * `encrypt_response` needs the client's ML-KEM pubkey, yet the U-isotope auth molecule conveys
 * only the U-atom `wallet_address` (hex) and the USER-remainder I-atom `pubkey` — NOT the AUTH
 * **source** wallet's ML-KEM pubkey, which is the one the client decrypts responses with. So the
 * validator fails with `ML-KEM encrypt failed: DecryptionKey`.
 *
 * @Disabled until that gap is closed (SDK conveys the source wallet's ML-KEM pubkey at auth +
 * the validator stores it). Then this asserts the encrypted result matches the plaintext one.
 * Run live (once enabled):
 * `CIPHERHASH_TEST_URL=http://localhost:8081/graphql ./gradlew test --tests …CipherHashLiveTest`
 */
@Disabled(
    "PQ-transport Phase E: blocked on the auth-pubkey conveyance fix (validator + SDK) — " +
        "the auth molecule does not convey the AUTH source wallet's ML-KEM pubkey. See the " +
        "cycle-161 gauntlet entry; enable once the validator stores the client ML-KEM pubkey."
)
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
