package wishKnish.knishIO.client

import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.net.URI

/**
 * Live ML-KEM768 `CipherHash` encrypted-transport round-trip against a running validator
 * (PQ-transport Phase E, completed cycle 162).
 *
 * End-to-end: the encrypted client authenticates (conveying its AUTH source wallet's ML-KEM
 * pubkey via a signed `walletPubkey` U-atom meta), then issues an encrypted `queryBalance` — the
 * validator ML-KEM-decrypts the request, executes it, and encrypts the response back to the
 * client's ML-KEM pubkey, which the client decrypts. Asserts the encrypted result matches the
 * plaintext baseline. Run live (gated on the env var alone: skipped when it is unset, and an
 * unreachable validator at a configured URL FAILS the test):
 * `CIPHERHASH_TEST_URL=http://localhost:8081/graphql ./gradlew test --tests …CipherHashLiveTest`
 */
@Tag("mlkem")
class CipherHashLiveTest {

    private fun serverUrl(): String {
        val url = System.getenv("CIPHERHASH_TEST_URL")
        Assumptions.assumeTrue(url != null) { "set CIPHERHASH_TEST_URL to run the live CipherHash test" }
        return url!!
    }

    @Test
    fun `encrypted CipherHash round-trip matches plaintext`() {
        val url = serverUrl()
        val secret = "phase-e-live-kotlin-secret-0123456789ABCDEF"

        // ONE session, transport toggled on it — the queried balance wallet stays fixed. (A fresh
        // second auth would rotate the USER remainder via ContinuID → a different address/position/
        // pubkey, which is correct protocol behaviour, not a transport bug — so it must NOT be the
        // variable under test.)
        //
        // The session authenticates PLAINTEXT on purpose. The AUTH wallet's ML-KEM pubkey is
        // conveyed as a signed walletPubkey U-atom meta regardless of `encrypt`
        // (KnishIOClient.kt:365-373), and the validator's CipherHash handler needs only that key —
        // so an `encrypt = false` session still speaks the encrypted transport. Authenticating with
        // `encrypt = true` instead would make the plaintext baseline leg below a silent downgrade,
        // which the validator rejects when ENFORCE_ENCRYPTED_TRANSPORT is at its secure default.
        val param = System.getenv("CIPHERHASH_MLKEM_PARAMETER_SET")?.toIntOrNull() ?: 1024
        val client = KnishIOClient(listOf(URI(url)), encrypt = false, mlkemParameterSet = param)
        client.requestAuthToken(secret = secret, encrypt = false)

        // Encrypted round-trip: the validator ML-KEM-decrypts the request, executes it, and
        // encrypts the response back to the client's ML-KEM pubkey; the client decrypts it.
        client.enableEncryption()
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

    /**
     * Live coverage of the enforcement path: extract_encrypt_flag → auth_tokens.encrypted →
     * requires_encrypted_transport. Also proves this SDK's signed `encrypt` meta literal is the
     * one the validator honours: a session that authenticated with `encrypt = true` must not be
     * able to fall back to plaintext.
     */
    @Test
    fun `an encrypt-true session is refused when it drops to plaintext`() {
        val url = serverUrl()
        val secret = "phase-e-enforcement-kotlin-secret-0123456789ABCDEF"

        val param = System.getenv("CIPHERHASH_MLKEM_PARAMETER_SET")?.toIntOrNull() ?: 1024
        val client = KnishIOClient(listOf(URI(url)), encrypt = true, mlkemParameterSet = param)
        client.requestAuthToken(secret = secret, encrypt = true)

        // The encrypted transport still works for this session.
        val encResp = client.queryBalance("USER")
        expectThat(encResp.success()).isTrue()

        // Dropping to plaintext on the same session is the silent downgrade the validator refuses.
        client.disableEncryption()
        val plainResp = client.queryBalance("USER")
        expectThat(plainResp.success()).isFalse()
        expectThat(plainResp.status().toString()).contains("CipherHash encrypted transport")
    }
}
