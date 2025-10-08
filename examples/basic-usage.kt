package examples

import wishKnish.knishIO.client.KnishIOClient
import wishKnish.knishIO.client.libraries.Strings
import java.net.URI

/**
 * Basic Usage Example
 * 
 * This example demonstrates the simplest way to get started with the Knish.IO Kotlin SDK.
 * It covers authentication, querying balances, and basic wallet operations.
 */
fun main() {
    // Step 1: Initialize the client
    // In production, use your actual node URIs
    val nodeUri = System.getenv("KNISHIO_NODE_URI") ?: "https://node.wishknish.com/graphql"
    
    val client = KnishIOClient(
        nodeUris = listOf(URI(nodeUri)),
        encrypt = true  // Enable encryption for secure communication
    )
    
    // Optional: Set a specific cell for your application
    val cellSlug = System.getenv("KNISHIO_CELL") ?: "myapp"
    client.setCellSlug(cellSlug)
    
    // Step 2: Generate or retrieve your secret
    // IMPORTANT: In production, use secure key management!
    val secret = System.getenv("KNISHIO_SECRET") ?: generateDemoSecret()
    
    println("=== Knish.IO Basic Usage Demo ===")
    println("Node: $nodeUri")
    println("Cell: $cellSlug")
    println()
    
    try {
        // Step 3: Authenticate with the node
        println("Authenticating...")
        val authResponse = client.requestAuthToken(secret)
        
        if (authResponse.success()) {
            println("✓ Authentication successful!")
            println("  Token: ${authResponse.payload()?.token}")
            println("  Wallet: ${authResponse.payload()?.wallet?.address}")
            println()
        } else {
            println("✗ Authentication failed: ${authResponse.reason()}")
            return
        }
        
        // Step 4: Query wallet balance
        println("Querying USER token balance...")
        val balanceResponse = client.queryBalance("USER")
        
        if (balanceResponse.success()) {
            val balance = balanceResponse.payload()?.balance ?: 0
            println("✓ Balance query successful!")
            println("  USER balance: $balance")
            println()
        } else {
            println("✗ Balance query failed: ${balanceResponse.reason()}")
        }
        
        // Step 5: List all wallets in the bundle
        println("Listing wallets...")
        val walletsResponse = client.queryWallets()
        
        if (walletsResponse.success()) {
            val wallets = walletsResponse.payload() ?: emptyList()
            println("✓ Found ${wallets.size} wallet(s):")
            
            wallets.forEach { wallet ->
                println("  - Token: ${wallet.token}")
                println("    Address: ${wallet.address}")
                println("    Balance: ${wallet.balance}")
            }
            println()
        } else {
            println("✗ Wallet query failed: ${walletsResponse.reason()}")
        }
        
        // Step 6: Query wallet bundle information
        println("Querying wallet bundle...")
        val bundleResponse = client.queryBundle(null) // Use current bundle
        
        if (bundleResponse.success()) {
            val bundle = bundleResponse.payload()
            println("✓ Bundle information:")
            println("  Hash: ${bundle?.bundleHash}")
            println("  Created: ${bundle?.createdAt}")
            println()
        }
        
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    }
    
    println("=== Demo Complete ===")
}

/**
 * Generate a demo secret for testing
 * WARNING: Never use this in production! Always use secure key generation.
 */
fun generateDemoSecret(): String {
    println("⚠️  Generating demo secret (NOT FOR PRODUCTION USE)")
    return Strings.generateSecret()
}