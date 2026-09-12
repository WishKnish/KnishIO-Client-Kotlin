package wishKnish.knishIO.client.storage.keystore

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import wishKnish.knishIO.client.exception.SecretStorageException
import wishKnish.knishIO.client.storage.MemoryStorageBackend
import wishKnish.knishIO.client.storage.SecretEnvelope
import wishKnish.knishIO.client.storage.StorageOptions
import wishKnish.knishIO.client.storage.StorageBackend
import java.security.KeyStore
import java.util.Base64

@RunWith(AndroidJUnit4::class)
class AndroidKeystoreSecretStorageProviderTest {

  private val testAliases = mutableListOf<String>()

  private fun nextAlias(): String {
    val alias = "knishio.test.${System.nanoTime()}"
    testAliases.add(alias)
    return alias
  }

  @After
  fun tearDown() {
    val ks = try {
      KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    } catch (_: Exception) {
      null
    }
    if (ks != null) {
      for (alias in testAliases) {
        try {
          if (ks.containsAlias(alias)) {
            ks.deleteEntry(alias)
          }
        } catch (_: Exception) {}
      }
    }
    testAliases.clear()
  }

  private fun createProviderIfHardwareAvailable(backend: StorageBackend, alias: String): AndroidKeystoreSecretStorageProvider? {
    return try {
      AndroidKeystoreSecretStorageProvider(backend, alias)
    } catch (e: SecretStorageException) {
      if (e.message?.contains("not hardware-backed") == true) {
        null
      } else {
        throw e
      }
    }
  }

  @Test
  fun custodyIsPlatformDerivedAndEnvelopeMatchesTheSharedFormat() {
    val alias = nextAlias()
    val backend = MemoryStorageBackend()
    val result = runCatching { AndroidKeystoreSecretStorageProvider(backend, alias) }

    if (result.isSuccess) {
      val provider = result.getOrThrow()
      assertTrue("isHardwareBacked must be true", provider.isHardwareBacked())
      assertTrue(
        "providerType must be TEE or StrongBox, got ${provider.providerType}",
        provider.providerType in setOf(
          AndroidKeystoreSecretStorageProvider.PROVIDER_TYPE_TEE,
          AndroidKeystoreSecretStorageProvider.PROVIDER_TYPE_STRONGBOX
        )
      )

      val bundle = "deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef"
      val secret = "MASTER-SECRET-ANDROID-PROBE"
      provider.storeSecret(bundle, secret, StorageOptions(label = "probe", allowUnrecoverable = true))

      assertEquals(secret, provider.retrieveSecret(bundle))
      val withResult = provider.withSecret(bundle) { it.substring(0, 13) }
      assertEquals("MASTER-SECRET", withResult)

      val storedRaw = backend.getItem("knishio:secret:$bundle")
      assertNotNull(storedRaw)

      val jsonElem = JSONObject(storedRaw!!)
      val metadata = jsonElem.getJSONObject("metadata")

      val requiredKeys = listOf("bundleHash", "createdAt", "hardwareBacked", "providerType")
      val forbiddenKeys = listOf("bundle_hash", "created_at", "hardware_backed", "provider_type")

      for (key in requiredKeys) {
        assertTrue("emitted metadata must contain $key", metadata.has(key))
      }
      for (key in forbiddenKeys) {
        assertFalse("emitted metadata must not contain $key", metadata.has(key))
      }

      assertEquals(true, metadata.getBoolean("hardwareBacked"))
      assertEquals(provider.providerType, metadata.getString("providerType"))

      val payload = SecretEnvelope.decode(storedRaw)
      assertEquals("AES-GCM", payload.algorithm)
      assertEquals(100000, payload.iterations)
      assertEquals(16, Base64.getDecoder().decode(payload.salt).size)
      assertEquals(12, Base64.getDecoder().decode(payload.iv).size)
    } else {
      // Software-level KeyMint on emulator: verified fail-closed!
      val ex = result.exceptionOrNull()
      assertTrue("Expected SecretStorageException, got $ex", ex is SecretStorageException)
      assertTrue(
        "Expected fail-closed message on non-hardware key, got: ${ex?.message}",
        ex?.message?.contains("not hardware-backed") == true
      )
    }
  }

  @Test
  fun secondInstanceWithSameAliasReadsTheFirstInstancesSecret() {
    val alias = nextAlias()
    val backend = MemoryStorageBackend()
    val provider1 = createProviderIfHardwareAvailable(backend, alias)
    if (provider1 == null) {
      // In software-only environment (emulator): verified that instance cannot claim custody
      val ex = assertThrows(SecretStorageException::class.java) {
        AndroidKeystoreSecretStorageProvider(backend, alias)
      }
      assertTrue(ex.message?.contains("not hardware-backed") == true)
      return
    }

    val bundle = "cafebabecafebabecafebabecafebabecafebabecafebabecafebabecafebabe"
    val secret = "PERSISTENT-DEVICE-SECRET-TEST"
    provider1.storeSecret(bundle, secret, StorageOptions(allowUnrecoverable = true))

    val provider2 = AndroidKeystoreSecretStorageProvider(backend, alias)
    assertEquals(secret, provider2.retrieveSecret(bundle))
  }

  @Test
  fun strongBoxRequirementNeverDegradesToTee() {
    val alias = nextAlias()
    val backend = MemoryStorageBackend()
    val result = runCatching {
      AndroidKeystoreSecretStorageProvider(
        backend = backend,
        keyAlias = alias,
        strongBox = StrongBoxPolicy.REQUIRED
      )
    }

    if (result.isSuccess) {
      val provider = result.getOrThrow()
      assertEquals(
        AndroidKeystoreSecretStorageProvider.PROVIDER_TYPE_STRONGBOX,
        provider.providerType
      )
      assertTrue(provider.isHardwareBacked())
    } else {
      val ex = result.exceptionOrNull()
      assertTrue("Expected SecretStorageException, got $ex", ex is SecretStorageException)
      assertTrue(
        "Expected exception message to mention StrongBox or non-hardware, got: ${ex?.message}",
        ex?.message?.contains("StrongBox") == true || ex?.message?.contains("not hardware-backed") == true
      )
    }
  }

  @Test
  fun callerPassphraseIsRejected() {
    val alias = nextAlias()
    val backend = MemoryStorageBackend()
    val provider = createProviderIfHardwareAvailable(backend, alias)
    if (provider == null) {
      // In software-only environment, provider instantiation fails closed
      val ex = assertThrows(SecretStorageException::class.java) {
        AndroidKeystoreSecretStorageProvider(backend, alias)
      }
      assertTrue(ex.message?.contains("not hardware-backed") == true)
      return
    }

    val bundle = "1111222233334444555566667777888811112222333344445555666677778888"
    assertThrows(SecretStorageException::class.java) {
      provider.storeSecret(bundle, "secret", StorageOptions(passphrase = "forbidden", allowUnrecoverable = true))
    }
    assertThrows(SecretStorageException::class.java) {
      provider.retrieveSecret(bundle, StorageOptions(passphrase = "forbidden"))
    }
    assertThrows(SecretStorageException::class.java) {
      val unused = provider.withSecret(bundle, StorageOptions(passphrase = "forbidden")) { it }
    }
  }

  @Test
  fun storeSecretFailsWithoutRecoveryPassphraseUnlessAllowUnrecoverable() {
    val alias = nextAlias()
    val backend = MemoryStorageBackend()
    val provider = createProviderIfHardwareAvailable(backend, alias) ?: return

    val bundle = "1111222233334444555566667777888811112222333344445555666677778888"
    val ex = assertThrows(SecretStorageException::class.java) {
      provider.storeSecret(bundle, "secret")
    }
    assertTrue(
      "Expected exception to mention recovery passphrase requirement, got: ${ex.message}",
      ex.message?.contains("Recovery passphrase required") == true
    )
  }

  @Test
  fun preferredPolicyResolvesToTheDeviceBestLevel() {
    val alias = nextAlias()
    val backend = MemoryStorageBackend()
    val provider = createProviderIfHardwareAvailable(backend, alias)
    if (provider == null) {
      val ex = assertThrows(SecretStorageException::class.java) {
        AndroidKeystoreSecretStorageProvider(backend, alias)
      }
      assertTrue(ex.message?.contains("not hardware-backed") == true)
      return
    }

    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    val key = keyStore.getKey(alias, null) as? javax.crypto.SecretKey
    assertNotNull(key)
    val factory = javax.crypto.SecretKeyFactory.getInstance("AES", "AndroidKeyStore")
    val keyInfo = factory.getKeySpec(key, android.security.keystore.KeyInfo::class.java) as android.security.keystore.KeyInfo

    assertTrue("requireUnlockedDevice should be true", provider.requireUnlockedDevice)
    assertTrue("isInsideSecureHardware should be true", keyInfo.isInsideSecureHardware)
    when (keyInfo.securityLevel) {
      android.security.keystore.KeyProperties.SECURITY_LEVEL_STRONGBOX -> {
        assertEquals(AndroidKeystoreSecretStorageProvider.PROVIDER_TYPE_STRONGBOX, provider.providerType)
      }
      android.security.keystore.KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT,
      android.security.keystore.KeyProperties.SECURITY_LEVEL_UNKNOWN_SECURE -> {
        assertEquals(AndroidKeystoreSecretStorageProvider.PROVIDER_TYPE_TEE, provider.providerType)
      }
    }
  }

  @Test
  fun disabledPolicyNeverYieldsStrongBox() {
    val alias = nextAlias()
    val backend = MemoryStorageBackend()
    val result = runCatching {
      AndroidKeystoreSecretStorageProvider(
        backend = backend,
        keyAlias = alias,
        strongBox = StrongBoxPolicy.DISABLED
      )
    }

    if (result.isSuccess) {
      val provider = result.getOrThrow()
      assertEquals(
        AndroidKeystoreSecretStorageProvider.PROVIDER_TYPE_TEE,
        provider.providerType
      )
    } else {
      val ex = result.exceptionOrNull()
      assertTrue(ex is SecretStorageException)
      assertTrue(ex?.message?.contains("not hardware-backed") == true)
    }
  }

  @Test
  fun attestationChainMatchesResolvedCustody() {
    val alias = nextAlias()
    val backend = MemoryStorageBackend()
    val provider = createProviderIfHardwareAvailable(backend, alias)
    if (provider == null) {
      val ex = assertThrows(SecretStorageException::class.java) {
        AndroidKeystoreSecretStorageProvider(backend, alias)
      }
      assertTrue(ex.message?.contains("not hardware-backed") == true)
      return
    }

    val challenge = ByteArray(32)
    java.security.SecureRandom().nextBytes(challenge)
    val chain = provider.attestCustody(challenge)
    assertTrue("attestation chain must have at least 2 certificates", chain.size >= 2)

    val summary = AndroidKeyAttestation.parse(chain[0])
    assertTrue("attestation challenge must match", challenge.contentEquals(summary.challenge))
    assertEquals(
      "attested keyMint security level must match providerType",
      provider.providerType,
      AndroidKeyAttestation.providerTypeFor(summary.keyMintSecurityLevel)
    )
  }

  @Test
  fun userAuthenticationRequiredKeyRefusesUnauthenticatedUse() {
    val alias = nextAlias()
    val backend = MemoryStorageBackend()
    val result = runCatching {
      AndroidKeystoreSecretStorageProvider(
        backend = backend,
        keyAlias = alias,
        userAuthenticationValiditySeconds = 1
      )
    }

    if (result.isFailure) {
      val ex = result.exceptionOrNull()
      assertTrue(
        "Expected fail-closed (no lock screen or non-hardware), got: ${ex?.message}",
        ex?.message?.contains("secure lock screen", ignoreCase = true) == true ||
          ex?.message?.contains("not hardware-backed", ignoreCase = true) == true
      )
      return
    }

    val provider = result.getOrThrow()
    val bundle = "3333333333333333333333333333333333333333333333333333333333333333"
    Thread.sleep(1500)
    val ex = assertThrows(SecretStorageException::class.java) {
      provider.storeSecret(bundle, "auth-required-secret")
    }
    assertTrue(
      "Expected message to mention user authentication, got: ${ex.message}",
      ex.message?.contains("user authentication") == true
    )
  }
}
