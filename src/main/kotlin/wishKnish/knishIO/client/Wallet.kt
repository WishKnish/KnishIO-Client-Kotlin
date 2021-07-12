package wishKnish.knishIO.client

import wishKnish.knishIO.client.libraries.Crypto
import wishKnish.knishIO.client.libraries.Shake256
import wishKnish.knishIO.client.libraries.Strings
import java.math.BigInteger


class Wallet(
    secret: String? = null,
    var token: String = "USER",
    var position: String? = null,
    var batchId: String? = null,
    var characters: String? = null
) {
    companion object {

        @JvmStatic
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
    var tokenUnits = arrayListOf<Map<String, Any>>()
    var bundle: String? = null



    init {
        this.bundle = secret?.let {
            this.position = this.position ?: Crypto.generateWalletPosition()
            prepareKeys(it)
            Crypto.generateBundleHash(it)
        }
    }

    private fun prepareKeys(secret: String) {
        if (this.key == null && this.address == null) {

            this.key = generatePrivateKey(
                secret = secret,
                token = this.token,
                position = this.position as String
            )
            this.address = generatePublicKey(key = this.key as String)
        }
    }

    fun initBatchId(sourceWallet: Wallet, remainder: Boolean = false) {
        sourceWallet.batchId?.let {
            this.batchId = if (remainder) it else Crypto.generateBatchId()
        }
    }

    fun isShadow(): Boolean {
        return this.position == null && this.address == null
    }

}
