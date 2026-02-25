package wishKnish.knishIO.client

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import wishKnish.knishIO.client.libraries.Crypto
import wishKnish.knishIO.client.data.MetaData
import java.io.File

/**
 * Kotlin Molecule Generator
 * Creates molecules using the Kotlin SDK and exports them to JSON
 * Part of bidirectional cross-platform testing
 */
class KotlinMoleculeGenerator {
    
    @Serializable
    data class KotlinMoleculeExport(
        val generator: String = "Kotlin SDK",
        val timestamp: String = System.currentTimeMillis().toString(),
        val secret: String,
        val molecules: List<KotlinMoleculeData>
    )
    
    @Serializable
    data class KotlinMoleculeData(
        val name: String,
        val molecularHash: String?,
        val cellSlug: String?,
        val bundle: String?,
        val status: String?,
        val createdAt: String?,
        val atoms: List<KotlinAtomData>
    )
    
    @Serializable
    data class KotlinAtomData(
        val position: String,
        val walletAddress: String,
        val isotope: String,
        val token: String,
        val value: String? = null,
        val metaType: String? = null,
        val metaId: String? = null,
        val meta: Map<String, String?>? = null,
        val otsFragment: String? = null,
        val index: Int
    )
    
    @Test
    @DisplayName("Generate Kotlin molecules for JavaScript validation")
    fun generateMoleculesForJavaScript() {
        println("\n🧬 Kotlin Molecule Generator\n")
        
        // Generate secret
        val secret = Crypto.generateSecret()
        println("Secret generated (length: ${secret.length})")
        
        val molecules = mutableListOf<KotlinMoleculeData>()
        
        try {
            // ===========================================
            // Test 1: Metadata Molecule
            // ===========================================
            println("\n📝 Creating Metadata Molecule...")
            
            val metaWallet = Wallet(secret, "USER")
            val metaMolecule = Molecule(secret, metaWallet, cellSlug = "test-cell")
            
            // Add metadata using initMeta
            metaMolecule.initMeta(
                meta = mutableListOf(
                    MetaData("foo", "Foo Value"),
                    MetaData("bar", "Bar Value"),
                    MetaData("timestamp", System.currentTimeMillis().toString())
                ),
                metaType = "TestMetaType",
                metaId = "TestMeta123"
            )
            
            // Sign the molecule
            metaMolecule.sign()
            
            println("✅ Metadata molecule created and signed")
            println("   Molecular hash: ${metaMolecule.molecularHash}")
            println("   Bundle: ${metaMolecule.bundle}")
            println("   Atoms: ${metaMolecule.atoms.size}")
            
            // Convert to exportable format
            molecules.add(KotlinMoleculeData(
                name = "metadata_molecule",
                molecularHash = metaMolecule.molecularHash,
                cellSlug = metaMolecule.cellSlug,
                bundle = metaMolecule.bundle,
                status = metaMolecule.status,
                createdAt = metaMolecule.createdAt,
                atoms = metaMolecule.atoms.map { atom ->
                    KotlinAtomData(
                        position = atom.position,
                        walletAddress = atom.walletAddress,
                        isotope = atom.isotope.toString(),
                        token = atom.token,
                        value = atom.value,
                        metaType = atom.metaType,
                        metaId = atom.metaId,
                        meta = atom.meta.associate { it.key to it.value },
                        otsFragment = atom.otsFragment,
                        index = atom.index
                    )
                }
            ))
            
            // ===========================================
            // Test 2: Wallet Creation Molecule
            // ===========================================
            println("\n📦 Creating Wallet Creation Molecule...")
            
            val walletSourceWallet = Wallet(secret, "USER")
            val walletMolecule = Molecule(secret, walletSourceWallet, cellSlug = "test-cell")
            
            // Create new wallet to register
            val newWallet = Wallet(secret, "TEST")
            
            // Add wallet creation atom
            walletMolecule.initWalletCreation(newWallet)
            
            // Sign the molecule
            walletMolecule.sign()
            
            println("✅ Wallet molecule created and signed")
            println("   Molecular hash: ${walletMolecule.molecularHash}")
            println("   Bundle: ${walletMolecule.bundle}")
            println("   Atoms: ${walletMolecule.atoms.size}")
            
            molecules.add(KotlinMoleculeData(
                name = "wallet_creation",
                molecularHash = walletMolecule.molecularHash,
                cellSlug = walletMolecule.cellSlug,
                bundle = walletMolecule.bundle,
                status = walletMolecule.status,
                createdAt = walletMolecule.createdAt,
                atoms = walletMolecule.atoms.map { atom ->
                    KotlinAtomData(
                        position = atom.position,
                        walletAddress = atom.walletAddress,
                        isotope = atom.isotope.toString(),
                        token = atom.token,
                        value = atom.value,
                        metaType = atom.metaType,
                        metaId = atom.metaId,
                        meta = atom.meta.associate { it.key to it.value },
                        otsFragment = atom.otsFragment,
                        index = atom.index
                    )
                }
            ))
            
            // ===========================================
            // Test 3: Value Transfer Molecule
            // ===========================================
            println("\n💰 Creating Value Transfer Molecule...")
            
            val senderWallet = Wallet(secret, "CRZY")
            // Set mock balance for testing
            senderWallet.balance = 1000.0
            
            val recipientSecret = Crypto.generateSecret()
            val recipientWallet = Wallet(recipientSecret, "CRZY")
            
            val transferMolecule = Molecule(secret, senderWallet, cellSlug = "test-cell")
            
            // Add value transfer atoms
            transferMolecule.initValue(
                recipientWallet = recipientWallet,
                amount = 100
            )
            
            // Sign the molecule
            transferMolecule.sign()
            
            println("✅ Transfer molecule created and signed")
            println("   Molecular hash: ${transferMolecule.molecularHash}")
            println("   Bundle: ${transferMolecule.bundle}")
            println("   Atoms: ${transferMolecule.atoms.size}")
            
            molecules.add(KotlinMoleculeData(
                name = "value_transfer",
                molecularHash = transferMolecule.molecularHash,
                cellSlug = transferMolecule.cellSlug,
                bundle = transferMolecule.bundle,
                status = transferMolecule.status,
                createdAt = transferMolecule.createdAt,
                atoms = transferMolecule.atoms.map { atom ->
                    KotlinAtomData(
                        position = atom.position,
                        walletAddress = atom.walletAddress,
                        isotope = atom.isotope.toString(),
                        token = atom.token,
                        value = atom.value,
                        metaType = atom.metaType,
                        metaId = atom.metaId,
                        meta = atom.meta.associate { it.key to it.value },
                        otsFragment = atom.otsFragment,
                        index = atom.index
                    )
                }
            ))
            
            // ===========================================
            // Export to JSON
            // ===========================================
            println("\n📁 Exporting molecules to JSON...")
            
            val exportData = KotlinMoleculeExport(
                secret = secret,
                molecules = molecules
            )
            
            val json = Json { 
                prettyPrint = true
                encodeDefaults = true
            }
            
            val jsonString = json.encodeToString(exportData)
            File("kotlin-molecules.json").writeText(jsonString)
            
            println("✅ Exported to kotlin-molecules.json")
            println("   Total molecules: ${molecules.size}")
            
            // ===========================================
            // Quick validation check
            // ===========================================
            println("\n🔍 Running self-validation...")
            
            val allMolecules = listOf(
                "metadata_molecule" to metaMolecule,
                "wallet_creation" to walletMolecule,
                "value_transfer" to transferMolecule
            )
            
            allMolecules.forEach { (name, molecule) ->
                try {
                    val isValid = molecule.check()
                    println("   ${if (isValid) "✅" else "❌"} $name: ${if (isValid) "Valid" else "Invalid"}")
                } catch (e: Exception) {
                    println("   ❌ $name: Invalid - ${e.message}")
                }
            }
            
            println("\n✨ Kotlin molecule generation complete!")
            println("📋 Next step: Import kotlin-molecules.json in JavaScript for validation")
            
        } catch (e: Exception) {
            println("❌ Error: ${e.message}")
            e.printStackTrace()
        }
    }
}