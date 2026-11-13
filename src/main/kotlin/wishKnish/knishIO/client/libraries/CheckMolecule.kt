@file:JvmName("CheckMolecule")

package wishKnish.knishIO.client.libraries

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import wishKnish.knishIO.client.Atom
import wishKnish.knishIO.client.Molecule
import wishKnish.knishIO.client.Wallet
import wishKnish.knishIO.client.exception.*
import kotlin.jvm.Throws

class CheckMolecule {
  companion object {

    @JvmStatic
    @Throws(
      MolecularHashMissingException::class, AtomsMissingException::class, NoSuchElementException::class
    )
    fun continuId(molecule: Molecule): Boolean {
      missing(molecule)

      val firstAtom = molecule.atoms.first()

      if (firstAtom.token == "USER" && molecule.atoms.none { it.isotope == 'I' }) {
        throw AtomsMissingException("Check::continuId() - Molecule is missing required ContinuID Atom!")
      }

      return true
    }

    @JvmStatic
    @Throws(
      MolecularHashMissingException::class, AtomsMissingException::class, BatchIdException::class
    )
    fun batchId(molecule: Molecule): Boolean {

      missing(molecule)

      val signingAtom = molecule.atoms.first()
      if (signingAtom.isotope == 'V' && ! signingAtom.batchId.isNullOrEmpty()) {
        val atoms = molecule.atoms.filter { it.isotope == 'V' }
        val remainderAtom = atoms.last()

        if (signingAtom.batchId != remainderAtom.batchId) {
          throw BatchIdException()
        }

        if (atoms.any { it.batchId.isNullOrEmpty() }) {
          throw BatchIdException()
        }
      }
      return true
    }

    @JvmStatic
    fun normalizedHash(hash: String): Map<Int, Int> {
      return normalize(enumerate(hash))
    }

    @JvmStatic
    fun enumerate(hash: String): Map<Int, Int> {
      val mapped = mapOf(
        '0' to - 8,
        '1' to - 7,
        '2' to - 6,
        '3' to - 5,
        '4' to - 4,
        '5' to - 3,
        '6' to - 2,
        '7' to - 1,
        '8' to 0,
        '9' to 1,
        'a' to 2,
        'b' to 3,
        'c' to 4,
        'd' to 5,
        'e' to 6,
        'f' to 7,
        'g' to 8
      )

      return mutableMapOf<Int, Int>().also {
        hash.lowercase().forEachIndexed { index, symbol -> mapped[symbol]?.let { value -> it[index] = value } }
      }
    }

    @JvmStatic
    fun normalize(mappedHash: Map<Int, Int>): Map<Int, Int> {
      val hash = mappedHash.toMutableMap()
      var total = hash.values.reduce { total, num -> total + num }
      val totalCondition = total < 0

      while (total !in 0..0) {
        for ((key, value) in hash) {
          if (if (totalCondition) value < 8 else value > - 8) {
            hash[key] = if (totalCondition) {
              total ++
              value + 1
            } else {
              total --
              value - 1
            }

            if (total == 0) break
          }
        }
      }

      return hash
    }

    @JvmStatic
    @Throws(
      MolecularHashMissingException::class, AtomsMissingException::class
    )
    fun missing(molecule: Molecule) {
      // No molecular hash?
      if (molecule.molecularHash.isNullOrEmpty()) {
        throw MolecularHashMissingException()
      }
      // No atoms?
      if (molecule.atoms.isEmpty()) {
        throw AtomsMissingException()
      }
    }

    /** The atom's meta as a key → value map (a repeated key keeps its last value), as JS `aggregatedMeta()`. */
    private fun aggregate(atom: Atom): Map<String, String?> = atom.meta.associate { it.key to it.value }

    private fun parseMetaJson(value: String, onError: (String) -> Exception): JsonElement {
      return try {
        Json.parseToJsonElement(value)
      } catch (e: SerializationException) {
        throw onError(e.message ?: "invalid JSON")
      }
    }

    private fun requireUserToken(atom: Atom, check: String) {
      if (atom.token != "USER") {
        throw WrongTokenTypeException("Check::$check() - \"${atom.token}\" is not a valid Token slug for \"${atom.isotope}\" isotope Atoms!")
      }
    }

    @JvmStatic
    @Throws(
      MolecularHashMissingException::class,
      AtomsMissingException::class,
      WrongTokenTypeException::class,
      AtomIndexException::class
    )
    fun isotopeI(molecule: Molecule): Boolean {
      missing(molecule)
      molecule.atoms.filter { it.isotope == 'I' }.forEach {
        requireUserToken(it, "isotopeI")
        if (it.index == 0) {
          throw AtomIndexException("Check::isotopeI() - Isotope \"${it.isotope}\" Atoms must have a non-zero index!")
        }
      }
      return true
    }

    @JvmStatic
    @Throws(
      MolecularHashMissingException::class,
      AtomsMissingException::class,
      WrongTokenTypeException::class,
      AtomIndexException::class
    )
    fun isotopeU(molecule: Molecule): Boolean {
      missing(molecule)
      molecule.atoms.filter { it.isotope == 'U' }.forEach {
        // AUTH (first login) or USER (a returning identity signs from its ContinuID pointer).
        if (it.token !in listOf("AUTH", "USER")) {
          throw WrongTokenTypeException("Check::isotopeU() - \"${it.token}\" is not a valid Token slug for \"${it.isotope}\" isotope Atoms!")
        }
        if (it.index != 0) {
          throw AtomIndexException("Check::isotopeU() - Isotope \"${it.isotope}\" Atoms must have an index equal to 0!")
        }
      }

      return true
    }

    @JvmStatic
    @Throws(
      MolecularHashMissingException::class,
      AtomsMissingException::class,
      WrongTokenTypeException::class,
      MetaMissingException::class,
      PolicyInvalidException::class
    )
    fun isotopeM(molecule: Molecule): Boolean {
      missing(molecule)
      val policyKeys = listOf("readPolicy", "writePolicy")
      molecule.atoms.filter { it.isotope == 'M' }.forEach { atom ->
        if (atom.meta.isEmpty()) {
          throw MetaMissingException()
        }
        requireUserToken(atom, "isotopeM")

        val metas = aggregate(atom)
        policyKeys.forEach { key ->
          val raw = metas[key]?.takeIf { it.isNotEmpty() } ?: return@forEach
          val policy = parseMetaJson(raw) { PolicyInvalidException("$key is not valid JSON: $it") }
          val entries = when (policy) {
            is JsonObject -> policy.entries.map { it.key to it.value }
            is JsonArray -> policy.mapIndexed { index, value -> index.toString() to value }
            else -> listOf()
          }
          entries.filter { (name, _) -> name !in policyKeys }.forEach { (name, holders) ->
            if (name !in metas.keys) {
              throw PolicyInvalidException("$name is missing from the meta.")
            }
            policyHolders(holders).forEach { holder ->
              if (!Wallet.isBundleHash(holder) && holder !in listOf("all", "self")) {
                throw PolicyInvalidException("$holder does not correspond to the format of the policy.")
              }
            }
          }
        }
      }

      return true
    }

    /** The values JS `for (const value of policyValue)` visits: array elements, or a string's characters. */
    private fun policyHolders(holders: JsonElement): List<String> = when {
      holders is JsonArray -> holders.map { (it as? JsonPrimitive)?.content ?: it.toString() }
      holders is JsonPrimitive && holders.isString -> holders.content.map { it.toString() }
      else -> throw PolicyInvalidException("$holders does not correspond to the format of the policy.")
    }

    @JvmStatic
    @Throws(
      MolecularHashMissingException::class,
      AtomsMissingException::class,
      WrongTokenTypeException::class,
      AtomIndexException::class
    )
    fun isotopeC(molecule: Molecule): Boolean {
      missing(molecule)
      molecule.atoms.filter { it.isotope == 'C' }.forEach {
        requireUserToken(it, "isotopeC")
        if (it.index != 0) {
          throw AtomIndexException("Check::isotopeC() - Isotope \"${it.isotope}\" Atoms must have an index equal to 0!")
        }
      }

      return true
    }

    @JvmStatic
    @Throws(
      MolecularHashMissingException::class,
      AtomsMissingException::class,
      WrongTokenTypeException::class,
      AtomIndexException::class,
      MetaMissingException::class
    )
    fun isotopeT(molecule: Molecule): Boolean {
      missing(molecule)
      molecule.atoms.filter { it.isotope == 'T' }.forEach {
        val metas = aggregate(it)
        // JS: String(atom.metaType).toLowerCase() === 'wallet' — a null metaType needs no wallet meta
        val required = if (it.metaType?.lowercase() == "wallet") listOf("position", "bundle", "token") else listOf("token")
        required.forEach { key ->
          if (metas[key].isNullOrEmpty()) {
            throw MetaMissingException("Check::isotopeT() - Required meta field \"${key}\" is missing!")
          }
        }

        requireUserToken(it, "isotopeT")

        if (it.index != 0) {
          throw AtomIndexException("Check::isotopeT() - Isotope \"${it.isotope}\" Atoms must have an index equal to 0!")
        }
      }

      return true
    }

    /**
     * R atoms carry a JS-form `policy` (only `read`/`write` entries) and/or `rule` (a non-empty list
     * of `{condition, callback}` rules), as JS `CheckMolecule.isotopeR()`.
     */
    @JvmStatic
    @Throws(
      MolecularHashMissingException::class, AtomsMissingException::class, MetaMissingException::class,
      RuleArgumentException::class, CodeException::class
    )
    fun isotopeR(molecule: Molecule): Boolean {
      missing(molecule)
      molecule.atoms.filter { it.isotope == 'R' }.forEach { atom ->
        val metas = aggregate(atom)

        metas["policy"]?.takeIf { it.isNotEmpty() }?.let { raw ->
          val policy = parseMetaJson(raw) { MetaMissingException("Check::isotopeR() - Incorrect policy format! $it") }
          if (policy !is JsonObject || !policy.keys.all { it in listOf("read", "write") }) {
            throw MetaMissingException("Check::isotopeR() - Mixing rules with politics!")
          }
        }

        metas["rule"]?.takeIf { it.isNotEmpty() }?.let { raw ->
          val rules = parseMetaJson(raw) { MetaMissingException("Check::isotopeR() - Incorrect rule format! $it") }
          if (rules !is JsonArray) {
            throw MetaMissingException("Check::isotopeR() - Incorrect rule format!")
          }
          rules.forEach { RuleMeta.rule(it) }
          if (rules.isEmpty()) {
            throw MetaMissingException("Check::isotopeR() - No rules!")
          }
        }
      }

      return true
    }

    /**
     * Validates P-isotope (Peering) atoms.
     *
     * Mirrors CheckMolecule.isotopeP() in the JavaScript reference.
     */
    @JvmStatic
    @Throws(
      MolecularHashMissingException::class, AtomsMissingException::class,
      MetaMissingException::class, WrongTokenTypeException::class
    )
    fun isotopeP(molecule: Molecule): Boolean {
      missing(molecule)
      molecule.atoms.filter { it.isotope == 'P' }.forEach { atom ->
        if (atom.token != "USER") {
          throw WrongTokenTypeException("Check::isotopeP() - \"${atom.token}\" is not a valid Token slug for \"${atom.isotope}\" isotope Atoms!")
        }

        val peerHost = atom.meta.find { it.key == "peerHost" }?.value
        if (peerHost.isNullOrEmpty()) {
          throw MetaMissingException("Check::isotopeP() - Required meta field \"peerHost\" is missing!")
        }
      }

      return true
    }

    /**
     * Validates A-isotope (Append Request) atoms.
     *
     * Mirrors CheckMolecule.isotopeA() in the JavaScript reference.
     */
    @JvmStatic
    @Throws(
      MolecularHashMissingException::class, AtomsMissingException::class,
      MetaMissingException::class, WrongTokenTypeException::class
    )
    fun isotopeA(molecule: Molecule): Boolean {
      missing(molecule)
      molecule.atoms.filter { it.isotope == 'A' }.forEach { atom ->
        if (atom.token != "USER") {
          throw WrongTokenTypeException("Check::isotopeA() - \"${atom.token}\" is not a valid Token slug for \"${atom.isotope}\" isotope Atoms!")
        }

        if (atom.metaType.isNullOrEmpty()) {
          throw MetaMissingException("Check::isotopeA() - Required field \"metaType\" is missing!")
        }

        if (atom.metaId.isNullOrEmpty()) {
          throw MetaMissingException("Check::isotopeA() - Required field \"metaId\" is missing!")
        }

        val action = atom.meta.find { it.key == "action" }?.value
        if (action.isNullOrEmpty()) {
          throw MetaMissingException("Check::isotopeA() - Required meta field \"action\" is missing!")
        }
      }

      return true
    }

    /**
     * Validates B-isotope (Buffer/Exchange) atoms.
     *
     * Mirrors CheckMolecule.isotopeB() in the JavaScript reference.
     *
     * This is the check isotopeV() delegates to. isotopeV() skips its V-only
     * conservation whenever B/F atoms are present, on the stated basis that "the B/F
     * atoms own the cross-isotope conservation" — and until this method existed,
     * nothing did. A buffer molecule that created or destroyed value verified clean.
     * Conservation over the combined V+B set is enforced here.
     */
    @JvmStatic
    @Throws(
      MolecularHashMissingException::class, AtomsMissingException::class,
      MetaMissingException::class, TransferMalformedException::class,
      TransferUnbalancedException::class
    )
    fun isotopeB(molecule: Molecule): Boolean {
      missing(molecule)
      val isotopeB = molecule.atoms.filter { it.isotope == 'B' }

      if (isotopeB.isEmpty()) {
        return true
      }

      isotopeB.forEach { atom ->
        // B atoms must reference a wallet bundle
        if (atom.metaType != "walletBundle") {
          throw MetaMissingException("Check::isotopeB() - B-isotope atoms must have metaType \"walletBundle\"!")
        }

        if (atom.metaId.isNullOrEmpty()) {
          throw MetaMissingException("Check::isotopeB() - B-isotope atoms must have a metaId!")
        }

        if (atom.value?.toDoubleOrNull() == null) {
          throw TransferMalformedException("Check::isotopeB() - B-isotope atom value is not a valid number!")
        }
      }

      // V+B balance conservation: sum of all V and B atom values must equal zero
      val vAtoms = molecule.atoms.filter { it.isotope == 'V' }
      if (vAtoms.isNotEmpty()) {
        val sum = (vAtoms + isotopeB).sumOf { it.value?.toDoubleOrNull() ?: 0.0 }
        if (sum != 0.0) {
          throw TransferUnbalancedException("Check::isotopeB() - V+B atom values do not balance to zero!")
        }
      }

      return true
    }

    /**
     * Validates F-isotope (Fusion/NFT) atoms.
     *
     * Mirrors CheckMolecule.isotopeF() in the JavaScript reference: identical to
     * isotopeB() plus a non-negative-value rule. Must stay paired with the
     * hasCrossIsotope gate in isotopeV(), which is keyed on B *or* F — without this,
     * an F-isotope molecule skips V-only conservation with nothing validating V+F.
     */
    @JvmStatic
    @Throws(
      MolecularHashMissingException::class, AtomsMissingException::class,
      MetaMissingException::class, TransferMalformedException::class,
      TransferUnbalancedException::class
    )
    fun isotopeF(molecule: Molecule): Boolean {
      missing(molecule)
      val isotopeF = molecule.atoms.filter { it.isotope == 'F' }

      if (isotopeF.isEmpty()) {
        return true
      }

      isotopeF.forEach { atom ->
        if (atom.metaType != "walletBundle") {
          throw MetaMissingException("Check::isotopeF() - F-isotope atoms must have metaType \"walletBundle\"!")
        }

        if (atom.metaId.isNullOrEmpty()) {
          throw MetaMissingException("Check::isotopeF() - F-isotope atoms must have a metaId!")
        }

        val value = atom.value?.toDoubleOrNull()
          ?: throw TransferMalformedException("Check::isotopeF() - F-isotope atom value is not a valid number!")

        if (value < 0.0) {
          throw TransferMalformedException("Check::isotopeF() - F-isotope atom value must not be negative!")
        }
      }

      // V+F balance conservation: sum of all V and F atom values must equal zero
      val vAtoms = molecule.atoms.filter { it.isotope == 'V' }
      if (vAtoms.isNotEmpty()) {
        val sum = (vAtoms + isotopeF).sumOf { it.value?.toDoubleOrNull() ?: 0.0 }
        if (sum != 0.0) {
          throw TransferUnbalancedException("Check::isotopeF() - V+F atom values do not balance to zero!")
        }
      }

      return true
    }

    @JvmStatic
    @JvmOverloads
    @Throws(
      MolecularHashMissingException::class,
      AtomsMissingException::class,
      TransferMismatchedException::class,
      TransferMalformedException::class,
      IllegalArgumentException::class,
      TransferToSelfException::class,
      TransferBalanceException::class,
      TransferRemainderException::class,
      TransferUnbalancedException::class
    )
    fun isotopeV(
      molecule: Molecule,
      sourceWallet: Wallet? = null
    ): Boolean {
      missing(molecule)
      if (molecule.atoms.none { it.isotope == 'V' }) {
        return true
      }

      val firstAtom = molecule.atoms.first()
      val isotope = molecule.atoms.filter { it.isotope == 'V' }

      // Cross-SDK parity (JS CheckMolecule.isotopeV): when B/F isotopes are present (buffer
      // deposit/withdraw, token fusion), the V-only conservation is NOT enforced — the B/F atoms
      // own the cross-isotope conservation. Bypass the V-only 2-atom/sum/remainder checks below.
      val hasCrossIsotope = molecule.atoms.any { it.isotope == 'B' || it.isotope == 'F' }

      if (!hasCrossIsotope && firstAtom.isotope == 'V' && isotope.size == 2) {
        val endAtom = isotope.last()

        if (firstAtom.token != endAtom.token) {
          throw TransferMismatchedException()
        }
        endAtom.value?.let {
          if (it.toDouble() < 0.0) {
            throw TransferMalformedException()
          }
        } ?: throw TransferMalformedException()
        // Conservation (JS CheckMolecule.isotopeV): the two V values must sum to zero.
        val firstValue = firstAtom.value?.toDoubleOrNull() ?: Double.NaN
        val endValue = endAtom.value?.toDoubleOrNull() ?: Double.NaN
        if (firstValue + endValue != 0.0) {
          throw TransferUnbalancedException()
        }

        return true
      }

      var sum = 0.0
      var value = 0.0

      molecule.atoms.forEachIndexed { index, vAtom ->
        // Not V? Next...
        if (vAtom.isotope != 'V') {
          return@forEachIndexed
        }

        if (firstAtom.token != vAtom.token) {
          throw TransferMismatchedException()
        }

        // Making sure all V atoms of the same token
        vAtom.value?.let {
          value = it.toDouble()
        } ?: throw IllegalArgumentException("Invalid isotope \"V\" values")

        // Checking non-primary atoms
        if (index > 0) {
          // Negative V atom in a non-primary position?
          if (value < 0) {
            throw TransferMalformedException()
          }
          // Cannot be sending and receiving from the same address
          if (vAtom.walletAddress == firstAtom.walletAddress) {
            throw TransferToSelfException()
          }
        }

        // Adding this Atom's value to the total sum
        sum += value

      }
      
      // All atoms must sum to zero for a balanced transaction (V-only; bypassed when B/F present)
      if (!hasCrossIsotope && sum != 0.0) {
        throw TransferUnbalancedException()
      }
      
      // If we're provided with a sourceWallet argument, we can perform additional check
      sourceWallet?.let { wallet ->
        firstAtom.value?.let {
          value = it.toDouble()
        } ?: throw IllegalArgumentException("Invalid isotope \"V\" values")

        val remainder = wallet.balance + value
        // Is there enough balance to send?
        if (remainder < 0) {
          throw TransferBalanceException()
        }
        // Does the remainder match what should be there in the source wallet, if provided?
        // (V-only invariant; bypassed when B/F atoms own the cross-isotope conservation.)
        if (!hasCrossIsotope && remainder != sum) {
          throw TransferRemainderException()
        }
      }
      // Without a sender wallet only V-only conservation (above) applies: the validator and
      // verifyFromServerData check served transfers this way, and a transfer's last V atom is its
      // remainder, which is normally non-zero.

      // Looks like we passed all the tests!
      return true
    }

    @JvmStatic
    @Throws(
      MolecularHashMissingException::class, AtomsMissingException::class, MolecularHashMismatchException::class
    )
    fun molecularHash(molecule: Molecule): Boolean {
      missing(molecule)
      val storedHash = molecule.molecularHash
      val calculatedHash = Atom.hashAtoms(atoms = molecule.atoms)
      
      if (storedHash != calculatedHash) {
        // Debug output to understand the mismatch
        println("DEBUG: Molecular hash mismatch!")
        println("  Stored hash:     $storedHash")
        println("  Calculated hash: $calculatedHash")
        println("  Atom count: ${molecule.atoms.size}")
        throw MolecularHashMismatchException()
      }
      // Looks like we passed all the tests!
      return true
    }

    @JvmStatic
    @Throws(
      MolecularHashMissingException::class, AtomsMissingException::class, AtomIndexException::class
    )
    fun index(molecule: Molecule): Boolean {
      missing(molecule)
      if (! molecule.atoms.groupBy { it.index }.filter { it.value.count() > 1 }.none()) {
        throw AtomIndexException()
      }
      return true
    }

    private val SIGNABLE_HASH = Regex("[0-9a-g]{64}")

    /**
     * True when [hash] is exactly 64 symbols of `[0-9a-g]` (the only shape that can be signed or verified).
     */
    @JvmStatic
    fun isSignableHash(hash: String?): Boolean {
      return hash != null && SIGNABLE_HASH.matches(hash)
    }

    private fun otsHex(concatenated: String): String {
      if (concatenated.length == 2048 || concatenated.length == 8192) {
        return concatenated
      }
      val hex = try {
        org.bouncycastle.util.encoders.Hex.toHexString(java.util.Base64.getDecoder().decode(concatenated))
      } catch (e: IllegalArgumentException) {
        throw SignatureMalformedException("OTS malformed", e)
      }
      if (hex.length != 2048 && hex.length != 8192) {
        throw SignatureMalformedException()
      }
      return hex
    }

    /**
     * Verifies the molecule's one-time signature (v1: 16 chains, v2: 64 chains).
     */
    @JvmStatic
    @Throws(
      MolecularHashMissingException::class,
      AtomsMissingException::class,
      SignatureMalformedException::class,
      SignatureMismatchException::class
    )
    fun ots(molecule: Molecule): Boolean {
      missing(molecule)
      if (!isSignableHash(molecule.molecularHash)) {
        throw SignatureMalformedException()
      }
      // Determine first atom
      val firstAtom = molecule.atoms.first()
      val walletAddress = firstAtom.walletAddress
      // Rebuilding OTS out of all the atoms (a missing fragment counts as empty)
      val ots = otsHex(molecule.atoms.joinToString("") { it.otsFragment ?: "" })

      // WOTS+ Verification: Reconstruct the public key fragments from signature
      val normalizedHash = normalizedHash(molecule.molecularHash !!)
      val digestSponge = Shake256.create()

      // Subdivide signature into segments of 128 characters each (16 for v1, 64 for v2)
      ots.chunked(128).forEachIndexed { idx, signatureChunk ->
        var workingFragment = signatureChunk
        
        // Apply WOTS+ verification iterations to reconstruct public key fragment
        // In signing: applied (8 - normalizedHash[idx]) iterations
        // In verification: apply (8 + normalizedHash[idx]) iterations
        // Total: (8 - normalizedHash[idx]) + (8 + normalizedHash[idx]) = 16 iterations from private key
        val verificationIterations = 8 + normalizedHash[idx]!!
        repeat(verificationIterations) {
          workingFragment = Shake256.hash(workingFragment, 64)
        }
        
        // This workingFragment is now the reconstructed "public key fragment"
        // which should match what wallet generation produces after 16 iterations from private key
        digestSponge.absorb(workingFragment)
      }
      
      // Generate address using the same final step as wallet generation
      val address = Shake256.hash(digestSponge.hexString(1024), 32)

      if (address != walletAddress) {
        throw SignatureMismatchException()
      }
      // Looks like we passed all the tests!
      return true
    }

    /**
     * Normalizes a served `createdAt` to the epoch-millisecond string atoms are hashed with:
     * null or "" gives null, integers and digit strings stay as decimal strings, RFC 3339
     * timestamps become epoch milliseconds, anything else is returned unchanged.
     */
    @JvmStatic
    fun toSignedTimestamp(value: Any?): String? = ServedMolecule.toSignedTimestamp(value)

    /**
     * Builds a [Molecule] from molecule JSON exactly as a validator's GraphQL API serves it
     * (`bundleHash`, `tokenSlug`, `metasJson`, RFC 3339 timestamps, optional atom `version`).
     */
    @JvmStatic
    fun fromServerData(json: String): Molecule = ServedMolecule.fromServerData(json)

    /**
     * Verifies served molecule JSON with the full molecule check (no sender wallet). A molecule
     * served without atom versions whose hash does not match is retried with version "4", then "3".
     * Never throws: malformed input gives `verified = false` with an error.
     */
    @JvmStatic
    fun verifyFromServerData(json: String): ServedVerification = ServedMolecule.verifyFromServerData(json)
  }
}
