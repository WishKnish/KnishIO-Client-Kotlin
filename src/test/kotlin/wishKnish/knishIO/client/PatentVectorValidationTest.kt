/*
 * Patent Vector Validation Tests
 *
 * Validates Kotlin SDK cryptographic operations against canonical patent
 * test vectors (Appendix B). These vectors provide reduction-to-practice
 * evidence for patent claims covering:
 *   - Claims 1-2: O(k) WOTS+ signature scheme
 *   - Claims 4-5: Base17 enumeration, ContinuID identity relay
 *   - Claim 8: Multi-isotope molecule composition
 *   - Claims 12-14: ContinuID chain relay
 *   - Claim 21: Multi-isotope dispatch
 *
 * Vector source: canonical-patent-vectors.json (Rust reference implementation)
 */

package wishKnish.knishIO.client

import kotlinx.serialization.json.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import wishKnish.knishIO.client.libraries.CheckMolecule
import wishKnish.knishIO.client.libraries.Crypto
import wishKnish.knishIO.client.libraries.Shake256
import wishKnish.knishIO.client.libraries.Strings

@DisplayName("Patent Vector Validation (Canonical Appendix B)")
class PatentVectorValidationTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val vectors: JsonObject by lazy {
        val stream = javaClass.classLoader.getResourceAsStream("canonical-patent-vectors.json")
            ?: throw IllegalStateException("canonical-patent-vectors.json not found in test resources")
        val content = stream.bufferedReader().use { it.readText() }
        json.parseToJsonElement(content).jsonObject["vectors"]!!.jsonObject
    }

    // =========================================================================
    // 0. generateSecret cross-SDK parity (Batch AO) — seed -> 2048 hex secret
    // =========================================================================

    @Nested
    @DisplayName("generateSecret — cross-SDK parity (Batch AO)")
    inner class GenerateSecret {

        private val secretTests by lazy {
            vectors["generate_secret"]!!.jsonObject["tests"]!!.jsonArray
        }

        @Test
        @DisplayName("Secret matches canonical 2048-hex vector")
        fun secretMatchesVector() {
            val test = secretTests[0].jsonObject
            val seed = test["seed"]!!.jsonPrimitive.content
            val expected = test["expectedSecret"]!!.jsonPrimitive.content

            val secret = Crypto.generateSecret(seed)
            assertEquals(expected, secret,
                "generateSecret must match the canonical cross-SDK vector")
        }
    }

    // =========================================================================
    // 0b. Atom value formatting cross-SDK parity (Batch AQ)
    //     A V/B/F numeric value must serialize as an integer string (the
    //     validator parses it as i128); whole-number floats carry NO ".0".
    // =========================================================================

    @Nested
    @DisplayName("Atom value formatting — cross-SDK parity (Batch AQ)")
    inner class AtomValueFormat {

        private val valueTests by lazy {
            vectors["atom_value_format"]!!.jsonObject["tests"]!!.jsonArray
        }

        @Test
        @DisplayName("Whole-number value serializes as an integer string (no trailing .0)")
        fun valueFormatMatchesVector() {
            valueTests.forEach { element ->
                val test = element.jsonObject
                val name = test["name"]!!.jsonPrimitive.content
                // Exercise the bug path: format from a Double.
                val value = test["value"]!!.jsonPrimitive.double
                val expected = test["expected"]!!.jsonPrimitive.content
                assertEquals(expected, Molecule.formatAtomValue(value),
                    "atom value format mismatch (cross-SDK parity) for $name")
            }
        }
    }

    // =========================================================================
    // 0c. Buffer-deposit conservation cross-SDK parity (Batch BF)
    //     init_deposit_buffer debits the FULL source balance so a PARTIAL deposit
    //     still conserves: source V -balance + buffer B +amount + remainder V
    //     +(balance-amount) = 0. Kotlin is the 5th SDK to join this lock
    //     (JS/PHP/Rust/Python already did — Kotlin previously lacked initDepositBuffer).
    // =========================================================================

    @Nested
    @DisplayName("Buffer-deposit conservation — cross-SDK parity (Batch BF)")
    inner class BufferDepositConservation {

        private val bufferTests by lazy {
            vectors["buffer_deposit_conservation"]!!.jsonObject["tests"]!!.jsonArray
        }

        @Test
        @DisplayName("initDepositBuffer conserves (V+B sum 0; full-balance debit)")
        fun bufferDepositConserves() {
            // The vector pins no secret — the assertion is value-only; any valid secret works.
            val secret = Crypto.generateSecret("BUFFER_DEPOSIT_TESTSEED")
            bufferTests.forEach { element ->
                val test = element.jsonObject
                val name = test["name"]!!.jsonPrimitive.content
                val sourceBalance = test["sourceBalance"]!!.jsonPrimitive.double
                val amount = test["amount"]!!.jsonPrimitive.int
                val expectedSourceValue = test["expectedSourceValue"]!!.jsonPrimitive.content
                val expectedBufferValue = test["expectedBufferValue"]!!.jsonPrimitive.content
                val expectedRemainderValue = test["expectedRemainderValue"]!!.jsonPrimitive.content
                val expectedSum = test["expectedSum"]!!.jsonPrimitive.content

                val source = Wallet.create(secret, "BUFTOK")
                source.balance = sourceBalance
                val molecule = Molecule(
                    secret = secret,
                    sourceWallet = source,
                    remainderWallet = Wallet.create(secret, "BUFTOK"),
                    cellSlug = "buftest"
                )
                molecule.initDepositBuffer(amount)

                var sum = 0L
                val vValues = mutableListOf<String>()
                var bValue: String? = null
                molecule.atoms.forEach { atom ->
                    if (atom.isotope == 'V' || atom.isotope == 'B') {
                        sum += (atom.value ?: "0").toLong()
                        if (atom.isotope == 'V') vValues.add(atom.value ?: "") else bValue = atom.value
                    }
                }

                // Emit order: source V (full-balance debit), buffer B (+amount), remainder V (+change).
                assertEquals(expectedSum, sum.toString(), "V+B conservation sum for $name")
                assertEquals(expectedSourceValue, vValues[0], "source V (full-balance debit) for $name")
                assertEquals(expectedBufferValue, bValue, "buffer B for $name")
                assertEquals(expectedRemainderValue, vValues[1], "remainder V for $name")
            }
        }
    }

    // =========================================================================
    // 0b. Buffer-withdraw conservation — cross-SDK lock (cycle 149)
    //     initWithdrawBuffer debits the FULL source balance so a partial
    //     withdraw still conserves (B+V sum 0). The withdraw analog of the
    //     deposit lock above; emits TWO B atoms (source, remainder) + ONE V
    //     atom (recipient). Kotlin was already correct — this regression-locks it.
    // =========================================================================

    @Nested
    @DisplayName("Buffer-withdraw conservation — cross-SDK parity (cycle 149)")
    inner class BufferWithdrawConservation {

        private val bufferTests by lazy {
            vectors["buffer_withdraw_conservation"]!!.jsonObject["tests"]!!.jsonArray
        }

        @Test
        @DisplayName("initWithdrawBuffer conserves (B+V sum 0; full-balance debit)")
        fun bufferWithdrawConserves() {
            // The vector pins no secret — the assertion is value-only; any valid secret works.
            val secret = Crypto.generateSecret("BUFFER_WITHDRAW_TESTSEED")
            bufferTests.forEach { element ->
                val test = element.jsonObject
                val name = test["name"]!!.jsonPrimitive.content
                val sourceBalance = test["sourceBalance"]!!.jsonPrimitive.double
                val amount = test["amount"]!!.jsonPrimitive.int
                val expectedSourceValue = test["expectedSourceValue"]!!.jsonPrimitive.content
                val expectedRecipientValue = test["expectedRecipientValue"]!!.jsonPrimitive.content
                val expectedRemainderValue = test["expectedRemainderValue"]!!.jsonPrimitive.content
                val expectedSum = test["expectedSum"]!!.jsonPrimitive.content

                val source = Wallet.create(secret, "BUFTOK")
                source.balance = sourceBalance
                val molecule = Molecule(
                    secret = secret,
                    sourceWallet = source,
                    remainderWallet = Wallet.create(secret, "BUFTOK"),
                    cellSlug = "buftest"
                )
                // Withdraw to the caller's own bundle (single recipient), mirroring the client wrapper.
                molecule.initWithdrawBuffer(mapOf(source.bundle !! to amount))

                var sum = 0L
                val bValues = mutableListOf<String>()
                var vValue: String? = null
                molecule.atoms.forEach { atom ->
                    if (atom.isotope == 'V' || atom.isotope == 'B') {
                        sum += (atom.value ?: "0").toLong()
                        if (atom.isotope == 'B') bValues.add(atom.value ?: "") else vValue = atom.value
                    }
                }

                // Emit order: source B (full-balance debit), recipient V (+amount), remainder B (+change).
                assertEquals(expectedSum, sum.toString(), "B+V conservation sum for $name")
                assertEquals(expectedSourceValue, bValues[0], "source B (full-balance debit) for $name")
                assertEquals(expectedRecipientValue, vValue, "recipient V for $name")
                assertEquals(expectedRemainderValue, bValues[1], "remainder B for $name")
            }
        }
    }

    // =========================================================================
    // 1. ContinuID Chain Relay (Patent Claims 5, 12-14)
    // =========================================================================

    @Nested
    @DisplayName("ContinuID Chain Relay — Claims 5, 12-14")
    inner class ContinuIdChainRelay {

        private val continuidTests by lazy {
            vectors["continuid_chain"]!!.jsonObject["tests"]!!.jsonArray
        }

        @Test
        @DisplayName("Bundle hash matches canonical vector")
        fun bundleHashMatchesVector() {
            val test = continuidTests[0].jsonObject
            val secret = test["secret"]!!.jsonPrimitive.content
            val expectedBundle = test["expectedBundle"]!!.jsonPrimitive.content

            val bundle = Crypto.generateBundleHash(secret)
            assertEquals(expectedBundle, bundle,
                "Bundle hash must match canonical vector for ContinuID relay")
        }

        @Test
        @DisplayName("Wallet address at position1 matches canonical vector")
        fun walletAddressAtPosition1() {
            val test = continuidTests[0].jsonObject
            val secret = test["secret"]!!.jsonPrimitive.content
            val position1 = test["position1"]!!.jsonPrimitive.content
            val expectedAddress1 = test["expectedAddress1"]!!.jsonPrimitive.content

            val wallet = Wallet(secret = secret, token = "USER", position = position1)
            assertEquals(expectedAddress1, wallet.address,
                "Wallet address at position1 must match canonical vector")
        }

        @Test
        @DisplayName("Position2 derived from SHAKE256(position1, 256) matches canonical vector")
        fun position2Derivation() {
            val test = continuidTests[0].jsonObject
            val position1 = test["position1"]!!.jsonPrimitive.content
            val expectedPosition2 = test["expectedPosition2"]!!.jsonPrimitive.content

            // Position2 is SHAKE256(position1, 256 bits = 32 bytes)
            val derivedPosition2 = Shake256.hash(position1, 32)
            assertEquals(expectedPosition2, derivedPosition2,
                "Position2 must be SHAKE256(position1, 32 bytes) per ContinuID relay spec")
        }

        @Test
        @DisplayName("Wallet address at position2 matches canonical vector")
        fun walletAddressAtPosition2() {
            val test = continuidTests[0].jsonObject
            val secret = test["secret"]!!.jsonPrimitive.content
            val expectedPosition2 = test["expectedPosition2"]!!.jsonPrimitive.content
            val expectedAddress2 = test["expectedAddress2"]!!.jsonPrimitive.content

            val wallet = Wallet(secret = secret, token = "USER", position = expectedPosition2)
            assertEquals(expectedAddress2, wallet.address,
                "Wallet address at position2 must match canonical vector")
        }

        @Test
        @DisplayName("ContinuID invariants: same bundle, different positions, different addresses")
        fun continuIdInvariants() {
            val test = continuidTests[0].jsonObject
            val secret = test["secret"]!!.jsonPrimitive.content
            val position1 = test["position1"]!!.jsonPrimitive.content
            val expectedPosition2 = test["expectedPosition2"]!!.jsonPrimitive.content

            val wallet1 = Wallet(secret = secret, token = "USER", position = position1)
            val wallet2 = Wallet(secret = secret, token = "USER", position = expectedPosition2)

            // Same bundle (derived from same secret)
            assertEquals(wallet1.bundle, wallet2.bundle,
                "ContinuID invariant: both wallets must share the same bundle hash")

            // Different positions
            assertNotEquals(wallet1.position, wallet2.position,
                "ContinuID invariant: sequential positions must differ")

            // Different addresses
            assertNotEquals(wallet1.address, wallet2.address,
                "ContinuID invariant: different positions must produce different addresses")
        }
    }

    // =========================================================================
    // 2. Base17 Enumeration (Patent Claim 5)
    // =========================================================================

    @Nested
    @DisplayName("Base17 Enumeration — Claim 5")
    inner class Base17Enumeration {

        private val base17Tests by lazy {
            vectors["base17_enumeration"]!!.jsonObject["tests"]!!.jsonArray
        }

        @Test
        @DisplayName("Zero hash converts to all-zero base17")
        fun zeroHash() {
            val test = base17Tests.first { it.jsonObject["name"]!!.jsonPrimitive.content == "zero_hash" }.jsonObject
            val hexInput = test["hexInput"]!!.jsonPrimitive.content
            val expectedBase17 = test["expectedBase17"]!!.jsonPrimitive.content

            val base17 = hexToBase17Padded(hexInput)
            assertEquals(expectedBase17, base17,
                "Zero hex must convert to all-zero base17 string")
        }

        @Test
        @DisplayName("Ones hash converts correctly")
        fun onesHash() {
            val test = base17Tests.first { it.jsonObject["name"]!!.jsonPrimitive.content == "ones_hash" }.jsonObject
            val hexInput = test["hexInput"]!!.jsonPrimitive.content
            val expectedBase17 = test["expectedBase17"]!!.jsonPrimitive.content

            val base17 = hexToBase17Padded(hexInput)
            assertEquals(expectedBase17, base17,
                "Ones hex hash must match canonical base17 output")
        }

        @Test
        @DisplayName("Typical hash converts correctly")
        fun typicalHash() {
            val test = base17Tests.first { it.jsonObject["name"]!!.jsonPrimitive.content == "typical_hash" }.jsonObject
            val hexInput = test["hexInput"]!!.jsonPrimitive.content
            val expectedBase17 = test["expectedBase17"]!!.jsonPrimitive.content

            val base17 = hexToBase17Padded(hexInput)
            assertEquals(expectedBase17, base17,
                "Typical hex hash must match canonical base17 output")
        }

        @Test
        @DisplayName("Max hex (all f's) converts correctly")
        fun maxHex() {
            val test = base17Tests.first { it.jsonObject["name"]!!.jsonPrimitive.content == "max_hex" }.jsonObject
            val hexInput = test["hexInput"]!!.jsonPrimitive.content
            val expectedBase17 = test["expectedBase17"]!!.jsonPrimitive.content

            val base17 = hexToBase17Padded(hexInput)
            assertEquals(expectedBase17, base17,
                "Max hex (all f's) must match canonical base17 output")
        }

        @Test
        @DisplayName("Alternating pattern converts correctly")
        fun alternatingPattern() {
            val test = base17Tests.first { it.jsonObject["name"]!!.jsonPrimitive.content == "alternating" }.jsonObject
            val hexInput = test["hexInput"]!!.jsonPrimitive.content
            val expectedBase17 = test["expectedBase17"]!!.jsonPrimitive.content

            val base17 = hexToBase17Padded(hexInput)
            assertEquals(expectedBase17, base17,
                "Alternating hex pattern must match canonical base17 output")
        }

        @Test
        @DisplayName("All base17 vectors have normalizedSum == 0")
        fun normalizedSumIsZero() {
            for (element in base17Tests) {
                val test = element.jsonObject
                val name = test["name"]!!.jsonPrimitive.content
                val hexInput = test["hexInput"]!!.jsonPrimitive.content
                val expectedNormalizedSum = test["normalizedSum"]!!.jsonPrimitive.int

                val base17 = hexToBase17Padded(hexInput)
                val normalizedHash = CheckMolecule.normalizedHash(base17)
                val actualSum = normalizedHash.values.sum()

                assertEquals(expectedNormalizedSum, actualSum,
                    "Normalized sum must be $expectedNormalizedSum for vector '$name'")
            }
        }
    }

    // =========================================================================
    // 3. Multi-Isotope Molecule (Patent Claims 8, 21)
    // =========================================================================

    @Nested
    @DisplayName("Multi-Isotope Molecule — Claims 8, 21")
    inner class MultiIsotopeMolecule {

        private val multiIsotopeTests by lazy {
            vectors["multi_isotope_molecule"]!!.jsonObject["tests"]!!.jsonArray
        }

        @Test
        @DisplayName("Bundle hash matches for multi-isotope secret")
        fun bundleHashMatches() {
            val test = multiIsotopeTests[0].jsonObject
            val secret = test["secret"]!!.jsonPrimitive.content
            val expectedBundle = test["expectedBundle"]!!.jsonPrimitive.content

            val bundle = Crypto.generateBundleHash(secret)
            assertEquals(expectedBundle, bundle,
                "Bundle hash must match canonical vector for multi-isotope test")
        }

        @Test
        @DisplayName("V-isotope position and address match canonical vector")
        fun vIsotopePositionAndAddress() {
            val test = multiIsotopeTests[0].jsonObject
            val secret = test["secret"]!!.jsonPrimitive.content
            val sourcePosition = test["invariants"]!!.jsonObject["source_position"]!!.jsonPrimitive.content
            val isotopes = test["isotopes"]!!.jsonObject
            val vData = isotopes["V"]!!.jsonObject
            val expectedPosition = vData["expectedPosition"]!!.jsonPrimitive.content
            val expectedAddress = vData["expectedAddress"]!!.jsonPrimitive.content
            val token = vData["token"]!!.jsonPrimitive.content

            // V-isotope position: SHAKE256(sourcePosition + "V", 32 bytes)
            val derivedPosition = Shake256.hash(sourcePosition + "V", 32)
            assertEquals(expectedPosition, derivedPosition,
                "V-isotope position must be SHAKE256(sourcePosition + 'V', 32)")

            // V-isotope wallet address at derived position
            val wallet = Wallet(secret = secret, token = token, position = derivedPosition)
            assertEquals(expectedAddress, wallet.address,
                "V-isotope wallet address must match canonical vector")
        }

        @Test
        @DisplayName("M-isotope position and address match canonical vector")
        fun mIsotopePositionAndAddress() {
            val test = multiIsotopeTests[0].jsonObject
            val secret = test["secret"]!!.jsonPrimitive.content
            val sourcePosition = test["invariants"]!!.jsonObject["source_position"]!!.jsonPrimitive.content
            val isotopes = test["isotopes"]!!.jsonObject
            val mData = isotopes["M"]!!.jsonObject
            val expectedPosition = mData["expectedPosition"]!!.jsonPrimitive.content
            val expectedAddress = mData["expectedAddress"]!!.jsonPrimitive.content
            val token = mData["token"]!!.jsonPrimitive.content

            // M-isotope position: SHAKE256(sourcePosition + "M", 32 bytes)
            val derivedPosition = Shake256.hash(sourcePosition + "M", 32)
            assertEquals(expectedPosition, derivedPosition,
                "M-isotope position must be SHAKE256(sourcePosition + 'M', 32)")

            // M-isotope wallet address at derived position
            val wallet = Wallet(secret = secret, token = token, position = derivedPosition)
            assertEquals(expectedAddress, wallet.address,
                "M-isotope wallet address must match canonical vector")
        }

        @Test
        @DisplayName("I-isotope position and address match canonical vector")
        fun iIsotopePositionAndAddress() {
            val test = multiIsotopeTests[0].jsonObject
            val secret = test["secret"]!!.jsonPrimitive.content
            val sourcePosition = test["invariants"]!!.jsonObject["source_position"]!!.jsonPrimitive.content
            val isotopes = test["isotopes"]!!.jsonObject
            val iData = isotopes["I"]!!.jsonObject
            val expectedPosition = iData["expectedPosition"]!!.jsonPrimitive.content
            val expectedAddress = iData["expectedAddress"]!!.jsonPrimitive.content
            val token = iData["token"]!!.jsonPrimitive.content

            // I-isotope position: SHAKE256(sourcePosition + "I", 32 bytes)
            val derivedPosition = Shake256.hash(sourcePosition + "I", 32)
            assertEquals(expectedPosition, derivedPosition,
                "I-isotope position must be SHAKE256(sourcePosition + 'I', 32)")

            // I-isotope wallet address at derived position
            val wallet = Wallet(secret = secret, token = token, position = derivedPosition)
            assertEquals(expectedAddress, wallet.address,
                "I-isotope wallet address must match canonical vector")
        }

        @Test
        @DisplayName("All isotope addresses are unique (multi-isotope invariant)")
        fun allAddressesUnique() {
            val test = multiIsotopeTests[0].jsonObject
            val secret = test["secret"]!!.jsonPrimitive.content
            val sourcePosition = test["invariants"]!!.jsonObject["source_position"]!!.jsonPrimitive.content
            val isotopes = test["isotopes"]!!.jsonObject

            val addresses = mutableSetOf<String>()
            for (isotopeKey in listOf("V", "M", "I")) {
                val data = isotopes[isotopeKey]!!.jsonObject
                val position = Shake256.hash(sourcePosition + isotopeKey, 32)
                val token = data["token"]!!.jsonPrimitive.content
                val wallet = Wallet(secret = secret, token = token, position = position)
                addresses.add(wallet.address!!)
            }

            assertEquals(3, addresses.size,
                "All three isotope addresses (V, M, I) must be unique")
        }

        @Test
        @DisplayName("All isotope wallets share the same bundle hash")
        fun sameBundleForAll() {
            val test = multiIsotopeTests[0].jsonObject
            val secret = test["secret"]!!.jsonPrimitive.content
            val sourcePosition = test["invariants"]!!.jsonObject["source_position"]!!.jsonPrimitive.content
            val isotopes = test["isotopes"]!!.jsonObject

            val bundles = mutableSetOf<String>()
            for (isotopeKey in listOf("V", "M", "I")) {
                val data = isotopes[isotopeKey]!!.jsonObject
                val position = Shake256.hash(sourcePosition + isotopeKey, 32)
                val token = data["token"]!!.jsonPrimitive.content
                val wallet = Wallet(secret = secret, token = token, position = position)
                bundles.add(wallet.bundle!!)
            }

            assertEquals(1, bundles.size,
                "All isotope wallets must share the same bundle hash (derived from same secret)")
        }
    }

    // =========================================================================
    // 4. BigInt Carry Edge Cases (Patent Claim 5)
    // =========================================================================

    @Nested
    @DisplayName("BigInt Carry Edge Cases — Claim 5")
    inner class BigIntCarryEdge {

        private val bigintTests by lazy {
            vectors["bigint_carry_edge"]!!.jsonObject["tests"]!!.jsonArray
        }

        @Test
        @DisplayName("65-char hex input produces correct SHAKE256 hash")
        fun hexInput65Chars() {
            val test = bigintTests.first { it.jsonObject["name"]!!.jsonPrimitive.content == "65_char_hex" }.jsonObject
            val input = test["input"]!!.jsonPrimitive.content
            val expectedHash = test["expectedShake256"]!!.jsonPrimitive.content

            val hash = Shake256.hash(input, 32)
            assertEquals(expectedHash, hash,
                "65-char hex input must produce correct SHAKE256 hash")
        }

        @Test
        @DisplayName("All-f 64-char hex input produces correct SHAKE256 hash")
        fun allF64Chars() {
            val test = bigintTests.first { it.jsonObject["name"]!!.jsonPrimitive.content == "all_f_64" }.jsonObject
            val input = test["input"]!!.jsonPrimitive.content
            val expectedHash = test["expectedShake256"]!!.jsonPrimitive.content

            val hash = Shake256.hash(input, 32)
            assertEquals(expectedHash, hash,
                "All-f 64-char hex input must produce correct SHAKE256 hash")
        }

        @Test
        @DisplayName("Boundary value 2^256 produces correct SHAKE256 hash")
        fun boundaryValue2_256() {
            val test = bigintTests.first { it.jsonObject["name"]!!.jsonPrimitive.content == "boundary_value_2_256" }.jsonObject
            val input = test["input"]!!.jsonPrimitive.content
            val expectedHash = test["expectedShake256"]!!.jsonPrimitive.content

            val hash = Shake256.hash(input, 32)
            assertEquals(expectedHash, hash,
                "Boundary value 2^256 must produce correct SHAKE256 hash")
        }

        @Test
        @DisplayName("Single high bit produces correct SHAKE256 hash")
        fun singleHighBit() {
            val test = bigintTests.first { it.jsonObject["name"]!!.jsonPrimitive.content == "single_high_bit" }.jsonObject
            val input = test["input"]!!.jsonPrimitive.content
            val expectedHash = test["expectedShake256"]!!.jsonPrimitive.content

            val hash = Shake256.hash(input, 32)
            assertEquals(expectedHash, hash,
                "Single high bit input must produce correct SHAKE256 hash")
        }

        @Test
        @DisplayName("SHAKE256 hashes convert to correct base17 representations")
        fun base17OfHashes() {
            for (element in bigintTests) {
                val test = element.jsonObject
                val name = test["name"]!!.jsonPrimitive.content
                val input = test["input"]!!.jsonPrimitive.content
                val expectedBase17 = test["expectedBase17OfHash"]!!.jsonPrimitive.content

                val hash = Shake256.hash(input, 32)
                val base17 = hexToBase17Padded(hash)
                assertEquals(expectedBase17, base17,
                    "Base17 of SHAKE256 hash must match canonical vector for '$name'")
            }
        }

        @Test
        @DisplayName("Private key generation produces expected key length (2048 chars)")
        fun expectedKeyLength() {
            for (element in bigintTests) {
                val test = element.jsonObject
                val name = test["name"]!!.jsonPrimitive.content
                val input = test["input"]!!.jsonPrimitive.content
                val expectedKeyLength = test["expectedKeyLength"]!!.jsonPrimitive.int

                // Use the input as both secret and position to test key generation
                val key = Wallet.generatePrivateKey(
                    secret = input,
                    token = "USER",
                    position = input
                )
                assertEquals(expectedKeyLength, key.length,
                    "Private key length must be $expectedKeyLength chars for vector '$name'")
            }
        }
    }

    // =========================================================================
    // 5. WOTS+ Roundtrip (Patent Claims 1-2, 5)
    // =========================================================================

    @Nested
    @DisplayName("WOTS+ Roundtrip — Claims 1-2, 5")
    inner class WotsRoundtrip {

        private val wotsTests by lazy {
            vectors["wots_roundtrip"]!!.jsonObject["tests"]!!.jsonArray
        }

        @Test
        @DisplayName("OTS address matches canonical vector")
        fun otsAddressMatchesVector() {
            val test = wotsTests[0].jsonObject
            val secret = test["secret"]!!.jsonPrimitive.content
            val token = test["token"]!!.jsonPrimitive.content
            val position = test["position"]!!.jsonPrimitive.content
            val expectedOtsAddress = test["expectedOtsAddress"]!!.jsonPrimitive.content

            // Generate wallet; its .address is the two-pass protocol address
            // (digest = SHAKE256(joined, 8192); address = SHAKE256(digest, 256)),
            // matching generate_address / CheckMolecule.ots -- the canonical OTS address.
            val wallet = Wallet(secret = secret, token = token, position = position)
            assertEquals(expectedOtsAddress, wallet.address,
                "OTS-derived wallet address must match canonical vector")
        }

        @Test
        @DisplayName("Molecular hash base17 matches canonical vector")
        fun molecularHashBase17() {
            val test = wotsTests[0].jsonObject
            val molecularHashHex = test["molecularHashHex"]!!.jsonPrimitive.content
            val expectedBase17 = test["molecularHashBase17"]!!.jsonPrimitive.content

            val base17 = hexToBase17Padded(molecularHashHex)
            assertEquals(expectedBase17, base17,
                "Molecular hash base17 conversion must match canonical vector")
        }

        @Test
        @DisplayName("Signature has correct fragment count (16)")
        fun signatureFragmentCount() {
            val test = wotsTests[0].jsonObject
            val secret = test["secret"]!!.jsonPrimitive.content
            val token = test["token"]!!.jsonPrimitive.content
            val position = test["position"]!!.jsonPrimitive.content
            val molecularHashBase17 = test["molecularHashBase17"]!!.jsonPrimitive.content
            val expectedFragmentCount = test["expectedSignatureFragmentCount"]!!.jsonPrimitive.int

            // Generate key and build signature fragments
            val key = Wallet.generatePrivateKey(secret = secret, token = token, position = position)
            val normalizedHash = CheckMolecule.normalizedHash(molecularHashBase17)

            // Build signature fragments (encode = true for signing)
            val keyChunks = key.chunked(128)

            assertEquals(expectedFragmentCount, keyChunks.size,
                "Private key must produce exactly $expectedFragmentCount chunks of 128 characters")
        }

        @Test
        @DisplayName("First signature fragment matches canonical vector")
        fun firstSignatureFragment() {
            val test = wotsTests[0].jsonObject
            val secret = test["secret"]!!.jsonPrimitive.content
            val token = test["token"]!!.jsonPrimitive.content
            val position = test["position"]!!.jsonPrimitive.content
            val molecularHashBase17 = test["molecularHashBase17"]!!.jsonPrimitive.content
            val expectedFragment0 = test["expectedSignatureFragment0"]!!.jsonPrimitive.content

            val key = Wallet.generatePrivateKey(secret = secret, token = token, position = position)
            val normalizedHash = CheckMolecule.normalizedHash(molecularHashBase17)
            val keyChunks = key.chunked(128)

            // Sign first chunk: iterate (8 - normalizedHash[0]) times
            var workingChunk = keyChunks[0]
            val iterations = 8 - normalizedHash[0]!!
            repeat(iterations) {
                workingChunk = Shake256.hash(workingChunk, 64)
            }

            assertEquals(expectedFragment0, workingChunk,
                "First signature fragment (index 0) must match canonical vector")
        }

        @Test
        @DisplayName("Last signature fragment matches canonical vector")
        fun lastSignatureFragment() {
            val test = wotsTests[0].jsonObject
            val secret = test["secret"]!!.jsonPrimitive.content
            val token = test["token"]!!.jsonPrimitive.content
            val position = test["position"]!!.jsonPrimitive.content
            val molecularHashBase17 = test["molecularHashBase17"]!!.jsonPrimitive.content
            val expectedFragment15 = test["expectedSignatureFragment15"]!!.jsonPrimitive.content

            val key = Wallet.generatePrivateKey(secret = secret, token = token, position = position)
            val normalizedHash = CheckMolecule.normalizedHash(molecularHashBase17)
            val keyChunks = key.chunked(128)

            // Sign last chunk: iterate (8 - normalizedHash[15]) times
            var workingChunk = keyChunks[15]
            val iterations = 8 - normalizedHash[15]!!
            repeat(iterations) {
                workingChunk = Shake256.hash(workingChunk, 64)
            }

            assertEquals(expectedFragment15, workingChunk,
                "Last signature fragment (index 15) must match canonical vector")
        }

        @Test
        @DisplayName("Full WOTS+ sign-then-verify roundtrip succeeds")
        fun fullSignVerifyRoundtrip() {
            val test = wotsTests[0].jsonObject
            val secret = test["secret"]!!.jsonPrimitive.content
            val token = test["token"]!!.jsonPrimitive.content
            val position = test["position"]!!.jsonPrimitive.content
            val molecularHashBase17 = test["molecularHashBase17"]!!.jsonPrimitive.content
            val expectedOtsAddress = test["expectedOtsAddress"]!!.jsonPrimitive.content
            val expectedVerified = test["expectedVerified"]!!.jsonPrimitive.boolean

            val key = Wallet.generatePrivateKey(secret = secret, token = token, position = position)
            val normalizedHash = CheckMolecule.normalizedHash(molecularHashBase17)
            val keyChunks = key.chunked(128)

            // Phase 1: Sign (encode) — iterate (8 - normalizedHash[idx]) times per chunk
            val signatureFragments = keyChunks.mapIndexed { idx, keyChunk ->
                var workingChunk = keyChunk
                val signIterations = 8 - normalizedHash[idx]!!
                repeat(signIterations) {
                    workingChunk = Shake256.hash(workingChunk, 64)
                }
                workingChunk
            }

            // Phase 2: Verify (decode) — iterate (8 + normalizedHash[idx]) times per fragment
            val digestSponge = Shake256.create()
            signatureFragments.forEachIndexed { idx, fragment ->
                var workingFragment = fragment
                val verifyIterations = 8 + normalizedHash[idx]!!
                repeat(verifyIterations) {
                    workingFragment = Shake256.hash(workingFragment, 64)
                }
                digestSponge.absorb(workingFragment)
            }

            // Phase 3: Derive address from reconstructed public key (two-pass:
            // digest = SHAKE256(joined, 8192) -> address = SHAKE256(digest, 256))
            val reconstructedAddress = Shake256.hash(digestSponge.hexString(1024), 32)

            assertEquals(expectedOtsAddress, reconstructedAddress,
                "Reconstructed OTS address must match wallet address (roundtrip verification)")
            assertEquals(expectedVerified, reconstructedAddress == expectedOtsAddress,
                "Verification result must match expected value")
        }
    }

    // =========================================================================
    // Utility: Hex-to-Base17 with padding (matches Atom.hashAtoms behavior)
    // =========================================================================

    private fun hexToBase17Padded(hex: String): String {
        val base17String = Strings.charsetBaseConvert(
            hex, 16, 17, "0123456789abcdef", "0123456789abcdefg"
        )
        return base17String.padStart(64, '0')
    }
}
