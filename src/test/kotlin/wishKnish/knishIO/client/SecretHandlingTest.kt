package wishKnish.knishIO.client

import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.spyk
import io.mockk.unmockkConstructor
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue
import wishKnish.knishIO.client.data.json.mutation.MoleculeMutation
import wishKnish.knishIO.client.data.json.query.ContinuId
import wishKnish.knishIO.client.httpClient.HttpClient
import wishKnish.knishIO.client.libraries.Crypto
import wishKnish.knishIO.client.libraries.EncryptionMode
import wishKnish.knishIO.client.libraries.HybridCrypto
import wishKnish.knishIO.client.storage.AesGcmSecretStorageProvider
import wishKnish.knishIO.client.storage.StorageOptions
import java.net.URI

/**
 * Hybrid encryption never downgrades to classical NaCl when the post-quantum path fails, and a
 * client whose secret lives only in a SecretStorageProvider authenticates and builds molecules
 * without re-storing the secret or keeping it in cleartext.
 */
class SecretHandlingTest {

  private val secret = Crypto.generateSecret("fleet-audit-2026-11")
  private val bundle = Crypto.generateBundleHash(secret)
  private val uri = URI("https://knish-test.io/graphql")

  @Test
  fun `a post-quantum failure throws instead of falling back to NaCl`() {
    val sender = Wallet(secret, "USER", "aa".repeat(32))
    val recipient = Wallet(Crypto.generateSecret("fleet-audit-2026-11-recipient"), "USER", "bb".repeat(32))
    // Neither key is the hex ML-KEM key the post-quantum path expects, so that path fails on both;
    // the NaCl key is one a classical fallback could still encrypt to.
    val keys = listOf(recipient.pubkey!!, recipient.getMyEncPublicKey()!!)

    keys.forEach { key ->
      val viaWallet = runCatching { sender.encryptStringHybrid("hello", key) }
      expectThat(viaWallet.isFailure).describedAs("Wallet.encryptStringHybrid -> ${viaWallet.getOrNull()}").isTrue()
      val viaHybrid = runCatching { HybridCrypto.encryptMessage("hello", key, EncryptionMode.HYBRID) }
      expectThat(viaHybrid.isFailure).describedAs("HybridCrypto.encryptMessage -> ${viaHybrid.getOrNull()}").isTrue()
    }
  }

  private fun recipient() = Wallet(Crypto.generateSecret("fleet-audit-2026-11-recipient"), "USER", "bb".repeat(32))

  /** A wallet without a post-quantum key: [Wallet.encryptStringHybrid] writes a `NaCl-Box` envelope. */
  private fun classicalSender() = Wallet(secret, "USER", "aa".repeat(32)).apply { pqPrivateKey = null; pqPublicKey = null }

  @Test
  fun `an envelope that claims post-quantum is never decrypted classically`() {
    val recipient = recipient()
    val classical = classicalSender().encryptString("forged", recipient.getMyEncPublicKey()!!)
    val downgrade = mapOf("version" to "2", "data" to classical)

    expectThat(recipient.decryptStringHybrid(downgrade)).isNull()
    expectThat(runCatching { HybridCrypto.decryptMessage(downgrade, recipient.getMyEncPrivateKey()!!, recipient.pqPrivateKey) }.isFailure).isTrue()

    // An unknown version is refused rather than tried both ways.
    val sealed = HybridCrypto.encryptMessage("forged", recipient.getMyEncPublicKey()!!, EncryptionMode.CLASSICAL_ONLY)
    val unknown = runCatching { HybridCrypto.decryptMessage(sealed + ("version" to "9"), recipient.getMyEncPrivateKey()!!, recipient.pqPrivateKey) }
    expectThat(unknown.exceptionOrNull()).describedAs("decryptMessage(version 9) -> ${unknown.getOrNull()}").isNotNull().isA<IllegalArgumentException>()
  }

  @Test
  fun `hybrid round trips still decrypt for wallets with and without a post-quantum key`() {
    val pqSender = Wallet(secret, "USER", "aa".repeat(32))
    val recipient = recipient()
    val pq = pqSender.encryptStringHybrid("hello", recipient.getPostQuantumPublicKey()!!)
    expectThat(pq["version"]).isEqualTo("2")
    expectThat(recipient.decryptStringHybrid(pq)).isEqualTo("hello")

    val box = classicalSender().encryptStringHybrid("hello", recipient.getMyEncPublicKey()!!)
    expectThat(box["algorithm"]).isEqualTo("NaCl-Box")
    expectThat(recipient.decryptStringHybrid(box)).isEqualTo("hello")
    val keyless = recipient().apply { pqPrivateKey = null; pqPublicKey = null }
    expectThat(keyless.decryptStringHybrid(box)).isEqualTo("hello")

    val sealed = HybridCrypto.encryptMessage("hello", recipient.getMyEncPublicKey()!!, EncryptionMode.CLASSICAL_ONLY)
    for (mode in listOf(EncryptionMode.HYBRID, EncryptionMode.CLASSICAL_ONLY)) {
      val wallet = recipient().apply { encryptionMode = mode }
      expectThat(wallet.decryptStringHybrid(sealed)).describedAs("$mode, NaCl-SealedBox").isEqualTo("hello")
      expectThat(wallet.decryptStringHybrid(box)).describedAs("$mode, NaCl-Box").isEqualTo("hello")
    }
  }

  @Test
  fun `a POST_QUANTUM_ONLY wallet refuses classical envelopes`() {
    val recipient = recipient().apply { encryptionMode = EncryptionMode.POST_QUANTUM_ONLY }
    val box = classicalSender().encryptStringHybrid("hello", recipient.getMyEncPublicKey()!!)
    val sealed = HybridCrypto.encryptMessage("hello", recipient.getMyEncPublicKey()!!, EncryptionMode.CLASSICAL_ONLY)

    expectThat(recipient.decryptStringHybrid(box)).describedAs("NaCl-Box").isNull()
    expectThat(recipient.decryptStringHybrid(sealed)).describedAs("NaCl-SealedBox").isNull()
    listOf("NaCl-Box" to box, "NaCl-SealedBox" to sealed).forEach { (kind, envelope) ->
      val direct = runCatching {
        HybridCrypto.decryptMessage(envelope, recipient.getMyEncPrivateKey()!!, recipient.pqPrivateKey, EncryptionMode.POST_QUANTUM_ONLY)
      }
      expectThat(direct.exceptionOrNull()).describedAs("$kind -> ${direct.getOrNull()}").isNotNull().isA<IllegalArgumentException>()
    }
  }

  private fun secretField(client: KnishIOClient): String =
    KnishIOClient::class.java.getDeclaredField("secret").apply { isAccessible = true }.get(client) as String

  @Test
  fun `a client with only secret storage authenticates and creates a wallet without re-storing the secret`() {
    val storage = spyk(AesGcmSecretStorageProvider(defaultPassphrase = "fleet-audit-pass"))
    storage.storeSecret(bundle, secret, StorageOptions(label = "Client Key"))

    val pointer = Wallet(secret, "USER", "c0ffee00".repeat(8))
    val authPayload = """{\"token\":\"auth-token\",\"time\":3600,\"key\":\"server-key\",\"pubkey\":\"server-key\",\"encrypt\":false,\"expiresAt\":2000000000}"""
    val proposals = mutableListOf<Molecule>()

    mockkConstructor(HttpClient::class)
    try {
      every { anyConstructed<HttpClient>().query(any()) } answers {
        check(firstArg<Any>() is ContinuId) { "unexpected query ${firstArg<Any>()::class.simpleName}" }
        """{"data":{"ContinuId":{"address":"${pointer.address}","bundleHash":"$bundle","tokenSlug":"USER","position":"${pointer.position}","amount":"0"}}}"""
      }
      every { anyConstructed<HttpClient>().mutate(any()) } answers {
        val molecule = firstArg<MoleculeMutation>().variables.molecule
        proposals.add(molecule)
        """{"data":{"ProposeMolecule":{"molecularHash":"${molecule.molecularHash}","status":"accepted","payload":"$authPayload"}}}"""
      }

      val client = KnishIOClient(listOf(uri), secretStorage = storage)
      client.setSecretStorage(storage, bundle)

      expectThat(client.getSecret()).isEqualTo(secret)
      client.requestAuthToken(cellSlug = "public")
      val response = client.createWallet("FOO")

      expectThat(response.success()).isTrue()
      expectThat(proposals.size).isEqualTo(2)
      expectThat(proposals.last().atoms.first().isotope).isEqualTo('C')
      expectThat(secretField(client)).isEmpty()
      verify(exactly = 1) { storage.storeSecret(any(), any(), any()) }
    } finally {
      unmockkConstructor(HttpClient::class)
    }
  }
}
