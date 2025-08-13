package wishKnish.knishIO.client

import org.junit.jupiter.api.Test
import strikt.api.*
import strikt.assertions.*
import wishKnish.knishIO.client.data.MetaData

class AtomTest {
    
    @Test
    fun `creates an atom with correct properties`() {
        val wallet = Wallet("test-secret", "TEST")
        val atom = Atom(
            position = wallet.position ?: "",
            walletAddress = wallet.address ?: "",
            isotope = 'V',
            token = "TEST",
            value = "100.0",
            metaType = "transfer",
            metaId = "test-transfer-id",
            meta = listOf(MetaData("description", "Test transfer")),
            index = 0
        )
        
        expectThat(atom) {
            get { position }.isEqualTo(wallet.position)
            get { walletAddress }.isEqualTo(wallet.address)
            get { isotope }.isEqualTo('V')
            get { token }.isEqualTo("TEST")
            get { value }.isEqualTo("100.0")
            get { metaType }.isEqualTo("transfer")
            get { metaId }.isEqualTo("test-transfer-id")
            get { index }.isEqualTo(0)
            get { meta }.hasSize(1)
            get { createdAt }.isNotEmpty()
        }
    }
    
    @Test
    fun `creates atom from wallet correctly`() {
        val wallet = Wallet("wallet-secret", "WALLET")
        val metadata = listOf(MetaData("name", "Test Wallet Atom"))
        
        val atom = Atom(
            position = wallet.position ?: "",
            walletAddress = wallet.address ?: "",
            isotope = 'M',
            token = wallet.token,
            metaType = "wallet",
            metaId = "test-wallet-id",
            meta = metadata
        )
        
        expectThat(atom) {
            get { position }.isEqualTo(wallet.position)
            get { walletAddress }.isEqualTo(wallet.address)
            get { token }.isEqualTo(wallet.token)
            get { isotope }.isEqualTo('M')
            get { metaType }.isEqualTo("wallet")
            get { metaId }.isEqualTo("test-wallet-id")
            get { meta }.isEqualTo(metadata)
        }
    }
    
    @Test
    fun `generates consistent hash for atoms`() {
        val wallet = Wallet("hash-test-secret", "HASH")
        val atom1 = Atom(
            position = wallet.position ?: "",
            walletAddress = wallet.address ?: "",
            isotope = 'V',
            token = "HASH",
            value = "50.0",
            index = 0
        )
        
        val atom2 = Atom(
            position = wallet.position ?: "",
            walletAddress = wallet.address ?: "",
            isotope = 'V', 
            token = "HASH",
            value = "25.0",
            index = 1
        )
        
        val atoms = listOf(atom1, atom2)
        val hash = Atom.hashAtoms(atoms)
        
        expectThat(hash) {
            isNotNull()
            get { this?.length }.isEqualTo(64) // Base17 hash should be 64 characters
        }
        
        // Hash should be consistent for same atoms
        val hash2 = Atom.hashAtoms(atoms)
        expectThat(hash).isEqualTo(hash2)
    }
    
    @Test
    fun `hashes atoms correctly`() {
        val wallet = Wallet("molecular-hash-secret", "MOLECULAR")
        
        val atoms = listOf(
            Atom(
                position = wallet.position ?: "",
                walletAddress = wallet.address ?: "",
                isotope = 'V',
                token = "MOLECULAR",
                value = "100.0",
                index = 0
            ),
            Atom(
                position = wallet.position ?: "",
                walletAddress = wallet.address ?: "",
                isotope = 'M',
                token = "MOLECULAR",
                metaType = "test",
                metaId = "hash-test",
                meta = listOf(MetaData("key", "value")),
                index = 1
            )
        )
        
        val hexHash = Atom.hashAtoms(atoms, "hex")
        val base17Hash = Atom.hashAtoms(atoms, "base17")
        val invalidOutput = Atom.hashAtoms(atoms, "invalid")
        
        expectThat(hexHash) {
            isNotNull()
            get { this?.length }.isEqualTo(64) // Hex hash should be 64 characters (32 bytes * 2)
            get { this?.matches("[0-9a-f]+".toRegex()) }.isEqualTo(true) // Should only contain hex characters
        }
        
        expectThat(base17Hash) {
            isNotNull()
            get { this?.length }.isEqualTo(64) // Base17 hash should be 64 characters 
            get { this?.matches("[0-9a-g]+".toRegex()) }.isEqualTo(true) // Should only contain base17 characters
        }
        
        expectThat(invalidOutput).isNull()
    }
    
    @Test
    fun `sorts atoms correctly`() {
        val wallet = Wallet("sorting-secret", "SORT")
        
        // Create atoms with non-sequential indexes
        val atom1 = Atom(
            position = wallet.position ?: "",
            walletAddress = wallet.address ?: "",
            isotope = 'V',
            token = "SORT",
            value = "10.0",
            index = 2
        )
        
        val atom2 = Atom(
            position = wallet.position ?: "",
            walletAddress = wallet.address ?: "",
            isotope = 'V',
            token = "SORT", 
            value = "20.0",
            index = 0
        )
        
        val atom3 = Atom(
            position = wallet.position ?: "",
            walletAddress = wallet.address ?: "",
            isotope = 'V',
            token = "SORT",
            value = "30.0", 
            index = 1
        )
        
        val unsortedAtoms = listOf(atom1, atom2, atom3)
        val sortedAtoms = Atom.sortAtoms(unsortedAtoms)
        
        expectThat(sortedAtoms) {
            hasSize(3)
            map { it.index }.containsExactly(0, 1, 2)
            map { it.value }.containsExactly("20.0", "30.0", "10.0")
        }
        
        // Ensure original list is not modified
        expectThat(unsortedAtoms.map { it.index }).containsExactly(2, 0, 1)
    }
    
    @Test
    fun `converts to JSON correctly`() {
        val wallet = Wallet("json-test-secret", "JSON")
        val atom = Atom(
            position = wallet.position ?: "",
            walletAddress = wallet.address ?: "",
            isotope = 'M',
            token = "JSON",
            metaType = "test",
            metaId = "json-test-id",
            meta = listOf(MetaData("format", "json"))
        )
        
        val json = atom.toJson()
        
        expectThat(json) {
            isNotEmpty()
            contains("\"isotope\":\"M\"")
            contains("\"token\":\"JSON\"")
            contains("\"metaType\":\"test\"")
        }
        
        // Should be able to convert back from JSON
        val reconstructed = Atom.jsonToObject(json)
        expectThat(reconstructed) {
            get { isotope }.isEqualTo(atom.isotope)
            get { token }.isEqualTo(atom.token)
            get { metaType }.isEqualTo(atom.metaType)
            get { metaId }.isEqualTo(atom.metaId)
        }
    }
    
    @Test
    fun `gets hashable values correctly`() {
        // Test that atoms include correct fields for hashing
        val wallet = Wallet("hashable-test-secret", "HASHABLE")
        val metadata = listOf(
            MetaData("key1", "value1"),
            MetaData("key2", "value2")
        )
        
        val atom = Atom(
            position = wallet.position ?: "",
            walletAddress = wallet.address ?: "",
            isotope = 'V',
            token = "HASHABLE",
            value = "123.45",
            batchId = "batch-123",
            metaType = "hashTest",
            metaId = "hash-id-456",
            meta = metadata,
            index = 5
        )
        
        // Verify all hashable fields are present and accessible
        expectThat(atom) {
            // Core fields used in hashing
            get { position }.isNotEmpty()
            get { walletAddress }.isNotEmpty()
            get { isotope }.isEqualTo('V')
            get { token }.isEqualTo("HASHABLE")
            get { value }.isEqualTo("123.45")
            get { batchId }.isEqualTo("batch-123")
            get { metaType }.isEqualTo("hashTest")
            get { metaId }.isEqualTo("hash-id-456")
            get { meta }.isEqualTo(metadata)
            get { createdAt }.isNotEmpty()
            
            // Index is not included in hash but is used for sorting
            get { index }.isEqualTo(5)
        }
        
        // Test that hashing is deterministic for same atom
        val hash1 = Atom.hashAtoms(listOf(atom))
        val hash2 = Atom.hashAtoms(listOf(atom))
        expectThat(hash1) {
            isNotNull()
            isEqualTo(hash2)
        }
        
        // Test that different atoms produce different hashes
        val differentAtom = Atom(
            position = atom.position,
            walletAddress = atom.walletAddress,
            isotope = atom.isotope,
            token = atom.token,
            value = "999.99", // Different value
            batchId = atom.batchId,
            metaType = atom.metaType,
            metaId = atom.metaId,
            meta = atom.meta,
            index = atom.index
        )
        
        val hash3 = Atom.hashAtoms(listOf(differentAtom))
        expectThat(hash3).isNotEqualTo(hash1)
    }
}