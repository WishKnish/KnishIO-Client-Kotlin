package examples

import wishKnish.knishIO.client.KnishIOClient
import wishKnish.knishIO.client.Wallet
import wishKnish.knishIO.client.libraries.Crypto
import wishKnish.knishIO.client.libraries.Strings
import java.net.URI

/**
 * Wallet Management Example
 * 
 * This example demonstrates wallet creation, management, bundle operations,
 * and shadow wallet claiming in the Knish.IO ecosystem.
 */
fun main() {
    // Initialize client
    val nodeUri = System.getenv("KNISHIO_NODE_URI") ?: "https://node.wishknish.com/graphql"
    val client = KnishIOClient(
        nodeUris = listOf(URI(nodeUri)),
        encrypt = true
    )
    
    val primarySecret = System.getenv("KNISHIO_SECRET") ?: Strings.generateSecret()
    
    println("=== Knish.IO Wallet Management Demo ===")
    println()
    
    try {
        // Step 1: Generate and examine wallet components
        println("1. Understanding wallet generation...")
        demonstrateWalletGeneration(primarySecret)
        println()
        
        // Step 2: Authenticate and get primary wallet
        println("2. Authenticating primary wallet...")
        val authResponse = client.requestAuthToken(primarySecret)
        if (!authResponse.success()) {
            println("✗ Authentication failed")
            return
        }
        
        val primaryWallet = authResponse.payload()?.wallet
        println("✓ Primary wallet authenticated")
        println("  Address: ${primaryWallet?.address}")
        println("  Bundle: ${primaryWallet?.bundle}")
        println("  Position: ${primaryWallet?.position}")
        println()
        
        // Step 3: Create wallets for different tokens
        println("3. Creating wallets for multiple tokens...")
        createMultiTokenWallets(client, primarySecret)
        println()
        
        // Step 4: Query wallet bundle
        println("4. Querying wallet bundle information...")
        queryWalletBundle(client, primaryWallet?.bundle)
        println()
        
        // Step 5: List all wallets
        println("5. Listing all wallets in bundle...")
        listBundleWallets(client)
        println()
        
        // Step 6: Create and claim shadow wallet
        println("6. Working with shadow wallets...")
        shadowWalletOperations(client, primarySecret)
        println()
        
        // Step 7: Wallet position management
        println("7. ContinuID position tracking...")
        positionManagement(primarySecret)
        println()
        
        // Step 8: Multi-bundle operations
        println("8. Managing multiple wallet bundles...")
        multiBundleOperations(client)
        
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    }
    
    println()
    println("=== Wallet Management Demo Complete ===")
}

/**
 * Demonstrate wallet generation mechanics
 */
fun demonstrateWalletGeneration(secret: String) {
    println("  Generating wallet components from secret...")
    
    // Generate bundle hash (same for all wallets from this secret)
    val bundleHash = Crypto.generateBundleHash(secret)
    println("  Bundle Hash: ${bundleHash.substring(0, 16)}...")
    
    // Generate wallets for different tokens
    val tokens = listOf("USER", "TOKEN1", "TOKEN2")
    
    tokens.forEach { token ->
        val wallet = Wallet(
            secret = secret,
            token = token,
            position = null  // Auto-generate position
        )
        
        println("  $token Wallet:")
        println("    Address: ${wallet.address?.substring(0, 16)}...")
        println("    Position: ${wallet.position?.substring(0, 16)}...")
        
        // Show that private key is deterministic
        val privateKey = Wallet.generatePrivateKey(
            secret = secret,
            token = token,
            position = wallet.position ?: ""
        )
        println("    Key fingerprint: ${privateKey.substring(0, 8)}...")
    }
}

/**
 * Create wallets for multiple token types
 */
fun createMultiTokenWallets(client: KnishIOClient, secret: String) {
    val tokens = listOf("DEMO", "POINTS", "NFT")
    
    tokens.forEach { token ->
        println("  Creating wallet for $token...")
        
        // Create wallet locally
        val wallet = Wallet(secret, token)
        
        // Declare on ledger
        val response = client.createWallet(token)
        
        if (response.success()) {
            println("    ✓ $token wallet created")
            println("      Address: ${wallet.address}")
        } else {
            println("    ℹ️ $token wallet may already exist")
        }
    }
}

/**
 * Query wallet bundle information
 */
fun queryWalletBundle(client: KnishIOClient, bundleHash: String?) {
    if (bundleHash == null) {
        println("  No bundle hash available")
        return
    }
    
    val bundleResponse = client.queryBundle(bundleHash)
    
    if (bundleResponse.success()) {
        val bundle = bundleResponse.payload()
        println("  ✓ Bundle information retrieved:")
        println("    Hash: ${bundle?.bundleHash}")
        println("    Created: ${bundle?.createdAt}")
        
        // Show wallets in bundle
        val wallets = bundle?.wallets ?: emptyList()
        println("    Wallets: ${wallets.size}")
        
        wallets.take(5).forEach { wallet ->
            println("      - ${wallet.token}: ${wallet.address?.substring(0, 16)}...")
        }
    }
}

/**
 * List all wallets in the current bundle
 */
fun listBundleWallets(client: KnishIOClient) {
    val walletsResponse = client.queryWallets()
    
    if (walletsResponse.success()) {
        val wallets = walletsResponse.payload() ?: emptyList()
        println("  ✓ Found ${wallets.size} wallet(s) in bundle:")
        
        // Group by token
        val walletsByToken = wallets.groupBy { it.token }
        
        walletsByToken.forEach { (token, tokenWallets) ->
            println("    $token (${tokenWallets.size} wallet${if (tokenWallets.size != 1) "s" else ""}):")
            tokenWallets.forEach { wallet ->
                val balance = wallet.balance
                if (balance > 0) {
                    println("      Address: ${wallet.address?.substring(0, 32)}...")
                    println("      Balance: $balance")
                }
            }
        }
    }
}

/**
 * Shadow wallet operations
 */
fun shadowWalletOperations(client: KnishIOClient, secret: String) {
    println("  Creating shadow wallet scenario...")
    
    // Create a "shadow" wallet (wallet without declared owner)
    val shadowSecret = Strings.generateSecret()
    val shadowWallet = Wallet(shadowSecret, "SHADOW")
    
    println("  Shadow wallet created:")
    println("    Address: ${shadowWallet.address}")
    
    // In a real scenario, someone would send tokens to this address
    // without the wallet being declared on the ledger
    
    // Later, claim the shadow wallet
    println("  Claiming shadow wallet...")
    
    val claimResponse = client.claimShadowWallet(
        token = "SHADOW",
        walletAddress = shadowWallet.address ?: "",
        molecules = null  // Optional: include pending molecules
    )
    
    if (claimResponse.success()) {
        println("  ✓ Shadow wallet claimed successfully")
        println("    Molecular hash: ${claimResponse.payload()?.molecularHash}")
    } else {
        println("  ℹ️ Shadow wallet claim not needed or failed")
    }
}

/**
 * ContinuID position management
 */
fun positionManagement(secret: String) {
    println("  Demonstrating ContinuID position chain...")
    
    // Create initial wallet
    val wallet1 = Wallet(secret, "POS", position = null)
    println("  Initial position: ${wallet1.position?.substring(0, 32)}...")
    
    // Generate next position in chain
    val nextPosition = Crypto.generatePosition()
    val wallet2 = Wallet(secret, "POS", position = nextPosition)
    println("  Next position:    ${wallet2.position?.substring(0, 32)}...")
    
    // Positions form a chain for identity continuity
    println("  ✓ Positions form continuous identity chain")
    
    // Show position-based key derivation
    val key1 = Wallet.generatePrivateKey(secret, "POS", wallet1.position ?: "")
    val key2 = Wallet.generatePrivateKey(secret, "POS", wallet2.position ?: "")
    
    println("  Different positions = different keys:")
    println("    Key 1: ${key1.substring(0, 16)}...")
    println("    Key 2: ${key2.substring(0, 16)}...")
}

/**
 * Multi-bundle operations
 */
fun multiBundleOperations(client: KnishIOClient) {
    println("  Working with multiple wallet bundles...")
    
    // Create wallets from different secrets (different bundles)
    val secrets = listOf(
        Strings.generateSecret(),
        Strings.generateSecret()
    )
    
    val bundles = mutableListOf<String>()
    
    secrets.forEachIndexed { index, secret ->
        // Authenticate with each secret
        val authResponse = client.requestAuthToken(secret)
        
        if (authResponse.success()) {
            val bundle = authResponse.payload()?.wallet?.bundle
            if (bundle != null) {
                bundles.add(bundle)
                println("  Bundle ${index + 1}: ${bundle.substring(0, 16)}...")
            }
        }
    }
    
    // Query specific bundle
    if (bundles.isNotEmpty()) {
        println("  Querying specific bundle...")
        val specificBundle = client.queryBundle(bundles.first())
        
        if (specificBundle.success()) {
            println("  ✓ Bundle query successful")
        }
    }
    
    println()
    println("  Key concepts:")
    println("  - One secret = one bundle (all wallets grouped)")
    println("  - Bundle contains wallets for all token types")
    println("  - Wallets in same bundle share identity")
    println("  - ContinuID ensures identity continuity")
}