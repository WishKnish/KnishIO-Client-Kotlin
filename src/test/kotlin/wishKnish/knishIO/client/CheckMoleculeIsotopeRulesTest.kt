package wishKnish.knishIO.client

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import wishKnish.knishIO.client.data.MetaData
import wishKnish.knishIO.client.exception.AtomIndexException
import wishKnish.knishIO.client.exception.MetaMissingException
import wishKnish.knishIO.client.exception.PolicyInvalidException
import wishKnish.knishIO.client.exception.RuleArgumentException
import wishKnish.knishIO.client.exception.WrongTokenTypeException
import wishKnish.knishIO.client.libraries.CheckMolecule
import wishKnish.knishIO.client.libraries.Crypto

/**
 * The I, M, C, T and R isotope rules of the JS reference (CheckMolecule.js): USER-only tokens,
 * readPolicy/writePolicy validation on M atoms, wallet meta on T atoms only when metaType is
 * "wallet", and the JS `rule`/`policy` meta form on R atoms (Molecule.createRule).
 */
class CheckMoleculeIsotopeRulesTest {

  private val secret = Crypto.generateSecret("fleet-audit-2026-11")
  private val positionA = "aa".repeat(32)
  private val positionC = "cc".repeat(32)
  private val bundle = Crypto.generateBundleHash(secret)
  private val source = Wallet(secret, "USER", positionA)

  // JS: Molecule.createRule({ rule: [{ condition: [{ key: 'k', value: 'v', comparison: '==' }], callback: [{ action: 'reject' }] }] })
  private val ruleJson = """[{"condition":[{"key":"k","value":"v","comparison":"=="}],"callback":[{"action":"reject"}]}]"""
  private val policyJson = """{"read":{"0":["all"]},"write":{"0":["self"]}}"""

  private fun atom(isotope: Char, token: String, index: Int, metaType: String? = null, meta: List<MetaData> = listOf()) =
    Atom(positionA, source.address!!, isotope, token, null, null, metaType, "meta-id", meta, index = index)

  private fun molecule(vararg atoms: Atom) = Molecule(secret, source).apply {
    atoms.forEach { addAtom(it) }
    molecularHash = "0".repeat(64)
  }

  @Test
  fun `I, M, C and T atoms must carry the USER token`() {
    val forged = "FORGED"
    assertThrows<WrongTokenTypeException> { CheckMolecule.isotopeI(molecule(atom('I', forged, 1))) }
    assertThrows<WrongTokenTypeException> { CheckMolecule.isotopeM(molecule(atom('M', forged, 0, meta = listOf(MetaData("name", "x"))))) }
    assertThrows<WrongTokenTypeException> { CheckMolecule.isotopeC(molecule(atom('C', forged, 0))) }
    assertThrows<WrongTokenTypeException> { CheckMolecule.isotopeT(molecule(atom('T', forged, 0, meta = listOf(MetaData("token", "TEST"))))) }

    assertDoesNotThrow { CheckMolecule.isotopeI(molecule(atom('I', "USER", 1))) }
    assertDoesNotThrow { CheckMolecule.isotopeM(molecule(atom('M', "USER", 0, meta = listOf(MetaData("name", "x"))))) }
    assertDoesNotThrow { CheckMolecule.isotopeC(molecule(atom('C', "USER", 0))) }
  }

  @Test
  fun `M atom policies must name existing meta keys and valid holders`() {
    val missingKey = listOf(MetaData("name", "x"), MetaData("readPolicy", """{"color":["all"]}"""))
    val badHolder = listOf(MetaData("name", "x"), MetaData("writePolicy", """{"name":["somebody"]}"""))
    val valid = listOf(MetaData("name", "x"), MetaData("writePolicy", """{"name":["self","$bundle"]}"""), MetaData("readPolicy", """{"name":["all"]}"""))

    assertThrows<PolicyInvalidException> { CheckMolecule.isotopeM(molecule(atom('M', "USER", 0, meta = missingKey))) }
    assertThrows<PolicyInvalidException> { CheckMolecule.isotopeM(molecule(atom('M', "USER", 0, meta = badHolder))) }
    assertDoesNotThrow { CheckMolecule.isotopeM(molecule(atom('M', "USER", 0, meta = valid))) }
  }

  @Test
  fun `T atoms need wallet meta only when metaType is wallet`() {
    val tokenOnly = listOf(MetaData("token", "TEST"))
    assertDoesNotThrow { CheckMolecule.isotopeT(molecule(atom('T', "USER", 0, metaType = null, meta = tokenOnly))) }
    assertDoesNotThrow { CheckMolecule.isotopeT(molecule(atom('T', "USER", 0, metaType = "walletBundle", meta = tokenOnly))) }
    assertThrows<MetaMissingException> { CheckMolecule.isotopeT(molecule(atom('T', "USER", 0, metaType = "Wallet", meta = tokenOnly))) }
    assertThrows<AtomIndexException> { CheckMolecule.isotopeT(molecule(atom('T', "USER", 1, meta = tokenOnly))) }
  }

  @Test
  fun `R atoms in the JS rule and policy form verify, malformed ones do not`() {
    val jsForm = listOf(MetaData("rule", ruleJson), MetaData("policy", policyJson))
    assertDoesNotThrow { CheckMolecule.isotopeR(molecule(atom('R', "USER", 0, meta = jsForm))) }
    assertDoesNotThrow { CheckMolecule.isotopeR(molecule(atom('R', "USER", 0, meta = listOf(MetaData("policy", policyJson))))) }

    val mixed = listOf(MetaData("policy", """{"read":{},"rule":{}}"""))
    val notList = listOf(MetaData("rule", """{"condition":[]}"""))
    val empty = listOf(MetaData("rule", "[]"))
    val noCallback = listOf(MetaData("rule", """[{"condition":[{"key":"k","value":"v","comparison":"=="}]}]"""))
    val badCondition = listOf(MetaData("rule", """[{"condition":[{"key":"k","value":""}],"callback":[{"action":"reject"}]}]"""))
    listOf(mixed, notList, empty, noCallback).forEach { meta ->
      assertThrows<MetaMissingException> { CheckMolecule.isotopeR(molecule(atom('R', "USER", 0, meta = meta))) }
    }
    assertThrows<RuleArgumentException> { CheckMolecule.isotopeR(molecule(atom('R', "USER", 0, meta = badCondition))) }
  }

  @Test
  fun `createRule builds the JS R atom and the molecule verifies`() {
    val molecule = Molecule(secret, source, Wallet(secret, "USER", positionC))
    molecule.createRule("TestType", "test-1", Json.parseToJsonElement(ruleJson).jsonArray)

    val rAtom = molecule.atoms.first()
    expectThat(rAtom.meta.map { it.key to it.value }).isEqualTo(listOf("rule" to ruleJson, "policy" to policyJson))

    // Digest pinned from the JS reference (Molecule.createRule, Atom.hashAtoms, createdAt 1700000000000).
    val pinned = rAtom.copy(createdAt = "1700000000000")
    expectThat(Atom.hashAtoms(listOf(pinned))).isEqualTo("02a4504adg9ab056afcc5accd13343ca19df3062d965ceb690618206ff53816a")

    molecule.sign()
    expectThat(molecule.check()).isTrue()
  }
}
