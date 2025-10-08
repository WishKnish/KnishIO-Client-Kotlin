package examples

import wishKnish.knishIO.client.KnishIOClient
import wishKnish.knishIO.client.Wallet
import wishKnish.knishIO.client.data.MetaData
import wishKnish.knishIO.client.libraries.Strings
import java.net.URI

/**
 * Token Operations Example
 * 
 * This example demonstrates how to create custom tokens, transfer them between wallets,
 * and manage token properties using the Knish.IO Kotlin SDK.
 */
fun main() {
    // Initialize client
    val nodeUri = System.getenv("KNISHIO_NODE_URI") ?: "https://node.wishknish.com/graphql"
    val client = KnishIOClient(
        nodeUris = listOf(URI(nodeUri)),
        encrypt = true
    )
    
    // Use environment secrets or generate for demo
    val creatorSecret = System.getenv("KNISHIO_SECRET") ?: Strings.generateSecret()
    val recipientSecret = System.getenv("KNISHIO_RECIPIENT_SECRET") ?: Strings.generateSecret()
    
    println("=== Knish.IO Token Operations Demo ===")
    println()
    
    try {
        // Step 1: Authenticate as token creator
        println("1. Authenticating token creator...")
        val authResponse = client.requestAuthToken(creatorSecret)
        if (!authResponse.success()) {
            println("✗ Authentication failed: ${authResponse.reason()}")
            return
        }
        println("✓ Creator authenticated")
        println()
        
        // Step 2: Create a new custom token
        println("2. Creating custom token 'DEMO'...")
        
        // Define token properties
        val tokenMeta = mutableListOf(
            MetaData("name", "Demo Token"),
            MetaData("fungibility", "fungible"),      // fungible or nonfungible
            MetaData("supply", "limited"),            // limited or replenishable
            MetaData("decimals", "2"),                // Number of decimal places
            MetaData("description", "A demo token for testing"),
            MetaData("icon", "🪙")
        )
        
        val createTokenResponse = client.createToken(
            token = "DEMO",
            amount = 1000000,  // Initial supply: 10,000.00 (with 2 decimals)
            meta = tokenMeta
        )
        
        if (createTokenResponse.success()) {
            println("✓ Token created successfully!")
            println("  Token: DEMO")
            println("  Initial supply: 10,000.00")
            println("  Molecular hash: ${createTokenResponse.payload()?.molecularHash}")
            println()
        } else {
            println("✗ Token creation failed: ${createTokenResponse.reason()}")
            // Try to continue with existing token
        }
        
        // Step 3: Query creator's balance
        println("3. Checking creator's DEMO balance...")
        val creatorBalance = client.queryBalance("DEMO")
        if (creatorBalance.success()) {
            val balance = creatorBalance.payload()?.balance ?: 0
            println("✓ Creator balance: ${balance / 100.0} DEMO")
            println()
        }
        
        // Step 4: Create recipient wallet
        println("4. Setting up recipient wallet...")
        
        // Authenticate as recipient to ensure wallet exists
        client.requestAuthToken(recipientSecret)
        val recipientWallet = Wallet(recipientSecret, "DEMO")
        
        // Declare the wallet on the ledger
        val createWalletResponse = client.createWallet("DEMO")
        if (createWalletResponse.success()) {
            println("✓ Recipient wallet created")
            println("  Address: ${recipientWallet.address}")
        } else {
            println("  Wallet may already exist")
        }
        println()
        
        // Step 5: Transfer tokens
        println("5. Transferring 100.00 DEMO to recipient...")
        
        // Re-authenticate as creator for transfer
        client.requestAuthToken(creatorSecret)
        
        val transferResponse = client.transferToken(
            recipient = recipientWallet,
            token = "DEMO",
            amount = 10000  // 100.00 with 2 decimals
        )
        
        if (transferResponse.success()) {
            println("✓ Transfer successful!")
            println("  Amount: 100.00 DEMO")
            println("  To: ${recipientWallet.address}")
            println("  Molecular hash: ${transferResponse.payload()?.molecularHash}")
            println()
        } else {
            println("✗ Transfer failed: ${transferResponse.reason()}")
            return
        }
        
        // Step 6: Verify balances after transfer
        println("6. Verifying balances after transfer...")
        
        // Check creator balance
        val creatorFinalBalance = client.queryBalance("DEMO")
        if (creatorFinalBalance.success()) {
            val balance = creatorFinalBalance.payload()?.balance ?: 0
            println("  Creator balance: ${balance / 100.0} DEMO")
        }
        
        // Check recipient balance
        client.requestAuthToken(recipientSecret)
        val recipientBalance = client.queryBalance("DEMO")
        if (recipientBalance.success()) {
            val balance = recipientBalance.payload()?.balance ?: 0
            println("  Recipient balance: ${balance / 100.0} DEMO")
        }
        println()
        
        // Step 7: Create a non-fungible token (NFT)
        println("7. Creating non-fungible token 'DEMONFT'...")
        
        client.requestAuthToken(creatorSecret)
        
        val nftMeta = mutableListOf(
            MetaData("name", "Demo NFT Collection"),
            MetaData("fungibility", "nonfungible"),
            MetaData("supply", "limited"),
            MetaData("decimals", "0"),  // NFTs don't have decimals
            MetaData("description", "Unique digital collectibles"),
            MetaData("maxSupply", "100")
        )
        
        val createNftResponse = client.createToken(
            token = "DEMONFT",
            amount = 1,  // Create first NFT
            meta = nftMeta
        )
        
        if (createNftResponse.success()) {
            println("✓ NFT created successfully!")
            println("  Token: DEMONFT")
            println("  Token ID: #1")
            println()
        }
        
        // Step 8: Create a replenishable token
        println("8. Creating replenishable token 'POINTS'...")
        
        val pointsMeta = mutableListOf(
            MetaData("name", "Reward Points"),
            MetaData("fungibility", "fungible"),
            MetaData("supply", "replenishable"),  // Can mint more
            MetaData("decimals", "0"),
            MetaData("description", "Loyalty reward points")
        )
        
        val createPointsResponse = client.createToken(
            token = "POINTS",
            amount = 1000,
            meta = pointsMeta
        )
        
        if (createPointsResponse.success()) {
            println("✓ Replenishable token created!")
            println("  Token: POINTS")
            println("  Initial supply: 1000")
            println()
        }
        
        // Step 9: List all tokens in wallet
        println("9. Listing all tokens in creator's wallet...")
        val walletsResponse = client.queryWallets()
        
        if (walletsResponse.success()) {
            val wallets = walletsResponse.payload() ?: emptyList()
            println("✓ Found ${wallets.size} token(s):")
            wallets.forEach { wallet ->
                if (wallet.balance > 0) {
                    println("  - ${wallet.token}: ${wallet.balance}")
                }
            }
        }
        
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    }
    
    println()
    println("=== Token Operations Demo Complete ===")
}