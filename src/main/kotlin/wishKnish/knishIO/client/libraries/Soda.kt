@file:JvmName("Soda")

package wishKnish.knishIO.client.libraries

import kotlinx.serialization.json.Json
import com.iwebpp.crypto.TweetNaclFast
import kotlinx.serialization.SerializationException
import java.security.GeneralSecurityException
import com.google.gson.Gson
import kotlinx.serialization.encodeToString
import kotlin.jvm.Throws
import kotlin.text.toByteArray


class Soda(private val base: String = "GMP") {
  companion object {
    private val jsonFormat: Json
      get() = Json {
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
      }
  }

  @Throws(IllegalArgumentException::class, GeneralSecurityException::class)
  fun <T : Collection<*>> encrypt(
    message: T,
    publicKey: String
  ): String {

    return encode(Gson().toJson(message).toByteArray().seal(decode(publicKey)))
  }

  @Throws(IllegalArgumentException::class, GeneralSecurityException::class)
  fun encrypt(
    message: String,
    publicKey: String
  ): String {
    return encode(jsonFormat.encodeToString(message).toByteArray().seal(decode(publicKey)))
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
    val preform = data.toString(Charsets.UTF_8)

    return try {
      jsonFormat.parseToJsonElement(preform)
    } catch (e: SerializationException) {
      preform
    }
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
    return Base58(this.base).decode(data)
  }

  @Throws(IllegalArgumentException::class)
  fun encode(data: ByteArray): String {
    return Base58(this.base).encode(data)
  }
}
