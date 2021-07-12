package wishKnish.knishIO.client.libraries

class Crypto {
    companion object {

        @JvmStatic
        fun generateBundleHash(secret: String) : String {
            return Shake256.hash(secret, 32)
        }

        @JvmStatic
        fun generateSecret( seed: String? = null, length: Int = 2048 ) : String {
            return when(seed) {
                null -> Strings.randomString(length)
                else -> Shake256.hash(seed, length/4)
            }
        }

        @JvmStatic
        fun generateBatchId(molecularHash: String? = null, index: Int? = null) : String {
            if ( molecularHash != null && index != null) {
                return this.generateBundleHash("$molecularHash$index")
            }

            return Strings.randomString(64);
        }

        @JvmStatic
        fun generateWalletPosition(saltLength: Int = 64) : String {
            return Strings.randomString(saltLength)
        }
    }
}
