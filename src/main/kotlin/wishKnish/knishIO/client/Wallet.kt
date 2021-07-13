package wishKnish.knishIO.client

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.bouncycastle.util.encoders.Base64
import wishKnish.knishIO.client.libraries.*
import wishKnish.knishIO.client.data.UnitData
import java.math.BigInteger
import java.security.GeneralSecurityException
import kotlin.jvm.Throws


class Wallet(
    secret: String? = null,
    var token: String = "USER",
    var position: String? = null,
    var batchId: String? = null,
    var characters: String? = null
) {
    companion object {

        @JvmStatic
        @Throws(NoSuchElementException::class)
        fun create(
            secretOrBundle: String? = null,
            token: String = "USER",
            batchId: String? = null,
            characters: String? = null
        ) : Wallet {
            val secret = secretOrBundle?.let {
                if (isBundleHash(it)) {
                    null
                }
                else it
            }

            val position = secret?.let {
                Crypto.generateWalletPosition()
            }

            return  Wallet(secret = secret, token = token, position = position, batchId = batchId, characters = characters).apply {
                bundle = secret?.let {
                    Crypto.generateBundleHash(it)
                } ?: secretOrBundle
            }
        }

        @JvmStatic
        fun getTokenUnits(unitsData: List<List<String>>): List<UnitData> {
            val result = mutableListOf<UnitData>()

            unitsData.forEach {
                result.add(UnitData(id = it[0], name = it[1], metas = it))
            }

            return result.toList()
        }

        @JvmStatic
        @Throws(NumberFormatException::class)
        fun generatePrivateKey(secret: String, token: String? = null, position: String) : String {
            val bigIntSecret = BigInteger(secret, 16)
            val indexedKey = bigIntSecret.add(BigInteger(position, 16))
            val intermediateKeySponge = Shake256.create()

            intermediateKeySponge.absorb(indexedKey.toString(16))
            token?.let { intermediateKeySponge.absorb(it) }

            return Shake256.hash(intermediateKeySponge.hexString(1024), 1024)
        }

        @JvmStatic
        fun isBundleHash(code: String) : Boolean {
            return code.length == 64 && code.matches(Regex("^(?:[a-fA-F0-9]+)?\$"))
        }

        @JvmStatic
        @Throws(NoSuchElementException::class)
        fun generateWalletPosition(saltLength:  Int = 64) : String {
            return Strings.randomString(saltLength)
        }

        @JvmStatic
        fun generatePublicKey(key: String) : String {
            val digestSponge = Shake256.create()

            key.chunked(128).forEach {
                var workingFragment = it

                (1..16).forEach { _ ->
                    workingFragment = Shake256.hash(workingFragment, 64)
                }

                digestSponge.absorb(workingFragment)
            }

            return Shake256.hash(digestSponge.hexString(1024), 32)
        }
    }

    var balance: Double = 0.0
    var key: String? = null
    var address: String? = null
    var privkey: String? = null
    var pubkey: String? = null
    var tokenUnits = arrayListOf<UnitData>()
    var bundle: String? = null

    init {
        bundle = secret?.let {
            position = position ?: Crypto.generateWalletPosition()
            prepareKeys(it)
            Crypto.generateBundleHash(it)
        }
    }

    private fun prepareKeys(secret: String) {
        if (key == null && address == null) {

            key = generatePrivateKey(
                secret = secret,
                token = token,
                position = position as String
            )
            address = generatePublicKey(key = key as String)
        }
    }

    fun initBatchId(sourceWallet: Wallet, remainder: Boolean = false) {
        sourceWallet.batchId?.let {
            batchId = if (remainder) it else Crypto.generateBatchId()
        }
    }

    fun isShadow(): Boolean {
        return position == null && address == null
    }

    fun hasTokenUnits(): Boolean {
        return tokenUnits.isNotEmpty()
    }

    fun tokenUnitsJson(): String? {
        if (hasTokenUnits()) {
            val result = arrayListOf<List<String>>()
            tokenUnits.forEach {
                result.add(listOf(it.id, it.name) + it.metas)
            }

            return result.toJsonElement().toString()
        }

        return null
    }

    fun splitUnits(units: List<UnitData>, remainderWallet: Wallet, recipientWallet: Wallet? = null) {

        // No units supplied, nothing to split
        if (units.isEmpty()) {
            return
        }

        // Init recipient & remainder token units
        val recipientTokenUnits = arrayListOf<UnitData>()
        val remainderTokenUnits = arrayListOf<UnitData>()

        tokenUnits.forEach {

            units.filter { unit ->
                it.id == unit.id
            }.forEach { unit ->
                recipientTokenUnits.add(unit)
            }

            units.filter { unit ->
                it.id != unit.id
            }.forEach { unit ->
                remainderTokenUnits.add(unit)
            }
        }

        // Reset token units to the sending value
        tokenUnits = recipientTokenUnits

        recipientWallet?.let {
            it.tokenUnits = recipientTokenUnits
        }

        remainderWallet.tokenUnits = remainderTokenUnits
    }

    @Throws(IllegalArgumentException::class)
    fun getMyEncPrivateKey(): String? {
        if (privkey == null) {
            key?.let {
                privkey = Crypto.generateEncPrivateKey(it, characters ?: "GMP")
            }
        }

        return privkey
    }

    @Throws(IllegalArgumentException::class, NumberFormatException::class)
    fun getMyEncPublicKey(): String? {
        val privateKey = getMyEncPrivateKey()

        if (pubkey == null) {
            privateKey?.let {
                pubkey = Crypto.generateEncPublicKey(it, characters ?: "GMP")
            }
        }

        return pubkey
    }

    @Throws(IllegalArgumentException::class, GeneralSecurityException::class)
    fun encryptMyMessage(message: List<*>, vararg publicKeys: String): Map<String, String> {
        val encrypt = mutableMapOf<String, String>()

        publicKeys.forEach {
            encrypt[Crypto.hashShare(it, characters ?: "GMP")] = Crypto.encryptMessage(
                message,
                it,
                characters ?: "GMP"
            )
        }

        return encrypt.toMap()
    }

    @Throws(IllegalArgumentException::class, GeneralSecurityException::class)
    fun encryptMyMessage(message: Map<*, *>, vararg publicKeys: String): Map<String, String> {
        val encrypt = mutableMapOf<String, String>()

        publicKeys.forEach {
            encrypt[Crypto.hashShare(it, characters ?: "GMP")] = Crypto.encryptMessage(
                message,
                it,
                characters ?: "GMP"
            )
        }

        return encrypt.toMap()
    }

    @Throws(IllegalArgumentException::class, GeneralSecurityException::class)
    fun encryptString(message: String, vararg publicKeys: String): String {
        val keys = publicKeys.toMutableList()
        val encrypt = mutableMapOf<String, String>()
        getMyEncPublicKey()?.let {
            keys.add(it)
        }
        keys.forEach {
            encrypt[Crypto.hashShare(it, characters ?: "GMP")] = Crypto.encryptMessage(
                message,
                it,
                characters ?: "GMP"
            )
        }

        return Base64.toBase64String(encrypt.toJsonElement().toString().toByteArray())
    }

    @Throws(IllegalArgumentException::class, GeneralSecurityException::class, SerializationException::class)
    fun decryptString(data: String, fallbackValue: String? = null): Any? {
        return try {
            val message = Json.parseToJsonElement(
                Base64.decode(data).joinToString("") { "${it.toInt().toChar()}" }
            ).decode()
            val decrypt = (message as? Map<String, String>)?.let { decryptMyMessage(it) }

            decrypt ?: fallbackValue
        } catch (e: Exception){
            fallbackValue
        }
    }

    @Throws(IllegalArgumentException::class, GeneralSecurityException::class, SerializationException::class)
    fun decryptMyMessage(message: String): Any? {
        val privateKey = getMyEncPrivateKey() ?: ""
        val publicKey = getMyEncPublicKey() ?: ""

        return Crypto.decryptMessage(message, privateKey, publicKey, characters ?: "GMP")
    }

    @Throws(IllegalArgumentException::class, GeneralSecurityException::class, SerializationException::class)
    fun decryptMyMessage(message: Map<String, String>): Any? {
        val publicKey = getMyEncPublicKey() ?: ""
        val privateKey = getMyEncPrivateKey() ?: ""
        val encrypt = message[Crypto.hashShare(publicKey)] ?: ""

        return Crypto.decryptMessage(encrypt, privateKey, publicKey, characters ?: "GMP")
    }

}
