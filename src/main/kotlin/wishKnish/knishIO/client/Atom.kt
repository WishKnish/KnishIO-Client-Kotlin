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


@Serializable
data class Atom(
  @JvmField var position: String, @JvmField var walletAddress: String, @JvmField var isotope: Char, @JvmField var token: String, @JvmField var value: String? = null, @JvmField var batchId: String? = null, @JvmField var metaType: String? = null, @JvmField var metaId: String? = null, @JvmField var meta: List<MetaData> = mutableListOf(), @JvmField var otsFragment: String? = null, @JvmField var index: Int = 0, @JvmField val createdAt: String = Strings.currentTimeMillis()
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

    @JvmStatic
    fun jsonToObject(json: String): Atom {
      return jsonFormat.decodeFromString(json)
    }

    @JvmStatic
    fun sortAtoms(atoms: List<Atom>): List<Atom> {
      val atomList = atoms.toMutableList()

      atomList.sortBy { it.index }

      return atomList.toList()
    }

    @JvmStatic
    private fun hash(atoms: List<Atom>): Shake256 {
      val atomList = sortAtoms(atoms)
      val molecularSponge = Shake256.create()
      val numberOfAtoms = atomList.size.toString()

      for (atom in atomList) {
        molecularSponge.absorb(numberOfAtoms)

        molecularHashSchema(atom).forEach { (property, value) ->
          if (value == null && property in arrayListOf("batchId", "pubkey", "characters")) {
            return@forEach
          }

          if (property == "meta") {
            @Suppress("UNCHECKED_CAST") (value as List<MetaData>).forEach { MetaData ->
              MetaData.value?.run {
                molecularSponge.absorb(MetaData.key)
                molecularSponge.absorb(this)
              }
            }
            return@forEach
          }

          if (property in arrayListOf("position", "walletAddress", "isotope")) {
            val content = value ?: ""

            molecularSponge.absorb(content.toString())

            return@forEach
          }

          value?.run {
            molecularSponge.absorb(toString())
          }
        }
      }

      return molecularSponge
    }

    @JvmStatic
    @Throws(IllegalArgumentException::class)
    fun hashAtoms(atoms: List<Atom>, output: String = "base17"): String? {
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
