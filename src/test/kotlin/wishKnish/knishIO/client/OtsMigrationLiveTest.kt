package wishKnish.knishIO.client

import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isTrue
import wishKnish.knishIO.client.data.MetaData
import wishKnish.knishIO.client.libraries.Crypto
import java.net.URI

/**
 * Live OTS version migration against a running validator: an identity created by a client at OTS
 * version 1 moves to version 2 when a client set to version 2 signs in, and stays at version 2 when
 * a client at version 1 signs in afterwards.
 *
 * Gated on OTS_MIGRATION_TEST_URL (skipped when it is unset):
 * `OTS_MIGRATION_TEST_URL=http://localhost:8081/graphql ./gradlew test --tests …OtsMigrationLiveTest`
 */
class OtsMigrationLiveTest {

    private fun serverUrl(): String {
        val url = System.getenv("OTS_MIGRATION_TEST_URL")
        Assumptions.assumeTrue(url != null) { "set OTS_MIGRATION_TEST_URL to run the live OTS migration test" }
        return url!!
    }

    @Test
    fun `a client set to OTS version 2 moves a version 1 identity to version 2`() {
        val url = serverUrl()
        val secret = Crypto.generateSecret()
        val bundle = Crypto.generateBundleHash(secret)

        fun client(otsVersion: Int): KnishIOClient =
            KnishIOClient(listOf(URI(url)), encrypt = false, otsVersion = otsVersion).apply {
                requestAuthToken(secret = secret, cellSlug = "public", encrypt = false)
            }

        fun head(knishio: KnishIOClient): Wallet {
            val wallet = knishio.queryContinuId(bundle, "USER").payload()
            expectThat(wallet).isNotNull()
            return wallet!!
        }

        fun version(wallet: Wallet): Int? = Wallet.detectOtsVersion(secret, "USER", wallet.position, wallet.address)

        // A client at version 1 creates the identity
        val first = client(1)
        val h1 = head(first)
        expectThat(version(h1)).isEqualTo(1)

        // A client at version 2 signs in from the version 1 head and leaves a version 2 head
        val second = client(2)
        val h2 = head(second)
        expectThat(h2.position).isNotEqualTo(h1.position)
        expectThat(version(h2)).isEqualTo(2)

        // The version 2 head signs a molecule
        val meta = second.createMeta(
            "ots-migration", "ots-migration-${System.currentTimeMillis()}", mutableListOf(MetaData("k", "v"))
        )
        expectThat(meta.success()).isTrue()
        val h3 = head(second)
        expectThat(h3.position).isNotEqualTo(h2.position)
        expectThat(version(h3)).isEqualTo(2)

        // A client at version 1 signs in and the identity stays at version 2
        val third = client(1)
        val h4 = head(third)
        expectThat(h4.position).isNotEqualTo(h3.position)
        expectThat(version(h4)).isEqualTo(2)
    }
}
