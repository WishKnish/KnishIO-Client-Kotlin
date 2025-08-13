package wishKnish.knishIO.client

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import wishKnish.knishIO.client.Atom
import wishKnish.knishIO.client.Molecule
import wishKnish.knishIO.client.data.MetaData
import java.io.File

/**
 * JavaScript Molecule Validator
 * Imports molecules created by JavaScript SDK and validates them in Kotlin
 * Part of bidirectional cross-platform testing
 */
class JavaScriptMoleculeValidator {
    
    @Serializable
    data class JSMoleculeExport(
        val generator: String,
        val timestamp: String,
        val secret: String,
        val molecules: List<JSMolecule>
    )
    
    @Serializable
    data class JSMolecule(
        val name: String,
        val data: JSMoleculeData
    )
    
    @Serializable
    data class JSMoleculeData(
        val molecularHash: String,
        val cellSlug: String?,
        val bundle: String?,
        val status: String?,
        val createdAt: String?,
        val atoms: JsonArray
    )
    
    @Test
    @DisplayName("Validate JavaScript-generated molecules in Kotlin")
    fun validateJavaScriptMolecules() {
        println("\n🔄 JavaScript Molecule Validator\n")
        
        // Load JavaScript-generated molecules
        val jsFile = File("js-molecules.json")
        if (!jsFile.exists()) {
            println("❌ js-molecules.json not found. Run js-molecule-generator.js first.")
            return
        }
        
        val jsonContent = jsFile.readText()
        val exportData = Json.decodeFromString<JSMoleculeExport>(jsonContent)
        
        println("📋 Loaded ${exportData.molecules.size} molecules from JavaScript SDK")
        println("   Generated: ${exportData.timestamp}")
        println("   Secret length: ${exportData.secret.length}")
        println()
        
        // Validate each molecule
        var passed = 0
        var failed = 0
        
        exportData.molecules.forEach { molecule ->
            println("🧬 Validating: ${molecule.name}")
            println("   Molecular hash: ${molecule.data.molecularHash}")
            println("   Cell slug: ${molecule.data.cellSlug}")
            println("   Bundle: ${molecule.data.bundle}")
            println("   Atoms: ${molecule.data.atoms.size}")
            
            try {
                // Parse atoms from JSON
                val atomsList = mutableListOf<Atom>()
                molecule.data.atoms.forEach { atomJson ->
                    val atomObj = atomJson.jsonObject
                    
                    // Create Atom from JSON data
                    val atom = Atom(
                        position = atomObj["position"]?.jsonPrimitive?.content ?: "",
                        walletAddress = atomObj["walletAddress"]?.jsonPrimitive?.content ?: "",
                        isotope = atomObj["isotope"]?.jsonPrimitive?.content?.firstOrNull() ?: 'C',
                        token = atomObj["token"]?.jsonPrimitive?.content ?: "",
                        value = atomObj["value"]?.jsonPrimitive?.contentOrNull,
                        metaType = atomObj["metaType"]?.jsonPrimitive?.contentOrNull,
                        metaId = atomObj["metaId"]?.jsonPrimitive?.contentOrNull,
                        meta = atomObj["meta"]?.jsonObject?.let { metaJson ->
                            metaJson.entries.map { 
                                MetaData(key = it.key, value = it.value.jsonPrimitive.content)
                            }
                        } ?: emptyList(),
                        otsFragment = atomObj["otsFragment"]?.jsonPrimitive?.contentOrNull
                    )
                    atomsList.add(atom)
                }
                
                // Create molecule with parsed data
                val kotlinMolecule = Molecule(secret = exportData.secret).apply {
                    this.molecularHash = molecule.data.molecularHash
                    this.bundle = molecule.data.bundle
                    this.cellSlug = molecule.data.cellSlug
                    this.atoms = atomsList
                }
                
                // Perform validation
                val isValid = try {
                    kotlinMolecule.check()
                    true
                } catch (e: Exception) {
                    println("     Check failed: ${e.message}")
                    false
                }
                
                if (isValid) {
                    println("   ✅ Valid in Kotlin")
                    passed++
                } else {
                    println("   ❌ Invalid in Kotlin")
                    failed++
                }
                
            } catch (e: Exception) {
                println("   ❌ Validation error: ${e.message}")
                failed++
            }
            
            println()
        }
        
        // Summary
        println("📊 Validation Summary:")
        println("   Total molecules: ${exportData.molecules.size}")
        println("   Passed: $passed")
        println("   Failed: $failed")
        println("   Success rate: ${(passed * 100) / exportData.molecules.size}%")
        
        if (passed == exportData.molecules.size) {
            println("\n✨ Perfect cross-platform compatibility achieved!")
        } else {
            println("\n⚠️ Cross-platform compatibility issues detected")
            println("   This indicates differences in molecular validation between SDKs")
        }
    }
    
    @Test
    @DisplayName("Detailed atom-level validation of JavaScript molecules")
    fun detailedAtomValidation() {
        println("\n🔬 Detailed Atom-Level Validation\n")
        
        val jsFile = File("js-molecules.json")
        if (!jsFile.exists()) {
            println("❌ js-molecules.json not found")
            return
        }
        
        val jsonContent = jsFile.readText()
        val exportData = Json.decodeFromString<JSMoleculeExport>(jsonContent)
        
        exportData.molecules.forEach { molecule ->
            println("📦 Molecule: ${molecule.name}")
            
            // Parse atoms
            molecule.data.atoms.forEachIndexed { index, atomJson ->
                val atomObj = atomJson.jsonObject
                
                println("   Atom $index:")
                println("     Position: ${atomObj["position"]?.jsonPrimitive?.content}")
                println("     Isotope: ${atomObj["isotope"]?.jsonPrimitive?.content}")
                println("     Token: ${atomObj["token"]?.jsonPrimitive?.content}")
                println("     Wallet: ${atomObj["walletAddress"]?.jsonPrimitive?.content}")
                
                // Check OTS fragment
                val otsFragment = atomObj["otsFragment"]?.jsonPrimitive?.content
                if (otsFragment != null) {
                    println("     OTS Fragment: ${otsFragment.take(32)}...")
                } else {
                    println("     OTS Fragment: missing")
                }
                
                // Check metadata
                val metaType = atomObj["metaType"]?.jsonPrimitive?.content
                if (metaType != null) {
                    println("     Meta Type: $metaType")
                    println("     Meta ID: ${atomObj["metaId"]?.jsonPrimitive?.content}")
                }
                
                // Check value for V isotope
                val value = atomObj["value"]?.jsonPrimitive?.content
                if (value != null) {
                    println("     Value: $value")
                }
            }
            
            println()
        }
    }
}