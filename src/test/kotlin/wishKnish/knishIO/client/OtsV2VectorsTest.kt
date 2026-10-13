/*
 * OTS v2 cross-SDK vectors: key derivation, address, the normalized hash, ML-KEM identities
 * of v2 wallets, molecule verdicts and the malformed declared-hash check.
 *
 * Reads the shared master ../shared-test-results/ots-v2-vectors.json (relative to the SDK root).
 * A standalone checkout has no master; the vector suite then registers one visible skip.
 * The v2 sign/verify round trip below always runs.
 */

package wishKnish.knishIO.client

import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import wishKnish.knishIO.client.data.MetaData
import wishKnish.knishIO.client.exception.SignatureMalformedException
import wishKnish.knishIO.client.exception.SignatureMismatchException
import wishKnish.knishIO.client.data.json.mutation.MoleculeMutation
import wishKnish.knishIO.client.httpClient.HttpClient
import wishKnish.knishIO.client.libraries.CheckMolecule
import wishKnish.knishIO.client.libraries.Crypto
import java.io.File
import java.net.URI

@DisplayName("OTS v2")
class OtsV2VectorsTest {

  private val json = Json { ignoreUnknownKeys = true }

  private val vectorsFile = File("../shared-test-results/ots-v2-vectors.json")

  private val exceptions = mapOf(
    "SignatureMismatch" to SignatureMismatchException::class.java,
    "SignatureMalformed" to SignatureMalformedException::class.java
  )

  private fun JsonObject.str(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

  private fun tests(vectors: JsonObject, family: String) =
    vectors[family]!!.jsonObject["tests"]!!.jsonArray.map { it.jsonObject }

  /** Rebuilds a molecule verbatim from vector JSON; the source wallet comes from the signing atom. */
  private fun reconstruct(data: JsonObject, secret: String? = null, sourceWallet: Wallet? = null): Molecule {
    val atoms = data["atoms"]!!.jsonArray.map { Atom.fromJSON(it.toString()) }
    val first = atoms.first()
    val source = sourceWallet ?: Wallet(secret = null, token = first.token, position = first.position).apply {
      address = first.walletAddress
    }
    return Molecule(secret = secret, sourceWallet = source, cellSlug = data.str("cellSlug")).apply {
      molecularHash = data.str("molecularHash")
      bundle = data.str("bundle")
      data.str("createdAt")?.let { createdAt = it }
      this.atoms.clear()
      this.atoms.addAll(atoms)
    }
  }

  @TestFactory
  @DisplayName("shared ots-v2-vectors.json")
  fun vectors(): List<DynamicNode> {
    assumeTrue(vectorsFile.isFile, "skipped: ../shared-test-results/ots-v2-vectors.json not found (standalone checkout)")
    val vectors = json.parseToJsonElement(vectorsFile.readText()).jsonObject["vectors"]!!.jsonObject
    val keyDerivation = tests(vectors, "key_derivation")
    val keyFrom = { name: String -> keyDerivation.first { it.str("name") == name } }

    val keys = tests(vectors, "key_derivation").map { v ->
      DynamicTest.dynamicTest(v.str("name")!!) {
        val secret = v.str("secret")!!
        val token = v.str("token")!!
        val position = v.str("position")!!
        assertEquals(v.str("expectedKeyV1"), Wallet.generatePrivateKey(secret, token, position))
        assertEquals(v.str("expectedKeyV2"), Wallet.generatePrivateKey(secret, token, position, 2))
        assertEquals(v.str("expectedAddressV1"), Wallet.generatePublicKey(v.str("expectedKeyV1")!!))
        assertEquals(v.str("expectedAddressV2"), Wallet.generatePublicKey(v.str("expectedKeyV2")!!))
        assertEquals(v.str("expectedAddressV2"), Wallet(secret, token, position, otsVersion = 2).address)
      }
    } + DynamicTest.dynamicTest("an unsupported OTS version is refused") {
      val error = assertThrows(IllegalArgumentException::class.java) { Wallet("x", otsVersion = 3) }
      assertEquals("KnishIO: unsupported OTS version 3; expected 1 or 2.", error.message)
    }

    val signature = tests(vectors, "signature").map { v ->
      DynamicTest.dynamicTest(v.str("name")!!) {
        val normalized = CheckMolecule.normalizedHash(v.str("molecularHash")!!)
        val expected = v["expectedNormalized"]!!.jsonArray.map { it.jsonPrimitive.int }
        assertEquals(expected, expected.indices.map { normalized[it] })
      }
    }

    val mlkem = tests(vectors, "mlkem_identity").map { v ->
      DynamicTest.dynamicTest(v.str("name")!!) {
        val k = keyFrom(v.str("keyFrom")!!)
        val pubkey = { set: Int ->
          Wallet(k.str("secret"), k.str("token")!!, k.str("position"), mlkemParameterSet = set, otsVersion = 2).pubkey
        }
        assertEquals(v.str("expectedMlKem1024Pubkey"), pubkey(1024))
        assertEquals(v.str("expectedMlKem768Pubkey"), pubkey(768))
      }
    }

    val moleculeTests = tests(vectors, "molecules")
    val verdicts = moleculeTests.map { v ->
      DynamicTest.dynamicTest("${v.str("name")} → ${v.str("expected")}") {
        val molecule = reconstruct(v["molecule"]!!.jsonObject)
        val expected = v.str("expected")!!
        if (expected == "verified") {
          assertTrue(Molecule.verify(molecule, molecule.sourceWallet))
        } else {
          assertThrows(exceptions.getValue(expected)) { Molecule.verify(molecule, molecule.sourceWallet) }
        }
      }
    }
    val resign = listOf("genuine_v2", "genuine_v1_control").map { name ->
      DynamicTest.dynamicTest("re-signing $name reproduces every otsFragment") {
        val v = moleculeTests.first { it.str("name") == name }
        val user = keyFrom("hex_secret_user")
        val secret = user.str("secret")!!
        val source = Wallet(secret, "USER", user.str("position"), otsVersion = v["otsVersion"]!!.jsonPrimitive.int)
        val molecule = reconstruct(v["molecule"]!!.jsonObject, secret, source)
        molecule.sign()
        val expected = v["molecule"]!!.jsonObject["atoms"]!!.jsonArray.map { it.jsonObject.str("otsFragment") }
        assertEquals(expected, molecule.atoms.map { it.otsFragment })
      }
    }

    val malformed = tests(vectors, "malformed_hash").map { v ->
      DynamicTest.dynamicTest("${v.str("name")} → ${v.str("expected")}") {
        val molecule = reconstruct(v["molecule"]!!.jsonObject)
        assertThrows(exceptions.getValue(v.str("expected")!!)) { CheckMolecule.ots(molecule) }
      }
    }

    return listOf(
      DynamicContainer.dynamicContainer("key_derivation", keys),
      DynamicContainer.dynamicContainer("signature", signature),
      DynamicContainer.dynamicContainer("mlkem_identity", mlkem),
      DynamicContainer.dynamicContainer("molecules", verdicts + resign),
      DynamicContainer.dynamicContainer("malformed_hash", malformed)
    )
  }

  @Test
  @DisplayName("v2 round trip: a 64-chain signature verifies and a changed fragment does not")
  fun v2RoundTrip() {
    val secret = "d".repeat(2048)
    val source = Wallet(secret, "USER", "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef", otsVersion = 2)
    val molecule = Molecule(secret, source)
    assertEquals(2, molecule.signingOtsVersion())
    assertEquals(2, molecule.remainderWallet!!.otsVersion)

    molecule.initMeta(mutableListOf(MetaData("role", "admin")), "AppRole", "role-1")
    molecule.sign(compressed = true)
    assertEquals(5464, molecule.atoms.joinToString("") { it.otsFragment ?: "" }.length)
    assertTrue(Molecule.verify(molecule, source))

    val fragment = molecule.atoms[0].otsFragment!!
    val idx = fragment.indexOfFirst { it == 'A' || it == 'B' }
    val flipped = if (fragment[idx] == 'A') 'B' else 'A'
    molecule.atoms[0].otsFragment = fragment.substring(0, idx) + flipped + fragment.substring(idx + 1)
    assertThrows(SignatureMismatchException::class.java) { Molecule.verify(molecule, source) }
  }

  private val clientSecret = "d".repeat(2048)
  private val clientPosition = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"
  private val v1Address = Wallet(clientSecret, "USER", clientPosition).address!!
  private val v2Address = Wallet(clientSecret, "USER", clientPosition, otsVersion = 2).address!!

  @Test
  @DisplayName("detectOtsVersion finds the version an address was derived at, in either preferred order")
  fun detectOtsVersion() {
    for (preferred in listOf(1, 2)) {
      assertEquals(2, Wallet.detectOtsVersion(clientSecret, "USER", clientPosition, v2Address, preferred))
      assertEquals(1, Wallet.detectOtsVersion(clientSecret, "USER", clientPosition, v1Address, preferred))
      assertEquals(2, Wallet.detectOtsVersion(clientSecret, "USER", clientPosition, v2Address.uppercase(), preferred))
      assertNull(Wallet.detectOtsVersion(clientSecret, "USER", clientPosition, Crypto.generateSecret(null, 64), preferred))
    }
  }

  @Test
  @DisplayName("otsVersion is settable on a keyless wallet only")
  fun setOtsVersion() {
    val keyless = Wallet(secret = null, token = "USER", position = clientPosition)
    keyless.otsVersion = 2
    assertEquals(2, keyless.otsVersion)

    val keyed = Wallet(clientSecret, "USER", clientPosition)
    keyed.otsVersion = 1
    val error = assertThrows(IllegalStateException::class.java) { keyed.otsVersion = 2 }
    assertEquals("KnishIO: cannot change the OTS version of a keyed wallet.", error.message)
    assertThrows(IllegalArgumentException::class.java) { keyless.otsVersion = 3 }
  }

  /** Runs [block] against a client whose transport answers every query with [continuIdData]. */
  private fun <T> withClient(otsVersion: Int, continuIdData: String, block: (KnishIOClient, MutableList<Molecule>) -> T): T {
    val authPayload = """{\"token\":\"auth-token\",\"time\":3600,\"key\":\"server-key\",\"pubkey\":\"server-key\",\"encrypt\":false,\"expiresAt\":2000000000}"""
    val proposals = mutableListOf<Molecule>()
    mockkConstructor(HttpClient::class)
    try {
      every { anyConstructed<HttpClient>().query(any()) } returns """{"data":{"ContinuId":$continuIdData}}"""
      every { anyConstructed<HttpClient>().mutate(any()) } answers {
        proposals.add(firstArg<MoleculeMutation>().variables.molecule)
        """{"data":{"ProposeMolecule":{"molecularHash":"hash","status":"accepted","payload":"$authPayload"}}}"""
      }
      return block(KnishIOClient(listOf(URI("http://localhost:8080/graphql")), otsVersion = otsVersion), proposals)
    } finally {
      unmockkConstructor(HttpClient::class)
    }
  }

  private fun continuIdData(address: String): String =
    """{"address":"$address","bundleHash":"${Crypto.generateBundleHash(clientSecret)}","tokenSlug":"USER","position":"$clientPosition","amount":"0"}"""

  @Test
  @DisplayName("a client at OTS version 2 creates v2 wallets from its secret")
  fun clientCreatesV2Wallets() {
    withClient(2, "null") { client, proposals ->
      assertEquals(2, client.otsVersion)
      client.requestAuthToken(secret = clientSecret, cellSlug = "public", encrypt = false)
      val auth = client.getAuthToken()!!.getWallet()!!
      assertEquals("AUTH", auth.token)
      assertEquals(2, auth.otsVersion)
      assertEquals(8192, auth.key!!.length)
      val authHead = proposals.single().atoms.first { it.isotope == 'I' }
      assertEquals(Wallet(clientSecret, "USER", authHead.position!!, otsVersion = 2).address, authHead.walletAddress)

      val source = client.sourceWallet()
      assertEquals(2, source.otsVersion)
      assertEquals(8192, source.key!!.length)

      client.createWallet("OTSV2")
      val meta = proposals.last().atoms.first { it.isotope == 'C' }.meta.associate { it.key to it.value }
      val position = meta.getValue("walletPosition")!!
      assertEquals(Wallet(clientSecret, "OTSV2", position, otsVersion = 2).address, meta["walletAddress"])
    }
    val error = assertThrows(IllegalArgumentException::class.java) {
      KnishIOClient(listOf(URI("http://localhost:8080/graphql")), otsVersion = 3)
    }
    assertEquals("KnishIO: unsupported OTS version 3; expected 1 or 2.", error.message)
  }

  @Test
  @DisplayName("a client at OTS version 1 binds a ledger wallet derived at v2 to version 2")
  fun bindsLedgerWalletVersion() {
    withClient(1, continuIdData(v2Address)) { client, _ ->
      client.setSecret(clientSecret)
      val source = client.sourceWallet()
      assertEquals(2, source.otsVersion)
      assertEquals(v2Address, source.address)
    }
  }

  @Test
  @DisplayName("a ledger wallet that neither version derives from the secret is refused")
  fun refusesUnderivableLedgerWallet() {
    val stranger = Wallet("someone-else", "USER", clientPosition).address!!
    withClient(1, continuIdData(stranger)) { client, _ ->
      client.setSecret(clientSecret)
      val error = assertThrows(IllegalStateException::class.java) { client.sourceWallet() }
      assertEquals("KnishIO: wallet $stranger is not derived from this secret at position $clientPosition under OTS version 1 or 2.", error.message)
    }
  }

  @Test
  @DisplayName("a client at OTS version 1 signs a returning v2 identity's login from its ContinuID pointer")
  fun detectsLoginPointerVersion() {
    withClient(1, continuIdData(v2Address)) { client, proposals ->
      client.requestAuthToken(secret = clientSecret, cellSlug = "public", encrypt = false)
      val wallet = client.getAuthToken()!!.getWallet()!!
      assertEquals("USER", wallet.token)
      assertEquals(2, wallet.otsVersion)
      assertEquals(v2Address, proposals.single().atoms[0].walletAddress)
      val pointerHead = proposals.single().atoms.first { it.isotope == 'I' }
      assertEquals(Wallet(clientSecret, "USER", pointerHead.position!!, otsVersion = 2).address, pointerHead.walletAddress)
    }
  }

  private fun authToken(wallet: Wallet): AuthToken = AuthToken.create(
    wishKnish.knishIO.client.data.graphql.types.AccessToken("t", 2000000000, "k", "k", false, 2000000000), wallet
  )

  @Test
  @DisplayName("an AuthToken snapshot keeps the session wallet's OTS version")
  fun snapshotRoundTrip() {
    val wallet = Wallet(clientSecret, "USER", clientPosition, otsVersion = 2)
    val snapshot = authToken(wallet).getSnapshot()
    assertEquals(2, snapshot.wallet.otsVersion)
    val restored = AuthToken.restore(snapshot, clientSecret).getWallet()!!
    assertEquals(2, restored.otsVersion)
    assertEquals(wallet.address, restored.address)
  }

  @Test
  @DisplayName("an AuthToken snapshot without otsVersion restores at version 1")
  fun snapshotWithoutOtsVersion() {
    val snapshot = authToken(Wallet(clientSecret, "USER", clientPosition)).getSnapshot()
    snapshot.wallet = snapshot.wallet.copy(otsVersion = null)
    val restored = AuthToken.restore(snapshot, clientSecret).getWallet()!!
    assertEquals(1, restored.otsVersion)
    assertEquals(v1Address, restored.address)

    snapshot.wallet = snapshot.wallet.copy(otsVersion = 3)
    val error = assertThrows(IllegalArgumentException::class.java) { AuthToken.restore(snapshot, clientSecret) }
    assertEquals("KnishIO: unsupported OTS version 3; expected 1 or 2.", error.message)
  }
}
