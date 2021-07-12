package wishKnish.knishIO.client.libraries

import kotlinx.serialization.json.Json
import com.iwebpp.crypto.TweetNaclFast


class Soda (private val base: String){

    fun <T: Collection<*>>encrypt(message: T, publicKey: String): String {
        val data = when(message) {
            is List<*> -> message.toJsonElement().toString().toByteArray()
            is Map<*, *> -> message.toJsonElement().toString().toByteArray()
            else -> throw IllegalArgumentException("The message parameter must be of type List or Map")
        }

        return this.encode(data.seal(this.decode(publicKey)))
    }

    fun decrypt(decrypted: String, privateKey: String, publicKey: String): Any? {
        val data = this
            .decode(decrypted)
            .sealOpen(this.decode(publicKey), this.decode(privateKey))

        if (data.isEmpty()) {
            return null
        }

        return Json.parseToJsonElement(data.joinToString("") {"${it.toInt().toChar()}"}).decode()
    }

    fun generatePrivateKey(key: String): String {
        val sponge = Shake256.create()
        sponge.absorb(key)
        return this.encode(sponge.squeeze(TweetNaclFast.Box.secretKeyLength))
    }

    fun decode(data: String): ByteArray {
        return data.decodeBase58(this.base)
    }

    fun encode(data: ByteArray): String {
        return data.encodeToBase58String(this.base)
    }
}
