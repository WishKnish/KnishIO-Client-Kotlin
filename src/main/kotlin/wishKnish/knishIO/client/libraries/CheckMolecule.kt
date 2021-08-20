@file:JvmName("CheckMolecule")

package wishKnish.knishIO.client.libraries

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import wishKnish.knishIO.client.Atom
import wishKnish.knishIO.client.Molecule
import wishKnish.knishIO.client.Wallet
import wishKnish.knishIO.client.exception.*
import kotlin.jvm.Throws

class CheckMolecule {
  companion object {

    @JvmStatic
    @Throws(
      MolecularHashMissingException::class,
      AtomsMissingException::class,
      NoSuchElementException::class
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
      if (signingAtom.isotope == 'V' && !signingAtom.batchId.isNullOrEmpty()) {
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

      while (total < 0 || total > 0) {
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
        if (it.token.isEmpty() || it.token != "USER") {
          throw WrongTokenTypeException("Check::isotopeI() - \"${it.token}\" is not a valid Token slug for \"${it.isotope}\" isotope Atoms!")
        }
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
        if (it.token.isEmpty() || it.token != "AUTH") {
          throw WrongTokenTypeException("Check::isotopeU() - \"${it.token}\" is not a valid Token slug for \"${it.isotope}\" isotope Atoms!")
        }
        if (it.index != 0) {
          throw AtomIndexException("Check::isotopeU() - Isotope \"${it.isotope}\" Atoms must have a non-zero index!")
        }
      }

      return true
    }

    @JvmStatic
    @Throws(
      MolecularHashMissingException::class,
      AtomsMissingException::class,
      WrongTokenTypeException::class,
      MetaMissingException::class
    )
    fun isotopeM(molecule: Molecule): Boolean {
      missing(molecule)
      molecule.atoms.filter { it.isotope == 'M' }.forEach {
        if (it.meta.isEmpty()) {
          throw MetaMissingException()
        }
        if (it.token.isEmpty() || it.token != "USER") {
          throw WrongTokenTypeException("Check::isotopeM() - \"${it.token}\" is not a valid Token slug for \"${it.isotope}\" isotope Atoms!")
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
    fun isotopeC(molecule: Molecule): Boolean {
      missing(molecule)
      molecule.atoms.filter { it.isotope == 'C' }.forEach {
        if (it.token.isEmpty() || it.token != "USER") {
          throw WrongTokenTypeException("Check::isotopeC() - \"${it.token}\" is not a valid Token slug for \"${it.isotope}\" isotope Atoms!")
        }
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
        if (it.metaType.isNullOrEmpty() || it.metaType !!.lowercase() == "wallet") {
          setOf("position", "bundle").forEach { key ->
            val metaData = it.meta.find { metaData -> metaData.key == key }
            if (metaData == null || metaData.value.isNullOrEmpty()) {
              throw MetaMissingException("Check::isotopeT() - Required meta field \"${key}\" is missing!")
            }
          }
        }

        setOf("token").forEach { key ->
          val metaData = it.meta.find { metaData -> metaData.key == key }
          if (metaData == null || metaData.value.isNullOrEmpty()) {
            throw MetaMissingException("Check::isotopeT() - Required meta field \"${key}\" is missing!")
          }
        }

        if (it.token.isEmpty() || it.token != "USER") {
          throw WrongTokenTypeException("Check::isotopeT() - \"${it.token}\" is not a valid Token slug for \"${it.isotope}\" isotope Atoms!")
        }

        if (it.index != 0) {
          throw AtomIndexException("Check::isotopeT() - Isotope \"${it.isotope}\" Atoms must have an index equal to 0!")
        }
      }

      return true
    }

    @JvmStatic
    @Throws(
      MolecularHashMissingException::class, AtomsMissingException::class, MetaMissingException::class
    )
    fun isotopeR(molecule: Molecule): Boolean {
      missing(molecule)
      molecule.atoms.filter { it.isotope == 'R' }.forEach {
        setOf("callback", "conditions", "rule").forEach { key ->
          val metaData =
            it.meta.find { metaData -> metaData.key == key } ?: throw MetaMissingException("Check::isotopeR() - Required meta field \"${key}\" is missing!")

          if (key == "conditions") {
            metaData.value?.let { value ->
              try {
                val conditions = Json.parseToJsonElement(value).decode()

                if (conditions !is List<*>) {
                  throw MetaMissingException("Check::isotopeR() - The condition field must contain a list!")
                }

                conditions.forEach { item ->
                  if (item !is Map<*, *>) {
                    throw MetaMissingException("Check::isotopeR() - The list of the field condition should contain a map!")
                  }
                  if (! item.keys.containsAll(listOf("key", "value", "comparison", "managedBy"))) {
                    throw MetaMissingException("Check::isotopeR() - Required condition field is missing!")
                  }
                }
              } catch (err: SerializationException) {
                throw MetaMissingException("Check::isotopeR() - Condition is formatted incorrectly! JSON string expected.")
              }
            } ?: throw MetaMissingException("Check::isotopeR() - The condition field cannot be empty!")
          }
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
      TransferRemainderException::class
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

      if (firstAtom.isotope == 'V' && isotope.size == 2) {
        val endAtom = isotope.last()

        if (firstAtom.token != endAtom.token) {
          throw TransferMismatchedException()
        }
        endAtom.value?.let {
          if (it.toDouble() < 0.0) {
            throw TransferMalformedException()
          }
        } ?: throw TransferMalformedException()

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
        if (remainder != sum) {
          throw TransferRemainderException()
        }
      } ?: if (value != 0.0) throw TransferRemainderException() // No sourceWallet, but have a remainder?

      // Looks like we passed all the tests!
      return true
    }

    @JvmStatic
    @Throws(
      MolecularHashMissingException::class, AtomsMissingException::class, MolecularHashMismatchException::class
    )
    fun molecularHash(molecule: Molecule): Boolean {
      missing(molecule)
      if (molecule.molecularHash != Atom.hashAtoms(atoms = molecule.atoms)) {
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

    @JvmStatic
    @Throws(
      MolecularHashMissingException::class,
      AtomsMissingException::class,
      SignatureMalformedException::class,
      SignatureMismatchException::class
    )
    fun ots(molecule: Molecule): Boolean {
      missing(molecule)
      // Determine first atom
      val firstAtom = molecule.atoms.first()
      val walletAddress = firstAtom.walletAddress
      // Rebuilding OTS out of all the atoms
      var ots = molecule.atoms.map { it.otsFragment }.reduce { accumulator, otsFragment -> accumulator + otsFragment }

      ots?.let {
        // Wrong size? Maybe it's compressed
        if (it.length != 2048) {
          ots = Strings.base64ToHex(it)
          // Still wrong? That's a failure
          if (ots !!.length != 2048) {
            throw SignatureMalformedException()
          }
        }
      } ?: throw SignatureMalformedException()

      // Subdivide Kk into 16 segments of 256 bytes (128 characters) each
      val keyFragments = molecule.signatureFragments(ots !!, false)
      // Absorb the hashed Kk into the sponge to receive the digest Dk
      val digest = Shake256.hash(keyFragments, 1024)
      // Squeeze the sponge to retrieve a 128 byte (64 character) string that should match the sender’s wallet address
      val address = Shake256.hash(digest, 32)

      if (address != walletAddress) {
        throw SignatureMismatchException()
      }
      // Looks like we passed all the tests!
      return true
    }
  }
}
