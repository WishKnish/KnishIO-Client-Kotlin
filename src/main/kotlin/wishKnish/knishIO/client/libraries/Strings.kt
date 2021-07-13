package wishKnish.knishIO.client.libraries

import java.math.BigInteger
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.jvm.Throws

class Strings {
    companion object {

        @JvmStatic
        @Throws(ArithmeticException::class)
        fun currentTimeMillis() : String {
            return ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
                .toString()
        }

        @JvmStatic
        @Throws(NoSuchElementException::class)
        fun randomString(length: Int, alphabet: String = "abcdef0123456789") : String {
            return (1..length)
                .map { alphabet.toList().random() }
                .joinToString("")
        }

        @JvmStatic
        @Throws(IllegalArgumentException::class)
        fun charsetBaseConvert(
            src: String,
            fromBase: Int,
            toBase: Int,
            srcSymbolTable: String? = null,
            destSymbolTable: String? = null
        ) : String {
            val baseSymbols = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz~`!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?¿¡"
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

            for ( item in src) {
                value = value.multiply(bigIntegerFromBase)
                    .add(srcTable.indexOf(item).toBigInteger())
            }

            if (value <= bigIntegerZero) {
                return "0"
            }

            var target = ""

            do {
                val idx = value.mod(bigIntegerToBase)
                target = "${destTable[idx.toInt()]}$target"
                value = value.div(bigIntegerToBase)
            } while (!value.equals(bigIntegerZero))

            return target
        }
    }
}
