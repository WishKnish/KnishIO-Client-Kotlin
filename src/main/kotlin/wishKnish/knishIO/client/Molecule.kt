package wishKnish.knishIO.client


import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import wishKnish.knishIO.client.data.MetaData
import wishKnish.knishIO.client.exception.BalanceInsufficientException
import wishKnish.knishIO.client.exception.MetaMissingException
import wishKnish.knishIO.client.exception.NegativeAmountException
import wishKnish.knishIO.client.libraries.DEFAULT_META_CONTEXT
import wishKnish.knishIO.client.libraries.Strings
import wishKnish.knishIO.client.libraries.USE_META_CONTEXT
import wishKnish.knishIO.client.libraries.merge


@Serializable
data class Molecule(
    val secret: String,
    @Transient var sourceWallet: Wallet = Wallet(),
    @Transient var remainderWallet: Wallet? = null,
    var cellSlug: String?
    ) {

    init {
        requireNotNull(sourceWallet.position) {
            return@requireNotNull "SourceWallet parameter not initialized by valid wallet"
        }

        remainderWallet = remainderWallet ?: Wallet.create(
            secretOrBundle = secret,
            token = sourceWallet.token,
            batchId = sourceWallet.batchId,
            characters = sourceWallet.characters
        )

        clear()
    }

    var createdAt = Strings.currentTimeMillis()
    var status: String? = null
    var bundle: String? = null
    var molecularHash: String? = null
    var cellSlugOrigin = cellSlug
    var atoms = mutableListOf<Atom>()



    val continuIdMetaType
        get() = "walletBundle"

    companion object {
        private val jsonFormat: Json
            get() =  Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
            }

        @JvmStatic
        fun generateNextAtomIndex(atoms: List<Atom>): Int {
            val length = atoms.size - 1

            return if (length > -1) atoms[length].index + 1 else 0
        }

        @JvmStatic
        fun jsonToObject(json: String): Molecule {
            return jsonFormat.decodeFromString(json)
        }
    }

    private fun toJson(): String {
        return jsonFormat.encodeToString(this)
    }

    override fun toString(): String {
        return toJson()
    }

    fun clear(): Molecule {
        createdAt = Strings.currentTimeMillis()
        bundle = null
        status = null
        molecularHash = null
        atoms = mutableListOf()

        return this
    }

    fun fill( molecule: Molecule ): Molecule {
        return this merge molecule
    }

    fun addAtom(atom: Atom): Molecule {
        molecularHash = null
        atoms.add(atom)
        atoms = Atom.sortAtoms(atoms).toMutableList()

        return this
    }

    fun finalMetas(metas: MutableList<MetaData>? = null, wallet: Wallet? = null): List<MetaData> {
        return (metas ?: mutableListOf()).let {
            (wallet ?: sourceWallet).let { wallet ->
                if (wallet.hasTokenUnits()) {
                    it.add(MetaData(key = "tokenUnits", value = wallet.tokenUnitsJson()))
                }

                it.add(MetaData(key = "pubkey", value = wallet.pubkey))
                it.add(MetaData(key = "characters", value = wallet.characters))
            }

            it
        }.toList()
    }

    fun contextMetas(metas: MutableList<MetaData>? = null, context: String? = null): List<MetaData> {
        return (metas ?: mutableListOf()).let {
            if (USE_META_CONTEXT) {
                it.add(MetaData(key = "context", value = context ?: DEFAULT_META_CONTEXT))
            }

            it
        }.toList()
    }

    fun addUserRemainderAtom(userRemainderWallet: Wallet): Molecule {
        return addAtom(
            Atom(
                position = userRemainderWallet.position!!,
                walletAddress = userRemainderWallet.address!!,
                isotope = "I",
                token = userRemainderWallet.token,
                metaType = "walletBundle",
                metaId = userRemainderWallet.bundle,
                meta = finalMetas(wallet = userRemainderWallet),
                index = generateIndex()
            )
        )
    }

    fun generateIndex(): Int {
        return generateNextAtomIndex(atoms)
    }

    fun replenishTokens(amount: Number, token: String, metas: MutableList<MetaData>? = null): Molecule {
        val meta = (metas ?: mutableListOf()).let {
            it.add(MetaData(key = "action", value = "add"))
            setOf("address", "position", "batchId").forEach { key ->
                it.forEach { metaData ->
                    if (metaData.key == key && metaData.value == null) {
                        throw MetaMissingException("Molecule::replenishTokens() - Missing $key in meta!")
                    }
                }
            }

            it
        }

        addAtom(
            Atom(
                position = sourceWallet.position!!,
                walletAddress = sourceWallet.address!!,
                isotope = "C",
                token = sourceWallet.token,
                value = amount.toString(),
                batchId = sourceWallet.batchId,
                metaType = "token",
                metaId = token,
                meta = finalMetas(meta),
                index = generateIndex()
            )
        )

        return addUserRemainderAtom(remainderWallet!!)
    }

    fun burnToken(amount: Number, walletBundle: String?): Molecule {
        if (amount.toDouble() < 0.0) {
            throw NegativeAmountException("Molecule::burnToken() - Amount to burn must be positive!")
        }

        if (sourceWallet.balance - amount.toDouble() < 0.0) {
            throw BalanceInsufficientException()
        }

        // Initializing a new Atom to remove tokens from source
        addAtom(
            Atom(
                position = sourceWallet.position!!,
                walletAddress = sourceWallet.address!!,
                isotope = "V",
                token = sourceWallet.token,
                value = (-amount.toDouble()).toString(),
                batchId = sourceWallet.batchId,
                meta = finalMetas(),
                index = generateIndex()
            )
        )

        return addAtom(
            Atom(
                position = remainderWallet!!.position!!,
                walletAddress = remainderWallet!!.address!!,
                isotope = "V",
                token = sourceWallet.token,
                value = (sourceWallet.balance - amount.toDouble()).toString(),
                batchId = remainderWallet!!.batchId,
                metaType = walletBundle?.let { "walletBundle" },
                metaId = walletBundle,
                meta = finalMetas(wallet = remainderWallet),
                index = generateIndex()
            )
        )
    }

    fun initValue(recipientWallet: Wallet, amount: Number): Molecule {
        if (sourceWallet.balance - amount.toDouble() < 0) {
            throw BalanceInsufficientException()
        }

        // Initializing a new Atom to remove tokens from source
        addAtom(
            Atom(
                position = sourceWallet.position!!,
                walletAddress = sourceWallet.address!!,
                isotope = "V",
                token = sourceWallet.token,
                value = (-amount.toDouble()).toString(),
                batchId = sourceWallet.batchId,
                meta = finalMetas(),
                index = generateIndex()
            )
        )

        // Initializing a new Atom to add tokens to recipient
        addAtom(
            Atom(
                position = recipientWallet.position!!,
                walletAddress = recipientWallet.address!!,
                isotope = "V",
                token = sourceWallet.token,
                value = amount.toDouble().toString(),
                batchId = recipientWallet.batchId,
                metaType = "walletBundle",
                metaId = recipientWallet.bundle,
                meta = finalMetas(wallet = recipientWallet),
                index = generateIndex()
            )
        )

        return addAtom(
            Atom(
                position = remainderWallet!!.position!!,
                walletAddress = remainderWallet!!.address!!,
                isotope = "V",
                token = sourceWallet.token,
                value = (sourceWallet.balance - amount.toDouble()).toString(),
                batchId = remainderWallet!!.batchId,
                metaType = "walletBundle",
                metaId = sourceWallet.bundle,
                meta = finalMetas(wallet = remainderWallet),
                index = generateIndex()
            )
        )
    }
}
