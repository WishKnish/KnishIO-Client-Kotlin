/*
                               (
                              (/(
                              (//(
                              (///(
                             (/////(
                             (//////(                          )
                            (////////(                        (/)
                            (////////(                       (///)
                           (//////////(                      (////)
                           (//////////(                     (//////)
                          (////////////(                    (///////)
                         (/////////////(                   (/////////)
                        (//////////////(                  (///////////)
                        (///////////////(                (/////////////)
                       (////////////////(               (//////////////)
                      (((((((((((((((((((              (((((((((((((((
                     (((((((((((((((((((              ((((((((((((((
                     (((((((((((((((((((            ((((((((((((((
                    ((((((((((((((((((((           (((((((((((((
                    ((((((((((((((((((((          ((((((((((((
                    (((((((((((((((((((         ((((((((((((
                    (((((((((((((((((((        ((((((((((
                    ((((((((((((((((((/      (((((((((
                    ((((((((((((((((((     ((((((((
                    (((((((((((((((((    (((((((
                   ((((((((((((((((((  (((((
                   #################  ##
                   ################  #
                  ################# ##
                 %################  ###
                 ###############(   ####
                ###############      ####
               ###############       ######
              %#############(        (#######
             %#############           #########
            ############(              ##########
           ###########                  #############
          #########                      ##############
        %######

        Powered by Knish.IO: Connecting a Decentralized World

Please visit https://github.com/WishKnish/KnishIO-Client-Kotlin for information.

License: https://github.com/WishKnish/KnishIO-Client-Kotlin/blob/master/LICENSE
*/
@file:JvmName("Atom")

package wishKnish.knishIO.client

import wishKnish.knishIO.client.libraries.Shake256
import wishKnish.knishIO.client.libraries.Strings
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import wishKnish.knishIO.client.data.MetaData
import kotlin.jvm.Throws

/**
 * Atom class used to form micro-transactions within a Molecule
 */
@Serializable data class Atom @JvmOverloads constructor(
  @JvmField var position: String,
  @JvmField var walletAddress: String,
  @JvmField var isotope: Char,
  @JvmField var token: String,
  @JvmField var value: String? = null,
  @JvmField var batchId: String? = null,
  @JvmField var metaType: String? = null,
  @JvmField var metaId: String? = null,
  @JvmField var meta: List<MetaData> = mutableListOf(),
  @JvmField var otsFragment: String? = null,
  @JvmField var index: Int = 0,
  @JvmField val createdAt: String = Strings.currentTimeMillis()
) {

  companion object {
    private val hashSchema
      get() = mutableMapOf<String, Any?>(
        "position" to null,
        "walletAddress" to null,
        "isotope" to null,
        "token" to null,
        "value" to null,
        "batchId" to null,
        "metaType" to null,
        "metaId" to null,
        "meta" to null,
        "createdAt" to null
      )

    private val jsonFormat: Json
      get() = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
      }

    /**
     * Populates and returns a schema object according to hash priority list
     * to ensure consistent hashing results across multiple platforms
     */
    @JvmStatic
    private fun molecularHashSchema(atom: Atom): Map<String, Any?> {
      val schema = hashSchema

      atom::class.memberProperties.forEach {
        if (it.visibility == KVisibility.PUBLIC) {
          if (schema.containsKey(it.name)) {
            schema[it.name] = it.getter.call(atom)
          }
        }
      }

      return schema.toMap()
    }

    /**
     * Converts a compliant JSON string into an Atom class instance
     */
    @JvmStatic
    fun jsonToObject(json: String): Atom {
      return jsonFormat.decodeFromString(json)
    }

    /**
     * Sort the atoms in a Molecule
     */
    @JvmStatic
    fun sortAtoms(atoms: List<Atom>): List<Atom> {
      val atomList = atoms.toMutableList()

      // Sort based on atomic index
      atomList.sortBy { it.index }

      return atomList.toList()
    }

    /**
     * Produces a hash of the atoms inside a molecule.
     * Used to generate the molecularHash field for Molecules.
     */
    @JvmStatic
    private fun hash(atoms: List<Atom>): Shake256 {
      val atomList = sortAtoms(atoms)
      val molecularSponge = Shake256.create()
      val numberOfAtoms = atomList.size.toString()

      // Hashing each atom in the molecule to produce a molecular hash
      for (atom in atomList) {
        molecularSponge.absorb(numberOfAtoms)

        molecularHashSchema(atom).forEach { (property, value) ->

          // Skip null values for all properties except position and walletAddress
          // This matches the JS implementation which only hashes non-null values
          if (value == null) {
            // For position and walletAddress, hash empty string when null
            if (property in arrayListOf("position", "walletAddress")) {
              molecularSponge.absorb("")
            }
            return@forEach
          }

          // Handle different property types
          when (property) {
            "meta" -> {
              // Hash individual meta keys and values
              @Suppress("UNCHECKED_CAST") 
              (value as List<MetaData>).forEach { metaData ->
                metaData.value?.let {
                  molecularSponge.absorb(metaData.key)
                  molecularSponge.absorb(it)
                }
              }
            }
            "isotope" -> {
              // Isotope is a Char, convert to String for hashing
              molecularSponge.absorb(value.toString())
            }
            else -> {
              // For all other properties, convert to string and hash
              molecularSponge.absorb(value.toString())
            }
          }
        }
      }

      return molecularSponge
    }

    /**
     * Produces a hash of the atoms inside a molecule.
     * Used to generate the molecularHash field for Molecules.
     */
    @JvmStatic
    @JvmOverloads
    @Throws(IllegalArgumentException::class)
    fun hashAtoms(
      atoms: List<Atom>,
      output: String = "base17"
    ): String? {

      val molecularSponge = hash(atoms)

      return when (output) {
        "hex" -> molecularSponge.hexString(32)
        "base17" -> Strings.charsetBaseConvert(
          molecularSponge.hexString(32), 16, 17, "0123456789abcdef", "0123456789abcdefg"
        ).padStart(64, '0')
        else -> null
      }
    }
  }

  fun toJson(): String {
    return jsonFormat.encodeToString(this)
  }

  override fun toString(): String {
    return toJson()
  }
}
