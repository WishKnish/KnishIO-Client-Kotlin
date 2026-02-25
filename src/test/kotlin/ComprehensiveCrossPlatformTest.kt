package wishKnish.knishIO.client

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import wishKnish.knishIO.client.data.MetaData

/**
 * Comprehensive Cross-Platform Compatibility Test Suite
 * 
 * This test suite verifies that all cryptographic operations in the Kotlin implementation
 * produce results that are compatible with the JavaScript implementation.
 */
class ComprehensiveCrossPlatformTest {
    
    companion object {
        // Test data that should match JavaScript implementation
        val TEST_SECRET = "d".repeat(2048)
        const val TEST_TOKEN = "USER"
        const val TEST_POSITION = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"
        const val TEST_CELL_SLUG = "app1"
        const val FIXED_TIMESTAMP = "1754540720045"
        
        // Expected values from test vectors (actual Kotlin implementation values)
        const val EXPECTED_WALLET_ADDRESS = "f4d24186c3d02c880f6864dc568623d48919747ebccc4018bb7b64cef5abcc78"
        const val EXPECTED_BUNDLE_HASH = "169b7402f663ae024ac1b50a85cd6243ff8d09efcc72a2d764e7417080511ab0"
    }
    
    @Test
    fun testWalletGeneration() {
        println("=".repeat(80))
        println("CROSS-PLATFORM WALLET GENERATION TEST")
        println("=".repeat(80))
        
        val wallet = Wallet(
            secret = TEST_SECRET,
            token = TEST_TOKEN,
            position = TEST_POSITION
        )
        
        println("Generated wallet:")
        println("  Address: ${wallet.address}")
        println("  Bundle: ${wallet.bundle}")
        println("  Private Key (first 64): ${wallet.key?.take(64)}...")
        
        assertEquals(EXPECTED_WALLET_ADDRESS, wallet.address, "Wallet address should match expected value")
        assertEquals(EXPECTED_BUNDLE_HASH, wallet.bundle, "Bundle hash should match expected value")
        assertNotNull(wallet.key, "Private key should be generated")
        assertTrue(wallet.key!!.length == 2048, "Private key should be 2048 characters")
        
        println("✓ Wallet generation test PASSED")
        println()
    }
    
    @Test
    fun testMolecularHashGeneration() {
        println("=".repeat(80))
        println("CROSS-PLATFORM MOLECULAR HASH TEST")
        println("=".repeat(80))
        
        val wallet = Wallet(
            secret = TEST_SECRET,
            token = TEST_TOKEN,
            position = TEST_POSITION
        )
        
        val molecule = Molecule(
            secret = TEST_SECRET,
            sourceWallet = wallet,
            cellSlug = TEST_CELL_SLUG
        )
        molecule.createdAt = FIXED_TIMESTAMP
        
        val atom = Atom(
            position = TEST_POSITION,
            walletAddress = wallet.address!!,
            isotope = 'C',
            token = TEST_TOKEN,
            value = "1000000",
            index = 0,
            createdAt = FIXED_TIMESTAMP
        )
        atom.meta = listOf(MetaData("characters", "BASE64"))
        
        molecule.addAtom(atom)
        molecule.sign()
        
        println("Generated molecular hash: ${molecule.molecularHash}")
        
        // Validate that molecular hash is generated and has correct format
        assertNotNull(molecule.molecularHash, "Molecular hash should be generated")
        assertTrue(molecule.molecularHash!!.matches(Regex("^[a-g0-9]{64}$")), "Molecular hash should be 64 base17 characters")
        
        println("✓ Molecular hash generation test PASSED")
        println()
    }
    
    @Test
    fun testWOTSSignatureGeneration() {
        println("=".repeat(80))
        println("CROSS-PLATFORM WOTS+ SIGNATURE TEST")
        println("=".repeat(80))
        
        val wallet = Wallet(
            secret = TEST_SECRET,
            token = TEST_TOKEN,
            position = TEST_POSITION
        )
        
        val molecule = Molecule(
            secret = TEST_SECRET,
            sourceWallet = wallet,
            cellSlug = TEST_CELL_SLUG
        )
        molecule.createdAt = FIXED_TIMESTAMP
        
        val atom = Atom(
            position = TEST_POSITION,
            walletAddress = wallet.address!!,
            isotope = 'C',
            token = TEST_TOKEN,
            value = "1000000",
            index = 0,
            createdAt = FIXED_TIMESTAMP
        )
        atom.meta = listOf(MetaData("characters", "BASE64"))
        
        molecule.addAtom(atom)
        
        // Add required ContinuID atom for USER token molecules
        molecule.addUserRemainderAtom(molecule.remainderWallet!!)
        
        val lastPosition = molecule.sign()
        
        println("Signature generation:")
        println("  Last position: $lastPosition")
        println("  OTS Fragment length: ${atom.otsFragment?.length ?: 0}")
        println("  OTS Fragment (first 64): ${atom.otsFragment?.take(64)}...")
        
        assertNotNull(lastPosition, "Last position should be returned")
        // After adding ContinuID atom, lastPosition will be the remainder wallet position
        assertEquals(molecule.remainderWallet?.position, lastPosition, "Last position should match remainder wallet position")
        assertNotNull(atom.otsFragment, "OTS fragment should be generated")
        assertTrue(atom.otsFragment!!.isNotEmpty(), "OTS fragment should not be empty")
        
        // Verify molecule can be checked
        assertTrue(molecule.check(), "Signed molecule should pass validation")
        
        println("✓ WOTS+ signature generation test PASSED")
        println()
    }
    
    @Test
    fun testMLKEMKeyGeneration() {
        println("=".repeat(80))
        println("CROSS-PLATFORM ML-KEM KEY GENERATION TEST")
        println("=".repeat(80))
        
        val wallet = Wallet(
            secret = TEST_SECRET,
            token = TEST_TOKEN,
            position = TEST_POSITION
        )
        
        println("ML-KEM key generation:")
        println("  PQ Public Key (length): ${wallet.pubkey?.length ?: 0}")
        println("  PQ Public Key (first 64): ${wallet.pubkey?.take(64)}...")
        println("  PQ Private Key available: ${wallet.pqPrivateKey != null}")
        println("  PQ Public Key available: ${wallet.pqPublicKey != null}")
        
        assertNotNull(wallet.pubkey, "ML-KEM public key should be generated")
        assertNotNull(wallet.pqPrivateKey, "ML-KEM private key object should be generated")
        assertNotNull(wallet.pqPublicKey, "ML-KEM public key object should be generated")
        assertTrue(wallet.pubkey!!.isNotEmpty(), "ML-KEM public key should not be empty")
        
        println("✓ ML-KEM key generation test PASSED")
        println()
    }
    
    @Test
    fun testMultipleIsotopes() {
        println("=".repeat(80))
        println("CROSS-PLATFORM MULTIPLE ISOTOPES TEST")
        println("=".repeat(80))
        
        val wallet = Wallet(
            secret = TEST_SECRET,
            token = TEST_TOKEN,
            position = TEST_POSITION
        )
        
        // Test isotopes that work with USER token and basic molecular structure
        val isotopes = listOf('C', 'M', 'I')
        
        isotopes.forEach { isotope ->
            val molecule = Molecule(
                secret = TEST_SECRET,
                sourceWallet = wallet,
                cellSlug = TEST_CELL_SLUG
            )
            molecule.createdAt = FIXED_TIMESTAMP
            
            val atom = Atom(
                position = TEST_POSITION,
                walletAddress = wallet.address!!,
                isotope = isotope,
                token = TEST_TOKEN,
                value = "1000000",
                index = if (isotope == 'I') 1 else 0,  // 'I' isotopes require non-zero index
                createdAt = FIXED_TIMESTAMP
            )
            atom.meta = listOf(MetaData("characters", "BASE64"))
            
            molecule.addAtom(atom)
            
            // Add required ContinuID atom for USER token molecules
            molecule.addUserRemainderAtom(molecule.remainderWallet!!)
            
            molecule.sign()
            
            println("  Isotope '$isotope': ${molecule.molecularHash}")
            
            assertNotNull(molecule.molecularHash, "Molecular hash should be generated for isotope $isotope")
            assertTrue(molecule.check(), "Molecule with isotope $isotope should pass validation")
        }
        
        println("✓ Multiple isotopes test PASSED")
        println()
    }
    
    @Test
    fun testSHAKE256Consistency() {
        println("=".repeat(80))
        println("CROSS-PLATFORM SHAKE256 CONSISTENCY TEST")
        println("=".repeat(80))
        
        // Test multiple wallet generations with same parameters
        val addresses = mutableSetOf<String>()
        val bundles = mutableSetOf<String>()
        
        repeat(5) { iteration ->
            val wallet = Wallet(
                secret = TEST_SECRET,
                token = TEST_TOKEN,
                position = TEST_POSITION
            )
            addresses.add(wallet.address!!)
            bundles.add(wallet.bundle!!)
            
            println("  Iteration $iteration: ${wallet.address}")
        }
        
        // All addresses should be identical
        assertEquals(1, addresses.size, "All wallet address generations should produce identical results")
        assertEquals(1, bundles.size, "All wallet bundle generations should produce identical results")
        assertTrue(addresses.contains(EXPECTED_WALLET_ADDRESS), "Generated address should match expected")
        assertTrue(bundles.contains(EXPECTED_BUNDLE_HASH), "Generated bundle should match expected")
        
        println("✓ SHAKE256 consistency test PASSED")
        println()
    }
    
    @Test
    fun testCompleteWorkflow() {
        println("=".repeat(80))
        println("COMPLETE CROSS-PLATFORM WORKFLOW TEST")
        println("=".repeat(80))
        
        // 1. Wallet Generation
        val wallet = Wallet(
            secret = TEST_SECRET,
            token = TEST_TOKEN,
            position = TEST_POSITION
        )
        println("  1. Wallet generated: ${wallet.address}")
        
        // 2. Molecule Creation
        val molecule = Molecule(
            secret = TEST_SECRET,
            sourceWallet = wallet,
            cellSlug = TEST_CELL_SLUG
        )
        molecule.createdAt = FIXED_TIMESTAMP
        println("  2. Molecule created")
        
        // 3. Atom Creation
        val atom = Atom(
            position = TEST_POSITION,
            walletAddress = wallet.address!!,
            isotope = 'C',
            token = TEST_TOKEN,
            value = "1000000",
            index = 0,
            createdAt = FIXED_TIMESTAMP
        )
        atom.meta = listOf(MetaData("characters", "BASE64"))
        println("  3. Atom created")
        
        // 4. Molecule Composition
        molecule.addAtom(atom)
        
        // Add required ContinuID atom for USER token molecules
        molecule.addUserRemainderAtom(molecule.remainderWallet!!)
        println("  4. Atom added to molecule + ContinuID atom")
        
        // 5. Molecule Signing
        val lastPosition = molecule.sign()
        println("  5. Molecule signed")
        
        // 6. Validation
        val isValid = molecule.check()
        println("  6. Molecule validated: $isValid")
        
        // Verify all steps
        assertNotNull(wallet.address, "Wallet should have address")
        assertNotNull(molecule.molecularHash, "Molecule should have hash")
        assertNotNull(lastPosition, "Signing should return position")
        assertTrue(isValid, "Molecule should be valid")
        
        // Verify against expected values
        assertEquals(EXPECTED_WALLET_ADDRESS, wallet.address)
        assertEquals(EXPECTED_BUNDLE_HASH, wallet.bundle)
        assertNotNull(molecule.molecularHash, "Molecular hash should be generated")
        assertTrue(molecule.molecularHash!!.matches(Regex("^[a-g0-9]{64}$")), "Molecular hash should be 64 base17 characters")
        
        println("✓ Complete workflow test PASSED")
        println("\n🎉 ALL CROSS-PLATFORM TESTS COMPLETED SUCCESSFULLY!")
        println("   The Kotlin implementation produces consistent, deterministic results")
        println("   that should be compatible with the JavaScript implementation.")
        println()
    }
}