package wishKnish.knishIO.client

import org.junit.jupiter.api.Test
import strikt.api.*
import strikt.assertions.*
import wishKnish.knishIO.client.data.MetaData

class MoleculeTest {
    
    @Test
    fun `molecule creation should initialize with proper defaults`() {
        val secret = "test-molecule-secret"
        val molecule = Molecule(secret)
        
        expectThat(molecule) {
            get { atoms }.isEmpty()
            get { bundle }.isNotNull()
            get { status }.isNull()
            get { createdAt }.isNotEmpty()
        }
    }
    
    @Test
    fun `molecule should add atoms correctly`() {
        val molecule = Molecule("test-secret")
        val wallet = Wallet("atom-wallet-secret", "TEST")
        
        val atom = Atom(
            position = wallet.position ?: "",
            walletAddress = wallet.address ?: "",
            isotope = 'V',
            token = wallet.token,
            value = "-100.0",
            metaType = null,
            metaId = null
        )
        
        molecule.addAtom(atom)
        
        expectThat(molecule.atoms) {
            hasSize(1)
            first().isEqualTo(atom)
        }
    }
    
    @Test
    fun `molecule should validate atom balance for value transfers`() {
        val molecule = Molecule("test-secret")
        val sourceWallet = Wallet("source-secret", "TEST")
        val destWallet = Wallet("dest-secret", "TEST")
        
        // Add source atom (negative value)
        molecule.addAtom(Atom(
            position = sourceWallet.position ?: "",
            walletAddress = sourceWallet.address ?: "",
            isotope = 'V',
            token = "TEST",
            value = "-100.0"
        ))
        
        // Add destination atom (positive value)
        molecule.addAtom(Atom(
            position = destWallet.position ?: "",
            walletAddress = destWallet.address ?: "",
            isotope = 'V',
            token = "TEST",
            value = "100.0"
        ))
        
        // Balance should be zero for valid transfer
        val balance = molecule.atoms
            .filter { it.isotope == 'V' && it.token == "TEST" }
            .mapNotNull { it.value?.toDoubleOrNull() }
            .sum()
        
        expectThat(balance).isEqualTo(0.0)
    }
    
    @Test
    fun `molecule should sign correctly`() {
        val secret = "signing-secret"
        val molecule = Molecule(secret)
        val wallet = Wallet(secret, "USER")
        
        // Add a simple atom
        molecule.addAtom(Atom(
            position = wallet.position ?: "",
            walletAddress = wallet.address ?: "",
            isotope = 'M',
            token = wallet.token,
            metaType = "test",
            metaId = "test-id",
            meta = listOf(MetaData("key", "value"))
        ))
        
        // Sign the molecule
        molecule.sign()
        
        expectThat(molecule) {
            get { molecularHash }.isNotNull()
            get { atoms.first().otsFragment }.isNotNull()
        }
    }
    
    @Test
    fun `molecule should generate unique molecular hashes`() {
        val secret = "hash-test-secret"
        val wallet = Wallet(secret, "TEST")
        
        // Create two similar molecules
        val molecule1 = Molecule(secret)
        molecule1.addAtom(createTestAtom(wallet, "value1"))
        molecule1.sign()
        
        val molecule2 = Molecule(secret)
        molecule2.addAtom(createTestAtom(wallet, "value2"))
        molecule2.sign()
        
        // Molecular hashes should be different
        expectThat(molecule1.molecularHash)
            .isNotEqualTo(molecule2.molecularHash)
    }
    
    @Test
    fun `molecule should enforce atomic integrity`() {
        val molecule = Molecule("integrity-test")
        val wallet = Wallet("test-wallet", "TEST")
        
        // Add atoms with proper indexing
        repeat(3) { index ->
            molecule.addAtom(Atom(
                position = wallet.position ?: "",
                walletAddress = wallet.address ?: "",
                isotope = 'M',
                token = wallet.token,
                metaType = "test",
                metaId = "test-$index",
                index = index
            ))
        }
        
        expectThat(molecule.atoms) {
            hasSize(3)
            map { it.index }.containsExactly(0, 1, 2)
        }
    }
    
    @Test
    fun `molecule should handle metadata correctly`() {
        val molecule = Molecule("metadata-test")
        val wallet = Wallet("meta-wallet", "META")
        
        val metadata = listOf(
            MetaData("name", "Test Item"),
            MetaData("description", "Test Description"),
            MetaData("value", "100")
        )
        
        molecule.sourceWallet = wallet
        molecule.initMeta(
            meta = metadata.toMutableList(),
            metaType = "TestType",
            metaId = "test-123"
        )
        
        expectThat(molecule.atoms) {
            hasSize(2) // initMeta creates both M atom and remainder atom
            any {
                get { isotope }.isEqualTo('M')
                get { metaType }.isEqualTo("TestType")
                get { metaId }.isEqualTo("test-123")
            }
        }
    }
    
    @Test
    fun `molecule should support token creation`() {
        val molecule = Molecule("token-creation-test")
        val wallet = Wallet("creator-wallet", "USER")
        
        val tokenMetadata = listOf(
            MetaData("name", "Test Token"),
            MetaData("symbol", "TST"),
            MetaData("decimals", "2"),
            MetaData("supply", "1000000")
        )
        
        molecule.sourceWallet = wallet
        molecule.initTokenCreation(
            recipientWallet = wallet,
            amount = 1000000.0,
            meta = tokenMetadata.toMutableList()
        )
        
        // Token creation is a C-atom (issuance) + ContinuID I-atom (the canonical shape;
        // the C-atom meta is the user meta + the prefixed wallet* keys, no longer == tokenMetadata).
        expectThat(molecule.atoms) {
            isNotEmpty()
            any {
                get { isotope }.isEqualTo('C')
                get { value }.isEqualTo("1000000")
            }
            any {
                get { isotope }.isEqualTo('I')
            }
        }
    }
    
    @Test
    fun `molecule should validate before signing`() {
        val molecule = Molecule("validation-test")
        
        // Empty molecule should not be valid
        expectThat(molecule.check()).isFalse()
        
        // Add an atom
        val wallet = Wallet("test", "TEST")
        molecule.addAtom(createTestAtom(wallet))

        // Still invalid before signing: check() requires a molecular hash, which is
        // only produced by signing. An unsigned molecule (molecularHash == null) is
        // not valid — even with atoms present.
        expectThat(molecule.check()).isFalse()
    }
    
    @Test
    fun `molecule initializes token transfer correctly`() {
        // Test initValue method for token transfers
        val secret = "transfer-test-secret"
        val sourceWallet = Wallet(secret, "TRANSFER")
        sourceWallet.balance = 1000.0 // Set source wallet balance
        
        val recipientWallet = Wallet("recipient-secret", "TRANSFER")
        
        val molecule = Molecule(secret)
        molecule.sourceWallet = sourceWallet
        molecule.remainderWallet = Wallet(secret + "-remainder", "TRANSFER")
        
        // Initialize value transfer
        molecule.initValue(recipientWallet, 250.0)
        
        expectThat(molecule.atoms) {
            hasSize(3) // Should have 3 atoms: source (negative), recipient (positive), remainder
            
            // First atom: removes the ENTIRE source balance (UTXO model)
            any {
                get { walletAddress }.isEqualTo(sourceWallet.address)
                get { isotope }.isEqualTo('V')
                get { value }.isEqualTo("-1000")
            }
            
            // Second atom: adds value to recipient
            any {
                get { walletAddress }.isEqualTo(recipientWallet.address)
                get { isotope }.isEqualTo('V')
                get { value }.isEqualTo("250")
                get { metaType }.isEqualTo("walletBundle")
                get { metaId }.isEqualTo(recipientWallet.bundle)
            }
            
            // Third atom: remainder goes to remainder wallet
            any {
                get { walletAddress }.isEqualTo(molecule.remainderWallet?.address)
                get { isotope }.isEqualTo('V')
                get { value }.isEqualTo("750") // 1000 - 250 = 750
                get { metaType }.isEqualTo("walletBundle")
            }
        }
        
        // Conservation now holds (UTXO model): -1000 (source) + 250 (recipient) + 750 (remainder) = 0,
        // matching the JS/cross-SDK reference and what the validator accepts.
    }
    
    @Test
    fun `molecule initializes buffer withdraw correctly (BVB conserves)`() {
        // initWithdrawBuffer: source B -balance, recipient V atoms (+amount), remainder B
        // +(balance-Σ); the B+V atom values conserve to 0. Mirrors JS/PHP/Rust/Python.
        val secret = "withdraw-buffer-test-secret"
        val source = Wallet.create(secret, "WTHTOK")
        source.balance = 100.0
        val bundleA = "a".repeat(64)
        val bundleB = "b".repeat(64)

        val molecule = Molecule(
            secret = secret,
            sourceWallet = source,
            remainderWallet = Wallet.create(secret, "WTHTOK"),
            cellSlug = "wthtest"
        )
        molecule.initWithdrawBuffer(mapOf(bundleA to 30, bundleB to 20))

        expectThat(molecule.atoms).hasSize(4)
        // Emit order: source B (-balance), recipient V x2 (+amount), remainder B (+change).
        expectThat(molecule.atoms.map { it.isotope }).isEqualTo(listOf('B', 'V', 'V', 'B'))
        expectThat(molecule.atoms.map { it.value }).isEqualTo(listOf("-100", "30", "20", "50"))
        // Conservation: B+V atom values sum to zero.
        expectThat(molecule.atoms.mapNotNull { it.value?.toLongOrNull() }.sum()).isEqualTo(0L)
        // Recipient V-atoms are wallet-less (empty position/address), keyed by metaId = bundle.
        expectThat(molecule.atoms[1]) {
            get { metaType }.isEqualTo("walletBundle")
            get { metaId }.isEqualTo(bundleA)
            get { position }.isEqualTo("")
        }
        expectThat(molecule.atoms[2].metaId).isEqualTo(bundleB)
    }

    @Test
    fun `molecule buffer withdraw with full balance leaves zero remainder`() {
        val secret = "withdraw-buffer-full-secret"
        val source = Wallet.create(secret, "WTHTOK")
        source.balance = 50.0

        val molecule = Molecule(
            secret = secret,
            sourceWallet = source,
            remainderWallet = Wallet.create(secret, "WTHTOK"),
            cellSlug = "wthtest"
        )
        molecule.initWithdrawBuffer(mapOf("c".repeat(64) to 50))

        expectThat(molecule.atoms.map { it.value }).isEqualTo(listOf("-50", "50", "0"))
        expectThat(molecule.atoms.mapNotNull { it.value?.toLongOrNull() }.sum()).isEqualTo(0L)
    }

    @Test
    fun `client-shaped buffer withdraw signs and checks (remainder = source)`() {
        // Mirrors KnishIOClient.withdrawBufferToken: the buffer wallet is BOTH source and
        // remainder, a single recipient (the caller's own bundle). Proves the mutation flow
        // (initWithdrawBuffer + sign + check) validates the B-V-B molecule end-to-end — i.e.
        // CheckMolecule accepts the two-same-position-B-atom buffer shape the client builds.
        val secret = "withdraw-buffer-clientshape-secret"
        val source = Wallet.create(secret, "WTHTOK")
        source.balance = 100.0
        val ownBundle = "d".repeat(64)

        // remainderWallet = source (the JS buffer-is-source-and-remainder semantics)
        val molecule = Molecule(
            secret = secret,
            sourceWallet = source,
            remainderWallet = source,
            cellSlug = "wthtest"
        )
        molecule.initWithdrawBuffer(mapOf(ownBundle to 40))
        molecule.sign()

        // No-throw + true == the B-V-B molecule (two B-atoms at the source position) validates.
        expectThat(molecule.check(source)).isTrue()
        expectThat(molecule.molecularHash).isNotNull()
        expectThat(molecule.atoms.map { it.isotope }).isEqualTo(listOf('B', 'V', 'B'))
        expectThat(molecule.atoms.map { it.value }).isEqualTo(listOf("-100", "40", "60"))
        expectThat(molecule.atoms.mapNotNull { it.value?.toLongOrNull() }.sum()).isEqualTo(0L)
    }

    private fun createTestAtom(wallet: Wallet, value: String = "test"): Atom {
        return Atom(
            position = wallet.position ?: "",
            walletAddress = wallet.address ?: "",
            isotope = 'M',
            token = wallet.token,
            metaType = "test",
            metaId = "test-id",
            meta = listOf(MetaData("key", value))
        )
    }
}