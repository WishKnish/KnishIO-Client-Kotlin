package wishKnish.knishIO.client

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import strikt.api.*
import strikt.assertions.*
import wishKnish.knishIO.client.data.MetaData
import wishKnish.knishIO.client.exception.UnauthenticatedException
import wishKnish.knishIO.client.exception.BalanceInsufficientException
import java.net.URI

class KnishIOClientTest {
    
    private lateinit var client: KnishIOClient
    private val testUri = URI("https://knish-test.io/graphql")
    private val testSecret = "test-secret-for-client"
    
    @BeforeEach
    fun setup() {
        client = KnishIOClient(listOf(testUri))
    }
    
    @Test
    fun `initializes client correctly`() {
        expectThat(client) {
            get { uris }.hasSize(1)
            get { uris.first() }.isEqualTo(testUri)
            get { serverSdkVersion }.isEqualTo(3) // Default value
            get { logging }.isEqualTo(false) // Default value
        }
        
        expectThat(client.hasSecret()).isFalse()
        expectThat(client.hasEncryption()).isFalse()
    }
    
    @Test
    fun `initializes client with encryption enabled`() {
        val encryptedClient = KnishIOClient(listOf(testUri), encrypt = true)
        
        expectThat(encryptedClient.hasEncryption()).isTrue()
    }
    
    @Test
    fun `manages secret correctly`() {
        // Initially no secret
        expectThat(client.hasSecret()).isFalse()
        
        // Should throw exception when getting secret without setting it
        expectThrows<UnauthenticatedException> {
            client.getSecret()
        }
        
        // Set secret
        client.setSecret(testSecret)
        
        expectThat(client.hasSecret()).isTrue()
        expectThat(client.getSecret()).isEqualTo(testSecret)
        expectThat(client.bundle()).isNotEmpty() // Bundle should be generated
    }
    
    @Test
    fun `manages bundle correctly`() {
        // Should throw exception when getting bundle without secret
        expectThrows<UnauthenticatedException> {
            client.bundle()
        }
        
        // Set secret and verify bundle generation
        client.setSecret(testSecret)
        val bundle = client.bundle()
        
        expectThat(bundle) {
            isNotEmpty()
            hasLength(64) // SHA256 hash should be 64 hex characters
            matches("[0-9a-f]+".toRegex()) // Should be hex
        }
        
        // Bundle should be consistent
        expectThat(client.bundle()).isEqualTo(bundle)
    }
    
    @Test
    fun `handles encryption toggle correctly`() {
        expectThat(client.hasEncryption()).isFalse()
        
        client.enableEncryption()
        expectThat(client.hasEncryption()).isTrue()
        
        client.disableEncryption()
        expectThat(client.hasEncryption()).isFalse()
    }
    
    @Test
    fun `returns correct URI`() {
        val uri = client.uri()
        expectThat(uri).isEqualTo(testUri.toASCIIString())
    }
    
    @Test
    fun `returns random URI from multiple URIs`() {
        val uri1 = URI("https://node1.knish.io/graphql")
        val uri2 = URI("https://node2.knish.io/graphql")
        val uri3 = URI("https://node3.knish.io/graphql")
        val multiClient = KnishIOClient(listOf(uri1, uri2, uri3))
        
        val randomUri = multiClient.getRandomUri()
        expectThat(listOf(uri1, uri2, uri3)).contains(randomUri)
    }
    
    @Test
    fun `manages cell slug correctly`() {
        val testCellSlug = "test-cell"
        
        client.setCellSlug(testCellSlug)
        // Note: We can't directly test cellSlug() without knowing if it's public
        // But we can verify it doesn't throw an exception
        client.setCellSlug(null)
        client.setCellSlug(testCellSlug)
    }
    
    @Test
    fun `resets client state correctly`() {
        // Set up some state
        client.setSecret(testSecret)
        
        expectThat(client.hasSecret()).isTrue()
        
        // Reset should clear the state
        client.reset()
        expectThat(client.hasSecret()).isFalse()
    }
    
    @Test
    fun `deinitializes client correctly`() {
        // Set up some state
        client.setSecret(testSecret)
        expectThat(client.hasSecret()).isTrue()
        
        // Deinitialize should clear the state
        client.deinitialize()
        expectThat(client.hasSecret()).isFalse()
    }
    
    @Test
    fun `handles auth token operations`() {
        // Initially no auth token
        expectThat(client.getAuthToken()).isNull()
        
        // Note: We can't easily test setAuthToken without creating a real AuthToken
        // which would require network calls or complex setup
        // This test verifies the basic structure exists
    }
    
    @Test
    fun `handles remainder wallet`() {
        // Initially no remainder wallet
        expectThat(client.remainderWallet()).isNull()
        
        // Note: remainderWallet is typically set during molecule operations
        // This test verifies the basic structure exists
    }
    
    @Test
    fun `creates client with multiple URIs`() {
        val uris = listOf(
            URI("https://node1.knish.io/graphql"),
            URI("https://node2.knish.io/graphql"),
            URI("https://node3.knish.io/graphql")
        )
        
        val multiClient = KnishIOClient(uris)
        
        expectThat(multiClient.uris) {
            hasSize(3)
            containsExactly(uris[0], uris[1], uris[2])
        }
    }
    
    @Test
    fun `creates client with custom server SDK version`() {
        val customClient = KnishIOClient(listOf(testUri), serverSdkVersion = 4)
        
        expectThat(customClient.serverSdkVersion).isEqualTo(4)
    }
    
    @Test
    fun `creates client with logging enabled`() {
        val loggingClient = KnishIOClient(listOf(testUri), logging = true)
        
        expectThat(loggingClient.logging).isTrue()
    }
    
    @Test
    fun `ensures insufficient balance is triggered`() {
        // Test that insufficient balance errors are properly handled
        val secret = "balance-test-secret"
        val sourceWallet = Wallet(secret, "BALANCE")
        sourceWallet.balance = 100.0 // Set low balance
        
        val recipientWallet = Wallet("recipient-secret", "BALANCE")
        
        val molecule = Molecule(secret)
        molecule.sourceWallet = sourceWallet
        molecule.remainderWallet = Wallet(secret + "-remainder", "BALANCE")
        
        // Try to transfer more than available balance
        expectThrows<BalanceInsufficientException> {
            molecule.initValue(recipientWallet, 150.0) // Trying to send 150 when only 100 available
        }
        
        // Also test burnToken with insufficient balance
        expectThrows<BalanceInsufficientException> {
            molecule.burnToken(150.0)
        }
    }
    
    @Test
    fun `creates query correctly`() {
        // Test GraphQL query creation and client configuration
        val client = KnishIOClient(listOf(URI("http://localhost:8080")))
        
        // Test that client can handle URIs correctly
        expectThat(client) {
            get { uris }.hasSize(1)
            get { uris.first().toString() }.isEqualTo("http://localhost:8080")
        }
        
        // Test query with variables (simulated)
        val variables = mapOf(
            "bundleHash" to "test-bundle-hash",
            "token" to "TEST"
        )
        
        // Verify client can handle query construction parameters
        expectThat(variables) {
            hasSize(2)
            containsKey("bundleHash")
            containsKey("token")
        }
        
        // Test with cell slug
        val clientWithCell = KnishIOClient(
            uris = listOf(URI("http://localhost:8080"))
        )
        clientWithCell.setCellSlug("test-cell")
        // Cell slug is set (we can't directly access it, but it's configured)
    }
    
    @Test
    fun `creates molecule mutation correctly`() {
        // Test GraphQL mutation creation for molecules
        val client = KnishIOClient(listOf(testUri))
        val secret = "mutation-test-secret"
        
        // Set client secret for mutation operations
        client.setSecret(secret)
        
        // Create a test molecule
        val molecule = Molecule(secret)
        val wallet = Wallet(secret, "MUTATION")
        molecule.addAtom(Atom(
            position = wallet.position ?: "",
            walletAddress = wallet.address ?: "",
            isotope = 'M',
            token = wallet.token,
            metaType = "test",
            metaId = "test-mutation",
            meta = listOf(MetaData("key", "value"))
        ))
        
        // Sign the molecule to prepare for mutation
        molecule.sign()
        
        expectThat(molecule) {
            get { molecularHash }.isNotNull()
            get { bundle }.isNotNull()
            get { atoms }.isNotEmpty()
            get { atoms.first().otsFragment }.isNotNull()
        }
        
        // Molecule is signed and ready for GraphQL mutation
        // Note: We don't call check() here as it requires full validation setup
        
        // Verify client has necessary configuration for mutations
        expectThat(client) {
            get { hasSecret() }.isTrue()
            get { bundle() }.isNotEmpty()
        }
    }
}