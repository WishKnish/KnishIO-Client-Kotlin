/*
 * Cross-platform vectors — verifies the Kotlin SDK against the shared
 * cross-platform-test-vectors.json (SHAKE256 / bundle_hash / wallet_generation /
 * molecular_hash / wots_signature, the Rust reference implementation).
 *
 * Sibling of the other SDKs' cross-platform tests (PHP CrossPlatformVectorsTest,
 * Rust cross_platform_vectors.rs, JS cross-platform-canonical.test.js) — one of
 * the two unified cross-SDK vector tests every package shares (the other is
 * PatentVectorValidationTest over canonical-patent-vectors.json).
 */

package wishKnish.knishIO.client

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Tag
import wishKnish.knishIO.client.libraries.Crypto
import wishKnish.knishIO.client.libraries.Shake256
import wishKnish.knishIO.client.libraries.NaClBox
import wishKnish.knishIO.client.libraries.Soda
import wishKnish.knishIO.client.libraries.sealOpen
import wishKnish.knishIO.client.data.graphql.types.AccessToken
import java.util.Base64

@DisplayName("Cross-Platform Vectors (shared cross-platform-test-vectors.json)")
class CrossPlatformVectorsTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val vectors: JsonObject by lazy {
        val stream = javaClass.classLoader.getResourceAsStream("cross-platform-test-vectors.json")
            ?: throw IllegalStateException("cross-platform-test-vectors.json not found in test resources")
        val content = stream.bufferedReader().use { it.readText() }
        json.parseToJsonElement(content).jsonObject["vectors"]!!.jsonObject
    }

    @TestFactory
    @DisplayName("SHAKE256 Vectors")
    fun shake256Vectors(): List<DynamicTest> {
        val tests = vectors["shake256"]!!.jsonObject["tests"]!!.jsonArray
        return tests.map { element ->
            val test = element.jsonObject
            val name = test["name"]!!.jsonPrimitive.content
            val input = test["input"]!!.jsonPrimitive.content
            val outputLength = test["outputLength"]!!.jsonPrimitive.int
            val expected = test["expected"]!!.jsonPrimitive.content

            DynamicTest.dynamicTest("SHAKE256: $name") {
                val result = Shake256.hash(input, outputLength)
                assertEquals(expected, result, "SHAKE256 mismatch for vector: $name")
            }
        }
    }

    @TestFactory
    @DisplayName("Bundle Hash Vectors")
    fun bundleHashVectors(): List<DynamicTest> {
        val tests = vectors["bundle_hash"]!!.jsonObject["tests"]!!.jsonArray
        return tests.map { element ->
            val test = element.jsonObject
            val name = test["name"]!!.jsonPrimitive.content
            val secret = test["secret"]!!.jsonPrimitive.content
            val expected = test["expected"]!!.jsonPrimitive.content

            DynamicTest.dynamicTest("Bundle hash: $name") {
                val result = Crypto.generateBundleHash(secret)
                assertEquals(expected, result, "Bundle hash mismatch for vector: $name")
            }
        }
    }

    @TestFactory
    @DisplayName("Wallet Address Vectors")
    fun walletAddressVectors(): List<DynamicTest> {
        val tests = vectors["wallet_generation"]!!.jsonObject["tests"]!!.jsonArray
        return tests.map { element ->
            val test = element.jsonObject
            val name = test["name"]!!.jsonPrimitive.content
            val secret = test["secret"]!!.jsonPrimitive.content
            val token = test["token"]!!.jsonPrimitive.content
            val position = test["position"]!!.jsonPrimitive.content
            val expectedBundle = test["expectedBundle"]!!.jsonPrimitive.content
            val expectedAddress = test["expectedAddress"]!!.jsonPrimitive.content

            DynamicTest.dynamicTest("Wallet: $name") {
                // Bundle hash must match
                val bundle = Crypto.generateBundleHash(secret)
                assertEquals(expectedBundle, bundle, "Bundle hash mismatch for wallet: $name")

                // Create wallet with explicit position and check address
                val wallet = Wallet(secret = secret, token = token, position = position)
                assertEquals(expectedAddress, wallet.address, "Wallet address mismatch for wallet: $name")
            }
        }
    }

    @Test
    @Tag("mlkem")
    @DisplayName("Active ML-KEM backend instance matches system property configuration")
    fun backendMatchesConfiguration() {
        val expected = System.getProperty(wishKnish.knishIO.client.libraries.MlKemBackend.BACKEND_PROPERTY) ?: "noble"
        assertEquals(expected, wishKnish.knishIO.client.libraries.MlKemBackend.instance.name, "MlKemBackend.instance must match configured property")
    }

    // ML-KEM768 keygen-from-seed is deterministic (FIPS-203) → byte-frozen pubkey, like a SHAKE vector.
    @Test
    @Tag("mlkem")
    @DisplayName("ML-KEM768 keygen")
    fun mlkem768Keygen() {
        val v = vectors["mlkem768"]!!.jsonObject["keygen"]!!.jsonObject
        val secret = v["secret"]!!.jsonPrimitive.content
        val token = v["token"]!!.jsonPrimitive.content
        val position = v["position"]!!.jsonPrimitive.content
        val expectedPubkey = v["expectedPubkey"]!!.jsonPrimitive.content

        val wallet = Wallet(secret = secret, token = token, position = position, mlkemParameterSet = 768)
        assertEquals(expectedPubkey, wallet.pubkey, "ML-KEM768 keygen pubkey mismatch")
    }

    // Encapsulation is non-deterministic, but decapsulation + AES-256-GCM decrypt is deterministic →
    // one frozen {cipherText, encryptedMessage} sample must decrypt to the canonical plaintext.
    @Test
    @Tag("mlkem")
    @DisplayName("ML-KEM768 decrypt")
    fun mlkem768Decrypt() {
        val v = vectors["mlkem768"]!!.jsonObject["decrypt"]!!.jsonObject
        val secret = v["secret"]!!.jsonPrimitive.content
        val token = v["token"]!!.jsonPrimitive.content
        val position = v["position"]!!.jsonPrimitive.content
        val cipherText = v["cipherText"]!!.jsonPrimitive.content
        val encryptedMessage = v["encryptedMessage"]!!.jsonPrimitive.content
        val expectedPlaintext = v["expectedPlaintext"]!!.jsonPrimitive.content

        val wallet = Wallet(secret = secret, token = token, position = position, mlkemParameterSet = 768)
        val plaintext = wallet.decryptMessage(
            mapOf("cipherText" to cipherText, "encryptedMessage" to encryptedMessage)
        )
        assertEquals(expectedPlaintext, plaintext, "ML-KEM768 decrypt plaintext mismatch")
    }
    @Test
    @Tag("mlkem")
    @DisplayName("ML-KEM1024 keygen")
    fun mlkem1024Keygen() {
        val v = vectors["mlkem1024"]!!.jsonObject["keygen"]!!.jsonObject
        val secret = v["secret"]!!.jsonPrimitive.content
        val token = v["token"]!!.jsonPrimitive.content
        val position = v["position"]!!.jsonPrimitive.content
        val expectedPubkey = v["expectedPubkey"]!!.jsonPrimitive.content

        val wallet = Wallet(secret = secret, token = token, position = position)
        assertEquals(expectedPubkey, wallet.pubkey, "ML-KEM1024 keygen pubkey mismatch")
    }

    @Test
    @Tag("mlkem")
    @DisplayName("ML-KEM1024 decrypt")
    fun mlkem1024Decrypt() {
        val v = vectors["mlkem1024"]!!.jsonObject["decrypt"]!!.jsonObject
        val secret = v["secret"]!!.jsonPrimitive.content
        val token = v["token"]!!.jsonPrimitive.content
        val position = v["position"]!!.jsonPrimitive.content
        val cipherText = v["cipherText"]!!.jsonPrimitive.content
        val encryptedMessage = v["encryptedMessage"]!!.jsonPrimitive.content
        val expectedPlaintext = v["expectedPlaintext"]!!.jsonPrimitive.content

        val wallet = Wallet(secret = secret, token = token, position = position)
        val plaintext = wallet.decryptMessage(
            mapOf("cipherText" to cipherText, "encryptedMessage" to encryptedMessage)
        )
        assertEquals(expectedPlaintext, plaintext, "ML-KEM1024 decrypt plaintext mismatch")
    }

    // Classical NaCl (X25519 scalarmult_base + crypto_box/secretbox) — byte-frozen
    // against the reference tweetnacl. Guards the BouncyCastle NaClBox reimplementation
    // that replaced the JitPack tweetnacl-java dependency.
    @Test
    @DisplayName("Classical NaCl (X25519 + crypto_box) vectors")
    fun naclVectors() {
        val v = vectors["nacl"]!!.jsonObject
        fun b64d(s: String): ByteArray = Base64.getDecoder().decode(s)
        fun b64e(b: ByteArray): String = Base64.getEncoder().encodeToString(b)
        fun hexd(s: String): ByteArray = ByteArray(s.length / 2) {
            ((s[it * 2].digitToInt(16) shl 4) or s[it * 2 + 1].digitToInt(16)).toByte()
        }

        // X25519 scalarmult_base: private → public, byte-frozen (both NaClBox + the public Soda path).
        v["scalarMultBase"]!!.jsonArray.forEach { el ->
            val o = el.jsonObject
            val sk = hexd(o["secretKeyHex"]!!.jsonPrimitive.content)
            val expected = o["expectedPublicKey"]!!.jsonPrimitive.content
            assertEquals(expected, b64e(NaClBox.scalarMultBase(sk)), "NaClBox.scalarMultBase mismatch")
            assertEquals(expected, Soda("BASE64").generatePublicKey(b64e(sk)), "Soda.generatePublicKey mismatch")
        }

        // crypto_box: deterministic ciphertext (encrypt) + open (decrypt), byte-frozen.
        val cb = v["cryptoBox"]!!.jsonObject
        val senderSk = hexd(cb["senderSecretKeyHex"]!!.jsonPrimitive.content)
        val recipSk = hexd(cb["recipientSecretKeyHex"]!!.jsonPrimitive.content)
        val recipPk = b64d(cb["recipientPublicKey"]!!.jsonPrimitive.content)
        val nonce = b64d(cb["nonce"]!!.jsonPrimitive.content)
        val plaintext = cb["plaintext"]!!.jsonPrimitive.content
        val expectedBox = cb["expectedBox"]!!.jsonPrimitive.content
        assertEquals(expectedBox, b64e(NaClBox.box(plaintext.toByteArray(), nonce, recipPk, senderSk)), "crypto_box ciphertext mismatch")
        val opened = NaClBox.boxOpen(b64d(expectedBox), nonce, NaClBox.scalarMultBase(senderSk), recipSk)
        assertNotNull(opened, "crypto_box open returned null")
        assertEquals(plaintext, String(opened!!), "crypto_box open mismatch")

        // sealed_box: open a frozen sealed blob (cross-impl decrypt oracle).
        val sb = v["sealedBox"]!!.jsonObject
        val sealed = b64d(sb["sealed"]!!.jsonPrimitive.content)
        val sealRecipPk = b64d(sb["recipientPublicKey"]!!.jsonPrimitive.content)
        val sealRecipSk = hexd(sb["recipientSecretKeyHex"]!!.jsonPrimitive.content)
        val expectedPlain = sb["expectedPlaintext"]!!.jsonPrimitive.content
        assertEquals(expectedPlain, String(sealed.sealOpen(sealRecipPk, sealRecipSk)), "sealed-box open mismatch")
    }

    /**
     * secret_storage_envelope: decrypt an envelope produced by ANOTHER SDK.
     *
     * This must decrypt the frozen payload rather than round-trip our own output.
     * A round-trip passes in every SDK regardless of metadata casing, which is
     * precisely why Rust 0.9.5 shipped snake_case keys and rejected a TypeScript
     * envelope with "missing field bundle_hash" before any crypto ran.
     */
    @Test
    @DisplayName("secret_storage_envelope: decrypts an envelope written by a peer SDK")
    fun secretStorageEnvelopeVectors() {
        val v = vectors["secret_storage_envelope"]!!.jsonObject
        for (t in v["tests"]!!.jsonArray) {
            val test = t.jsonObject
            val payload = test["payload"]!!.jsonObject
            val bundleHash = test["bundleHash"]!!.jsonPrimitive.content

            val backend = wishKnish.knishIO.client.storage.MemoryStorageBackend()
            backend.setItem(test["storageKey"]!!.jsonPrimitive.content, payload.toString())
            val provider = wishKnish.knishIO.client.storage.AesGcmSecretStorageProvider(backend)

            assertEquals(
                test["expectedPlaintext"]!!.jsonPrimitive.content,
                provider.retrieveSecret(
                    bundleHash,
                    wishKnish.knishIO.client.storage.StorageOptions(
                        passphrase = test["passphrase"]!!.jsonPrimitive.content
                    )
                ),
                "failed to decrypt envelope from ${test["producedBy"]!!.jsonPrimitive.content}"
            )

            // The metadata key set is the half of the format that diverged, so assert what
            // KOTLIN EMITS - not the fixture's own keys, which would only restate the vector.
            //
            // Required-keys + forbidden-keys: optional metadata keys (like `label`) are
            // OMITTED when unset (frozen 2026-09-11: explicitNulls = false in SecretEnvelope).
            // A consumer checking 'label' in metadata reads the same answer across all SDKs.
            val ourBackend = wishKnish.knishIO.client.storage.MemoryStorageBackend()
            wishKnish.knishIO.client.storage.AesGcmSecretStorageProvider(ourBackend).storeSecret(
                bundleHash,
                test["expectedPlaintext"]!!.jsonPrimitive.content,
                wishKnish.knishIO.client.storage.StorageOptions(
                    passphrase = test["passphrase"]!!.jsonPrimitive.content
                )
            )
            val emittedMetadata = json
                .parseToJsonElement(ourBackend.getItem(test["storageKey"]!!.jsonPrimitive.content)!!)
                .jsonObject["metadata"]!!.jsonObject
            val emitted = emittedMetadata.keys

            for (key in test["requiredMetadataKeys"]!!.jsonArray.map { it.jsonPrimitive.content }) {
                assertTrue(emitted.contains(key), "Kotlin must emit `$key`; emitted $emitted")
            }
            for (key in test["forbiddenMetadataKeys"]!!.jsonArray.map { it.jsonPrimitive.content }) {
                assertFalse(
                    emitted.contains(key),
                    "`$key` is the 0.9.5 snake_case divergence and must never be emitted"
                )
            }
            val convention = test["optionalKeyConvention"]?.jsonPrimitive?.content
            if (convention == "omit-when-absent") {
                for (key in test["optionalMetadataKeys"]!!.jsonArray.map { it.jsonPrimitive.content }) {
                    assertFalse(
                        emittedMetadata.containsKey(key),
                        "Kotlin must omit unset optional key `$key` under convention $convention"
                    )
                }
            }
            assertEquals(false, emittedMetadata["hardwareBacked"]!!.jsonPrimitive.boolean, "a software provider must never emit hardwareBacked=true")
            assertEquals("aes-gcm", emittedMetadata["providerType"]!!.jsonPrimitive.content)
        }
    }

    // =====================================================================================
    // Backwards compatibility: a build whose default parameter set is ML-KEM-1024 must still
    // read records a pre-bump (ML-KEM-768-only) peer produced for it.
    // =====================================================================================

    private val mlkem768Decrypt: JsonObject
        get() = vectors["mlkem768"]!!.jsonObject["decrypt"]!!.jsonObject

    private fun JsonObject.str(key: String): String = this[key]!!.jsonPrimitive.content

    /** (a) The whole point of dual-identity inbound decryption: no second wallet, no step-back. */
    @Test
    @Tag("mlkem")
    @DisplayName("A default (1024) wallet decrypts a frozen 768 envelope addressed to its own 768 identity")
    fun default1024WalletDecryptsFrozen768Envelope() {
        val v = mlkem768Decrypt
        val wallet = Wallet(
            secret = v.str("secret"),
            token = v.str("token"),
            position = v.str("position")
        )
        assertEquals(1024, wallet.mlkemParameterSet, "the wallet under test must be at the default")

        val plaintext = wallet.decryptMessage(
            mapOf(
                "cipherText" to v.str("cipherText"),
                "encryptedMessage" to v.str("encryptedMessage")
            )
        )
        assertEquals(v.str("expectedPlaintext"), plaintext, "1024 default must read the pre-bump 768 envelope")
    }

    /**
     * (b) Permissive inbound must NOT move what the wallet advertises — that value goes into
     * signed molecule meta (`walletPubkey`) and into auth, so moving it would change hashed bytes.
     */
    @Test
    @DisplayName("The advertised public key is still ML-KEM-1024 (1568 raw bytes)")
    fun advertisedPubkeyStays1024() {
        val v = mlkem768Decrypt
        val wallet = Wallet(
            secret = v.str("secret"),
            token = v.str("token"),
            position = v.str("position")
        )
        assertEquals(1568, Base64.getDecoder().decode(wallet.pubkey!!).size)
    }

    /** (d) A ciphertext at neither parameter set must still fail on the existing observable. */
    @Test
    @DisplayName("A ciphertext matching neither parameter set still returns null")
    fun ciphertextAtNeitherParameterSetReturnsNull() {
        val v = mlkem768Decrypt
        val wallet = Wallet(
            secret = v.str("secret"),
            token = v.str("token"),
            position = v.str("position")
        )
        val malformed = Base64.getEncoder().encodeToString(ByteArray(64))
        assertNull(
            wallet.decryptMessage(
                mapOf("cipherText" to malformed, "encryptedMessage" to v.str("encryptedMessage"))
            )
        )
    }

    /**
     * (e) The transport is map-addressed by `hashShare(recipientPubkey)`. A pre-bump sender
     * addressed its envelope to our 768 hash share, so without trying both shares the length
     * dispatch exercised by (a) is never even reached on the wire path.
     */
    @Test
    @Tag("mlkem")
    @DisplayName("The CipherHash map path finds an envelope addressed to the 768 hash share")
    fun cipherHashMapPathFinds768AddressedEnvelope() {
        val v = mlkem768Decrypt
        val wallet = Wallet(
            secret = v.str("secret"),
            token = v.str("token"),
            position = v.str("position")
        )
        val wallet768 = Wallet(
            secret = v.str("secret"),
            token = v.str("token"),
            position = v.str("position"),
            mlkemParameterSet = 768
        )
        val map = mapOf(
            Crypto.hashShare(wallet768.pubkey!!, "BASE64") to mapOf(
                "cipherText" to v.str("cipherText"),
                "encryptedMessage" to v.str("encryptedMessage")
            )
        )

        // decryptMyMessageML returns the RAW decrypted text (the transport parser consumes it),
        // and the frozen payload is a JSON-encoded string.
        val raw = wallet.decryptMyMessageML(map)
        assertNotNull(raw, "the 768-addressed entry must be found")
        assertEquals(v.str("expectedPlaintext"), com.google.gson.Gson().fromJson(raw, String::class.java))
    }

    // =====================================================================================
    // Session snapshots keep their ML-KEM parameter set across a restore.
    // =====================================================================================

    private fun authTokenFor(wallet: Wallet): AuthToken = AuthToken.create(
        AccessToken("T", 9999999, wallet.pubkey!!, wallet.pubkey!!, true, 9999999),
        wallet,
        true
    )

    @Test
    @DisplayName("A stepped-back 768 session survives a snapshot round trip")
    fun steppedBack768SessionSurvivesSnapshotRoundTrip() {
        val v = mlkem768Decrypt
        val wallet = Wallet(
            secret = v.str("secret"),
            token = "AUTH",
            position = v.str("position"),
            mlkemParameterSet = 768
        )
        val snapshot = authTokenFor(wallet).getSnapshot()

        val restored = AuthToken.restore(snapshot, v.str("secret")).getWallet()!!
        assertEquals(wallet.pubkey, restored.pubkey)
    }

    /**
     * The shape an 0.9.x build persisted: no parameter-set field anywhere, and `pubkey` is the
     * validator's 768 key because every pre-bump session was 768. Falling back to the
     * constructor default (now 1024) is precisely the defect.
     */
    @Test
    @DisplayName("A legacy snapshot with no parameter set restores as 768, not the 1024 default")
    fun legacySnapshotRestoresAs768() {
        val v = mlkem768Decrypt
        val wallet768 = Wallet(
            secret = v.str("secret"),
            token = "AUTH",
            position = v.str("position"),
            mlkemParameterSet = 768
        )
        val legacySnapshot = authTokenFor(wallet768).getSnapshot().apply {
            // Strip the field an 0.9.x snapshot never carried.
            wallet = AuthToken.Wallet(wallet768.position, wallet768.characters)
        }

        val restored = AuthToken.restore(legacySnapshot, v.str("secret")).getWallet()!!
        assertEquals(wallet768.pubkey, restored.pubkey)
        assertEquals(1184, Base64.getDecoder().decode(restored.pubkey!!).size)
        assertNotEquals(1568, Base64.getDecoder().decode(restored.pubkey!!).size)
    }

    // =====================================================================================
    // A frozen pre-bump ML-KEM-768 auth molecule validates from a 1024 default build.
    // =====================================================================================

    private val legacyMolecule: JsonObject
        get() = vectors["legacyMlkem768AuthMolecule"]!!.jsonObject

    /**
     * Fails loudly if the fixture is ever regenerated at the 1024 default — at which point it
     * would no longer be evidence about pre-bump records at all.
     */
    @Test
    @DisplayName("legacyMlkem768AuthMolecule: the U-atom walletPubkey meta really is an ML-KEM-768 key")
    fun legacyMoleculeCarriesA768WalletPubkey() {
        val legacy = legacyMolecule
        val walletPubkeys = legacy["molecule"]!!.jsonObject["atoms"]!!.jsonArray
            .flatMap { it.jsonObject["meta"]?.jsonArray ?: JsonArray(emptyList()) }
            .filter { it.jsonObject["key"]!!.jsonPrimitive.content == "walletPubkey" }
            .map { it.jsonObject["value"]!!.jsonPrimitive.content }

        assertEquals(1, walletPubkeys.size)
        assertEquals(
            legacy["expectedWalletPubkeyBytes"]!!.jsonPrimitive.int,
            Base64.getDecoder().decode(walletPubkeys[0]).size
        )
    }

    @Test
    @DisplayName("legacyMlkem768AuthMolecule: its molecular hash still verifies")
    fun legacyMoleculeHashVerifies() {
        val legacy = legacyMolecule
        val atoms = legacy["atoms"]!!.jsonArray.map { Atom.fromJSON(it.toString()) }
        assertEquals(
            legacy["expectedMolecularHash"]!!.jsonPrimitive.content,
            Atom.hashAtoms(atoms)
        )
    }

    /**
     * Reconstructs a molecule from server-data JSON the way the cross-SDK self-test does
     * (`CrossSdkMoleculeDeserializer` → verbatim field copy, never a "create atom" constructor
     * that would regenerate `createdAt`), plus the signed molecule's own source wallet as the
     * validation context. `Molecule.fromJSON` cannot be used here: it builds the instance with
     * a default `Wallet()` and no secret, which the `Molecule` initialiser rejects outright
     * ("SourceWallet parameter not initialized by valid wallet").
     */
    private fun reconstructMolecule(data: JsonObject): Molecule {
        val sw = data["sourceWallet"]!!.jsonObject
        val sourceWallet = Wallet(
            secret = null,
            token = sw.str("token"),
            position = sw.str("position"),
            characters = sw.str("characters")
        ).apply {
            address = sw.str("address")
            bundle = sw.str("bundle")
            pubkey = sw.str("pubkey")
        }

        return Molecule(
            secret = null,
            sourceWallet = sourceWallet,
            cellSlug = data["cellSlug"]!!.jsonPrimitive.content
        ).apply {
            molecularHash = data.str("molecularHash")
            bundle = data.str("bundle")
            createdAt = data.str("createdAt")
            atoms.clear()
            data["atoms"]!!.jsonArray.forEach { atoms.add(Atom.fromJSON(it.toString())) }
        }
    }

    @Test
    @DisplayName("legacyMlkem768AuthMolecule: full check() — hash plus WOTS+ signature — passes")
    fun legacyMoleculeFullCheckPasses() {
        val legacy = legacyMolecule
        val molecule = reconstructMolecule(legacy["molecule"]!!.jsonObject)
        assertEquals(legacy["expectedMolecularHash"]!!.jsonPrimitive.content, molecule.molecularHash)
        assertTrue(molecule.check(molecule.sourceWallet), "pre-bump 768 molecule must validate")
    }
}
