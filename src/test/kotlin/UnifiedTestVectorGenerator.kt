package wishKnish.knishIO.client

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import wishKnish.knishIO.client.libraries.Crypto
import wishKnish.knishIO.client.data.MetaData
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

/**
 * Unified Test Vector Generator for Kotlin SDK
 * 
 * Generates test vectors using SDK methods only (KISS & YAGNI)
 * Compatible with unified validation architecture
 */
class UnifiedTestVectorGenerator {
    
    // Fixed timestamp for reproducible molecular hashes
    private val FIXED_TIMESTAMP = 1640995200000L
    
    @Test
    @DisplayName("Generate test vectors for unified validation architecture")
    fun generateUnifiedTestVectors() {
        println("\n🚀 Kotlin Test Vector Generator v1.0")
        println("   Using SDK built-in methods")
        println("================================================")
        
        val results = mutableMapOf<String, Any>()
        
        try {
            // Load common inputs
            val inputFile = File("../../validation/unified-test-vectors/common-inputs.json")
            if (!inputFile.exists()) {
                println("❌ Common inputs not found at: ${inputFile.absolutePath}")
                println("   Please ensure you're running from the Kotlin SDK directory")
                return
            }
            
            val gson = Gson()
            val commonInputs = gson.fromJson(inputFile.readText(), Map::class.java)
            println("📋 Loaded common inputs v${commonInputs["version"]}")
            
            val wallets = commonInputs["wallets"] as Map<String, Map<String, Any>>
            val testCases = commonInputs["test_cases"] as Map<String, Map<String, Any>>
            
            println("\n🧪 Generating Test Vectors...\n")
            
            // 1. Simple Transfer
            println("1️⃣  Simple Transfer")
            val simpleTransferResult = try {
                val testCase = testCases["simple_transfer"]!!
                val fromWallet = wallets[testCase["from"]]!!
                val toWallet = wallets[testCase["to"]]!!
                
                val molecule = generateSimpleTransfer(
                    fromSeed = fromWallet["seed"] as String,
                    toSeed = toWallet["seed"] as String,
                    amount = (testCase["amount"] as Double).toInt(),
                    cellSlug = testCase["cellSlug"] as String
                )
                
                println("  ✅ Generated with hash: ${molecule.molecularHash}")
                val otsCount = molecule.atoms.count { it.otsFragment != null && it.otsFragment!!.isNotEmpty() }
                println("  ✅ OTS fragments: $otsCount/${molecule.atoms.size} atoms")
                
                mapOf(
                    "description" to testCase["description"],
                    "molecule" to moleculeToMap(molecule),
                    "success" to true
                )
            } catch (e: Exception) {
                println("  ❌ Failed: ${e.message}")
                mapOf(
                    "description" to testCases["simple_transfer"]!!["description"],
                    "error" to e.message,
                    "success" to false
                )
            }
            results["simple_transfer"] = simpleTransferResult
            
            // 2. Metadata Creation
            println("\n2️⃣  Metadata Creation")
            val metadataResult = try {
                val testCase = testCases["metadata_creation"]!!
                val walletData = wallets[testCase["wallet"]]!!
                
                val molecule = generateMetadataCreation(
                    walletSeed = walletData["seed"] as String,
                    metaType = testCase["metaType"] as String,
                    metaId = testCase["metaId"] as String,
                    meta = testCase["meta"] as Map<String, String>,
                    cellSlug = testCase["cellSlug"] as String
                )
                
                println("  ✅ Generated with hash: ${molecule.molecularHash}")
                val otsCount = molecule.atoms.count { it.otsFragment != null && it.otsFragment!!.isNotEmpty() }
                println("  ✅ OTS fragments: $otsCount/${molecule.atoms.size} atoms")
                
                mapOf(
                    "description" to testCase["description"],
                    "molecule" to moleculeToMap(molecule),
                    "success" to true
                )
            } catch (e: Exception) {
                println("  ❌ Failed: ${e.message}")
                mapOf(
                    "description" to testCases["metadata_creation"]!!["description"],
                    "error" to e.message,
                    "success" to false
                )
            }
            results["metadata_creation"] = metadataResult
            
            // 3. Wallet Creation
            println("\n3️⃣  Wallet Creation")
            val walletCreationResult = try {
                val testCase = testCases["wallet_creation"]!!
                val creatorWallet = wallets[testCase["creator"]]!!
                
                val molecule = generateWalletCreation(
                    creatorSeed = creatorWallet["seed"] as String,
                    newToken = testCase["newToken"] as String,
                    cellSlug = testCase["cellSlug"] as String
                )
                
                println("  ✅ Generated with hash: ${molecule.molecularHash}")
                val otsCount = molecule.atoms.count { it.otsFragment != null && it.otsFragment!!.isNotEmpty() }
                println("  ✅ OTS fragments: $otsCount/${molecule.atoms.size} atoms")
                
                mapOf(
                    "description" to testCase["description"],
                    "molecule" to moleculeToMap(molecule),
                    "success" to true
                )
            } catch (e: Exception) {
                println("  ❌ Failed: ${e.message}")
                mapOf(
                    "description" to testCases["wallet_creation"]!!["description"],
                    "error" to e.message,
                    "success" to false
                )
            }
            results["wallet_creation"] = walletCreationResult
            
            // Save results
            val output = mapOf(
                "generator" to "Kotlin SDK",
                "version" to "1.0.0",
                "generated" to java.time.Instant.now().toString(),
                "sdk" to mapOf(
                    "name" to "KnishIO-Client-Kotlin",
                    "version" to "1.0.0-RC1",
                    "method" to "generateSecret(seed) + SDK generation"
                ),
                "molecules" to results
            )
            
            val outputFile = File("../../validation/unified-test-vectors/molecules/kotlin-molecules.json")
            outputFile.parentFile.mkdirs()
            
            val gsonPretty = GsonBuilder().setPrettyPrinting().create()
            outputFile.writeText(gsonPretty.toJson(output))
            
            println("\n💾 Saved to: ${outputFile.absolutePath}")
            
            // Summary
            println("\n================================================")
            println("📊 Summary:")
            val successful = results.count { (it.value as Map<*, *>)["success"] == true }
            println("  Total: ${results.size} test cases")
            println("  Successful: $successful")
            println("  Failed: ${results.size - successful}")
            
            if (successful == results.size) {
                println("\n✅ All test vectors generated successfully!")
            } else {
                println("\n⚠️  Some test vectors failed. Check logs above.")
            }
            
        } catch (e: Exception) {
            println("\n❌ Fatal error: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Convert molecule to map for JSON serialization
     */
    private fun moleculeToMap(molecule: Molecule): Map<String, Any?> {
        return mapOf(
            "molecularHash" to molecule.molecularHash,
            "cellSlug" to molecule.cellSlug,
            "createdAt" to molecule.createdAt,
            "bundle" to molecule.bundle,
            "status" to molecule.status,
            "atoms" to molecule.atoms.map { atom ->
                mapOf(
                    "position" to atom.position,
                    "walletAddress" to atom.walletAddress,
                    "isotope" to atom.isotope.toString(),
                    "token" to atom.token,
                    "value" to atom.value,
                    "metaType" to atom.metaType,
                    "metaId" to atom.metaId,
                    "meta" to atom.meta.map { meta ->
                        mapOf(
                            "key" to meta.key,
                            "value" to meta.value
                        )
                    },
                    "otsFragment" to atom.otsFragment,
                    "batchId" to atom.batchId,
                    "index" to atom.index
                )
            }
        )
    }
    
    /**
     * Generate simple transfer molecule using SDK methods
     */
    private fun generateSimpleTransfer(fromSeed: String, toSeed: String, amount: Int, cellSlug: String): Molecule {
        // Generate secrets from seeds using SDK method
        val sourceSecret = Crypto.generateSecret(fromSeed)
        val recipientSecret = Crypto.generateSecret(toSeed)
        
        // Create wallets - SDK generates bundle, address, position
        val sourceWallet = Wallet(sourceSecret, "TEST")
        sourceWallet.balance = amount.toDouble() // Set exact amount for zero remainder
        
        val recipientWallet = Wallet(recipientSecret, "TEST")
        recipientWallet.balance = 500.0
        
        println("  ✓ Created wallet from seed \"$fromSeed\"")
        println("    Bundle: ${sourceWallet.bundle}")
        println("    Address: ${sourceWallet.address}")
        
        println("  ✓ Created wallet from seed \"$toSeed\"")
        println("    Bundle: ${recipientWallet.bundle}")
        println("    Address: ${recipientWallet.address}")
        
        // Create molecule
        val molecule = Molecule(
            secret = sourceSecret,
            sourceWallet = sourceWallet,
            cellSlug = cellSlug
        )
        
        // Set fixed timestamp for reproducibility
        molecule.createdAt = FIXED_TIMESTAMP.toString()
        
        // Initialize value transfer - SDK creates remainder wallet automatically
        molecule.initValue(recipientWallet, amount.toDouble())
        
        // Sign the molecule - creates OTS fragments
        molecule.sign()
        
        // Verify using SDK's check
        molecule.check()
        
        return molecule
    }
    
    /**
     * Generate metadata creation molecule
     */
    private fun generateMetadataCreation(
        walletSeed: String,
        metaType: String,
        metaId: String,
        meta: Map<String, String>,
        cellSlug: String
    ): Molecule {
        // Generate secret from seed
        val secret = Crypto.generateSecret(walletSeed)
        
        // Create wallet - use USER token for metadata
        val sourceWallet = Wallet(secret, "USER")
        sourceWallet.balance = 1000.0
        
        println("  ✓ Created wallet from seed \"$walletSeed\"")
        println("    Bundle: ${sourceWallet.bundle}")
        println("    Address: ${sourceWallet.address}")
        
        // Create molecule
        val molecule = Molecule(
            secret = secret,
            sourceWallet = sourceWallet,
            cellSlug = cellSlug
        )
        
        // Set fixed timestamp
        molecule.createdAt = (FIXED_TIMESTAMP + 1000).toString()
        
        // Convert meta map to MetaData list
        val metaList = meta.map { (key, value) ->
            MetaData(key, value)
        }.toMutableList()
        
        // Initialize metadata using SDK method
        molecule.initMeta(
            meta = metaList,
            metaType = metaType,
            metaId = metaId
        )
        
        // Sign the molecule
        molecule.sign()
        
        // Verify
        molecule.check()
        
        return molecule
    }
    
    /**
     * Generate wallet creation molecule
     */
    private fun generateWalletCreation(creatorSeed: String, newToken: String, cellSlug: String): Molecule {
        // Generate secret from seed
        val secret = Crypto.generateSecret(creatorSeed)
        
        // Create source wallet - use USER token for wallet creation
        val sourceWallet = Wallet(secret, "USER")
        sourceWallet.balance = 1000.0
        
        println("  ✓ Created wallet from seed \"$creatorSeed\"")
        println("    Bundle: ${sourceWallet.bundle}")
        println("    Address: ${sourceWallet.address}")
        
        // Create new wallet to be registered
        val newWallet = Wallet(secret, newToken)
        
        // Create molecule
        val molecule = Molecule(
            secret = secret,
            sourceWallet = sourceWallet,
            cellSlug = cellSlug
        )
        
        // Set fixed timestamp
        molecule.createdAt = (FIXED_TIMESTAMP + 3000).toString()
        
        // Initialize wallet creation using SDK method
        molecule.initWalletCreation(newWallet)
        
        // Sign the molecule
        molecule.sign()
        
        // Verify
        molecule.check()
        
        return molecule
    }
}