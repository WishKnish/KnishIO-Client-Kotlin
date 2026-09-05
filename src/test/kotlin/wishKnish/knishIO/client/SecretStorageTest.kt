package wishKnish.knishIO.client

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.*
import wishKnish.knishIO.client.exception.SecretStorageException
import wishKnish.knishIO.client.libraries.Crypto
import wishKnish.knishIO.client.libraries.SecureMemory
import wishKnish.knishIO.client.storage.*
import java.net.URI

class SecretStorageTest {

  private val testSeed = "knishio-kotlin-hardware-storage-test"
  private val canonicalSecret = Crypto.generateSecret(testSeed)
  private val canonicalBundle = Crypto.generateBundleHash(canonicalSecret)

  @Test
  fun `SecureMemory zeroizes byte arrays and char arrays`() {
    val bytes = byteArrayOf(1, 2, 3, 4, 5)
    SecureMemory.zeroize(bytes)
    expectThat(bytes.toList()).containsExactly(0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte())

    val chars = charArrayOf('a', 'b', 'c')
    SecureMemory.zeroize(chars)
    expectThat(chars.toList()).containsExactly('\u0000', '\u0000', '\u0000')
  }

  @Test
  fun `SecureMemory withSecureBytes zeroes buffer after execution`() {
    val bytes = byteArrayOf(42, 43, 44)
    var observed: Byte = 0
    SecureMemory.withSecureBytes(bytes) {
      observed = it[0]
    }
    expectThat(observed).isEqualTo(42.toByte())
    expectThat(bytes.toList()).containsExactly(0.toByte(), 0.toByte(), 0.toByte())
  }

  @Test
  fun `SecureMemory constantTimeEquals compares correctly`() {
    expectThat(SecureMemory.constantTimeEquals("secret123", "secret123")).isTrue()
    expectThat(SecureMemory.constantTimeEquals("secret123", "secret124")).isFalse()
    expectThat(SecureMemory.constantTimeEquals("secret123", "secret12")).isFalse()
  }

  @Test
  fun `MemorySecretStorageProvider performs full CRUD and withSecret`() {
    val provider = MemorySecretStorageProvider()

    expectThat(provider.providerType).isEqualTo("memory")
    expectThat(provider.isHardwareBacked()).isFalse()
    expectThat(provider.isAvailable()).isTrue()

    val bundle = "test-bundle-hash-123"
    val secret = "test-secret-value-xyz"

    expectThat(provider.hasSecret(bundle)).isFalse()
    expectThat(provider.retrieveSecret(bundle)).isNull()

    provider.storeSecret(bundle, secret, StorageOptions(label = "Test Key"))
    expectThat(provider.hasSecret(bundle)).isTrue()
    expectThat(provider.retrieveSecret(bundle)).isEqualTo(secret)

    val list = provider.listSecrets()
    expectThat(list).hasSize(1)
    expectThat(list.first().bundleHash).isEqualTo(bundle)
    expectThat(list.first().label).isEqualTo("Test Key")

    val length = provider.withSecret(bundle) { it.length }
    expectThat(length).isEqualTo(secret.length)

    val deleted = provider.deleteSecret(bundle)
    expectThat(deleted).isTrue()
    expectThat(provider.hasSecret(bundle)).isFalse()
  }

  @Test
  fun `MemorySecretStorageProvider throws when secret is not found`() {
    val provider = MemorySecretStorageProvider()
    expectThrows<SecretStorageException> {
      provider.withSecret("missing-bundle") { it }
    }
  }

  @Test
  fun `AesGcmSecretStorageProvider encrypts and decrypts correctly`() {
    val provider = AesGcmSecretStorageProvider(
      defaultPassphrase = "test-encryption-passphrase"
    )

    expectThat(provider.isAvailable()).isTrue()
    expectThat(provider.providerType).isEqualTo("aes-gcm")

    provider.storeSecret(canonicalBundle, canonicalSecret, StorageOptions(label = "Prod Seed"))
    expectThat(provider.hasSecret(canonicalBundle)).isTrue()

    val retrieved = provider.retrieveSecret(canonicalBundle)
    expectThat(retrieved).isEqualTo(canonicalSecret)

    val prefix = provider.withSecret(canonicalBundle) { it.substring(0, 16) }
    expectThat(prefix).isEqualTo(canonicalSecret.substring(0, 16))

    val list = provider.listSecrets()
    expectThat(list).hasSize(1)
    expectThat(list.first().bundleHash).isEqualTo(canonicalBundle)
    expectThat(list.first().label).isEqualTo("Prod Seed")
  }

  @Test
  fun `AesGcmSecretStorageProvider fails with wrong passphrase`() {
    val provider = AesGcmSecretStorageProvider()
    val bundle = "bundle-wrong-pass-test"
    val secret = "super-secret"

    provider.storeSecret(bundle, secret, StorageOptions(passphrase = "correct-password"))

    expectThrows<SecretStorageException> {
      provider.retrieveSecret(bundle, StorageOptions(passphrase = "wrong-password"))
    }

    expectThrows<SecretStorageException> {
      provider.withSecret(bundle, StorageOptions(passphrase = "wrong-password")) { it }
    }
  }

  @Test
  fun `SecretStorageFactory creates requested provider types`() {
    val memoryProvider = SecretStorageFactory.createDefault(type = "memory")
    expectThat(memoryProvider.providerType).isEqualTo("memory")

    val aesProvider = SecretStorageFactory.createDefault(type = "aes-gcm", defaultPassphrase = "pass")
    expectThat(aesProvider.providerType).isEqualTo("aes-gcm")
  }

  @Test
  fun `KnishIOClient integrates with SecretStorageProvider without cleartext retention`() {
    val storage = AesGcmSecretStorageProvider(defaultPassphrase = "client-secure-pass")
    storage.storeSecret(canonicalBundle, canonicalSecret, StorageOptions(label = "Client Key"))

    val client = KnishIOClient(
      uris = listOf(URI("https://api.test.knish.io/graphql")),
      secretStorage = storage
    )
    client.setSecretStorage(storage, canonicalBundle)

    expectThat(client.hasSecret()).isTrue()
    expectThat(client.hasBundle()).isTrue()
    expectThat(client.bundle()).isEqualTo(canonicalBundle)

    // Verify cleartext secret is not retained in the client's internal secret property
    // Reflection check:
    val secretField = KnishIOClient::class.java.getDeclaredField("secret")
    secretField.isAccessible = true
    val internalSecret = secretField.get(client) as String
    expectThat(internalSecret).isEmpty()

    // Can retrieve secret via storage
    val retrieved = client.retrieveSecret()
    expectThat(retrieved).isEqualTo(canonicalSecret)

    // createMolecule unwraps from storage and signs
    val sourceWallet = Wallet(
      secret = canonicalSecret,
      token = "USER",
      position = "0".repeat(64)
    )

    val molecule = client.createMolecule(
      sourceWallet = sourceWallet
    )

    expectThat(molecule.bundle).isEqualTo(canonicalBundle)
    expectThat(molecule.sourceWallet).isEqualTo(sourceWallet)
    expectThat(molecule.remainderWallet).isNotNull()

    // Sign the molecule
    val atom = Atom(
      position = sourceWallet.position ?: "",
      walletAddress = sourceWallet.address ?: "",
      isotope = 'C',
      token = "USER",
      value = "0"
    )
    molecule.addAtom(atom)

    val signature = molecule.sign()
    expectThat(signature).isNotNull().isNotEmpty()
    expectThat(molecule.molecularHash).isNotNull()
  }

  @Test
  fun `KnishIOClient setSecret auto-syncs to storage and reset clears it`() {
    val client = KnishIOClient(listOf(URI("https://api.test.knish.io/graphql")))

    expectThat(client.hasSecret()).isFalse()
    expectThat(client.getSecretStorage()).isNull()

    client.setSecret(canonicalSecret)

    expectThat(client.hasSecret()).isTrue()
    expectThat(client.bundle()).isEqualTo(canonicalBundle)
    expectThat(client.getSecret()).isEqualTo(canonicalSecret)

    val storage = client.getSecretStorage()
    expectThat(storage).isNotNull()
    expectThat(storage?.hasSecret(canonicalBundle)).isTrue()
    expectThat(storage?.retrieveSecret(canonicalBundle)).isEqualTo(canonicalSecret)

    client.reset()

    expectThat(client.hasSecret()).isFalse()
    expectThat(client.getSecretStorage()).isNull()
  }

  @Test
  fun `withSecret propagates caller exceptions unwrapped`() {
    val storage = AesGcmSecretStorageProvider(defaultPassphrase = "client-secure-pass")
    val bundle = "cafebabe".repeat(8)
    storage.storeSecret(bundle, "secret-for-boundary-test", StorageOptions())

    // A caller failure is NOT a decryption failure and must not be re-labelled as one.
    expectThrows<IllegalStateException> {
      storage.withSecret(bundle, StorageOptions()) { throw IllegalStateException("caller failure") }
    }

    // The SDK's own failures still surface as SecretStorageException.
    expectThrows<SecretStorageException> {
      storage.withSecret(bundle, StorageOptions(passphrase = "wrong-password")) { it }
    }
  }
}
