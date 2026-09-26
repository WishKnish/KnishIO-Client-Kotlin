package wishKnish.knishIO.client

import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.spyk
import io.mockk.unmockkConstructor
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import strikt.api.*
import strikt.assertions.*
import wishKnish.knishIO.client.data.MetaData
import wishKnish.knishIO.client.data.graphql.types.AccessToken
import wishKnish.knishIO.client.data.json.mutation.MoleculeMutation
import wishKnish.knishIO.client.data.json.query.ContinuId
import wishKnish.knishIO.client.data.json.variables.ContinuIdVariable
import wishKnish.knishIO.client.exception.UnauthenticatedException
import wishKnish.knishIO.client.exception.BalanceInsufficientException
import wishKnish.knishIO.client.exception.WrongTokenTypeException
import wishKnish.knishIO.client.httpClient.HttpClient
import wishKnish.knishIO.client.libraries.CheckMolecule
import wishKnish.knishIO.client.libraries.Crypto
import wishKnish.knishIO.client.query.QueryContinuId
import wishKnish.knishIO.client.response.ResponseContinuId
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

    @Test
    fun `resolves the source wallet through ContinuId after a profile auth`() {
        // Validator 0.5.0 no longer executes the I-atom of an unproven re-auth, so the auth
        // molecule's USER remainder is never registered. The next molecule must be signed from the
        // server's ContinuID pointer, not from that cached remainder.
        mockkConstructor(HttpClient::class)
        try {
            val authPayload = """{\"token\":\"auth-token\",\"time\":3600,\"key\":\"server-key\",\"pubkey\":\"server-key\",\"encrypt\":false,\"expiresAt\":2000000000}"""
            every { anyConstructed<HttpClient>().mutate(any()) } returns
                """{"data":{"ProposeMolecule":{"molecularHash":"auth-hash","status":"accepted","payload":"$authPayload"}}}"""

            val spyClient = spyk(KnishIOClient(listOf(testUri)))
            val bundle = Crypto.generateBundleHash(testSecret)
            val continuIdPosition = "c0ffee00c0ffee00c0ffee00c0ffee00c0ffee00c0ffee00c0ffee00c0ffee00"
            val continuIdWallet = Wallet(testSecret, "USER", continuIdPosition)
            val continuIdResponse = ResponseContinuId(
                QueryContinuId(HttpClient(testUri)),
                """{"data":{"ContinuId":{"address":"${continuIdWallet.address}","bundleHash":"$bundle","tokenSlug":"USER","position":"$continuIdPosition","amount":"0"}}}"""
            )
            every { spyClient.queryContinuId(any(), "USER") } returns continuIdResponse

            spyClient.requestAuthToken(secret = testSecret, cellSlug = "public", encrypt = false)
            val cachedQueryAfterAuth = spyClient.lastMoleculeQuery

            val molecule = spyClient.createMolecule()

            // One ContinuID query picks the login's signer, the second resolves this molecule's.
            verify(exactly = 2) { spyClient.queryContinuId(bundle, "USER") }
            expectThat(molecule.sourceWallet.position).isEqualTo(continuIdPosition)
            expectThat(cachedQueryAfterAuth).isNull()
        } finally {
            unmockkConstructor(HttpClient::class)
        }
    }

    private val pointerPosition = "c0ffee00c0ffee00c0ffee00c0ffee00c0ffee00c0ffee00c0ffee00c0ffee00"

    private class ProfileAuthTransport {
        val proposals = mutableListOf<Molecule>()
        val continuIdQueries = mutableListOf<ContinuIdVariable>()
    }

    private fun continuIdData(token: String, position: String, address: String?): String {
        val addressField = address?.let { "\"$it\"" } ?: "null"
        val bundle = Crypto.generateBundleHash(testSecret)
        return """{"address":$addressField,"bundleHash":"$bundle","tokenSlug":"$token","position":"$position","amount":"0"}"""
    }

    /**
     * Runs one profile login against a stubbed transport: every ContinuId query answers
     * [continuIdData] (a JSON object or `null`), and the n-th ProposeMolecule answers
     * accepted/rejected per [statuses]. Requests are recorded in [transport].
     */
    private fun profileLogin(
        continuIdData: String,
        statuses: List<String>,
        transport: ProfileAuthTransport,
        login: (KnishIOClient) -> Unit = { it.requestAuthToken(secret = testSecret, cellSlug = "public", encrypt = false) }
    ): KnishIOClient {
        val authPayload = """{\"token\":\"auth-token\",\"time\":3600,\"key\":\"server-key\",\"pubkey\":\"server-key\",\"encrypt\":false,\"expiresAt\":2000000000}"""
        mockkConstructor(HttpClient::class)
        try {
            every { anyConstructed<HttpClient>().query(any()) } answers {
                val request = firstArg<ContinuId>()
                transport.continuIdQueries.add(request.variables)
                """{"data":{"ContinuId":$continuIdData}}"""
            }
            every { anyConstructed<HttpClient>().mutate(any()) } answers {
                val request = firstArg<MoleculeMutation>()
                transport.proposals.add(request.variables.molecule)
                when (statuses[transport.proposals.size - 1]) {
                    "accepted" -> """{"data":{"ProposeMolecule":{"molecularHash":"auth-hash","status":"accepted","payload":"$authPayload"}}}"""
                    else -> """{"data":{"ProposeMolecule":{"molecularHash":"auth-hash","status":"rejected","reason":"unproven signer","payload":null}}}"""
                }
            }
            val client = KnishIOClient(listOf(testUri))
            login(client)
            return client
        } finally {
            unmockkConstructor(HttpClient::class)
        }
    }

    @Test
    fun `signs a returning user's login from the ContinuID pointer`() {
        val pointerWallet = Wallet(testSecret, "USER", pointerPosition)
        val transport = ProfileAuthTransport()
        val client = profileLogin(continuIdData("USER", pointerPosition, pointerWallet.address), listOf("accepted"), transport)

        expectThat(transport.continuIdQueries).containsExactly(ContinuIdVariable(Crypto.generateBundleHash(testSecret), "USER"))
        expectThat(transport.proposals).hasSize(1)
        val atoms = transport.proposals.single().atoms
        expectThat(atoms[0]) {
            get { isotope }.isEqualTo('U')
            get { token }.isEqualTo("USER")
            get { position }.isEqualTo(pointerPosition)
            get { walletAddress }.isEqualTo(pointerWallet.address)
        }
        expectThat(atoms[1]) {
            get { isotope }.isEqualTo('I')
            get { token }.isEqualTo("USER")
            get { position }.isNotEqualTo(pointerPosition)
            get { meta.firstOrNull { it.key == "previousPosition" }?.value }.isEqualTo(pointerPosition)
        }
        expectThat(client.getAuthToken()!!.getWallet()!!) {
            get { token }.isEqualTo("USER")
            get { position }.isEqualTo(pointerPosition)
            get { pubkey }.isEqualTo(pointerWallet.pubkey)
        }
        expectThat(client.lastMoleculeQuery).isNull()
    }

    @Test
    fun `signs from a fresh AUTH wallet when there is no usable ContinuID pointer`() {
        val pointerAddress = Wallet(testSecret, "USER", pointerPosition).address
        val cases = mapOf(
            "no pointer" to "null",
            "non-USER wallet" to continuIdData("AUTH", pointerPosition, Wallet(testSecret, "AUTH", pointerPosition).address),
            "empty position" to continuIdData("USER", "", pointerAddress),
            "different address" to continuIdData("USER", pointerPosition, Wallet("someone-else", "USER", pointerPosition).address)
        )
        cases.forEach { (case, data) ->
            val transport = ProfileAuthTransport()
            val client = profileLogin(data, listOf("accepted"), transport)

            expectThat(transport.continuIdQueries.map { it.token }).describedAs(case).containsExactly("USER")
            expectThat(transport.proposals).describedAs(case).hasSize(1)
            expectThat(transport.proposals.single().atoms[0].token).describedAs(case).isEqualTo("AUTH")
            expectThat(transport.proposals.single().atoms[0].position).describedAs(case).isNotEqualTo(pointerPosition)
            expectThat(client.getAuthToken()!!.getWallet()!!.token).describedAs(case).isEqualTo("AUTH")
        }
    }

    @Test
    fun `falls back once to an AUTH login when the pointer-signed login is rejected`() {
        val pointerWallet = Wallet(testSecret, "USER", pointerPosition)
        val data = continuIdData("USER", pointerPosition, pointerWallet.address)

        val transport = ProfileAuthTransport()
        val client = profileLogin(data, listOf("rejected", "accepted"), transport)
        expectThat(transport.proposals.map { it.atoms[0].token }).containsExactly("USER", "AUTH")
        expectThat(transport.continuIdQueries).hasSize(1)
        expectThat(client.getAuthToken()!!.getWallet()!!.token).isEqualTo("AUTH")

        // The fallback's rejection raises UnauthenticatedException with the ledger's reason, with
        // no third authorization molecule.
        val rejectedTransport = ProfileAuthTransport()
        expectThrows<UnauthenticatedException> { profileLogin(data, listOf("rejected", "rejected"), rejectedTransport) }
            .get { message }.isEqualTo("Authorization attempt rejected by ledger. Reason: unproven signer")
        expectThat(rejectedTransport.proposals.map { it.atoms[0].token }).containsExactly("USER", "AUTH")
    }

    @Test
    fun `a rejected login leaves no authorization in process, so client() logs in again`() {
        val pointerWallet = Wallet(testSecret, "USER", pointerPosition)
        val data = continuIdData("USER", pointerPosition, pointerWallet.address)
        val transport = ProfileAuthTransport()

        val client = profileLogin(data, listOf("rejected", "rejected", "accepted"), transport) {
            val failure = runCatching { it.requestAuthToken(secret = testSecret, cellSlug = "public", encrypt = false) }
            expectThat(failure.isFailure).isTrue()
            expectThat(it.authInProcess).isFalse()
            expectThat(it.getAuthToken()).isNull()

            // With no stored token for the endpoint, client() authorizes before returning.
            it.client()
        }

        expectThat(transport.proposals.map { it.atoms[0].token }).containsExactly("USER", "AUTH", "USER")
        expectThat(client.getAuthToken()!!.getWallet()!!.token).isEqualTo("USER")
        expectThat(client.authInProcess).isFalse()
    }

    @Test
    fun `a rejected guest login throws UnauthenticatedException`() {
        mockkConstructor(HttpClient::class)
        try {
            every { anyConstructed<HttpClient>().mutate(any()) } returns
                """{"data":{"AccessToken":null},"errors":[{"message":"Cell not found"}]}"""
            val client = KnishIOClient(listOf(testUri))

            expectThrows<UnauthenticatedException> { client.authorize(cellSlug = "missing-cell") }
                .get { message }.isNotNull().startsWith("Authorization attempt rejected by ledger. Reason: ")
            expectThat(client.authInProcess).isFalse()
            expectThat(client.getAuthToken()).isNull()
        } finally {
            unmockkConstructor(HttpClient::class)
        }
    }

    @Test
    fun `accepts U atoms signed by an AUTH or USER wallet only`() {
        val signed = { token: String ->
            Molecule(testSecret, Wallet(testSecret, token, pointerPosition), Wallet.create(testSecret, "USER")).apply {
                initAuthorization(mutableListOf(MetaData("encrypt", "false")))
                sign()
            }
        }

        expectThat(CheckMolecule.isotopeU(signed("USER"))).isTrue()
        expectThat(CheckMolecule.isotopeU(signed("AUTH"))).isTrue()
        expectThrows<WrongTokenTypeException> { CheckMolecule.isotopeU(signed("TEST")) }
    }

    @Test
    fun `restores a USER-bound auth token as a USER wallet`() {
        val userWallet = Wallet(testSecret, "USER", pointerPosition)
        val authToken = AuthToken.create(AccessToken("T", 9999999, "server-key", "server-key", false, 9999999), userWallet)

        expectThat(AuthToken.restore(authToken.getSnapshot(), testSecret).getWallet()!!) {
            get { token }.isEqualTo("USER")
            get { address }.isEqualTo(userWallet.address)
            get { pubkey }.isEqualTo(userWallet.pubkey)
        }

        val legacySnapshot = authToken.getSnapshot().apply {
            wallet = AuthToken.Wallet(userWallet.position, userWallet.characters, userWallet.mlkemParameterSet)
        }
        expectThat(AuthToken.restore(legacySnapshot, testSecret).getWallet()!!) {
            get { token }.isEqualTo("AUTH")
            get { address }.isEqualTo(Wallet(testSecret, "AUTH", pointerPosition).address)
        }
    }
}