/*
 * The ContinuID I-atom registers a USER wallet, as in the other SDKs: a non-USER remainder is
 * replaced with a USER wallet derived from the molecule secret, the I-atom carries the remainder's
 * batchId, and the client's default remainder is a USER wallet.
 */

package wishKnish.knishIO.client

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import wishKnish.knishIO.client.data.MetaData
import wishKnish.knishIO.client.libraries.Crypto
import java.net.URI

class ContinuIdRemainderTokenTest {

  private val secret = "f".repeat(2048)
  private val position = "c0ffee" + "0".repeat(58)

  private fun addressAt(token: String, position: String, otsVersion: Int = 1): String =
    Wallet(secret, token, position, otsVersion = otsVersion).address!!

  private fun continuIdHead(molecule: Molecule): Atom = molecule.atoms.first { it.isotope == 'I' }

  @Test
  fun `a non-USER remainder is replaced with a USER wallet`() {
    val remainder = Wallet.create(secret, "TKN", batchId = "batch")
    val molecule = Molecule(secret, Wallet(secret, "USER", position), remainder)
    molecule.initMeta(mutableListOf(MetaData("k", "v")), "ots", "remainder")

    val head = continuIdHead(molecule)
    assertEquals("USER", head.token)
    assertNotEquals(remainder.position, head.position)
    assertEquals(addressAt("USER", head.position!!), head.walletAddress)
    assertNull(head.batchId)
    assertEquals("USER", molecule.remainderWallet!!.token)
  }

  @Test
  fun `the replacement takes the successor OTS version`() {
    val molecule = Molecule(secret, Wallet(secret, "USER", position), Wallet.create(secret, "TKN"), minOtsVersion = 2)
    molecule.initMeta(mutableListOf(MetaData("k", "v")), "ots", "remainder")

    val head = continuIdHead(molecule)
    assertEquals(addressAt("USER", head.position!!, 2), head.walletAddress)
  }

  @Test
  fun `a missing remainder is replaced with a USER wallet`() {
    val molecule = Molecule(secret, Wallet(secret, "USER", position))
    molecule.remainderWallet = null
    molecule.initMeta(mutableListOf(MetaData("k", "v")), "ots", "remainder")

    val head = continuIdHead(molecule)
    assertEquals("USER", head.token)
    assertEquals(addressAt("USER", head.position!!), head.walletAddress)
    assertEquals(head.walletAddress, molecule.remainderWallet!!.address)
  }

  @Test
  fun `the replacement keeps the molecule's ML-KEM parameter set`() {
    val source = Wallet(secret, "USER", position, mlkemParameterSet = 768)
    val molecule = Molecule(secret, source, Wallet.create(secret, "TKN", mlkemParameterSet = 768), mlkemParameterSet = 768)
    molecule.initMeta(mutableListOf(MetaData("k", "v")), "ots", "remainder")

    assertEquals(768, molecule.remainderWallet!!.mlkemParameterSet)
    val pubkey = continuIdHead(molecule).meta.first { it.key == "pubkey" }.value!!
    assertEquals(1184, java.util.Base64.getDecoder().decode(pubkey).size)
  }

  @Test
  fun `a non-USER remainder without the secret fails closed`() {
    val bundleHash = Crypto.generateBundleHash(secret)
    val sourceAddress = addressAt("USER", position)
    val source = Wallet(null, "USER", position).apply {
      address = sourceAddress
      bundle = bundleHash
    }
    val molecule = Molecule(null, source, Wallet.create(bundleHash, "TKN"))

    val error = assertThrows(IllegalStateException::class.java) {
      molecule.initMeta(mutableListOf(MetaData("k", "v")), "ots", "remainder")
    }
    assertEquals("KnishIO: a ContinuID remainder needs the molecule secret.", error.message)
  }

  @Test
  fun `the ContinuID atom carries the remainder's batchId`() {
    val remainder = Wallet.create(secret, "USER", batchId = "batch")
    val molecule = Molecule(secret, Wallet(secret, "USER", position), remainder)
    molecule.initMeta(mutableListOf(MetaData("k", "v")), "ots", "remainder")

    val head = continuIdHead(molecule)
    assertEquals(remainder.address, head.walletAddress)
    assertEquals("batch", head.batchId)
  }

  @Test
  fun `the client default remainder is a USER wallet`() {
    val client = KnishIOClient(listOf(URI("http://127.0.0.1:9/graphql")), otsVersion = 2)
    val source = Wallet(secret, "TKN", position, batchId = "batch", characters = "BASE64")

    val molecule = client.createMolecule(secret, source)

    val remainder = client.remainderWallet()!!
    assertEquals("USER", remainder.token)
    assertEquals("batch", remainder.batchId)
    assertEquals("BASE64", remainder.characters)
    assertEquals(2, remainder.otsVersion)
    assertSame(remainder, molecule.remainderWallet)
  }
}
