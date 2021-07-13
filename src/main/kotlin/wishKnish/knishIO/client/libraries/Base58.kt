package wishKnish.knishIO.client.libraries

import org.bouncycastle.crypto.digests.SHA256Digest


private const val ENCODED_ZERO = '1'
private const val CHECKSUM_SIZE = 4
private enum class ALPHABET(val value: String) {
    IPFS("123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"),
    RIPPLE("rpshnaf39wBUDNEGHJKLM4PQRST7VWXYZ2bcdeCg65jkm8oFqi1tuvAxyz"),
    FLICKR("123456789abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ"),
    BITCOIN("123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"),
    GMP("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuv")
}


private inline fun <reified T : Enum<T>> enumContains(name: String): Boolean {
    return enumValues<T>().any { it.name == name}
}

private fun ByteArray.sha256(): ByteArray {
    val digest = SHA256Digest()

    digest.update(this, 0, size)

    val out = ByteArray(digest.digestSize)

    digest.doFinal(out, 0)

    return out
}


private fun divmod(number: ByteArray, firstDigit: UInt, base: UInt, divisor: UInt): UInt {

    var remainder = 0.toUInt()

    for (i in firstDigit until number.size.toUInt()) {
        val digit = number[i.toInt()].toUByte()
        val temp = remainder * base + digit

        number[i.toInt()] = (temp / divisor).toByte()
        remainder = temp % divisor
    }

    return remainder
}


@Throws(IllegalArgumentException::class)
fun ByteArray.encodeToBase58String(characters: String = "GMP"): String {
    if (!enumContains<ALPHABET>(characters)) {
        throw IllegalArgumentException("there is no such ALPHABET [$characters]")
    }

    val alphabet = ALPHABET.valueOf(characters).value
    val input = copyOf(size)

    if (input.isEmpty()) {
        return ""
    }

    var zeros = 0

    while (zeros < input.size && input[zeros].toInt() == 0) {
        ++zeros
    }

    val encoded = CharArray(input.size * 2)
    var outputStart = encoded.size
    var inputStart = zeros

    while (inputStart < input.size) {
        encoded[--outputStart] = alphabet[divmod(input, inputStart.toUInt(), 256.toUInt(), 58.toUInt()).toInt()]
        if (input[inputStart].toInt() == 0) {
            ++inputStart
        }
    }

    while (outputStart < encoded.size && encoded[outputStart] == ENCODED_ZERO) {
        ++outputStart
    }

    while (--zeros >= 0) {
        encoded[--outputStart] = ENCODED_ZERO
    }

    return String(encoded, outputStart, encoded.size - outputStart)
}


@Throws(NumberFormatException::class, IllegalArgumentException::class)
fun String.decodeBase58(characters: String = "GMP"): ByteArray {

    if (!enumContains<ALPHABET>(characters)) {
        throw IllegalArgumentException("there is no such ALPHABET [$characters]")
    }

    val alphabet = ALPHABET.valueOf(characters).value
    val alphabetIndices by lazy {
        IntArray(128) { alphabet.indexOf(it.toChar()) }
    }

    if (isEmpty()) {
        return ByteArray(0)
    }

    val input58 = ByteArray(length)

    for (i in 0 until length) {
        val c = this[i]
        val digit = if (c.code < 128) alphabetIndices[c.code] else -1
        if (digit < 0) {
            throw NumberFormatException("Illegal character $c at position $i")
        }
        input58[i] = digit.toByte()
    }

    var zeros = 0

    while (zeros < input58.size && input58[zeros].toInt() == 0) {
        ++zeros
    }

    val decoded = ByteArray(length)
    var outputStart = decoded.size
    var inputStart = zeros

    while (inputStart < input58.size) {
        decoded[--outputStart] = divmod(input58, inputStart.toUInt(), 58.toUInt(), 256.toUInt()).toByte()

        if (input58[inputStart].toInt() == 0) {
            ++inputStart
        }
    }

    while (outputStart < decoded.size && decoded[outputStart].toInt() == 0) {
        ++outputStart
    }

    return decoded.copyOfRange(outputStart - zeros, decoded.size)
}

@Throws(IllegalArgumentException::class)
fun ByteArray.encodeToBase58WithChecksum() = ByteArray(size + CHECKSUM_SIZE).apply {
    System.arraycopy(this@encodeToBase58WithChecksum, 0, this, 0, this@encodeToBase58WithChecksum.size)

    val checksum = this@encodeToBase58WithChecksum.sha256().sha256()

    System.arraycopy(checksum, 0, this, this@encodeToBase58WithChecksum.size, CHECKSUM_SIZE)

}.encodeToBase58String()


@Throws(IllegalArgumentException::class, NumberFormatException::class)
fun String.decodeBase58WithChecksum(): ByteArray {
    val rawBytes = decodeBase58()

    if (rawBytes.size < CHECKSUM_SIZE) {
        throw IllegalArgumentException("Too short for checksum: $this l:  ${rawBytes.size}")
    }

    val checksum = rawBytes.copyOfRange(rawBytes.size - CHECKSUM_SIZE, rawBytes.size)
    val payload = rawBytes.copyOfRange(0, rawBytes.size - CHECKSUM_SIZE)
    val hash = payload.sha256().sha256()
    val computedChecksum = hash.copyOfRange(0, CHECKSUM_SIZE)

    if (checksum.contentEquals(computedChecksum)) {
        return payload
    }
    else {
        throw IllegalArgumentException("Checksum mismatch: $checksum is not computed checksum $computedChecksum")
    }
}


@Throws(IllegalArgumentException::class)
fun String.toStringEncodeToBase58(characters: String = "GMP"): String = toByteArray().encodeToBase58String(characters)


@Throws(IllegalArgumentException::class)
fun String.toStringDecodeBase58(characters: String = "GMP"): String = decodeBase58(characters).joinToString("") { "${it.toInt().toChar()}" }
