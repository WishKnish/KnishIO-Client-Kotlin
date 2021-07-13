package wishKnish.knishIO.client.libraries

import org.bouncycastle.crypto.digests.SHAKEDigest
import org.bouncycastle.util.encoders.Hex


class Shake256 {
    private val digest = SHAKEDigest(256)

    companion object {
        @JvmStatic
        fun create(): Shake256 {
            return Shake256()
        }

        @JvmStatic
        fun hash(data: String, length: Int): String {
            val shaked = SHAKEDigest(256)

            data.forEach {
                shaked.update(it.code.toByte())
            }

            val output = ByteArray(length)

            shaked.doFinal(output, 0, length)

            return Hex.toHexString(output)
        }
    }

    fun absorb(text: String): Shake256 {
        text.forEach {
            digest.update(it.code.toByte())
        }

        return this
    }

    fun squeeze(length: Int): ByteArray {
        val output = ByteArray(length)

        digest.doFinal(output, 0, length)

        return output
    }

    fun hexString(length: Int): String {
        return Hex.toHexString(this.squeeze(length))
    }
}
