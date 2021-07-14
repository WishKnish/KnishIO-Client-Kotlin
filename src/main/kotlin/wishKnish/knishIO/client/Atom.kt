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
data class  Atom(
    var position: String,
    var walletAddress: String,
    var isotope: String,
    var token: String? = null,
    var value: String? = null,
    var batchId: String? = null,
    var metaType: String? = null,
    var metaId: String? = null,
    var meta: List<MetaData> = mutableListOf(),
    var otsFragment: String? = null,
    var index: Int = 0,
    val createdAt: String = Strings.currentTimeMillis()
    ) {

    companion object {
        private val jsonFormat: Json
            get() =  Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
            }

        @JvmStatic
        fun jsonToObject(json: String): Atom {
            return jsonFormat.decodeFromString(json)
        }

        @JvmStatic
        fun sortAtoms(atoms: List<Atom>) : List<Atom> {
            val atomList = atoms.toMutableList()

            atomList.sortBy { it.index }

            return atomList.toList()
        }

        @JvmStatic
        private fun hash(atoms: List<Atom>) : Shake256 {
            val atomList = sortAtoms(atoms)
            val molecularSponge = Shake256.create()
            val numberOfAtoms = atomList.size.toString()

            for (atom in atomList) {
                molecularSponge.absorb(numberOfAtoms)

                atom::class.memberProperties.forEach {
                    if (it.visibility == KVisibility.PUBLIC) {
                        val value = it.getter.call(atom)

                        if (value == null && it.name in arrayListOf("batchId", "pubkey", "characters")) {
                            return@forEach
                        }

                        if (it.name in arrayListOf("otsFragment", "index")) {
                            return@forEach
                        }

                        if (it.name == "meta") {
                            (value as List<MetaData>).forEach { MetaData ->
                                MetaData.value?.run {
                                    molecularSponge.absorb(MetaData.key)
                                    molecularSponge.absorb(this)
                                }
                            }
                            return@forEach
                        }

                        if (it.name in arrayListOf("position", "walletAddress", "isotope")) {
                            val content = value ?: ""

                            molecularSponge.absorb(content.toString())

                            return@forEach
                        }

                        value?.run {
                            molecularSponge.absorb(toString())
                        }
                    }
                }
            }

            return molecularSponge
        }

        @JvmStatic
        @Throws(IllegalArgumentException::class)
        fun hashAtoms(atoms: List<Atom>, output: String = "base17") : String? {
            val molecularSponge = hash(atoms)

            return when(output) {
                "hex" -> molecularSponge.hexString(32)
                "base17" -> Strings.charsetBaseConvert(
                    molecularSponge.hexString(32),
                    16,
                    17,
                    "0123456789abcdef",
                    "0123456789abcdefg"
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
