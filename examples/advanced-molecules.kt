package examples

import wishKnish.knishIO.client.Atom
import wishKnish.knishIO.client.KnishIOClient
import wishKnish.knishIO.client.Molecule
import wishKnish.knishIO.client.Wallet
import wishKnish.knishIO.client.data.MetaData
import wishKnish.knishIO.client.libraries.Crypto
import wishKnish.knishIO.client.libraries.Strings
import java.net.URI

/**
 * Advanced Molecules Example
 * 
 * This example demonstrates manual molecule construction for complex transactions
 * including multi-party transfers, batch operations, and custom atomic compositions.
 */
fun main() {
    // Initialize client
    val nodeUri = System.getenv("KNISHIO_NODE_URI") ?: "https://node.wishknish.com/graphql"
    val client = KnishIOClient(
        nodeUris = listOf(URI(nodeUri)),
        encrypt = true
    )
    
    val secret = System.getenv("KNISHIO_SECRET") ?: Strings.generateSecret()
    
    println("=== Knish.IO Advanced Molecules Demo ===")
    println()
    
    try {
        // Authenticate
        println("Authenticating...")
        val authResponse = client.requestAuthToken(secret)
        if (!authResponse.success()) {
            println("✗ Authentication failed")
            return
        }
        
        val sourceWallet = authResponse.payload()?.wallet
        if (sourceWallet == null) {
            println("✗ No wallet found")
            return
        }
        println("✓ Authenticated with wallet: ${sourceWallet.address}")
        println()
        
        // Example 1: Multi-party transfer in single molecule
        println("1. Creating multi-party transfer molecule...")
        multiPartyTransfer(client, secret, sourceWallet)
        println()
        
        // Example 2: Batch metadata operations
        println("2. Creating batch metadata molecule...")
        batchMetadataOperations(client, secret, sourceWallet)
        println()
        
        // Example 3: Complex token creation with metadata
        println("3. Creating complex token with metadata...")
        complexTokenCreation(client, secret, sourceWallet)
        println()
        
        // Example 4: Atomic swap (trade tokens)
        println("4. Creating atomic swap molecule...")
        atomicSwap(client, secret)
        println()
        
        // Example 5: Custom molecule with all isotope types
        println("5. Creating molecule with multiple isotopes...")
        multiIsotopeMolecule(client, secret, sourceWallet)
        
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    }
    
    println()
    println("=== Advanced Molecules Demo Complete ===")
}

/**
 * Example 1: Multi-party transfer
 * Send tokens to multiple recipients in a single atomic transaction
 */
fun multiPartyTransfer(client: KnishIOClient, secret: String, sourceWallet: Wallet) {
    println("  Creating molecule for multi-party transfer...")
    
    // Create molecule
    val molecule = Molecule(
        secret = secret,
        sourceWallet = sourceWallet,
        cellSlug = "demo"
    )
    
    // Generate recipient wallets
    val recipients = listOf(
        Wallet(Strings.generateSecret(), "USER"),
        Wallet(Strings.generateSecret(), "USER"),
        Wallet(Strings.generateSecret(), "USER")
    )
    
    var totalAmount = 0L
    var atomIndex = 0
    
    // Add recipient atoms
    recipients.forEachIndexed { index, recipient ->
        val amount = (index + 1) * 100L // 100, 200, 300
        totalAmount += amount
        
        val recipientAtom = Atom(
            position = recipient.position ?: Crypto.generatePosition(),
            walletAddress = recipient.address ?: "",
            isotope = 'V',
            token = "USER",
            value = amount.toString(),
            index = atomIndex++
        )
        molecule.addAtom(recipientAtom)
        println("    → Recipient ${index + 1}: $amount USER")
    }
    
    // Add source deduction atom
    val sourceAtom = Atom(
        position = sourceWallet.position ?: Crypto.generatePosition(),
        walletAddress = sourceWallet.address ?: "",
        isotope = 'V',
        token = "USER",
        value = (-totalAmount).toString(),
        index = atomIndex++
    )
    molecule.addAtom(sourceAtom)
    
    // Sign and validate
    molecule.sign()
    if (molecule.check()) {
        println("  ✓ Multi-party transfer molecule valid")
        println("  Molecular hash: ${molecule.molecularHash}")
        
        // Submit to ledger
        val response = client.proposeMolecule(molecule)
        if (response.success()) {
            println("  ✓ Multi-party transfer successful!")
        }
    } else {
        println("  ✗ Molecule validation failed")
    }
}

/**
 * Example 2: Batch metadata operations
 * Store multiple metadata entries in a single molecule
 */
fun batchMetadataOperations(client: KnishIOClient, secret: String, sourceWallet: Wallet) {
    println("  Creating batch metadata molecule...")
    
    val molecule = Molecule(
        secret = secret,
        sourceWallet = sourceWallet
    )
    
    // Add multiple metadata atoms
    val metadataEntries = listOf(
        Triple("Config", "app_config", mapOf("version" to "1.0", "theme" to "dark")),
        Triple("User", "preferences", mapOf("lang" to "en", "timezone" to "UTC")),
        Triple("Analytics", "session_001", mapOf("duration" to "3600", "pages" to "15"))
    )
    
    metadataEntries.forEachIndexed { index, (metaType, metaId, data) ->
        val metaList = mutableListOf<MetaData>()
        data.forEach { (key, value) ->
            metaList.add(MetaData(key, value))
        }
        
        val metaAtom = Atom(
            position = sourceWallet.position ?: Crypto.generatePosition(),
            walletAddress = sourceWallet.address ?: "",
            isotope = 'M',
            metaType = metaType,
            metaId = metaId,
            meta = metaList,
            index = index
        )
        molecule.addAtom(metaAtom)
        println("    → Metadata: $metaType/$metaId")
    }
    
    // Sign and submit
    molecule.sign()
    if (molecule.check()) {
        println("  ✓ Batch metadata molecule valid")
        val response = client.proposeMolecule(molecule)
        if (response.success()) {
            println("  ✓ Batch metadata stored!")
        }
    }
}

/**
 * Example 3: Complex token creation
 * Create a token with initial distribution and metadata
 */
fun complexTokenCreation(client: KnishIOClient, secret: String, sourceWallet: Wallet) {
    println("  Creating complex token molecule...")
    
    val molecule = Molecule(
        secret = secret,
        sourceWallet = sourceWallet
    )
    
    // Token definition atom (isotope U)
    val tokenMeta = mutableListOf(
        MetaData("name", "Advanced Token"),
        MetaData("fungibility", "fungible"),
        MetaData("supply", "limited"),
        MetaData("decimals", "4"),
        MetaData("maxSupply", "21000000"),
        MetaData("mintable", "false"),
        MetaData("burnable", "true")
    )
    
    val tokenAtom = Atom(
        position = sourceWallet.position ?: Crypto.generatePosition(),
        walletAddress = sourceWallet.address ?: "",
        isotope = 'U',
        token = "ADV",
        value = "10000000", // 1000.0000 with 4 decimals
        meta = tokenMeta,
        index = 0
    )
    molecule.addAtom(tokenAtom)
    
    // Add initial distribution atoms
    val distributions = listOf(
        Pair("treasury", 5000000L),  // 50% to treasury
        Pair("team", 2000000L),       // 20% to team
        Pair("community", 3000000L)   // 30% to community
    )
    
    distributions.forEachIndexed { index, (label, amount) ->
        val distWallet = Wallet(Strings.generateSecret(), "ADV")
        
        val distAtom = Atom(
            position = distWallet.position ?: Crypto.generatePosition(),
            walletAddress = distWallet.address ?: "",
            isotope = 'V',
            token = "ADV",
            value = amount.toString(),
            metaType = "Distribution",
            metaId = label,
            index = index + 1
        )
        molecule.addAtom(distAtom)
        println("    → $label: ${amount / 10000.0} ADV")
    }
    
    // Sign and submit
    molecule.sign()
    if (molecule.check()) {
        println("  ✓ Complex token molecule valid")
        val response = client.proposeMolecule(molecule)
        if (response.success()) {
            println("  ✓ Token created with distribution!")
        }
    }
}

/**
 * Example 4: Atomic swap
 * Trade tokens between two parties atomically
 */
fun atomicSwap(client: KnishIOClient, secret: String) {
    println("  Creating atomic swap molecule...")
    
    // Create two parties
    val party1Secret = secret
    val party2Secret = Strings.generateSecret()
    
    val party1Wallet = Wallet(party1Secret, "TOKEN1")
    val party2Wallet = Wallet(party2Secret, "TOKEN2")
    
    // Create swap molecule
    val molecule = Molecule(secret = party1Secret)
    
    // Party 1 sends TOKEN1
    molecule.addAtom(Atom(
        position = party1Wallet.position ?: Crypto.generatePosition(),
        walletAddress = party1Wallet.address ?: "",
        isotope = 'V',
        token = "TOKEN1",
        value = "-100",
        index = 0
    ))
    
    // Party 2 receives TOKEN1
    molecule.addAtom(Atom(
        position = party2Wallet.position ?: Crypto.generatePosition(),
        walletAddress = party2Wallet.address ?: "",
        isotope = 'V',
        token = "TOKEN1",
        value = "100",
        index = 1
    ))
    
    // Party 2 sends TOKEN2
    molecule.addAtom(Atom(
        position = party2Wallet.position ?: Crypto.generatePosition(),
        walletAddress = party2Wallet.address ?: "",
        isotope = 'V',
        token = "TOKEN2",
        value = "-50",
        index = 2
    ))
    
    // Party 1 receives TOKEN2
    molecule.addAtom(Atom(
        position = party1Wallet.position ?: Crypto.generatePosition(),
        walletAddress = party1Wallet.address ?: "",
        isotope = 'V',
        token = "TOKEN2",
        value = "50",
        index = 3
    ))
    
    // Both parties must sign (in production)
    molecule.sign()
    
    if (molecule.check()) {
        println("  ✓ Atomic swap molecule valid")
        println("    Party 1: -100 TOKEN1, +50 TOKEN2")
        println("    Party 2: +100 TOKEN1, -50 TOKEN2")
    }
}

/**
 * Example 5: Multi-isotope molecule
 * Combine different operation types in one transaction
 */
fun multiIsotopeMolecule(client: KnishIOClient, secret: String, sourceWallet: Wallet) {
    println("  Creating multi-isotope molecule...")
    
    val molecule = Molecule(
        secret = secret,
        sourceWallet = sourceWallet
    )
    
    // C isotope - ContinuID update
    molecule.addAtom(Atom(
        position = Crypto.generatePosition(),
        walletAddress = sourceWallet.address ?: "",
        isotope = 'C',
        token = "USER",
        index = 0
    ))
    println("    → ContinuID update (C)")
    
    // V isotope - Value transfer
    molecule.addAtom(Atom(
        position = sourceWallet.position ?: Crypto.generatePosition(),
        walletAddress = sourceWallet.address ?: "",
        isotope = 'V',
        token = "USER",
        value = "-10",
        index = 1
    ))
    println("    → Value transfer (V)")
    
    // M isotope - Metadata
    molecule.addAtom(Atom(
        position = sourceWallet.position ?: Crypto.generatePosition(),
        walletAddress = sourceWallet.address ?: "",
        isotope = 'M',
        metaType = "Transaction",
        metaId = "tx_001",
        meta = mutableListOf(
            MetaData("type", "complex"),
            MetaData("timestamp", System.currentTimeMillis().toString())
        ),
        index = 2
    ))
    println("    → Metadata storage (M)")
    
    // I isotope - Instance for batch
    val batchId = "batch_${System.currentTimeMillis()}"
    molecule.addAtom(Atom(
        position = sourceWallet.position ?: Crypto.generatePosition(),
        walletAddress = sourceWallet.address ?: "",
        isotope = 'I',
        batchId = batchId,
        metaType = "BatchOperation",
        metaId = "instance_001",
        index = 3
    ))
    println("    → Batch instance (I)")
    
    // Sign and validate
    molecule.sign()
    if (molecule.check()) {
        println("  ✓ Multi-isotope molecule valid")
        println("  Molecular hash: ${molecule.molecularHash}")
    }
}