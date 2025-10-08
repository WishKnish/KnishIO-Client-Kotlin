/*
                               (
                              (/(
                              (//(
                              (///(
                             (/////(
                             (//////(                          )
                            (////////(                        (/)
                            (////////(                       (///)
                           (//////////(                      (////)
                           (//////////(                     (//////)
                          (////////////(                    (///////)
                         (/////////////(                   (/////////)
                        (//////////////(                  (///////////)
                        (///////////////(                (/////////////)
                       (////////////////(               (//////////////)
                      (((((((((((((((((((              (((((((((((((((
                     (((((((((((((((((((              ((((((((((((((
                     (((((((((((((((((((            ((((((((((((((
                    ((((((((((((((((((((           (((((((((((((
                    ((((((((((((((((((((          ((((((((((((
                    (((((((((((((((((((         ((((((((((((
                    (((((((((((((((((((        ((((((((((
                    ((((((((((((((((((/      (((((((((
                    ((((((((((((((((((     ((((((((
                    (((((((((((((((((    (((((((
                   ((((((((((((((((((  (((((
                   #################  ##
                   ################  #
                  ################# ##
                 %################  ###
                 ###############(   ####
                ###############      ####
               ###############       ######
              %#############(        (#######
             %#############           #########
            ############(              ##########
           ###########                  #############
          #########                      ##############
        %######

        Powered by Knish.IO: Connecting a Decentralized World

Please visit https://github.com/WishKnish/KnishIO-Client-Kotlin for information.

License: https://github.com/WishKnish/KnishIO-Client-Kotlin/blob/master/LICENSE
*/

/**
 * Knish.IO Kotlin SDK Self-Test Script
 * 
 * This script performs self-contained tests to validate SDK functionality
 * and ensure cross-SDK compatibility. It reads test configurations from a
 * shared JSON file and outputs results in a standardized format.
 * 
 * Full parity with JavaScript SDK self-test implementation.
 */

import com.google.gson.*
import wishKnish.knishIO.client.*
import wishKnish.knishIO.client.data.MetaData
import wishKnish.knishIO.client.libraries.Crypto
import java.io.File
import java.lang.reflect.Type
import java.time.Instant
import kotlin.system.exitProcess

// ANSI Color codes for console output
object Colors {
    const val RESET = "\u001B[0m"
    const val BLACK = "\u001B[30m"
    const val RED = "\u001B[31m"
    const val GREEN = "\u001B[32m"
    const val YELLOW = "\u001B[33m"
    const val BLUE = "\u001B[34m"
    const val PURPLE = "\u001B[35m"
    const val CYAN = "\u001B[36m"
    const val WHITE = "\u001B[37m"
    const val BOLD = "\u001B[1m"
}

// Helper function for colored logging
fun log(message: String, color: String = Colors.RESET) {
    println("$color$message${Colors.RESET}")
}

fun logTest(testName: String, passed: Boolean, errorDetail: String? = null) {
    val status = if (passed) "✅ PASS" else "❌ FAIL"
    val color = if (passed) Colors.GREEN else Colors.RED
    log("  $status: $testName", color)
    if (!passed && errorDetail != null) {
        log("    $errorDetail", Colors.RED)
    }
}

// Test result data structures matching JavaScript format
data class CryptoTestResult(
    val passed: Boolean,
    val secret: String,
    val bundle: String,
    val expectedSecret: String,
    val expectedBundle: String
)

data class TransferTestResult(
    val passed: Boolean,
    val molecularHash: String?,
    val atomCount: Int,
    val validationError: String? = null,
    val hasRemainder: Boolean? = null
)

data class SelfTestResults(
    val sdk: String,
    val version: String,
    val timestamp: String,
    val tests: Map<String, Any>,
    val molecules: Map<String, String>,
    val crossSdkCompatible: Boolean
)

/**
 * Custom Gson adapter for secure Molecule serialization
 * Uses Molecule's toJSON() method to exclude sensitive fields like private keys
 */
class SecureMoleculeAdapter : JsonSerializer<Molecule> {
    override fun serialize(src: Molecule?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        if (src == null) {
            return JsonNull.INSTANCE
        }

        // Use Molecule's secure toJSON() method which excludes secret, privkey, etc.
        val secureMap = src.toJSON(
            includeValidationContext = true,
            includeOtsFragments = true,
            secureMode = true
        )

        // Convert the Map to JsonElement
        return context?.serialize(secureMap) ?: JsonObject()
    }
}

/**
 * Custom deserializer for cross-SDK molecule validation
 * Handles JavaScript JSON structure that differs from Kotlin's Molecule constructor
 */
class CrossSdkMoleculeDeserializer : JsonDeserializer<Molecule> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Molecule {
        val jsonObject = json?.asJsonObject ?: throw JsonParseException("Invalid molecule JSON")
        
        // Defensive check: Don't parse ML-KEM768 data as molecules
        if (!jsonObject.has("atoms") && jsonObject.has("publicKey")) {
            throw IllegalArgumentException("ML-KEM768 data detected - should not be parsed as Molecule (missing atoms array)")
        }
        
        // Create a minimal molecule for validation purposes
        // Use a dummy secret and wallet since they're not in the JavaScript JSON
        val dummySecret = "0000000000000000000000000000000000000000000000000000000000000000"
        val molecule = Molecule(
            secret = dummySecret,
            sourceWallet = Wallet(dummySecret, "USER", null, null, null),
            cellSlug = if (jsonObject.has("cellSlug") && !jsonObject.get("cellSlug").isJsonNull) 
                jsonObject.get("cellSlug").asString else null
        )
        
        // Set the non-transient fields from JSON - handle null values
        molecule.status = if (jsonObject.has("status") && !jsonObject.get("status").isJsonNull) 
            jsonObject.get("status").asString else null
        molecule.bundle = if (jsonObject.has("bundle") && !jsonObject.get("bundle").isJsonNull) 
            jsonObject.get("bundle").asString else null
        molecule.molecularHash = if (jsonObject.has("molecularHash") && !jsonObject.get("molecularHash").isJsonNull) 
            jsonObject.get("molecularHash").asString else null
        molecule.createdAt = if (jsonObject.has("createdAt") && !jsonObject.get("createdAt").isJsonNull) 
            jsonObject.get("createdAt").asString else ""
        
        // Clear and add atoms from JSON with null safety
        molecule.atoms.clear()
        val atomsArray = jsonObject.getAsJsonArray("atoms")
        if (atomsArray == null) {
            throw IllegalArgumentException("Missing atoms array in molecule data - this may be ML-KEM768 data")
        }
        
        for (atomElement in atomsArray) {
            val atomObj = atomElement.asJsonObject
            
            // Helper function to safely get string value
            fun getStringOrNull(key: String): String? {
                return if (atomObj.has(key) && !atomObj.get(key).isJsonNull) 
                    atomObj.get(key).asString else null
            }
            
            val atom = Atom(
                position = atomObj.get("position").asString,
                walletAddress = atomObj.get("walletAddress").asString,
                isotope = atomObj.get("isotope").asString.first(), // Convert to Char
                token = atomObj.get("token").asString,
                value = getStringOrNull("value"),
                metaType = getStringOrNull("metaType"),
                metaId = getStringOrNull("metaId"),
                meta = mutableListOf<MetaData>().apply {
                    if (atomObj.has("meta") && !atomObj.get("meta").isJsonNull) {
                        atomObj.getAsJsonArray("meta").forEach { metaElement ->
                            val metaObj = metaElement.asJsonObject
                            add(MetaData(
                                metaObj.get("key").asString,
                                metaObj.get("value").asString
                            ))
                        }
                    }
                },
                otsFragment = getStringOrNull("otsFragment"),
                createdAt = getStringOrNull("createdAt") ?: "" // Provide default for non-null
            ).apply {
                // Set index if present
                if (atomObj.has("index") && !atomObj.get("index").isJsonNull) {
                    index = atomObj.get("index").asInt
                }
                // Set batchId if present
                if (atomObj.has("batchId") && !atomObj.get("batchId").isJsonNull) {
                    batchId = atomObj.get("batchId").asString
                }
            }
            molecule.atoms.add(atom)
        }
        
        return molecule
    }
}

class KotlinSelfTest {
    // Create separate Gson instances for normal and cross-SDK deserialization
    // Use SecureMoleculeAdapter to prevent private key leakage in serialization
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(Molecule::class.java, SecureMoleculeAdapter())
        .create()
    private val crossSdkGson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(Molecule::class.java, CrossSdkMoleculeDeserializer())
        .create()
    private val testResults = mutableMapOf<String, Any>()
    private val moleculeStorage = mutableMapOf<String, String>()
    private var crossSdkCompatible = true
    
    /**
     * Debug utility to inspect molecule structure (matching JS implementation)
     */
    private fun inspectMolecule(molecule: Molecule, name: String = "molecule") {
        log("\n🔍 INSPECTING ${name.uppercase()}:", Colors.BLUE)
        log("  Molecular Hash: ${molecule.molecularHash ?: "NOT_SET"}")
        log("  Secret: ${if (molecule.secret != null) "SET (length: ${molecule.secret!!.length})" else "NOT_SET"}")
        log("  Bundle: ${molecule.bundle ?: "NOT_SET"}")
        log("  Source Wallet: ${molecule.sourceWallet?.address?.take(16)?.plus("...") ?: "NOT_SET"}")
        log("  Remainder Wallet: ${molecule.remainderWallet?.address?.take(16)?.plus("...") ?: "NOT_SET"}")
        log("  Atoms (${molecule.atoms.size}):")
        
        var totalValue = 0.0
        molecule.atoms.forEachIndexed { index, atom ->
            val value = atom.value?.toDoubleOrNull() ?: 0.0
            totalValue += value
            val walletSnippet = atom.walletAddress?.take(16)?.plus("...") ?: ""
            log("    [$index] ${atom.isotope}: ${atom.value} ($walletSnippet) index=${atom.index}")
        }
        
        val balanceStatus = if (totalValue == 0.0) "✅ BALANCED" else "❌ UNBALANCED"
        log("  Total Value: $totalValue $balanceStatus")
        log("  Cell Slug: ${molecule.cellSlug ?: "NOT_SET"}")
        log("  Status: ${molecule.status ?: "NOT_SET"}")
    }
    
    /**
     * Step-by-step validation diagnostic (matching JS implementation)
     */
    private fun diagnoseValidation(molecule: Molecule, senderWallet: Wallet? = null, name: String = "molecule") {
        log("\n🔬 VALIDATING ${name.uppercase()} STEP-BY-STEP:", Colors.BLUE)
        
        try {
            log("  Molecule has ${molecule.atoms.size} atoms")
            if (molecule.atoms.isNotEmpty()) {
                log("  First atom isotope: ${molecule.atoms[0].isotope}")
            }
            log("  Molecular hash present: ${!molecule.molecularHash.isNullOrEmpty()}")
            log("  Source wallet provided: ${senderWallet != null}")
            
            // Check for common issues
            if (molecule.molecularHash.isNullOrEmpty()) {
                log("    ❌ Missing molecular hash", Colors.RED)
            }
            
            if (molecule.atoms.isEmpty()) {
                log("    ❌ No atoms in molecule", Colors.RED)
            }
            
            // Check atom indices
            molecule.atoms.forEachIndexed { i, atom ->
                if (atom.index == null) {
                    log("    ❌ Atom $i has null index", Colors.RED)
                } else {
                    log("    ✅ Atom $i index: ${atom.index}", Colors.GREEN)
                }
            }
            
            // Try basic validation with error catching
            try {
                val result = molecule.check(senderWallet)
                log("  Basic validation result: $result", if (result) Colors.GREEN else Colors.RED)
            } catch (validationError: Exception) {
                log("  Basic validation error: ${validationError.message}", Colors.RED)
            }
            
        } catch (error: Exception) {
            log("  ❌ Diagnostic error: ${error.message}", Colors.RED)
        }
    }
    
    fun loadTestConfig(): JsonObject? {
        return try {
            // Embedded test configuration for SDK self-containment (Kotlin best practices)
            val defaultConfigJson = """{
              "tests": {
                "crypto": {
                  "seed": "TESTSEED",
                  "secret": "e8ffc86d60fc6a73234a834166e7436e21df6c3209dfacc8d0bd6595707872c3799abbf7deee0f9c4b58de1fd89b9abb67a207558208d5ccf550c227d197c24e9fcc3707aeb53c4031d38392020ff72bcaa0f728aa8bc3d47d95ff0afc04d8fcdb69bff638ce56646c154fc92aa517d3c40f550d2ccacbd921724e1d94b82aed2c8e172a8a7ed5a6963f5890157fe77222b97af3787741f9d3cec0b40aec6f07ae4b2b24614f0a20e035aee0df04e176175dc100eb1b00dd7ea95c28cdec47958336945333c3bef24719ed949fa56d1541f24c725d4f374a533bf255cf22f4596147bcd1ba05abcecbe9b12095e1fdddb094616894c366498be0b5785c180100efb3c5b689fc1c01131633fe1775df52a970e9472ab7bc0c19f5742b9e9436753cd16024b2d326b763eca68c414755a0d2fdbb927f007e9413f1190578b2033a03d29387f5aea71b07a5ce80fbfd45be4a15440faadeac50e41846022894fc683a52328b470bc1860c8b038d7258f504178918502b93d84d8b0fbef3e02f89f83cb1ff033a2bdbdf2a2ba78d80c12aa8b2d6c10d76c468186bd4a4e9eacc758546bb50ed7b1ee241cc5b93ff924c7bbee6778b27789e1f9104c917fc93f735eee5b25c07a883788f3d2e0771e751c4f59b76f8426027ac2b07a2ca84534433d0a1b86cef3288e7d79e8b175a3955848cfd1dfbdcd6b5bafcf6789e56e8ef40af",
                  "bundle": "fee9c2b9a964d060eb4645c4001db805c3c4b0cc9bba12841036eba4bf44b831"
                },
                "metaCreation": {
                  "seed": "TESTSEED",
                  "token": "USER",
                  "sourcePosition": "0123456789abcdeffedcba9876543210fedcba9876543210fedcba9876543210",
                  "metaType": "TestMeta",
                  "metaId": "TESTMETA123",
                  "metadata": {
                    "name": "Test Metadata",
                    "description": "This is a test metadata for SDK testing."
                  }
                },
                "simpleTransfer": {
                  "sourceSeed": "TESTSEED",
                  "recipientSeed": "RECIPIENTSEED",
                  "balance": 1000,
                  "amount": 1000,
                  "token": "TEST",
                  "sourcePosition": "0123456789abcdeffedcba9876543210fedcba9876543210fedcba9876543210",
                  "recipientPosition": "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210"
                },
                "complexTransfer": {
                  "sourceSeed": "TESTSEED",
                  "recipient1Seed": "RECIPIENTSEED",
                  "recipient2Seed": "RECIPIENT2SEED",
                  "sourceBalance": 1000,
                  "amount1": 500,
                  "amount2": 500,
                  "token": "TEST",
                  "sourcePosition": "0123456789abcdeffedcba9876543210fedcba9876543210fedcba9876543210",
                  "recipient1Position": "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210",
                  "recipient2Position": "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789"
                },
                "mlkem768": {
                  "seed": "TESTSEED",
                  "token": "ENCRYPT",
                  "position": "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
                  "plaintext": "Hello ML-KEM768 cross-platform test message!"
                }
              }
            }"""
            
            // Support optional external config override via system property
            val configPath = System.getProperty("KNISHIO_TEST_CONFIG") ?: System.getenv("KNISHIO_TEST_CONFIG")
            
            if (configPath != null) {
                val configFile = File(configPath)
                if (configFile.exists()) {
                    log("📄 Using external config: ${configFile.absolutePath}", Colors.BLUE)
                    return JsonParser.parseString(configFile.readText()).asJsonObject
                } else {
                    log("⚠️  External config path specified but file not found: ${configFile.absolutePath}", Colors.YELLOW)
                    log("   Using embedded configuration", Colors.BLUE)
                }
            }
            
            JsonParser.parseString(defaultConfigJson).asJsonObject
        } catch (e: Exception) {
            log("  ❌ ERROR: Failed to load configuration: ${e.message}", Colors.RED)
            null
        }
    }
    
    /**
     * Test 1: Crypto Test
     * Validates that SDK generates correct secrets and bundle hashes
     */
    fun testCrypto(config: JsonObject): Boolean {
        log("\n1. Crypto Test", Colors.BLUE)
        val testConfig = config.getAsJsonObject("tests").getAsJsonObject("crypto")
        
        return try {
            val seed = testConfig.get("seed").asString
            val expectedSecret = testConfig.get("secret").asString
            val expectedBundle = testConfig.get("bundle").asString
            
            // Generate secret from seed
            val secret = Crypto.generateSecret(seed, expectedSecret.length)
            val secretMatch = secret == expectedSecret
            logTest("Secret generation (seed: \"$seed\")", secretMatch)
            
            // Generate bundle hash from secret
            val bundle = Crypto.generateBundleHash(secret)
            val bundleMatch = bundle == expectedBundle
            logTest("Bundle hash generation", bundleMatch)
            
            testResults["crypto"] = CryptoTestResult(
                passed = secretMatch && bundleMatch,
                secret = secret,
                bundle = bundle,
                expectedSecret = expectedSecret,
                expectedBundle = expectedBundle
            )
            
            secretMatch && bundleMatch
        } catch (error: Exception) {
            log("  ❌ ERROR: ${error.message}", Colors.RED)
            testResults["crypto"] = mapOf(
                "passed" to false,
                "error" to error.message
            )
            false
        }
    }
    
    /**
     * Test 2: Metadata Creation Test
     * Creates and validates a metadata molecule
     */
    fun testMetaCreation(config: JsonObject): Boolean {
        log("\n2. Metadata Creation Test", Colors.BLUE)
        val testConfig = config.getAsJsonObject("tests").getAsJsonObject("metaCreation")
        
        return try {
            // Generate secret and create signing wallet
            val seed = testConfig.get("seed").asString
            val token = testConfig.get("token").asString
            val sourcePosition = testConfig.get("sourcePosition").asString
            val metaType = testConfig.get("metaType").asString
            val metaId = testConfig.get("metaId").asString
            
            val secret = Crypto.generateSecret(seed, 1024)
            val bundle = Crypto.generateBundleHash(secret)
            
            val sourceWallet = Wallet(secret, token, sourcePosition)
            logTest("Source wallet creation", true)
            
            // Create molecule instance
            val molecule = Molecule(secret, sourceWallet)
            
            // Convert metadata object to MetaData list
            val metaObject = testConfig.getAsJsonObject("metadata")
            val metaList = mutableListOf<MetaData>()
            for ((key, value) in metaObject.entrySet()) {
                metaList.add(MetaData(key, value.asString))
            }
            
            // Initialize metadata molecule
            val metaAtom = Atom(
                position = sourcePosition,
                walletAddress = sourceWallet.address ?: "",
                isotope = 'M',
                token = token,
                metaType = metaType,
                metaId = metaId,
                meta = metaList,
                index = 0
            )
            molecule.atoms.add(metaAtom)
            
            // Add required ContinuID atom
            molecule.addUserRemainderAtom(molecule.remainderWallet!!)
            
            logTest("Metadata molecule initialization", true)
            
            // Sign the molecule
            molecule.sign()
            logTest("Molecule signing", true)
            
            // Debug: Inspect molecule before validation
            inspectMolecule(molecule, "metadata molecule")
            
            // Step-by-step validation diagnostic
            diagnoseValidation(molecule, sourceWallet, "metadata molecule")
            
            // Validate the molecule with detailed error capture
            var isValid = false
            var validationError: String? = null
            try {
                isValid = molecule.check(sourceWallet)
                if (!isValid) {
                    validationError = "Validation returned false (no exception thrown)"
                }
            } catch (error: Exception) {
                isValid = false
                validationError = error.message
            }
            
            logTest("Molecule validation", isValid, validationError)
            
            // Store serialized molecule for cross-SDK verification
            moleculeStorage["metadata"] = gson.toJson(molecule)
            
            testResults["metaCreation"] = TransferTestResult(
                passed = isValid,
                molecularHash = molecule.molecularHash,
                atomCount = molecule.atoms.size,
                validationError = validationError
            )
            
            isValid
        } catch (error: Exception) {
            log("  ❌ ERROR: ${error.message}", Colors.RED)
            testResults["metaCreation"] = mapOf(
                "passed" to false,
                "error" to error.message
            )
            false
        }
    }
    
    /**
     * Test 3: Simple Transfer Test
     * Creates a value transfer with no remainder
     */
    fun testSimpleTransfer(config: JsonObject): Boolean {
        log("\n3. Simple Transfer Test", Colors.BLUE)
        val testConfig = config.getAsJsonObject("tests").getAsJsonObject("simpleTransfer")
        
        return try {
            // Create source wallet for value transfer
            val sourceSeed = testConfig.get("sourceSeed").asString
            val recipientSeed = testConfig.get("recipientSeed").asString
            val balance = testConfig.get("balance").asInt.toDouble()
            val amount = testConfig.get("amount").asInt.toDouble()
            val token = testConfig.get("token").asString
            val sourcePosition = testConfig.get("sourcePosition").asString
            val recipientPosition = testConfig.get("recipientPosition").asString
            
            val sourceSecret = Crypto.generateSecret(sourceSeed, 1024)
            val sourceBundle = Crypto.generateBundleHash(sourceSecret)
            
            val sourceWallet = Wallet(sourceSecret, token, sourcePosition)
            sourceWallet.balance = balance
            logTest("Source wallet creation", true)
            
            // Create recipient wallet
            val recipientSecret = Crypto.generateSecret(recipientSeed, 1024)
            val recipientWallet = Wallet(recipientSecret, token, recipientPosition)
            logTest("Recipient wallet creation", true)
            
            // Create molecule for value transfer
            val molecule = Molecule(sourceSecret, sourceWallet)
            
            // Initialize value transfer
            molecule.initValue(recipientWallet, amount)
            
            logTest("Value transfer initialization", true)
            
            // Sign the molecule
            molecule.sign()
            logTest("Molecule signing", true)
            
            // Debug: Inspect molecule before validation
            inspectMolecule(molecule, "simple transfer molecule")
            
            // Step-by-step validation diagnostic
            diagnoseValidation(molecule, sourceWallet, "simple transfer molecule")
            
            // Validate the molecule with detailed error capture
            var isValid = false
            var validationError: String? = null
            try {
                isValid = molecule.check(sourceWallet)
                if (!isValid) {
                    validationError = "Validation returned false (no exception thrown)"
                }
            } catch (error: Exception) {
                isValid = false
                validationError = error.message
            }
            
            logTest("Molecule validation", isValid, validationError)
            
            // Store serialized molecule for cross-SDK verification
            moleculeStorage["simpleTransfer"] = gson.toJson(molecule)
            
            testResults["simpleTransfer"] = TransferTestResult(
                passed = isValid,
                molecularHash = molecule.molecularHash,
                atomCount = molecule.atoms.size,
                validationError = validationError
            )
            
            isValid
        } catch (error: Exception) {
            log("  ❌ ERROR: ${error.message}", Colors.RED)
            testResults["simpleTransfer"] = mapOf(
                "passed" to false,
                "error" to error.message
            )
            false
        }
    }
    
    /**
     * Test 4: Complex Transfer Test
     * Creates a value transfer with remainder
     */
    fun testComplexTransfer(config: JsonObject): Boolean {
        log("\n4. Complex Transfer Test", Colors.BLUE)
        val testConfig = config.getAsJsonObject("tests").getAsJsonObject("complexTransfer")
        
        return try {
            // Read config for complex transfer
            val sourceSeed = testConfig.get("sourceSeed").asString
            val recipient1Seed = testConfig.get("recipient1Seed").asString
            val sourceBalance = testConfig.get("sourceBalance").asInt.toDouble()
            val amount1 = testConfig.get("amount1").asInt.toDouble()
            val token = testConfig.get("token").asString
            val sourcePosition = testConfig.get("sourcePosition").asString
            val recipient1Position = testConfig.get("recipient1Position").asString
            
            // Create source wallet
            val sourceSecret = Crypto.generateSecret(sourceSeed, 1024)
            val sourceBundle = Crypto.generateBundleHash(sourceSecret)
            
            val sourceWallet = Wallet(sourceSecret, token, sourcePosition)
            sourceWallet.balance = sourceBalance
            logTest("Source wallet creation", true)
            
            // Create remainder wallet from source secret (matching JavaScript)
            val remainderPosition = "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789"
            val remainderWallet = Wallet(sourceSecret, token, remainderPosition)
            logTest("Remainder wallet creation", true)
            
            // Create recipient wallet
            val recipient1Secret = Crypto.generateSecret(recipient1Seed, 1024)
            val recipient1Wallet = Wallet(recipient1Secret, token, recipient1Position)
            logTest("Recipient wallet creation", true)
            
            // Create molecule with manual atom construction for proper balance
            val molecule = Molecule(sourceSecret, sourceWallet)
            
            // Manually create atoms to debit full balance from source
            molecule.addAtom(Atom(
                position = sourceWallet.position ?: "",
                walletAddress = sourceWallet.address ?: "",
                isotope = 'V',
                token = token,
                value = (-sourceBalance).toInt().toString(),
                index = 0
            ))
            
            // Add recipient atom
            molecule.addAtom(Atom(
                position = recipient1Wallet.position ?: "",
                walletAddress = recipient1Wallet.address ?: "",
                isotope = 'V',
                token = token,
                value = amount1.toInt().toString(),
                metaType = "walletBundle",
                metaId = recipient1Wallet.bundle,
                index = 1
            ))
            
            // Add remainder atom (matching JavaScript's initValue behavior)
            val remainderValue = sourceBalance - amount1
            molecule.addAtom(Atom(
                position = remainderWallet.position ?: "",
                walletAddress = remainderWallet.address ?: "",
                isotope = 'V',
                token = token,
                value = remainderValue.toInt().toString(),
                metaType = "walletBundle",
                metaId = remainderWallet.bundle,
                index = 2
            ))
            
            logTest("Value transfer with remainder initialization", true)
            
            // Sign the molecule
            molecule.sign()
            logTest("Molecule signing", true)
            
            // Debug: Inspect molecule before validation
            inspectMolecule(molecule, "complex transfer molecule")
            
            // Step-by-step validation diagnostic
            diagnoseValidation(molecule, sourceWallet, "complex transfer molecule")
            
            // Validate the molecule with detailed error capture
            var isValid = false
            var validationError: String? = null
            try {
                isValid = molecule.check(sourceWallet)
                if (!isValid) {
                    validationError = "Validation returned false (no exception thrown)"
                }
            } catch (error: Exception) {
                isValid = false
                validationError = error.message
            }
            
            logTest("Molecule validation", isValid, validationError)
            
            // Store serialized molecule for cross-SDK verification
            moleculeStorage["complexTransfer"] = gson.toJson(molecule)
            
            testResults["complexTransfer"] = TransferTestResult(
                passed = isValid,
                molecularHash = molecule.molecularHash,
                atomCount = molecule.atoms.size,
                validationError = validationError,
                hasRemainder = true
            )
            
            isValid
        } catch (error: Exception) {
            log("  ❌ ERROR: ${error.message}", Colors.RED)
            testResults["complexTransfer"] = mapOf(
                "passed" to false,
                "error" to error.message
            )
            false
        }
    }
    
    /**
     * Test 5: ML-KEM768 Encryption Test
     * Tests post-quantum encryption/decryption compatibility
     */
    fun testMLKEM768(config: JsonObject): Boolean {
        log("\n5. ML-KEM768 Encryption Test", Colors.BLUE)
        val testConfig = config.getAsJsonObject("tests").getAsJsonObject("mlkem768")
        
        return try {
            val seed = testConfig.get("seed").asString
            val token = testConfig.get("token").asString
            val position = testConfig.get("position").asString
            val plaintext = testConfig.get("plaintext").asString
            
            // Create encryption wallet from seed
            val secret = Crypto.generateSecret(seed, 1024)
            val bundle = Crypto.generateBundleHash(secret)
            
            val encryptionWallet = Wallet(secret, token, position)
            
            logTest("Encryption wallet creation", true)
            
            // Get ML-KEM768 public key (non-deterministic)
            val publicKey = encryptionWallet.pubkey
            val publicKeyGenerated = !publicKey.isNullOrEmpty()
            logTest("ML-KEM768 public key generation", publicKeyGenerated)
            
            if (!publicKeyGenerated) {
                testResults["mlkem768"] = mapOf(
                    "passed" to false,
                    "error" to "ML-KEM768 public key generation failed"
                )
                return false
            }
            
            // Encrypt plaintext message for ourselves using direct method (matching JavaScript SDK)
            val encryptedData = encryptionWallet.encryptMessage(plaintext, publicKey!!)
            
            val encryptionSuccess = encryptedData.isNotEmpty() && 
                encryptedData.containsKey("cipherText") && encryptedData.containsKey("encryptedMessage")
            logTest("Message encryption (self-encryption)", encryptionSuccess)
            
            if (!encryptionSuccess) {
                testResults["mlkem768"] = mapOf(
                    "passed" to false,
                    "error" to "Message encryption failed"
                )
                return false
            }
            
            // Decrypt the encrypted message using direct method (matching JavaScript SDK)
            val decryptedMessage = encryptionWallet.decryptMessage(encryptedData)
            
            val decryptionSuccess = decryptedMessage == plaintext
            logTest("Message decryption and verification", decryptionSuccess)
            
            if (!decryptionSuccess) {
                log("    Expected: $plaintext", Colors.YELLOW)
                log("    Actual: $decryptedMessage", Colors.YELLOW)
                testResults["mlkem768"] = mapOf(
                    "passed" to false,
                    "error" to "Message decryption verification failed"
                )
                return false
            }
            
            val testPassed = publicKeyGenerated && encryptionSuccess && decryptionSuccess
            
            // Store ML-KEM768 data for cross-SDK verification (non-deterministic outputs)
            val mlkemData = mapOf(
                "publicKey" to publicKey,
                "encryptedData" to encryptedData,
                "originalPlaintext" to plaintext,
                "sdk" to "Kotlin"
            )
            moleculeStorage["mlkem768"] = gson.toJson(mlkemData)
            
            testResults["mlkem768"] = mapOf(
                "passed" to testPassed,
                "publicKeyGenerated" to publicKeyGenerated,
                "encryptionSuccess" to encryptionSuccess,
                "decryptionSuccess" to decryptionSuccess,
                "plaintextLength" to plaintext.length
            )
            
            testPassed
        } catch (error: Exception) {
            log("  ❌ ERROR: ${error.message}", Colors.RED)
            error.printStackTrace()
            testResults["mlkem768"] = mapOf(
                "passed" to false,
                "error" to error.message
            )
            false
        }
    }
    
    /**
     * Test 6: Negative Test Cases - Anti-Cheating Validation
     * Tests that validation properly fails for invalid molecules
     */
    fun testNegativeCases(config: JsonObject): Boolean {
        log("\n6. Negative Test Cases (Anti-Cheating)", Colors.BLUE)
        
        val cryptoConfig = config.getAsJsonObject("tests").getAsJsonObject("crypto")
        val seed = cryptoConfig.get("seed").asString
        var allNegativeTestsPassed = true
        
        return try {
            val secret = Crypto.generateSecret(seed, 2048)
            val bundle = Crypto.generateBundleHash(secret)
            
            val sourceWallet = Wallet(secret, "TEST", "0123456789abcdeffedcba9876543210fedcba9876543210fedcba9876543210")
            sourceWallet.balance = 1000.0
            
            // Test 1: Missing Molecular Hash (should fail)
            try {
                val invalidMolecule = Molecule(secret, sourceWallet)
                
                // Add a valid atom but don't sign (no molecular hash)
                val atom = Atom(
                    position = sourceWallet.position ?: "",
                    walletAddress = sourceWallet.address ?: "",
                    isotope = 'V',
                    token = "TEST",
                    value = "-100"
                )
                invalidMolecule.atoms.add(atom)
                
                // This should fail because there's no molecular hash
                val shouldFail = invalidMolecule.check(sourceWallet)
                if (shouldFail) {
                    log("  ❌ FAIL: Missing molecular hash validation (should FAIL) - Invalid molecule passed validation", Colors.RED)
                    allNegativeTestsPassed = false
                } else {
                    log("  ✅ PASS: Missing molecular hash validation (should FAIL)", Colors.GREEN)
                }
            } catch (e: Exception) {
                // Exception is expected for missing molecular hash
                log("  ✅ PASS: Missing molecular hash validation (should FAIL)", Colors.GREEN)
            }
            
            // Test 2: Invalid Molecular Hash (should fail)
            try {
                val invalidMolecule = Molecule(secret, sourceWallet)
                
                val atom = Atom(
                    position = sourceWallet.position ?: "",
                    walletAddress = sourceWallet.address ?: "",
                    isotope = 'V',
                    token = "TEST",
                    value = "-100"
                )
                invalidMolecule.atoms.add(atom)
                
                // Sign normally
                invalidMolecule.sign()
                
                // Then corrupt the molecular hash
                invalidMolecule.molecularHash = "invalid_hash_that_should_fail_validation_check_12345678"
                
                val shouldFail = invalidMolecule.check(sourceWallet)
                if (shouldFail) {
                    log("  ❌ FAIL: Invalid molecular hash validation (should FAIL) - Corrupted molecule passed validation", Colors.RED)
                    allNegativeTestsPassed = false
                } else {
                    log("  ✅ PASS: Invalid molecular hash validation (should FAIL)", Colors.GREEN)
                }
            } catch (e: Exception) {
                // Exception is expected for invalid molecular hash
                log("  ✅ PASS: Invalid molecular hash validation (should FAIL)", Colors.GREEN)
            }
            
            // Test 3: Unbalanced Transfer (should fail)
            try {
                val invalidMolecule = Molecule(secret, sourceWallet)
                
                // Create unbalanced atoms (doesn't sum to zero)
                val debitAtom = Atom(
                    position = sourceWallet.position ?: "",
                    walletAddress = sourceWallet.address ?: "",
                    isotope = 'V',
                    token = "TEST",
                    value = "-1000" // Debit full balance
                )
                invalidMolecule.atoms.add(debitAtom)
                
                val creditAtom = Atom(
                    position = sourceWallet.position ?: "",
                    walletAddress = sourceWallet.address ?: "",
                    isotope = 'V',
                    token = "TEST",
                    value = "500"  // Credit only half - unbalanced!
                )
                invalidMolecule.atoms.add(creditAtom)
                
                invalidMolecule.sign()
                
                val shouldFail = invalidMolecule.check(sourceWallet)
                if (shouldFail) {
                    log("  ❌ FAIL: Unbalanced transfer validation (should FAIL) - Unbalanced molecule passed validation", Colors.RED)
                    allNegativeTestsPassed = false
                } else {
                    log("  ✅ PASS: Unbalanced transfer validation (should FAIL)", Colors.GREEN)
                }
            } catch (e: Exception) {
                // Exception is expected for unbalanced transfers
                log("  ✅ PASS: Unbalanced transfer validation (should FAIL)", Colors.GREEN)
            }
            
            testResults["negativeCases"] = mapOf(
                "passed" to allNegativeTestsPassed,
                "description" to "Anti-cheating validation tests",
                "testCount" to 3
            )
            
            allNegativeTestsPassed
        } catch (e: Exception) {
            log("  ❌ ERROR: ${e.message}", Colors.RED)
            testResults["negativeCases"] = mapOf(
                "passed" to false,
                "error" to e.message
            )
            false
        }
    }
    
    /**
     * Test 7: Cross-SDK Validation
     * Loads and validates molecules from other SDKs (if available)
     */
    fun testCrossSdkValidation(config: JsonObject? = null): Boolean {
        log("\n7. Cross-SDK Validation", Colors.BLUE)
        
        // Check if cross-validation is disabled (Round 1 molecule generation only)
        if (System.getenv("KNISHIO_DISABLE_CROSS_VALIDATION") == "true") {
            log("  ⏭️  Cross-validation disabled for Round 1 (molecule generation only)", Colors.YELLOW)
            return true
        }
        
        // Configurable shared results directory for cross-platform testing
        val sharedResultsDir = System.getenv("KNISHIO_SHARED_RESULTS") ?: "../shared-test-results"
        val resultsDir = File(sharedResultsDir)
        
        if (!resultsDir.exists()) {
            log("  ⏭️  No other SDK results found for cross-validation", Colors.YELLOW)
            return true
        }
        
        val resultFiles = resultsDir.listFiles { file ->
            file.name.endsWith(".json") && !file.name.contains("kotlin")
        } ?: emptyArray()
        
        if (resultFiles.isEmpty()) {
            log("  ⏭️  No other SDK results found for cross-validation", Colors.YELLOW)
            return true
        }
        
        var allValid = true
        
        for (file in resultFiles) {
            val sdkName = file.name.replace("-results.json", "")
            
            try {
                val otherResults = JsonParser.parseString(file.readText()).asJsonObject
                val molecules = otherResults.getAsJsonObject("molecules")
                
                if (molecules != null) {
                    for ((moleculeType, moleculeData) in molecules.entrySet()) {
                        try {
                            if (moleculeType == "mlkem768") {
                                // Special handling for ML-KEM768 cross-SDK compatibility
                                val moleculeJson = moleculeData.asString
                                val mlkemData = JsonParser.parseString(moleculeJson).asJsonObject
                                val otherPublicKey = mlkemData.get("publicKey").asString
                                
                                // Create our own encryption wallet using the same configuration
                                if (config != null) {
                                    val testConfig = config.getAsJsonObject("tests").getAsJsonObject("mlkem768")
                                    val secret = Crypto.generateSecret(testConfig.get("seed").asString, 1024)
                                    val ourWallet = Wallet(
                                        secret,
                                        testConfig.get("token").asString,
                                        testConfig.get("position").asString
                                    )
                                    
                                    
                                    // Cross-SDK ML-KEM768 Decryption Test
                                    // Step 5 already verified encryption works, Step 6 tests interoperability
                                    var decryptionCompatible = false
                                    
                                    try {
                                        // Test: Can we decrypt their encrypted message and recover original plaintext?
                                        val otherEncryptedData = mlkemData.getAsJsonObject("encryptedData")
                                        val encryptedDataMap = mapOf(
                                            "cipherText" to otherEncryptedData.get("cipherText").asString,
                                            "encryptedMessage" to otherEncryptedData.get("encryptedMessage").asString
                                        )
                                        val originalPlaintext = mlkemData.get("originalPlaintext").asString
                                        
                                        val decryptedFromThem = ourWallet.decryptMessage(encryptedDataMap)
                                        decryptionCompatible = decryptedFromThem == originalPlaintext
                                        
                                        if (decryptionCompatible) {
                                            log("    ✅ Can decrypt $sdkName encrypted message", Colors.GREEN)
                                        } else {
                                            log("    ❌ Cannot decrypt $sdkName message (expected: \"$originalPlaintext\", got: \"$decryptedFromThem\")", Colors.RED)
                                        }
                                    } catch (error: Exception) {
                                        log("    ❌ Decryption OF $sdkName failed: ${error.message}", Colors.RED)
                                        decryptionCompatible = false
                                    }
                                    
                                    logTest("$sdkName $moleculeType decryption compatibility", decryptionCompatible)
                                    
                                    if (!decryptionCompatible) {
                                        allValid = false
                                    }
                                } else {
                                    log("  ⚠️  No config available for ML-KEM768 cross-SDK test", Colors.YELLOW)
                                }
                                continue // Skip to next iteration to avoid standard molecule processing
                            } else {
                                log("  Processing standard molecule $moleculeType from $sdkName", Colors.BLUE)
                                // Parse and validate molecule from other SDK
                                val moleculeJson = moleculeData.asString
                                // Use crossSdkGson for deserializing molecules from other SDKs
                                val molecule = crossSdkGson.fromJson(moleculeJson, Molecule::class.java)
                                
                                // Debug validation for complex molecules
                                if (moleculeType == "complexTransfer") {
                                    log("    Debug: Complex transfer molecule has ${molecule.atoms.size} atoms", Colors.BLUE)
                                    molecule.atoms.forEach { atom ->
                                        log("      Atom: ${atom.isotope}=${atom.value} @ ${atom.walletAddress.substring(0, 8)}...", Colors.BLUE)
                                    }
                                }
                                
                                // Create source wallet if it's a value transfer (for proper validation)
                                var sourceWallet: Wallet? = null
                                if (molecule.atoms.isNotEmpty() && molecule.atoms[0].isotope == 'V') {
                                    // For value transfers, create a source wallet with balance for validation
                                    // Get the first atom's value (should be negative)
                                    val sourceValue = molecule.atoms[0].value?.toDoubleOrNull() ?: 0.0
                                    if (sourceValue < 0) {
                                        // Create a source wallet with the absolute value as balance
                                        sourceWallet = Wallet(
                                            secret = "0000000000000000000000000000000000000000000000000000000000000000",
                                            token = molecule.atoms[0].token ?: "TEST",
                                            position = molecule.atoms[0].position
                                        )
                                        sourceWallet.balance = -sourceValue // Convert negative to positive balance
                                        sourceWallet.address = molecule.atoms[0].walletAddress
                                    }
                                }
                                
                                // Use the molecule's check() method for full cryptographic validation
                                val isValid = try {
                                    molecule.check(sourceWallet)
                                } catch (e: Exception) {
                                    log("    Validation exception: ${e.message}", Colors.RED)
                                    false
                                }
                                
                                logTest("$sdkName $moleculeType molecule validation", isValid)
                                
                                if (!isValid) {
                                    allValid = false
                                }
                            }
                        } catch (error: Exception) {
                            val errorType = when {
                                error.message?.contains("ML-KEM768") == true -> "ML-KEM768 parsing error"
                                error.message?.contains("atoms") == true -> "Missing atoms array"
                                else -> "General validation error"
                            }
                            logTest("$sdkName $moleculeType validation", false)
                            log("    Error ($errorType): ${error.message}", Colors.RED)
                            log("    This may indicate ML-KEM768 data being parsed as standard molecule", Colors.YELLOW)
                            allValid = false
                        }
                    }
                }
            } catch (error: Exception) {
                log("  ❌ Failed to load $sdkName results: ${error.message}", Colors.RED)
            }
        }
        
        crossSdkCompatible = allValid
        return allValid
    }
    
    /**
     * Save test results to file
     */
    fun saveResults() {
        // Configurable shared results directory for cross-platform testing
        val sharedResultsDir = System.getenv("KNISHIO_SHARED_RESULTS") ?: "../shared-test-results"
        val resultsDir = File(sharedResultsDir)
        
        if (!resultsDir.exists()) {
            resultsDir.mkdirs()
        }
        
        val results = SelfTestResults(
            sdk = "Kotlin",
            version = "1.0.0-RC1",
            timestamp = Instant.now().toString(),
            tests = testResults,
            molecules = moleculeStorage,
            crossSdkCompatible = crossSdkCompatible
        )
        
        val resultsFile = File(resultsDir, "kotlin-results.json")
        resultsFile.writeText(gson.toJson(results))
        
        log("\n📁 Results saved to: ${resultsFile.absolutePath}", Colors.BLUE)
    }
    
    /**
     * Print summary report
     */
    fun printSummary() {
        log("\n═══════════════════════════════════════════", Colors.BLUE)
        log("            TEST SUMMARY REPORT", Colors.BLUE)
        log("═══════════════════════════════════════════", Colors.BLUE)
        
        val totalTests = testResults.size
        val passedTests = testResults.values.count { 
            when (it) {
                is Map<*, *> -> it["passed"] as? Boolean ?: false
                is CryptoTestResult -> it.passed
                is TransferTestResult -> it.passed
                else -> false
            }
        }
        val failedTests = totalTests - passedTests
        
        log("\nSDK: Kotlin v1.0.0-RC1")
        log("Timestamp: ${Instant.now()}")
        
        val summaryColor = if (passedTests == totalTests) Colors.GREEN else Colors.RED
        log("\nTests Passed: $passedTests/$totalTests", summaryColor)
        
        if (failedTests > 0) {
            log("\nFailed Tests:", Colors.RED)
            for ((testName, testResult) in testResults) {
                val passed = when (testResult) {
                    is Map<*, *> -> testResult["passed"] as? Boolean ?: false
                    is CryptoTestResult -> testResult.passed
                    is TransferTestResult -> testResult.passed
                    else -> false
                }
                if (!passed) {
                    val error = when (testResult) {
                        is Map<*, *> -> testResult["error"] as? String ?: "Unknown error"
                        is TransferTestResult -> testResult.validationError ?: "Validation failed"
                        else -> "Unknown error"
                    }
                    log("  - $testName: $error", Colors.RED)
                }
            }
        }
        
        log("\nCross-SDK Compatible: ${if (crossSdkCompatible) "✅ YES" else "❌ NO"}", 
            if (crossSdkCompatible) Colors.GREEN else Colors.RED)
        
        log("\n═══════════════════════════════════════════", Colors.BLUE)
    }
    
    /**
     * Main test runner
     */
    fun run(): Int {
        // Check for cross-validation-only mode (Round 2)
        if (System.getenv("KNISHIO_CROSS_VALIDATION_ONLY") == "true") {
            log("═══════════════════════════════════════════", Colors.BLUE)
            log("    Knish.IO Kotlin SDK Cross-Validation Only", Colors.BLUE)
            log("═══════════════════════════════════════════", Colors.BLUE)

            // CRITICAL FIX: Load existing Round 1 results to preserve molecules
            val sharedResultsDir = System.getenv("KNISHIO_SHARED_RESULTS") ?: "../shared-test-results"
            val existingResultsPath = File("$sharedResultsDir/kotlin-results.json")
            if (existingResultsPath.exists()) {
                try {
                    val existingData = Gson().fromJson(existingResultsPath.readText(), JsonObject::class.java)

                    // Preserve Round 1 test results
                    if (existingData.has("tests") && existingData.get("tests").isJsonObject) {
                        val existingTests = existingData.getAsJsonObject("tests")
                        for ((key, value) in existingTests.entrySet()) {
                            // Convert JsonElement to appropriate type for testResults
                            testResults[key] = gson.fromJson(value, Any::class.java)
                        }
                    }

                    // Preserve Round 1 molecules
                    if (existingData.has("molecules") && existingData.get("molecules").isJsonObject) {
                        val existingMolecules = existingData.getAsJsonObject("molecules")
                        if (existingMolecules.has("metadata") && existingMolecules.get("metadata").isJsonPrimitive) {
                            moleculeStorage["metadata"] = existingMolecules.get("metadata").asString
                        }
                        if (existingMolecules.has("simpleTransfer") && existingMolecules.get("simpleTransfer").isJsonPrimitive) {
                            moleculeStorage["simpleTransfer"] = existingMolecules.get("simpleTransfer").asString
                        }
                        if (existingMolecules.has("complexTransfer") && existingMolecules.get("complexTransfer").isJsonPrimitive) {
                            moleculeStorage["complexTransfer"] = existingMolecules.get("complexTransfer").asString
                        }
                        if (existingMolecules.has("mlkem768") && existingMolecules.get("mlkem768").isJsonPrimitive) {
                            moleculeStorage["mlkem768"] = existingMolecules.get("mlkem768").asString
                        }
                        log("✅ Preserved Round 1 molecules for cross-validation", Colors.GREEN)
                    }
                } catch (e: Exception) {
                    log("⚠️  Could not load existing results: ${e.message}", Colors.YELLOW)
                }
            }

            // Only run cross-SDK validation
            val config = loadTestConfig()
            if (config == null) {
                return 1
            }
            
            val crossSdkResult = testCrossSdkValidation(config)

            // Save results and print summary (cross-validation only)
            saveResults()
            log("\n═══════════════════════════════════════════", Colors.BLUE)
            log("            CROSS-VALIDATION SUMMARY", Colors.BLUE)
            log("═══════════════════════════════════════════", Colors.BLUE)
            val compatStatus = if (crossSdkCompatible) "✅ YES" else "❌ NO"
            val compatColor = if (crossSdkCompatible) Colors.GREEN else Colors.RED
            log("Cross-SDK Compatible: $compatStatus", compatColor)
            log("═══════════════════════════════════════════", Colors.BLUE)

            // Exit based on cross-validation results only
            return if (crossSdkResult) 0 else 1
        }

        // Normal mode: Run all tests (Round 1 or standalone)
        log("═══════════════════════════════════════════", Colors.BLUE)
        log("    Knish.IO Kotlin SDK Self-Test", Colors.BLUE)
        log("═══════════════════════════════════════════", Colors.BLUE)
        
        val config = loadTestConfig()
        if (config == null) {
            return 1
        }
        
        // Run all tests
        testCrypto(config)
        testMetaCreation(config)
        testSimpleTransfer(config)
        testComplexTransfer(config)
        testMLKEM768(config)
        testNegativeCases(config)
        testCrossSdkValidation(config)
        
        // Save results and print summary
        saveResults()
        printSummary()
        
        // Exit with appropriate code
        val allPassed = testResults.values.all { 
            when (it) {
                is Map<*, *> -> it["passed"] as? Boolean ?: false
                is CryptoTestResult -> it.passed
                is TransferTestResult -> it.passed
                else -> false
            }
        } && crossSdkCompatible
        
        return if (allPassed) 0 else 1
    }
}

fun main() {
    val exitCode = KotlinSelfTest().run()
    exitProcess(exitCode)
}