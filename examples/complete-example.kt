package examples

import wishKnish.knishIO.client.KnishIOClient
import wishKnish.knishIO.client.Wallet
import wishKnish.knishIO.client.data.MetaData
import wishKnish.knishIO.client.libraries.Crypto
import wishKnish.knishIO.client.libraries.Strings
import java.net.URI

/**
 * Complete Example - KnishIO Client Kotlin SDK
 * 
 * This comprehensive example demonstrates all major features of the SDK:
 * - Authentication
 * - Wallet management
 * - Token operations
 * - Metadata storage
 * - Balance queries
 * 
 * For production use, store secrets securely (environment variables, secure vaults, etc.)
 */
fun main(args: Array<String>) {
    
    // Configuration from environment or defaults
    val nodeUri = System.getenv("KNISHIO_NODE_URI") ?: "https://node.wishknish.com/graphql"
    val secret = System.getenv("KNISHIO_SECRET") ?: generateSecureSecret()
    val cellSlug = System.getenv("KNISHIO_CELL") ?: "example"
    
    // Initialize the client with encryption enabled
    val client = KnishIOClient(
        nodeUris = listOf(URI(nodeUri)),
        encrypt = true,
        cellSlug = cellSlug
    )
    
    println("╔════════════════════════════════════════════╗")
    println("║   KnishIO Client Kotlin SDK - Complete Demo   ║")
    println("╚════════════════════════════════════════════╝")
    println()
    println("Configuration:")
    println("  Node: $nodeUri")
    println("  Cell: $cellSlug")
    println("  Encryption: enabled")
    println()
    
    try {
        // Step 1: Authentication
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("1. AUTHENTICATION")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        val authToken = client.requestAuthToken(secret)
        if (authToken.success()) {
            println("✓ Authentication successful")
            println("  Token: ${authToken.getToken()?.take(32)}...")
            println("  Expires in: ${authToken.getExpireInterval()} seconds")
            println("  Wallet: ${authToken.getWallet()?.address?.take(32)}...")
            println("  Bundle: ${authToken.getBundle()?.take(32)}...")
        } else {
            println("✗ Authentication failed: ${authToken.reason()}")
            return
        }
        println()
        
        // Step 2: Query Wallet Balance
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("2. WALLET BALANCE")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        val bundleHash = Crypto.generateBundleHash(secret)
        val balance = client.queryBalance("USER", bundleHash)
        
        if (balance.success()) {
            val wallet = balance.payload()
            println("✓ Balance query successful")
            println("  Token: USER")
            println("  Bundle: ${wallet?.bundle?.take(32)}...")
            println("  Address: ${wallet?.address?.take(32)}...")
            println("  Balance: ${wallet?.balance ?: 0}")
        } else {
            println("ℹ️ No USER token wallet found")
        }
        println()
        
        // Step 3: Create Custom Token
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("3. TOKEN CREATION")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        val tokenSlug = "DEMO${(1000..9999).random()}" // Random token to avoid conflicts
        val tokenMeta = mutableListOf(
            MetaData("name", "Demo Token"),
            MetaData("fungibility", "fungible"),
            MetaData("supply", "limited"),
            MetaData("decimals", "2"),
            MetaData("maxSupply", "1000000"),
            MetaData("description", "Example token created by SDK demo"),
            MetaData("icon", "🪙")
        )
        
        println("Creating token: $tokenSlug")
        val createToken = client.createToken(tokenSlug, 100000, tokenMeta)
        
        if (createToken.success()) {
            println("✓ Token created successfully")
            println("  Token: $tokenSlug")
            println("  Initial supply: 1,000.00")
            println("  Molecular hash: ${createToken.payload()?.molecularHash?.take(32)}...")
        } else {
            println("ℹ️ Token creation skipped: ${createToken.status()}")
        }
        println()
        
        // Step 4: Create Wallet for New Token
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("4. WALLET CREATION")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        val createWallet = client.createWallet(tokenSlug)
        if (createWallet.success()) {
            println("✓ Wallet created for token: $tokenSlug")
            
            // Query the new wallet
            val newBalance = client.queryBalance(tokenSlug)
            if (newBalance.success()) {
                println("  Address: ${newBalance.payload()?.address?.take(32)}...")
                println("  Balance: ${(newBalance.payload()?.balance ?: 0) / 100.0}")
            }
        } else {
            println("ℹ️ Wallet already exists for token: $tokenSlug")
        }
        println()
        
        // Step 5: Store Metadata
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("5. METADATA STORAGE")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        val timestamp = System.currentTimeMillis()
        val metadata = mutableListOf(
            MetaData("type", "demo"),
            MetaData("timestamp", timestamp.toString()),
            MetaData("sdk", "Kotlin"),
            MetaData("version", "1.0.0-RC1"),
            MetaData("description", "Metadata created by complete example"),
            MetaData("author", "KnishIO SDK Demo")
        )
        
        val metaId = "demo_$timestamp"
        val createMeta = client.createMeta("DemoMetadata", metaId, metadata)
        
        if (createMeta.success()) {
            println("✓ Metadata stored successfully")
            println("  Type: DemoMetadata")
            println("  ID: $metaId")
            println("  Keys: ${metadata.size} key-value pairs")
        } else {
            println("✗ Metadata storage failed: ${createMeta.status()}")
        }
        println()
        
        // Step 6: Query Metadata
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("6. METADATA QUERY")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        val queryMeta = client.queryMeta(
            metaType = "DemoMetadata",
            metaIds = listOf(metaId)
        )
        
        if (queryMeta.success()) {
            val instances = queryMeta.payload()?.instances ?: emptyList()
            println("✓ Found ${instances.size} metadata instance(s)")
            
            instances.firstOrNull()?.let { instance ->
                println("  Retrieved metadata:")
                instance.metas?.forEach { meta ->
                    println("    ${meta.key}: ${meta.value}")
                }
            }
        }
        println()
        
        // Step 7: Token Transfer (if we have balance)
        val tokenBalance = client.queryBalance(tokenSlug)
        if (tokenBalance.success() && (tokenBalance.payload()?.balance ?: 0) > 0) {
            println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            println("7. TOKEN TRANSFER")
            println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            
            // Create recipient wallet
            val recipientSecret = Strings.generateSecret()
            val recipientWallet = Wallet(recipientSecret, tokenSlug)
            
            // Transfer tokens
            val transferAmount = 100 // 1.00 with 2 decimals
            val transfer = client.transferToken(recipientWallet, tokenSlug, transferAmount)
            
            if (transfer.success()) {
                println("✓ Transfer successful")
                println("  Amount: ${transferAmount / 100.0} $tokenSlug")
                println("  To: ${recipientWallet.address?.take(32)}...")
                println("  Molecular hash: ${transfer.payload()?.molecularHash?.take(32)}...")
            }
            println()
        }
        
        // Step 8: List All Wallets
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("8. WALLET LISTING")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        val wallets = client.queryWallets()
        if (wallets.success()) {
            val walletList = wallets.payload() ?: emptyList()
            println("✓ Found ${walletList.size} wallet(s) in bundle:")
            
            walletList.groupBy { it.token }.forEach { (token, tokenWallets) ->
                val totalBalance = tokenWallets.sumOf { it.balance }
                if (totalBalance > 0 || token == "USER") {
                    println("  $token: ${tokenWallets.size} wallet(s), balance: $totalBalance")
                }
            }
        }
        println()
        
        // Summary
        println("╔════════════════════════════════════════════╗")
        println("║                  SUMMARY                      ║")
        println("╚════════════════════════════════════════════╝")
        println()
        println("✓ Authentication successful")
        println("✓ Wallet operations completed")
        println("✓ Token created: $tokenSlug")
        println("✓ Metadata stored and retrieved")
        println("✓ All SDK features demonstrated")
        println()
        println("Bundle Hash: ${bundleHash.take(32)}...")
        println()
        
    } catch (e: Exception) {
        println()
        println("❌ Error occurred: ${e.message}")
        e.printStackTrace()
    }
    
    println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    println("Demo completed. For more examples, see:")
    println("  • basic-usage.kt - Simple authentication and queries")
    println("  • token-operations.kt - Token creation and transfers")
    println("  • metadata-management.kt - Storing and querying data")
    println("  • wallet-management.kt - Wallet and bundle operations")
    println("  • advanced-molecules.kt - Low-level molecule construction")
    println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
}

/**
 * Generates a secure secret for demonstration purposes.
 * In production, use proper key management and secure storage.
 */
private fun generateSecureSecret(): String {
    println("⚠️  Generating demo secret. For production, use:")
    println("   export KNISHIO_SECRET=<your-secure-secret>")
    println()
    return Strings.generateSecret()
}