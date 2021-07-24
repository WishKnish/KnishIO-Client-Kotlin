@file:JvmName("Soda")

package wishKnish.knishIO.client.libraries

import kotlinx.serialization.json.Json
import com.iwebpp.crypto.TweetNaclFast
import kotlinx.serialization.SerializationException
import java.security.GeneralSecurityException
import kotlin.jvm.Throws


class Soda(private val base: String = "GMP") {

  @Throws(IllegalArgumentException::class, GeneralSecurityException::class)
  fun encrypt(
    message: List<*>,
    publicKey: String
  ): String {
    return encode(message.toJsonElement().toString().toByteArray().seal(decode(publicKey)))
  }

  @Throws(IllegalArgumentException::class, GeneralSecurityException::class)
  fun encrypt(
    message: Map<*, *>,
    publicKey: String
  ): String {
    return encode(message.toJsonElement().toString().toByteArray().seal(decode(publicKey)))
  }

  @Throws(IllegalArgumentException::class, GeneralSecurityException::class)
  fun encrypt(
    message: String,
    publicKey: String
  ): String {
    return encode(message.toByteArray().seal(decode(publicKey)))
  }

  @Throws(NumberFormatException::class, IllegalArgumentException::class, SerializationException::class)
  fun decrypt(
    decrypted: String,
    privateKey: String,
    publicKey: String
  ): Any? {
    val data = decode(decrypted).sealOpen(decode(publicKey), decode(privateKey))

    if (data.isEmpty()) {
      return null
    }

    return Json.parseToJsonElement(data.joinToString("") { "${it.toInt().toChar()}" }).decode()
  }

  @Throws(IllegalArgumentException::class)
  fun generatePrivateKey(key: String): String {
    val sponge = Shake256.create()
    sponge.absorb(key)
    return encode(sponge.squeeze(TweetNaclFast.Box.secretKeyLength))
  }

  @Throws(
    NumberFormatException::class, IllegalArgumentException::class
  )
  fun generatePublicKey(privateKey: String): String {
    return encode(TweetNaclFast.Box.keyPair_fromSecretKey(decode(privateKey)).publicKey)
  }

  @Throws(IllegalArgumentException::class)
  fun shortHash(key: String): String {
    return encode((Shake256.create()).absorb(key).squeeze(8))
  }

  @Throws(NumberFormatException::class, IllegalArgumentException::class)
  fun decode(data: String): ByteArray {
    return data.decodeBase58(this.base)
  }

  @Throws(IllegalArgumentException::class)
  fun encode(data: ByteArray): String {
    return data.encodeToBase58String(this.base)
  }
}
