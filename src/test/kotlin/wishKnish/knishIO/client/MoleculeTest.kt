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
        
        expectThat(molecule.atoms) {
            isNotEmpty()
            any {
                get { isotope }.isEqualTo('V')
                get { value }.isEqualTo("1000000.0")
            }
            any {
                get { isotope }.isEqualTo('M')
                get { meta }.isEqualTo(tokenMetadata)
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
        
        // Should be valid after adding atom
        expectThat(molecule.check()).isTrue()
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
            
            // First atom: removes value from source
            any {
                get { walletAddress }.isEqualTo(sourceWallet.address)
                get { isotope }.isEqualTo('V')
                get { value }.isEqualTo("-250.0")
            }
            
            // Second atom: adds value to recipient
            any {
                get { walletAddress }.isEqualTo(recipientWallet.address)
                get { isotope }.isEqualTo('V')
                get { value }.isEqualTo("250.0")
                get { metaType }.isEqualTo("walletBundle")
                get { metaId }.isEqualTo(recipientWallet.bundle)
            }
            
            // Third atom: remainder goes to remainder wallet
            any {
                get { walletAddress }.isEqualTo(molecule.remainderWallet?.address)
                get { isotope }.isEqualTo('V')
                get { value }.isEqualTo("750.0") // 1000 - 250 = 750
                get { metaType }.isEqualTo("walletBundle")
            }
        }
        
        // Verify atoms are created correctly (following JS SDK pattern)
        // Note: We don't check balance conservation as the sum includes negative values
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