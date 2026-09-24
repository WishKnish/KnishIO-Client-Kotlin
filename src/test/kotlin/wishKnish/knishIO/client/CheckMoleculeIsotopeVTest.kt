package wishKnish.knishIO.client

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.assertDoesNotThrow
import wishKnish.knishIO.client.exception.TransferUnbalancedException
import wishKnish.knishIO.client.libraries.CheckMolecule
import wishKnish.knishIO.client.libraries.Crypto

/**
 * Cross-SDK parity (JS CheckMolecule.isotopeV): a 2-atom V transaction must conserve value:
 * the two V atoms' values must sum to zero (e.g. source -1000 + recipient +1000 = 0).
 * An unbalanced 2-atom transfer (e.g. -1000 / +500) must be rejected with TransferUnbalancedException.
 */
class CheckMoleculeIsotopeVTest {

  private fun signedTwoAtomTransfer(debit: String, credit: String): Molecule {
    val secret = Crypto.generateSecret("isotope-v-two-atom-test", 2048)
    val source = Wallet(secret, "TEST", "0123456789abcdeffedcba9876543210fedcba9876543210fedcba9876543210")
    val recipient = Wallet(secret, "TEST", "fedcba98765432100123456789abcdef0123456789abcdef0123456789abcdef")
    val molecule = Molecule(secret, source)
    molecule.addAtom(Atom(source.position!!, source.address!!, 'V', "TEST", debit, index = 0))
    molecule.addAtom(Atom(recipient.position!!, recipient.address!!, 'V', "TEST", credit, index = 1))
    molecule.sign()
    return molecule
  }

  @Test
  fun `an unbalanced 2-atom V transfer is rejected`() {
    assertThrows<TransferUnbalancedException> {
      CheckMolecule.isotopeV(signedTwoAtomTransfer("-1000", "500"))
    }
  }

  @Test
  fun `a balanced 2-atom V transfer passes`() {
    assertDoesNotThrow {
      CheckMolecule.isotopeV(signedTwoAtomTransfer("-1000", "1000"))
    }
  }
}
