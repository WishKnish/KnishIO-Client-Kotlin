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
    // Hash schema with JavaScript-compatible property ordering for cross-SDK molecular hash generation
    private val hashSchema
      get() = linkedMapOf<String, Any?>(
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
     * Returns atom properties in the correct order for molecular hash generation.
     * Ensures JavaScript-compatible property ordering for cross-SDK compatibility.
     */
    @JvmStatic
    private fun getHashableValues(atom: Atom): LinkedHashMap<String, Any?> {
      // Return properties in exact JavaScript order for cross-SDK compatibility
      return linkedMapOf(
        "position" to atom.position,
        "walletAddress" to atom.walletAddress,
        "isotope" to atom.isotope,
        "token" to atom.token,
        "value" to atom.value,
        "batchId" to atom.batchId,
        "metaType" to atom.metaType,
        "metaId" to atom.metaId,
        "meta" to atom.meta,
        "createdAt" to atom.createdAt
      )
    }

    /**
     * Populates and returns a schema object according to hash priority list
     * to ensure consistent hashing results across multiple platforms
     */
    @JvmStatic
    private fun molecularHashSchema(atom: Atom): Map<String, Any?> {
      return getHashableValues(atom)
    }

    /**
     * Converts a compliant JSON string into an Atom class instance
     */
    @JvmStatic
    fun jsonToObject(json: String): Atom {
      return jsonFormat.decodeFromString(json)
    }

    /**
     * Creates an Atom instance from JSON data (Kotlin best practices)
     * 
     * Handles cross-SDK atom deserialization with robust error handling following
     * JavaScript canonical patterns for perfect cross-platform compatibility.
     *
     * @param json JSON string or data to deserialize
     * @param validateStructure Validate required fields (default: true)
     * @param strictMode Strict validation mode (default: false)
     * @return Reconstructed atom instance
     * @throws Exception If JSON is invalid or required fields are missing
     */
    @JvmStatic
    @JvmOverloads
    fun fromJSON(
      json: String,
      validateStructure: Boolean = true,
      strictMode: Boolean = false
    ): Atom {
      return try {
        // Parse JSON safely using Gson for flexibility
        val gson = com.google.gson.GsonBuilder()
          .setLenient()
          .create()
        val jsonObject = gson.fromJson(json, com.google.gson.JsonObject::class.java)
        
        // Validate required fields in strict mode
        if (strictMode || validateStructure) {
          val requiredFields = listOf("position", "walletAddress", "isotope", "token")
          for (field in requiredFields) {
            if (!jsonObject.has(field) || jsonObject.get(field).isJsonNull) {
              throw IllegalArgumentException("Required field '$field' is missing or empty")
            }
          }
        }

        // Create atom instance with required fields
        val atom = Atom(
          position = jsonObject.get("position")?.asString ?: "",
          walletAddress = jsonObject.get("walletAddress")?.asString ?: "",
          isotope = jsonObject.get("isotope")?.asString?.firstOrNull() ?: 'V',
          token = jsonObject.get("token")?.asString ?: "",
          value = jsonObject.get("value")?.takeIf { !it.isJsonNull }?.asString,
          batchId = jsonObject.get("batchId")?.takeIf { !it.isJsonNull }?.asString,
          metaType = jsonObject.get("metaType")?.takeIf { !it.isJsonNull }?.asString,
          metaId = jsonObject.get("metaId")?.takeIf { !it.isJsonNull }?.asString,
          index = jsonObject.get("index")?.asInt ?: 0,
          createdAt = jsonObject.get("createdAt")?.asString ?: Strings.currentTimeMillis()
        )

        // Set additional properties that may not be in constructor
        if (jsonObject.has("otsFragment") && !jsonObject.get("otsFragment").isJsonNull) {
          atom.otsFragment = jsonObject.get("otsFragment").asString
        }

        // Reconstruct meta array if present
        if (jsonObject.has("meta") && jsonObject.get("meta").isJsonArray) {
          val metaArray = jsonObject.getAsJsonArray("meta")
          val metaList = mutableListOf<MetaData>()
          
          for (metaElement in metaArray) {
            if (metaElement.isJsonObject) {
              val metaObj = metaElement.asJsonObject
              if (metaObj.has("key") && metaObj.has("value")) {
                metaList.add(MetaData(
                  key = metaObj.get("key").asString,
                  value = metaObj.get("value")?.takeIf { !it.isJsonNull }?.asString
                ))
              }
            }
          }
          
          atom.meta = metaList.toList()
        }

        atom

      } catch (e: Exception) {
        throw Exception("Atom deserialization failed: ${e.message}")
      }
    }

    /**
     * Sort the atoms in a Molecule using JavaScript-compatible comparison logic
     */
    @JvmStatic
    fun sortAtoms(atoms: List<Atom>): List<Atom> {
      val atomList = atoms.toMutableList()

      // JavaScript-compatible atom sorting for cross-SDK consistency
      atomList.sortWith { a, b ->
        when {
          a.index != b.index -> a.index.compareTo(b.index)
          else -> 0
        }
      }

      return atomList.toList()
    }

    /**
     * Produces a hash of the atoms inside a molecule using JavaScript-compatible patterns.
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

        // Get atom properties in JavaScript-compatible order
        getHashableValues(atom).forEach { (property, value) ->

          // JavaScript-compatible null handling: position and walletAddress always included
          if (value == null) {
            if (property in listOf("position", "walletAddress")) {
              molecularSponge.absorb("")
            }
            // Skip other null values (JavaScript behavior)
            return@forEach
          }

          // Handle different property types with JavaScript-compatible patterns
          when (property) {
            "meta" -> {
              // Process metadata with proper ordering
              @Suppress("UNCHECKED_CAST") 
              val metaList = value as List<MetaData>
              for (metaData in metaList) {
                if (metaData.value != null) {
                  molecularSponge.absorb(metaData.key)
                  molecularSponge.absorb(metaData.value!!)
                }
              }
            }
            "isotope" -> {
              // Isotope character handling
              molecularSponge.absorb(value.toString())
            }
            else -> {
              // All other properties as strings
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
        "base17" -> {
          val hexString = molecularSponge.hexString(32)
          val base17String = Strings.charsetBaseConvert(
            hexString, 16, 17, "0123456789abcdef", "0123456789abcdefg"
          )
          // Ensure proper 64-character padding with '0' for JavaScript compatibility
          base17String.padStart(64, '0')
        }
        else -> null
      }
    }
  }

  /**
   * Returns JSON-ready object for cross-SDK compatibility (Kotlin best practices)
   * 
   * Provides clean serialization of atomic operations with optional OTS fragments following
   * JavaScript canonical patterns for perfect cross-platform compatibility.
   *
   * @param includeOtsFragments Include OTS signature fragments (default: true)
   * @param validateFields Validate required fields (default: false)
   * @return JSON-serializable Map
   * @throws Exception If atom is in invalid state for serialization
   */
  @JvmOverloads
  fun toJSON(
    includeOtsFragments: Boolean = true,
    validateFields: Boolean = false
  ): Map<String, Any?> {
    return try {
      // Validate required fields if requested
      if (validateFields) {
        val requiredFields = listOf("position", "walletAddress", "isotope", "token")
        for (field in requiredFields) {
          val fieldValue = when (field) {
            "position" -> position
            "walletAddress" -> walletAddress
            "isotope" -> isotope.toString()
            "token" -> token
            else -> null
          }
          if (fieldValue.isNullOrBlank()) {
            throw IllegalArgumentException("Required field '$field' is missing or empty")
          }
        }
      }

      // Core atom properties (always included)
      val serialized = mutableMapOf<String, Any?>(
        "position" to position,
        "walletAddress" to walletAddress,
        "isotope" to isotope.toString(),
        "token" to token,
        "value" to value,
        "batchId" to batchId,
        "metaType" to metaType,
        "metaId" to metaId,
        "meta" to meta,
        "index" to index,
        "createdAt" to createdAt
      )

      // Optional OTS fragments (can be large, so optional)
      if (includeOtsFragments && !otsFragment.isNullOrBlank()) {
        serialized["otsFragment"] = otsFragment
      }

      serialized.toMap()

    } catch (e: Exception) {
      throw Exception("Atom serialization failed: ${e.message}")
    }
  }

  fun toJson(): String {
    return jsonFormat.encodeToString(this)
  }

  override fun toString(): String {
    return toJson()
  }
}