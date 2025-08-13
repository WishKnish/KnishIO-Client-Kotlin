@file:JvmName("Strings")

package wishKnish.knishIO.client.libraries

import org.bouncycastle.util.encoders.Base64
import org.bouncycastle.util.encoders.Hex
import java.math.BigInteger
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.jvm.Throws

class Strings {
  companion object {
    private val secureRandom = SecureRandom()

    @JvmStatic
    fun hexToBase64(str: String): String {
      return Base64.toBase64String(Hex.decode(str))
    }

    @JvmStatic
    fun base64ToHex(str: String): String {
      return Hex.toHexString(Base64.decode(str))
    }

    @JvmStatic
    @Throws(ArithmeticException::class)
    fun currentTimeMillis(): String {
      return ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault()).toInstant().toEpochMilli().toString()
    }

    @JvmStatic
    @JvmOverloads
    @Throws(NoSuchElementException::class)
    fun randomString(
      length: Int,
      alphabet: String = "abcdef0123456789"
    ): String {
      require(length > 0) { "Length must be positive" }
      require(alphabet.isNotEmpty()) { "Alphabet must not be empty" }
      
      val alphabetArray = alphabet.toCharArray()
      val result = CharArray(length)
      
      for (i in 0 until length) {
        val randomIndex = secureRandom.nextInt(alphabetArray.size)
        result[i] = alphabetArray[randomIndex]
      }
      
      return String(result)
    }

    @JvmStatic
    @JvmOverloads
    @Throws(IllegalArgumentException::class)
    fun charsetBaseConvert(
      src: String,
      fromBase: Int,
      toBase: Int,
      srcSymbolTable: String? = null,
      destSymbolTable: String? = null
    ): String {
      val baseSymbols =
        "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz~`!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?¿¡"
      val srcTable = srcSymbolTable ?: baseSymbols
      val destTable = destSymbolTable ?: srcTable

      if (fromBase > srcTable.length || toBase > destTable.length) {
        throw IllegalArgumentException(
          "Can't convert $src to base $toBase greater than symbol table length. src-table: ${srcTable.length} dest-table: ${destTable.length}"
        )
      }

      var value = BigInteger.valueOf(0)
      val bigIntegerZero = BigInteger.valueOf(0)
      val bigIntegerToBase = toBase.toBigInteger()
      val bigIntegerFromBase = fromBase.toBigInteger()

      for (item in src) {
        value = value.multiply(bigIntegerFromBase).add(srcTable.indexOf(item).toBigInteger())
      }

      if (value <= bigIntegerZero) {
        return "0"
      }

      var target = ""

      do {
        val idx = value.mod(bigIntegerToBase)
        target = "${destTable[idx.toInt()]}$target"
        value = value.div(bigIntegerToBase)
      } while (value != bigIntegerZero)

      return target
    }
  }
}
