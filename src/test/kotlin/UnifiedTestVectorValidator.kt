/**
 * Kotlin SDK Validator v1.0.0
 * 
 * Validates test vectors using Kotlin SDK methods exclusively.
 * Following KISS and YAGNI principles - no manual validation logic.
 */

package knishio.test

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import wishKnish.knishIO.client.Molecule
import wishKnish.knishIO.client.Atom
import wishKnish.knishIO.client.Meta
import wishKnish.knishIO.client.Wallet
import wishKnish.knishIO.client.data.MetaData
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter

data class ValidationResult(
    val valid: Boolean,
    val message: String,
    val errorType: String? = null
)

data class OTSValidation(
    val valid: Boolean,
    val total: Int,
    val withOTS: Int,
    val message: String
)

data class TestCaseResult(
    val description: String,
    val otsValidation: OTSValidation? = null,
    val sdkValidation: ValidationResult? = null,
    val molecularHash: String? = null,
    val atomCount: Int? = null,
    val overall: Boolean? = null,
    val generationFailed: Boolean? = null,
    val error: String? = null
)

data class SDKResults(
    val generator: String?,
    val version: String?,
    val testCases: Map<String, TestCaseResult>
)

data class Summary(
    var totalMolecules: Int = 0,
    var validMolecules: Int = 0,
    var invalidMolecules: Int = 0,
    var withOTS: Int = 0,
    var withoutOTS: Int = 0
)

data class ValidationResults(
    val timestamp: String,
    val validator: String,
    val method: String,
    val sdks: MutableMap<String, SDKResults> = mutableMapOf(),
    val summary: Summary = Summary()
)

class UnifiedTestVectorValidator {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val results = ValidationResults(
        timestamp = Instant.now().toString(),
        validator = "Kotlin SDK v1.0",
        method = "SDK built-in validation (molecule.check())"
    )
    
    /**
     * Validate molecule using SDK methods only
     */
    private fun validateMoleculeWithSDK(moleculeData: Map<String, Any?>): ValidationResult {
        return try {
            // Create Molecule with dummy secret (for validation only)
            val molecule = Molecule(
                secret = "dummy-secret-for-validation",
                cellSlug = moleculeData["cellSlug"] as? String
            )
            
            // Set molecule fields
            molecule.molecularHash = moleculeData["molecularHash"] as? String
            molecule.bundle = moleculeData["bundle"] as? String
            molecule.status = moleculeData["status"] as? String
            molecule.createdAt = moleculeData["createdAt"] as? String ?: ""
            
            // Convert atom maps to Atom objects
            val atomsList = mutableListOf<Atom>()
            val atomsData = moleculeData["atoms"] as? List<Map<String, Any?>> ?: emptyList()
            
            for (atomData in atomsData) {
                val isotopeStr = atomData["isotope"] as? String ?: "V"
                
                // Convert meta data to MetaData list
                val metaList = mutableListOf<MetaData>()
                when (val metaRaw = atomData["meta"]) {
                    is List<*> -> {
                        for (metaItem in metaRaw) {
                            if (metaItem is Map<*, *>) {
                                val key = metaItem["key"] as? String ?: ""
                                val value = metaItem["value"] as? String
                                metaList.add(MetaData(key, value))
                            }
                        }
                    }
                    is Map<*, *> -> {
                        for ((key, value) in metaRaw) {
                            metaList.add(MetaData(key.toString(), value?.toString()))
                        }
                    }
                }
                
                val atom = Atom(
                    position = atomData["position"] as? String ?: "",
                    walletAddress = atomData["walletAddress"] as? String ?: "",
                    isotope = if (isotopeStr.isNotEmpty()) isotopeStr[0] else 'V',
                    token = atomData["token"] as? String ?: "",
                    value = atomData["value"] as? String,
                    batchId = atomData["batchId"] as? String,
                    metaType = atomData["metaType"] as? String,
                    metaId = atomData["metaId"] as? String,
                    meta = metaList,
                    otsFragment = atomData["otsFragment"] as? String,
                    index = (atomData["index"] as? Number)?.toInt() ?: 0,
                    createdAt = atomData["createdAt"] as? String ?: ""
                )
                atomsList.add(atom)
            }
            molecule.atoms = atomsList
            
            // Use SDK's built-in validation
            molecule.check()
            
            ValidationResult(
                valid = true,
                message = "SDK validation passed (molecule.check() succeeded)"
            )
        } catch (e: Exception) {
            val errorType = e.javaClass.simpleName.take(2)
            ValidationResult(
                valid = false,
                message = e.message ?: e.javaClass.simpleName,
                errorType = errorType
            )
        }
    }
    
    /**
     * Validate OTS fragments
     */
    private fun validateOTSFragments(atoms: List<Map<String, Any?>>): OTSValidation {
        val totalAtoms = atoms.size
        val atomsWithOTS = atoms.count { it["otsFragment"] != null && it["otsFragment"].toString().isNotEmpty() }
        
        if (atomsWithOTS > 0) {
            results.summary.withOTS++
        } else {
            results.summary.withoutOTS++
        }
        
        return OTSValidation(
            valid = atomsWithOTS == totalAtoms,
            total = totalAtoms,
            withOTS = atomsWithOTS,
            message = if (atomsWithOTS == totalAtoms) {
                "All $totalAtoms atoms have OTS fragments"
            } else {
                "$atomsWithOTS/$totalAtoms atoms have OTS fragments"
            }
        )
    }
    
    /**
     * Load test vectors from file
     */
    private fun loadTestVectors(sdk: String): Map<String, Any?>? {
        val filePath = File("../../validation/unified-test-vectors/molecules/$sdk-molecules.json")
        
        if (!filePath.exists()) {
            return null
        }
        
        return try {
            gson.fromJson(filePath.readText(), Map::class.java) as Map<String, Any?>
        } catch (e: Exception) {
            println("  ❌ Failed to parse $sdk test vectors: ${e.message}")
            null
        }
    }
    
    /**
     * Validate molecules from a specific SDK
     */
    private fun validateSDK(sdk: String) {
        println("\nValidating ${sdk.uppercase()} molecules...")
        println("-".repeat(60))
        
        val testVectors = loadTestVectors(sdk)
        
        if (testVectors == null) {
            println("  ⚠ No test vectors found for $sdk")
            return
        }
        
        println("  Generator: ${testVectors["generator"] ?: "Unknown"}")
        println("  Version: ${testVectors["version"] ?: "Unknown"}")
        println("  Generated: ${testVectors["timestamp"] ?: testVectors["generated"] ?: "Unknown"}")
        
        val testCasesResults = mutableMapOf<String, TestCaseResult>()
        
        // Support both 'testCases' and 'molecules' keys
        val testCases = (testVectors["testCases"] ?: testVectors["molecules"]) as? Map<String, Map<String, Any?>> ?: emptyMap()
        
        for ((testName, testData) in testCases) {
            results.summary.totalMolecules++
            
            if (testData.containsKey("error")) {
                // Test case failed during generation
                println("\n  ⚠ $testName: Generation failed")
                println("    ${testData["error"]}")
                testCasesResults[testName] = TestCaseResult(
                    description = testData["description"] as? String ?: "",
                    generationFailed = true,
                    error = testData["error"] as? String
                )
                results.summary.invalidMolecules++
            } else {
                println("\n  📋 $testName")
                
                val moleculeData = testData["molecule"] as? Map<String, Any?> ?: emptyMap()
                val atoms = moleculeData["atoms"] as? List<Map<String, Any?>> ?: emptyList()
                
                // Validate OTS fragments
                val otsValidation = validateOTSFragments(atoms)
                val otsStatus = if (otsValidation.valid) "✓" else "✗"
                println("    OTS Fragments: $otsStatus ${otsValidation.message}")
                
                // Validate molecule with SDK
                val sdkValidation = validateMoleculeWithSDK(moleculeData)
                val sdkStatus = if (sdkValidation.valid) "✓" else "✗"
                print("    SDK Validation: $sdkStatus ")
                
                if (sdkValidation.valid) {
                    println(sdkValidation.message)
                    results.summary.validMolecules++
                } else {
                    println(sdkValidation.errorType)
                    println("      ${sdkValidation.message}")
                    results.summary.invalidMolecules++
                }
                
                testCasesResults[testName] = TestCaseResult(
                    description = testData["description"] as? String ?: "",
                    otsValidation = otsValidation,
                    sdkValidation = sdkValidation,
                    molecularHash = moleculeData["molecularHash"] as? String,
                    atomCount = atoms.size,
                    overall = otsValidation.valid && sdkValidation.valid
                )
            }
        }
        
        results.sdks[sdk] = SDKResults(
            generator = testVectors["generator"] as? String,
            version = testVectors["version"] as? String,
            testCases = testCasesResults
        )
    }
    
    /**
     * Save results to JSON file
     */
    private fun saveResults() {
        val outputDir = File("../../validation/unified-test-vectors/validation-matrix")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        
        val outputPath = File(outputDir, "kotlin-sdk-validation-results.json")
        outputPath.writeText(gson.toJson(results))
        
        println("\n💾 Results saved to: ${outputPath.absolutePath}")
    }
    
    /**
     * Run validation
     */
    fun run() {
        println("\n🔬 KnishIO Kotlin SDK Validator v1.0")
        println("   Using SDK Built-in Methods Only (KISS & YAGNI)")
        println("=".repeat(60))
        
        // Get list of available SDK test vectors
        val moleculesDir = File("../../validation/unified-test-vectors/molecules")
        val availableSDKs = if (moleculesDir.exists()) {
            moleculesDir.listFiles()
                ?.filter { it.name.endsWith("-molecules.json") }
                ?.map { it.name.replace("-molecules.json", "") }
                ?: emptyList()
        } else {
            emptyList()
        }
        
        if (availableSDKs.isEmpty()) {
            println("\n❌ No test vectors found!")
            println("   Run generators first to create test vectors.")
            System.exit(1)
        }
        
        println("\nFound test vectors for: ${availableSDKs.joinToString(", ")}")
        
        // Validate each SDK's output
        for (sdk in availableSDKs) {
            validateSDK(sdk)
        }
        
        // Summary
        println("\n${"=".repeat(60)}")
        println("📊 Validation Summary")
        println("-".repeat(60))
        
        with(results.summary) {
            println("  Total Molecules: $totalMolecules")
            println("  Valid (SDK check): $validMolecules")
            println("  Invalid (SDK check): $invalidMolecules")
            println("  With OTS Fragments: $withOTS")
            println("  Without OTS Fragments: $withoutOTS")
            
            // Calculate success rates
            if (totalMolecules > 0) {
                val validRate = (validMolecules.toDouble() / totalMolecules * 100).format(1)
                val otsRate = (withOTS.toDouble() / totalMolecules * 100).format(1)
                
                println("\n  SDK Validation Rate: $validRate%")
                println("  OTS Coverage Rate: $otsRate%")
                
                when {
                    validRate == "100.0" && otsRate == "100.0" -> {
                        println("\n  ✅ All molecules valid with complete OTS coverage!")
                    }
                    validRate.toDouble() >= 75 && otsRate.toDouble() >= 75 -> {
                        println("\n  ⚠️  Partial validation success")
                    }
                    else -> {
                        println("\n  ❌ Validation issues detected")
                    }
                }
            }
        }
        
        // Per-SDK summary
        println("\nPer-SDK Results:")
        for ((sdk, sdkResults) in results.sdks) {
            val testCases = sdkResults.testCases
            val valid = testCases.values.count { it.overall == true }
            val total = testCases.size
            
            val status = if (valid == total) "✓" else "✗"
            println("  $sdk: $valid/$total valid $status")
        }
        
        // Save results
        saveResults()
        
        // Exit code based on validation
        val exitCode = if (results.summary.validMolecules == results.summary.totalMolecules) 0 else 1
        System.exit(exitCode)
    }
}

fun Double.format(decimals: Int): String = "%.${decimals}f".format(this)

fun main() {
    UnifiedTestVectorValidator().run()
}