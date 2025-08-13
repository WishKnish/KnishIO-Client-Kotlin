package wishKnish.knishIO.client

import org.junit.jupiter.api.Test
import wishKnish.knishIO.client.data.MetaData

/**
 * Fresh Test Vector Generator
 * 
 * Generates current molecular hash test vectors from the Kotlin implementation
 * that can be used to verify JavaScript compatibility.
 */
class FreshTestVectorGenerator {
    
    @Test
    fun generateCurrentKotlinTestVectors() {
        println("=".repeat(80))
        println("FRESH KOTLIN TEST VECTOR GENERATION")
        println("=".repeat(80))
        
        val testSecret = "d".repeat(2048)
        val testToken = "USER"
        val testPosition = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"
        val testCellSlug = "app1"
        val fixedTimestamp = "1754540720045"
        
        println("\n1. INPUT PARAMETERS:")
        println("   Secret: ${testSecret.take(20)}... (${testSecret.length} chars)")
        println("   Token: $testToken")
        println("   Position: $testPosition")
        println("   Cell: $testCellSlug")
        println("   Timestamp: $fixedTimestamp")
        
        // Generate wallet
        val wallet = Wallet(
            secret = testSecret,
            token = testToken,
            position = testPosition
        )
        
        println("\n2. WALLET RESULTS:")
        println("   Address: ${wallet.address}")
        println("   Bundle: ${wallet.bundle}")
        println("   Private Key (first 64 chars): ${wallet.key?.take(64)}...")
        
        // Create molecule
        val molecule = Molecule(
            secret = testSecret,
            sourceWallet = wallet,
            cellSlug = testCellSlug
        )
        
        // Set fixed timestamp
        molecule.createdAt = fixedTimestamp
        
        // Create atom with fixed timestamp
        val atom = Atom(
            position = testPosition,
            walletAddress = wallet.address!!,
            isotope = 'C',
            token = testToken,
            value = "1000000",
            index = 0,
            createdAt = fixedTimestamp
        )
        
        // Add metadata
        atom.meta = listOf(
            MetaData("characters", "BASE64")
        )
        
        molecule.addAtom(atom)
        molecule.sign()
        
        println("\n3. MOLECULAR RESULTS:")
        println("   Molecular Hash: ${molecule.molecularHash}")
        println("   Bundle Hash: ${molecule.bundle}")
        println("   Molecule Created At: ${molecule.createdAt}")
        
        println("\n4. ATOM DETAILS:")
        println("   Position: ${atom.position}")
        println("   Wallet Address: ${atom.walletAddress}")
        println("   Isotope: ${atom.isotope}")
        println("   Token: ${atom.token}")
        println("   Value: ${atom.value}")
        println("   Index: ${atom.index}")
        println("   Created At: ${atom.createdAt}")
        println("   Metadata Count: ${atom.meta.size}")
        atom.meta.forEach { meta ->
            println("     ${meta.key}: ${meta.value}")
        }
        
        println("\n5. JAVASCRIPT TEST VECTOR:")
        println("   // Use these values in JavaScript to test compatibility")
        println("   const testVector = {")
        println("     secret: '${testSecret.take(20)}...', // ${testSecret.length} 'd' characters")
        println("     token: '$testToken',")
        println("     position: '$testPosition',")
        println("     cellSlug: '$testCellSlug',")
        println("     createdAt: '$fixedTimestamp',")
        println("     atom: {")
        println("       position: '$testPosition',")
        println("       walletAddress: '${wallet.address}',")
        println("       isotope: '${atom.isotope}',")
        println("       token: '$testToken',")
        println("       value: '${atom.value}',")
        println("       index: ${atom.index},")
        println("       createdAt: '$fixedTimestamp',")
        println("       meta: [")
        atom.meta.forEach { meta ->
            println("         { key: '${meta.key}', value: '${meta.value}' }")
        }
        println("       ]")
        println("     },")
        println("     expected: {")
        println("       walletAddress: '${wallet.address}',")
        println("       bundleHash: '${wallet.bundle}',")
        println("       molecularHash: '${molecule.molecularHash}'")
        println("     }")
        println("   };")
        
        println("\n6. VERIFICATION STEPS FOR JAVASCRIPT:")
        println("   1. Generate wallet from secret")
        println("   2. Verify wallet.address matches: ${wallet.address}")
        println("   3. Verify bundle hash matches: ${wallet.bundle}")
        println("   4. Create molecule with same parameters")
        println("   5. Create atom with same data (including fixed createdAt)")
        println("   6. Sign molecule")
        println("   7. Verify molecular hash matches: ${molecule.molecularHash}")
        
        // Generate a few more test vectors with different configurations
        println("\n7. ADDITIONAL TEST VECTORS:")
        
        val additionalConfigs = listOf(
            mapOf("isotope" to 'V', "value" to "500000", "description" to "Value transfer"),
            mapOf("isotope" to 'M', "value" to "0", "description" to "Meta transaction"),
            mapOf("isotope" to 'U', "value" to "100000", "description" to "User transaction")
        )
        
        additionalConfigs.forEach { config ->
            val testAtom = Atom(
                position = testPosition,
                walletAddress = wallet.address!!,
                isotope = config["isotope"] as Char,
                token = testToken,
                value = config["value"] as String,
                index = 0,
                createdAt = fixedTimestamp
            )
            testAtom.meta = listOf(MetaData("characters", "BASE64"))
            
            val testMolecule = Molecule(
                secret = testSecret,
                sourceWallet = wallet,
                cellSlug = testCellSlug
            )
            testMolecule.createdAt = fixedTimestamp
            testMolecule.addAtom(testAtom)
            testMolecule.sign()
            
            println("   ${config["description"]}: ${testMolecule.molecularHash}")
        }
        
        println("=".repeat(80))
    }
    
    @Test
    fun generateMinimalTestVector() {
        println("=".repeat(80))
        println("MINIMAL TEST VECTOR (NO METADATA)")
        println("=".repeat(80))
        
        val testSecret = "d".repeat(2048)
        val testToken = "USER"
        val testPosition = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"
        val fixedTimestamp = "1754540720045"
        
        val wallet = Wallet(
            secret = testSecret,
            token = testToken,
            position = testPosition
        )
        
        val molecule = Molecule(
            secret = testSecret,
            sourceWallet = wallet
        )
        molecule.createdAt = fixedTimestamp
        
        val atom = Atom(
            position = testPosition,
            walletAddress = wallet.address!!,
            isotope = 'C',
            token = testToken,
            value = "1000000",
            index = 0,
            createdAt = fixedTimestamp
        )
        // NO metadata
        
        molecule.addAtom(atom)
        molecule.sign()
        
        println("   Minimal molecular hash (no metadata): ${molecule.molecularHash}")
        
        println("=".repeat(80))
    }
}