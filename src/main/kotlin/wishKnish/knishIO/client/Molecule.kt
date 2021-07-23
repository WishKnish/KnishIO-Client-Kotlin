@file:JvmName("Molecule")
package wishKnish.knishIO.client


import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import wishKnish.knishIO.client.data.MetaData
import wishKnish.knishIO.client.exception.*
import wishKnish.knishIO.client.libraries.*
import kotlin.jvm.Throws
import kotlin.math.ceil
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties


@Serializable
data class Molecule(
    @Transient val secret: String? = null,
    @Transient var sourceWallet: Wallet = Wallet(),
    @Transient var remainderWallet: Wallet? = null,
    @JvmField var cellSlug: String? = null
    ) {

    @JvmField var createdAt: String = Strings.currentTimeMillis()
    @JvmField var status: String? = null
    @JvmField var bundle: String? = null
    @JvmField var molecularHash: String? = null
    @JvmField var atoms: MutableList<Atom> = mutableListOf()

    init {
        if (sourceWallet.position == null && molecularHash == null) {
            throw IllegalArgumentException("SourceWallet parameter not initialized by valid wallet")
        }

        if (molecularHash == null) {
            remainderWallet = remainderWallet ?: Wallet.create(
                secretOrBundle = secret,
                token = sourceWallet.token,
                batchId = sourceWallet.batchId,
                characters = sourceWallet.characters
            )

            clear()
        }
    }

    @Transient
    var cellSlugOrigin = cellSlug
    val continuIdMetaType
        get() = "walletBundle"
    val cellSlugDelimiter
        get() = "."

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

        @JvmStatic
        @Throws(
            MolecularHashMissingException::class,
            AtomsMissingException::class,
            AtomIndexException::class,
            TransferMismatchedException::class,
            TransferMalformedException::class,
            IllegalArgumentException::class,
            TransferToSelfException::class,
            TransferBalanceException::class,
            TransferRemainderException::class,
            SignatureMalformedException::class,
            SignatureMismatchException::class,
            MolecularHashMismatchException::class,
            MetaMissingException::class,
            WrongTokenTypeException::class
        )
        fun verify(molecule: Molecule, sourceWallet: Wallet? = null): Boolean {
            return CheckMolecule.molecularHash(molecule)
                    && CheckMolecule.ots(molecule)
                    && CheckMolecule.index(molecule)
                    && CheckMolecule.batchId(molecule)
                    && CheckMolecule.continuId(molecule)
                    && CheckMolecule.isotopeM(molecule)
                    && CheckMolecule.isotopeT(molecule)
                    && CheckMolecule.isotopeC(molecule)
                    && CheckMolecule.isotopeU(molecule)
                    && CheckMolecule.isotopeI(molecule)
                    && CheckMolecule.isotopeR(molecule)
                    && CheckMolecule.isotopeV(molecule, sourceWallet)
        }
    }

    fun cellSlugBase(): String {
        return (cellSlug ?: "").split(cellSlugDelimiter).first()
    }

    private fun toJson(): String {
        return jsonFormat.encodeToString(this)
    }

    override fun toString(): String {
        return toJson()
    }

    @Throws(
        MolecularHashMissingException::class,
        AtomsMissingException::class,
        AtomIndexException::class,
        TransferMismatchedException::class,
        TransferMalformedException::class,
        IllegalArgumentException::class,
        TransferToSelfException::class,
        TransferBalanceException::class,
        TransferRemainderException::class,
        SignatureMalformedException::class,
        SignatureMismatchException::class,
        MolecularHashMismatchException::class,
        MetaMissingException::class,
        WrongTokenTypeException::class
    )
    fun check(sourceWallet: Wallet? = null): Boolean {
        return verify(this, sourceWallet)
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

    fun finalMetas(metas: MutableList<MetaData> = mutableListOf(), wallet: Wallet? = null): List<MetaData> {
        return metas.also {
            (wallet ?: sourceWallet).let { wallet ->
                if (wallet.hasTokenUnits()) {
                    it.add(MetaData(key = "tokenUnits", value = wallet.tokenUnitsJson()))
                }

                it.add(MetaData(key = "pubkey", value = wallet.pubkey))
                it.add(MetaData(key = "characters", value = wallet.characters))
            }
        }.toList()
    }

    fun contextMetas(metas: MutableList<MetaData> = mutableListOf(), context: String? = null): List<MetaData> {
        return metas.also {
            if (USE_META_CONTEXT) {
                it.add(MetaData(key = "context", value = context ?: DEFAULT_META_CONTEXT))
            }
        }.toList()
    }

    fun addUserRemainderAtom(userRemainderWallet: Wallet): Molecule {
        return addAtom(
            Atom(
                position = userRemainderWallet.position!!,
                walletAddress = userRemainderWallet.address!!,
                isotope = 'I',
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

    @Throws(MetaMissingException::class)
    fun replenishTokens(amount: Number, token: String, metas: MutableList<MetaData> = mutableListOf()): Molecule {
        val meta = metas.also {
            setOf("address", "position", "batchId").forEach { key ->
                it.forEach { metaData ->
                    if (metaData.key == key && metaData.value == null) {
                        throw MetaMissingException("Molecule::replenishTokens() - Missing $key in meta!")
                    }
                }
            }
            it.add(MetaData(key = "action", value = "add"))
        }

        addAtom(
            Atom(
                position = sourceWallet.position!!,
                walletAddress = sourceWallet.address!!,
                isotope = 'C',
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

    @Throws(NegativeAmountException::class)
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
                isotope = 'V',
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
                isotope = 'V',
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

    @Throws(BalanceInsufficientException::class)
    fun initValue(recipientWallet: Wallet, amount: Number): Molecule {
        if (sourceWallet.balance - amount.toDouble() < 0) {
            throw BalanceInsufficientException()
        }

        // Initializing a new Atom to remove tokens from source
        addAtom(
            Atom(
                position = sourceWallet.position!!,
                walletAddress = sourceWallet.address!!,
                isotope = 'V',
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
                isotope = 'V',
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
                isotope = 'V',
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

    fun initWalletCreation(newWallet: Wallet): Molecule {
        val metas = mutableListOf(
            MetaData(key = "address", value = newWallet.address),
            MetaData(key = "token", value = newWallet.token),
            MetaData(key = "bundle", value = newWallet.bundle),
            MetaData(key = "position", value = newWallet.position),
            MetaData(key = "amount", value = "0"),
            MetaData(key = "batchId", value = newWallet.batchId)
        )

        addAtom(
            Atom(
                position = sourceWallet.position!!,
                walletAddress = sourceWallet.address!!,
                isotope = 'C',
                token = sourceWallet.token,
                batchId = sourceWallet.batchId,
                metaType = "wallet",
                metaId = sourceWallet.address,
                meta = finalMetas(
                    metas = contextMetas(metas).toMutableList(),
                    wallet = newWallet
                ),
                index = generateIndex()
            )
        )

        return addUserRemainderAtom(remainderWallet!!)
    }

    fun initTokenCreation(recipientWallet: Wallet, amount: Number, meta: MutableList<MetaData> = mutableListOf()): Molecule {
        val metas = meta.also {
            setOf("walletAddress", "walletPosition", "walletPubkey", "walletCharacters").forEach { key ->
                // Importing wallet fields into meta object
                if (it.none { mataData -> mataData.key == key }) {
                    Wallet::class.memberProperties.find { property ->
                        property.name == key.substring(6).lowercase()
                    }?.let { property ->
                        if (property.visibility == KVisibility.PUBLIC) {
                            val value = property.getter.call(recipientWallet) as? String
                            it.add(MetaData(key = key, value = value))
                        }
                    }
                }
            }
        }

        addAtom(
            Atom(
                position = sourceWallet.position!!,
                walletAddress = sourceWallet.address!!,
                isotope = 'C',
                token = sourceWallet.token,
                value = amount.toString(),
                batchId = recipientWallet.batchId,
                metaType = "token",
                metaId = recipientWallet.token,
                meta = finalMetas(metas = contextMetas(metas).toMutableList(), wallet = sourceWallet),
                index = generateIndex()
            )
        )

        return addUserRemainderAtom(remainderWallet!!)
    }

    @Throws(MetaMissingException::class)
    fun createRule(metaType: String, metaId: String, meta: MutableList<MetaData>): Molecule {
        setOf("conditions", "callback", "rule").forEach { key ->
            if (meta.none { mataData -> mataData.key == key }) {
                throw MetaMissingException("Molecule::createRule() - Value for required meta key $key in missing!")
            }
        }

        addAtom(
            Atom(
                position = sourceWallet.position!!,
                walletAddress = sourceWallet.address!!,
                isotope = 'R',
                token = sourceWallet.token,
                metaType = metaType,
                metaId = metaId,
                meta = finalMetas(metas = meta, wallet = sourceWallet),
                index = generateIndex()
            )
        )

        return addUserRemainderAtom(remainderWallet!!)
    }

    fun initShadowWalletClaim(token: String, wallet: Wallet): Molecule {
        val metas = mutableListOf(
            MetaData(key = "tokenSlug", value = token),
            MetaData(key = "walletAddress", value = wallet.address),
            MetaData(key = "walletPosition", value = wallet.position),
            MetaData(key = "batchId", value = wallet.batchId)
        )

        addAtom(
            Atom(
                position = sourceWallet.position!!,
                walletAddress = sourceWallet.address!!,
                isotope = 'C',
                token = sourceWallet.token,
                metaType = "wallet",
                metaId = wallet.address,
                meta = finalMetas(metas = contextMetas(metas).toMutableList(), wallet = wallet),
                index = generateIndex()
            )
        )

        return addUserRemainderAtom(remainderWallet!!)
    }

    fun initIdentifierCreation(type: String, contact: String, code: String): Molecule {
        val metas = mutableListOf(
            MetaData(key = "code", value = code),
            MetaData(key = "hash", value = Crypto.generateBundleHash(contact))
        )

        addAtom(
            Atom(
                position = sourceWallet.position!!,
                walletAddress = sourceWallet.address!!,
                isotope = 'C',
                token = sourceWallet.token,
                metaType = "identifier",
                metaId = type,
                meta = finalMetas(metas = metas, wallet = sourceWallet),
                index = generateIndex()
            )
        )

        return addUserRemainderAtom(remainderWallet!!)
    }

    fun initMeta(meta: MutableList<MetaData>, metaType: String, metaId: String): Molecule {
        addAtom(
            Atom(
                position = sourceWallet.position!!,
                walletAddress = sourceWallet.address!!,
                isotope = 'M',
                token = sourceWallet.token,
                batchId = sourceWallet.batchId,
                metaType = metaType,
                metaId = metaId,
                meta = finalMetas(metas = meta,  wallet = sourceWallet),
                index = generateIndex()
            )
        )

        return addUserRemainderAtom(remainderWallet!!)
    }

    fun initTokenRequest(
        token: String,
        amount: Number,
        metaType: String,
        metaId: String,
        meta: MutableList<MetaData> = mutableListOf(),
        batchId: String?
    ): Molecule {
        meta.find { mataData -> mataData.key == "token" }?.run { value = token } ?: meta.add(MetaData("token", token))

        addAtom(
            Atom(
                position = sourceWallet.position!!,
                walletAddress = sourceWallet.address!!,
                isotope = 'T',
                token = sourceWallet.token,
                value = amount.toString(),
                batchId = batchId,
                metaType = metaType,
                metaId = metaId,
                meta = finalMetas(metas = meta),
                index = generateIndex()
            )
        )

        return addUserRemainderAtom(remainderWallet!!)
    }

    fun initAuthorization(meta: MutableList<MetaData>): Molecule {
        return addAtom(
            Atom(
                position = sourceWallet.position!!,
                walletAddress = sourceWallet.address!!,
                isotope = 'U',
                token = sourceWallet.token,
                batchId = sourceWallet.batchId,
                meta = meta,
                index = generateIndex()
            )
        )
    }

    @Throws(AtomsMissingException::class, IllegalArgumentException::class)
    fun sign(anonymous: Boolean = false, compressed: Boolean = true): String? {

        requireNotNull(secret) {
            "The molecule was created without a secret signature. The operation is not possible."
        }

        // Do we have atoms?
        if (atoms.isEmpty()) {
            throw AtomsMissingException()
        }
        // Derive the user's bundle
        if (!anonymous) {
            bundle = Crypto.generateBundleHash(secret)
        }
        // Hash atoms to get molecular hash
        molecularHash = Atom.hashAtoms(atoms = atoms)
        // Determine first atom
        val firstAtom = atoms.first()
        // Generate the private signing key for this molecule
        val key = Wallet.generatePrivateKey(secret = secret, token = firstAtom.token, position = firstAtom.position)

        // Building a one-time-signature
        var signatureFragments = signatureFragments(key)

        // Compressing the OTS
        if (compressed) {
            signatureFragments = Strings.hexToBase64(signatureFragments)
        }

        var lastPosition: String? = null
        // Chunking the signature across multiple atoms
        signatureFragments.chunked(
            ceil(signatureFragments.length.toDouble() / atoms.size).toInt()
        ).forEachIndexed { chunkCount, chunk ->
            atoms[chunkCount].otsFragment = chunk
            lastPosition = atoms[ chunkCount ].position
        }

        return lastPosition
    }

    fun signatureFragments(key: String, encode: Boolean = true): String {
        // Subdivide Kk into 16 segments of 256 bytes (128 characters) each
        val keyChunks = key.chunked(128)
        // Convert Hm to numeric notation, and then normalize
        val normalizedHash = CheckMolecule.normalizedHash(molecularHash!!)

        // Building a one-time-signature
        return mutableListOf<String>().also {
            keyChunks.forEachIndexed { idx, keyChunk ->
                var workingChunk = keyChunk
                var iterationCount = 0
                val condition = 8 + normalizedHash[ idx ]!! * (if (encode) -1 else 1)

                while (iterationCount < condition) {
                    workingChunk = Shake256.hash(workingChunk, 64)
                    iterationCount++
                }

                it.add(workingChunk)
            }
        }.joinToString("")
    }
}
