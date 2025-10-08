/**
 * Knish.IO Kotlin SDK Complete Integration Test
 *
 * This test exactly mirrors the JavaScript SDK integration test pattern
 * to validate true cross-platform API compatibility. No simplification -
 * implements identical test scenarios with proper API usage.
 * 
 * Usage:
 *   ./gradlew test --tests "*FullIntegrationTest*" -PgraphqlUrl=https://testnet.knish.io/graphql
 *   KNISHIO_API_URL=https://localhost:8000/graphql ./gradlew test --tests "*FullIntegrationTest*"
 */

package wishKnish.knishIO.client

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import wishKnish.knishIO.client.data.MetaData
import wishKnish.knishIO.client.data.graphql.types.TokenUnit
import wishKnish.knishIO.client.libraries.Crypto
import java.io.File
import java.net.URI
import java.time.Instant
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FullIntegrationTest {
    
    // Configuration matching JavaScript integration test exactly
    private val graphqlUrl = System.getProperty("graphqlUrl") 
        ?: System.getenv("KNISHIO_API_URL") 
        ?: "http://localhost:8000/graphql"
    
    private val cellSlug = System.getProperty("cellSlug")
        ?: System.getenv("KNISHIO_CELL_SLUG")
        ?: "KOTLIN_FULL_INTEGRATION"
    
    private val timeout = 30000
    private val retries = 3
    private val retryDelay = 1000L
    
    // Test configuration matching JavaScript patterns exactly
    private val testSecret = Crypto.generateSecret("KOTLIN_INTEGRATION_AUTH")
    private val testConfig = mapOf(
        "metadata" to mapOf(
            "metaType" to "KotlinIntegrationTest",
            "metaId" to "KOTLIN_TEST_${System.currentTimeMillis()}_${Random.nextInt(1000)}",
            "metadata" to mapOf(
                "test_name" to "Kotlin SDK Integration Test",
                "timestamp" to Instant.now().toString(),
                "sdk_version" to "1.0.0-RC1",
                "language" to "Kotlin",
                "platform" to "JVM", 
                "description" to "Live integration test for KnishIOClient functionality"
            )
        ),
        "tokens" to mapOf(
            "testTokenSlug" to "KOTLIN_INT_TEST_${System.currentTimeMillis().toString(36).uppercase()}",
            "initialSupply" to 12000,
            "tokenMeta" to mapOf(
                "name" to "Kotlin Integration Test Token",
                "symbol" to "KITT",
                "fungibility" to "fungible", 
                "supply" to "limited",
                "decimals" to "2"
            )
        ),
        "transfers" to mapOf(
            "transferAmount" to 120,
            "recipientSecret" to Crypto.generateSecret("KOTLIN_INTEGRATION_RECIPIENT")
        )
    )
    
    private val results = mutableMapOf<String, Any>()
    
    @BeforeAll
    fun setup() {
        println("🦄 Knish.IO Kotlin SDK Complete Integration Test")
        println("═".repeat(60))
        println("🌐 Server: $graphqlUrl")
        println("📱 Cell: $cellSlug") 
        println("🔧 Language: Kotlin (Exact JS Pattern)")
        println("⚡ Method: Full API compatibility validation")
        println("🎯 Goal: No simplification - expose real compatibility issues")
        
        results["sdk"] = "Kotlin"
        results["testType"] = "Complete Integration (JS Pattern)"
        results["version"] = "1.0.0-RC1"
        results["timestamp"] = Instant.now().toString()
        results["server"] = mapOf(
            "url" to graphqlUrl,
            "cellSlug" to cellSlug
        )
        results["tests"] = mutableMapOf<String, Any>()
        results["language"] = "Kotlin"
        results["platform"] = "JVM"
        results["pattern"] = "JavaScript SDK Mirror"
    }
    
    // Helper functions
    private fun logTest(testName: String, passed: Boolean, errorDetail: String? = null, responseTime: Long? = null) {
        val status = if (passed) "✅ PASS" else "❌ FAIL" 
        val timeStr = responseTime?.let { " (${it}ms)" } ?: ""
        println("  $status: $testName$timeStr")
        if (!passed && errorDetail != null) {
            println("    $errorDetail")
        }
    }
    
    private fun logSection(sectionName: String) {
        println("\n$sectionName")
        println("═".repeat(sectionName.length + 4))
    }

    // Retry logic matching JavaScript executeWithRetry pattern
    private suspend fun <T> executeWithRetry(
        operationName: String, 
        maxRetries: Int = retries,
        operation: suspend () -> T
    ): Pair<T?, Long> {
        val startTime = System.currentTimeMillis()
        
        for (attempt in 1..maxRetries) {
            try {
                val result = operation()
                val responseTime = System.currentTimeMillis() - startTime
                return Pair(result, responseTime)
            } catch (e: Exception) {
                if (attempt == maxRetries) {
                    throw Exception("$operationName failed after $maxRetries attempts: ${e.message}")
                }
                println("    Attempt $attempt/$maxRetries failed: ${e.message}")
                delay(retryDelay)
            }
        }
        throw Exception("Unreachable code")
    }
    
    @Test
    @Order(1)
    fun testClientConnectivityAndAuthentication() = runBlocking {
        logSection("1. Kotlin Client Connectivity and Authentication Test")
        
        val testResults = mutableMapOf<String, Any>()
        
        try {
            // Test 1.1: Client initialization (convert JS object pattern to Kotlin)
            val uris = listOf(URI(graphqlUrl))
            val client = KnishIOClient(uris = uris, logging = false)
            client.cellSlug = cellSlug
            
            logTest("Kotlin client initialization", true)
            
            // Test 1.2: Authentication exactly as JavaScript does
            val (authResponse, authTime) = executeWithRetry("Kotlin Authentication") {
                client.requestAuthToken(
                    secret = testSecret,
                    cellSlug = cellSlug, 
                    encrypt = false
                )
            }
            
            // Check response success - adapt JS pattern to Kotlin response type
            val authSuccess = authResponse != null
            logTest("Server authentication (Kotlin)", authSuccess,
                if (!authSuccess) "Auth failed: No auth token returned" else null, authTime)
            
            if (authSuccess) {
                // Test 1.3: Basic connectivity with query (matching JS pattern)
                val (balanceResponse, queryTime) = executeWithRetry("Basic connectivity query") {
                    client.queryBalance("USER")
                }
                
                val querySuccess = balanceResponse?.success() == true
                logTest("Basic server query (Kotlin)", querySuccess,
                    if (!querySuccess) "Failed to execute basic query" else null, queryTime)
                
                testResults["passed"] = authSuccess && querySuccess
                testResults["authenticationTime"] = authTime
                testResults["queryTime"] = queryTime
                testResults["language"] = "Kotlin"
                testResults["pattern"] = "JavaScript Mirror"
            } else {
                testResults["passed"] = false
                testResults["error"] = "Authentication failed"
            }
            
        } catch (e: Exception) {
            logTest("Kotlin client connectivity", false, e.message)
            testResults["passed"] = false
            testResults["error"] = e.message as Any
        }
        
        (results["tests"] as MutableMap<String, Any>)["connectivity"] = testResults
        
        // Store client for other tests if successful
        if (testResults["passed"] == true) {
            results["client"] = "initialized"
        }
    }
    
    @Test
    @Order(2)
    fun testMetadataOperations() = runBlocking {
        logSection("2. Kotlin Metadata Operations Integration Test")
        
        val testResults = mutableMapOf<String, Any>()
        
        try {
            // Re-create client (following JS pattern)
            val uris = listOf(URI(graphqlUrl))
            val client = KnishIOClient(uris = uris, logging = false)
            client.cellSlug = cellSlug
            client.requestAuthToken(secret = testSecret, cellSlug = cellSlug)
            
            val metadataConfig = testConfig["metadata"] as Map<String, Any>
            val metadataObject = metadataConfig["metadata"] as Map<String, String>
            
            // Convert JavaScript metadata object to Kotlin MetaData list
            val metaList = mutableListOf<MetaData>()
            metadataObject.forEach { (key, value) ->
                metaList.add(MetaData(key, value))
            }
            
            // Test 2.1: Create metadata via KnishIOClient (exact JS pattern)
            val (createResponse, createTime) = executeWithRetry("Create metadata") {
                client.createMeta(
                    metaType = metadataConfig["metaType"] as String,
                    metaId = metadataConfig["metaId"] as String,
                    meta = metaList
                )
            }
            
            val createSuccess = createResponse?.success() == true
            val molecularHash = createResponse?.molecule()?.molecularHash
            
            logTest("Create metadata via Kotlin client", createSuccess,
                if (!createSuccess) "Creation failed: ${createResponse?.reason() ?: "Unknown error"}" else null,
                createTime)
            
            if (!createSuccess) {
                testResults["passed"] = false
                testResults["error"] = "Failed to create metadata"
                (results["tests"] as MutableMap<String, Any>)["metadata"] = testResults
                return@runBlocking
            }
            
            // Wait for propagation (matching JS pattern)
            delay(2000)
            
            // Test 2.2: Query metadata back via KnishIOClient (exact JS pattern)  
            val (queryResponse, queryTime) = executeWithRetry("Query metadata") {
                client.queryMeta(
                    metaType = metadataConfig["metaType"] as String,
                    metaIds = listOf(metadataConfig["metaId"] as String)
                )
            }
            
            val querySuccess = queryResponse != null && queryResponse.isNotEmpty()
            val retrievedMeta = if (querySuccess) queryResponse.firstOrNull() else null
            
            logTest("Query metadata via Kotlin client", querySuccess,
                if (!querySuccess) "No metadata retrieved" else null, queryTime)
            
            // Test 2.3: Validate metadata content (exact JS validation pattern)
            var contentValid = false
            if (querySuccess && retrievedMeta != null) {
                // Check if retrieved metadata contains original key-value pairs
                val originalKeys = metadataObject.keys
                contentValid = retrievedMeta.metas?.any { meta ->
                    originalKeys.any { key -> meta.key == key && metadataObject[key] == meta.value }
                } ?: false
            }
            
            logTest("Kotlin metadata content validation", contentValid,
                if (!contentValid) "Retrieved metadata does not match created metadata" else null)
            
            testResults["passed"] = createSuccess && querySuccess && contentValid
            testResults["molecularHash"] = molecularHash
            testResults["createTime"] = createTime
            testResults["queryTime"] = queryTime
            testResults["metaCount"] = queryResponse.size
            testResults["contentMatched"] = contentValid
            testResults["language"] = "Kotlin"
            testResults["pattern"] = "JavaScript Mirror"
            
        } catch (e: Exception) {
            logTest("Kotlin metadata operations", false, e.message)
            testResults["passed"] = false
            testResults["error"] = e.message as Any
        }
        
        (results["tests"] as MutableMap<String, Any>)["metadata"] = testResults
    }
    
    @Test
    @Order(3)
    fun testTokenOperations() = runBlocking {
        logSection("3. Kotlin Token Operations Integration Test")
        
        val testResults = mutableMapOf<String, Any>()
        
        try {
            // Re-create client (following JS pattern)
            val uris = listOf(URI(graphqlUrl))
            val client = KnishIOClient(uris = uris, logging = false)
            client.cellSlug = cellSlug
            client.requestAuthToken(secret = testSecret, cellSlug = cellSlug)
            
            val tokensConfig = testConfig["tokens"] as Map<String, Any>
            val tokenMetaObject = tokensConfig["tokenMeta"] as Map<String, String>
            
            // Convert JavaScript token metadata object to Kotlin MetaData list
            val tokenMetaList = mutableListOf<MetaData>()
            tokenMetaObject.forEach { (key, value) ->
                tokenMetaList.add(MetaData(key, value))
            }
            
            // Test 3.1: Create new token via KnishIOClient (exact JS pattern)
            val (tokenResponse, createTime) = executeWithRetry("Create token") {
                client.createToken(
                    token = tokensConfig["testTokenSlug"] as String,
                    amount = tokensConfig["initialSupply"] as Int,
                    meta = tokenMetaList
                )
            }
            
            val tokenSuccess = tokenResponse?.success() == true
            val tokenMolecularHash = tokenResponse?.molecule()?.molecularHash
            
            logTest("Create token via Kotlin client", tokenSuccess,
                if (!tokenSuccess) "Token creation failed: ${tokenResponse?.reason() ?: "Unknown error"}" else null,
                createTime)
            
            if (!tokenSuccess) {
                testResults["passed"] = false
                testResults["error"] = "Failed to create token"
                (results["tests"] as MutableMap<String, Any>)["tokens"] = testResults
                return@runBlocking
            }
            
            // Wait for propagation (matching JS pattern)
            delay(2000)
            
            // Test 3.2: Query balance for the new token (exact JS pattern)
            val (balanceResponse, balanceTime) = executeWithRetry("Query token balance") {
                client.queryBalance(tokensConfig["testTokenSlug"] as String)
            }
            
            val balancePayload = balanceResponse?.payload()
            val balanceSuccess = balancePayload != null
            val actualBalance = "0" // TODO: Extract actual balance from Kotlin response
            
            logTest("Query token balance (Kotlin)", balanceSuccess,
                if (!balanceSuccess) "No balance found for created token" else null, balanceTime)
            
            // Test 3.3: Validate balance amount (exact JS validation pattern)
            val expectedBalance = tokensConfig["initialSupply"] as Int
            val actualBalanceInt = actualBalance.toDoubleOrNull()?.toInt() ?: 0
            val balanceMatches = balanceSuccess && actualBalanceInt == expectedBalance
            
            logTest("Kotlin balance amount validation", balanceMatches,
                if (!balanceMatches) "Expected $expectedBalance, got $actualBalance" else null)
            
            testResults["passed"] = tokenSuccess && balanceSuccess && balanceMatches
            testResults["tokenSlug"] = tokensConfig["testTokenSlug"]
            testResults["molecularHash"] = tokenMolecularHash
            testResults["createTime"] = createTime
            testResults["balanceTime"] = balanceTime
            testResults["expectedBalance"] = expectedBalance
            testResults["actualBalance"] = actualBalance
            testResults["balanceMatches"] = balanceMatches
            testResults["language"] = "Kotlin"
            testResults["pattern"] = "JavaScript Mirror"
            
            // Store token slug for transfer test
            if (testResults["passed"] == true) {
                results["tokenSlug"] = tokensConfig["testTokenSlug"] as Any
            }
            
        } catch (e: Exception) {
            logTest("Kotlin token operations", false, e.message)
            testResults["passed"] = false
            testResults["error"] = e.message as Any
        }
        
        (results["tests"] as MutableMap<String, Any>)["tokens"] = testResults
    }
    
    @Test
    @Order(4)
    fun testWalletAndTransferOperations() = runBlocking {
        logSection("4. Kotlin Wallet and Transfer Operations Test")
        
        val testResults = mutableMapOf<String, Any>()
        
        try {
            val tokenSlug = results["tokenSlug"] as? String
            
            if (tokenSlug == null) {
                testResults["passed"] = false
                testResults["error"] = "No token available for transfer"
                (results["tests"] as MutableMap<String, Any>)["transfers"] = testResults
                return@runBlocking
            }
            
            // Re-create main client
            val uris = listOf(URI(graphqlUrl))
            val client = KnishIOClient(uris = uris, logging = false)
            client.cellSlug = cellSlug
            client.requestAuthToken(secret = testSecret, cellSlug = cellSlug)
            
            val transferConfig = testConfig["transfers"] as Map<String, Any>
            
            // Test 4.1: Create recipient client (exact JS pattern)
            val recipientClient = KnishIOClient(uris = uris, logging = false)
            recipientClient.cellSlug = cellSlug
            
            // Test 4.2: Authenticate recipient (exact JS pattern)
            val (recipientAuth, _) = executeWithRetry("Recipient authentication") {
                recipientClient.requestAuthToken(
                    secret = transferConfig["recipientSecret"] as String,
                    cellSlug = cellSlug,
                    encrypt = false
                )
            }
            
            val recipientAuthSuccess = recipientAuth != null
            logTest("Kotlin recipient authentication", recipientAuthSuccess)
            
            if (!recipientAuthSuccess) {
                testResults["passed"] = false
                testResults["error"] = "Recipient authentication failed"
                (results["tests"] as MutableMap<String, Any>)["transfers"] = testResults
                return@runBlocking
            }
            
            // Test 4.3: Create recipient wallet (exact JS pattern)
            val (walletResponse, walletTime) = executeWithRetry("Create recipient wallet") {
                recipientClient.createWallet(tokenSlug)
            }
            
            val walletSuccess = walletResponse?.success() == true
            logTest("Create recipient wallet (Kotlin)", walletSuccess,
                if (!walletSuccess) "Wallet creation failed: ${walletResponse?.reason() ?: "Unknown error"}" else null,
                walletTime)
            
            if (!walletSuccess) {
                testResults["passed"] = false
                testResults["error"] = "Failed to create recipient wallet"
                (results["tests"] as MutableMap<String, Any>)["transfers"] = testResults
                return@runBlocking
            }
            
            // Wait for wallet propagation (matching JS pattern)
            delay(2000)
            
            // Test 4.4: Execute transfer via transferToken (exact JS pattern)
            val recipientBundle = recipientClient.bundle()
            
            val (transferResponse, transferTime) = executeWithRetry("Execute token transfer") {
                client.transferToken(
                    recipient = recipientBundle,
                    token = tokenSlug,
                    amount = transferConfig["transferAmount"] as Int
                )
            }
            
            val transferSuccess = transferResponse?.success() == true
            val transferMolecularHash = transferResponse?.molecule()?.molecularHash
            
            logTest("Execute token transfer (Kotlin)", transferSuccess,
                if (!transferSuccess) "Transfer failed: ${transferResponse?.reason() ?: "Unknown error"}" else null,
                transferTime)
            
            if (!transferSuccess) {
                testResults["passed"] = false
                testResults["error"] = "Token transfer failed"
                (results["tests"] as MutableMap<String, Any>)["transfers"] = testResults
                return@runBlocking
            }
            
            // Wait for transfer propagation (matching JS pattern)
            delay(3000)
            
            // Test 4.5: Verify recipient balance (exact JS validation pattern)
            val (recipientBalance, balanceTime) = executeWithRetry("Query recipient balance") {
                recipientClient.queryBalance(tokenSlug)
            }
            
            val recipientBalancePayload = recipientBalance?.payload()
            val balanceVerifySuccess = recipientBalancePayload != null
            val receivedAmount = 0 // TODO: Extract received amount from Kotlin balance response
            val expectedAmount = transferConfig["transferAmount"] as Int
            val balanceCorrect = receivedAmount == expectedAmount
            
            logTest("Verify Kotlin recipient balance", balanceVerifySuccess && balanceCorrect,
                if (!balanceCorrect) "Expected $expectedAmount, received $receivedAmount" else null,
                balanceTime)
            
            testResults["passed"] = transferSuccess && balanceVerifySuccess && balanceCorrect
            testResults["transferAmount"] = expectedAmount
            testResults["receivedAmount"] = receivedAmount
            testResults["molecularHash"] = transferMolecularHash
            testResults["walletTime"] = walletTime
            testResults["transferTime"] = transferTime
            testResults["balanceTime"] = balanceTime
            testResults["balanceCorrect"] = balanceCorrect
            testResults["language"] = "Kotlin"
            testResults["pattern"] = "JavaScript Mirror"
            
        } catch (e: Exception) {
            logTest("Kotlin wallet and transfer operations", false, e.message)
            testResults["passed"] = false
            testResults["error"] = e.message as Any
        }
        
        (results["tests"] as MutableMap<String, Any>)["transfers"] = testResults
    }
    
    @AfterAll
    fun cleanup() {
        runBlocking {
            // Save results matching JavaScript integration test format
            val resultsDir = System.getenv("KNISHIO_SHARED_RESULTS") 
                ?: "../shared-test-results"
            
            val resultsFile = File(resultsDir)
            if (!resultsFile.exists()) {
                resultsFile.mkdirs()
            }
            
            // Calculate final results
            val tests = results["tests"] as Map<String, Map<String, Any>>
            val totalTests = tests.size
            val passedTests = tests.values.count { it["passed"] == true }
            val failedTests = totalTests - passedTests
            
            results["networkStats"] = mapOf(
                "totalTests" to totalTests,
                "passedTests" to passedTests,
                "failedTests" to failedTests
            )
            results["overallSuccess"] = passedTests == totalTests
            
            val outputFile = File(resultsFile, "kotlin-full-integration-results.json")
            val jsonString = Json { prettyPrint = true }.encodeToString(
                JsonElement.serializer(), 
                Json.parseToJsonElement(Json.encodeToString(results))
            )
            outputFile.writeText(jsonString)
            
            println("\n📁 Kotlin results saved to: ${outputFile.absolutePath}")
            
            // Print summary matching JavaScript pattern
            logSection("KOTLIN COMPLETE INTEGRATION TEST SUMMARY")
            
            println("SDK: ${results["sdk"]} v${results["version"]}")
            println("Language: Kotlin (JavaScript Pattern Mirror)")
            println("Platform: JVM with API Compatibility")
            println("Server: $graphqlUrl")
            println("Pattern: Exact JavaScript SDK integration test replication")
            
            val color = if (passedTests == totalTests) "✅" else "❌"
            println("\nTests Passed: $passedTests/$totalTests")
            
            if (failedTests > 0) {
                println("\nFailed Tests:")
                tests.forEach { (testName, testResult) ->
                    if (testResult["passed"] != true) {
                        println("  - $testName: ${testResult["error"] ?: "Test failed"}")
                    }
                }
            }
            
            println("\n═".repeat(60))
            println("$color Kotlin Complete Integration tests ${if (passedTests == totalTests) "PASSED" else "FAILED"}")
            println("🎯 This test reveals real cross-platform compatibility issues vs simplification")
            
            // Assert for JUnit
            if (passedTests < totalTests) {
                val failureMessage = "Kotlin integration tests failed: $failedTests/$totalTests tests failed. " +
                    "Failed tests: ${tests.filterValues { it["passed"] != true }.keys.joinToString(", ")}"
                fail(failureMessage)
            }
        }
    }
}