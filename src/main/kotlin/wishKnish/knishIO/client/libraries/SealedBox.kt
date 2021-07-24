package wishKnish.knishIO.client.libraries

import com.iwebpp.crypto.TweetNaclFast
import org.bouncycastle.crypto.digests.Blake2bDigest
import java.security.GeneralSecurityException


private const val NONCEBYTES = 24
private const val PUBLICKEYBYTES = 32
private const val MACBYTES = 10
private const val SEALBYTES = PUBLICKEYBYTES + MACBYTES

@kotlin.jvm.Throws(IllegalArgumentException::class)
internal fun ByteArray.sealOpen(
  pk: ByteArray,
  sk: ByteArray
): ByteArray {

  if (size < SEALBYTES) {
    throw IllegalArgumentException("Ciphertext too short")
  }

  val pkSender = copyOfRange(0, PUBLICKEYBYTES)
  val cipherTextWitHmac = copyOfRange(PUBLICKEYBYTES, size)
  val nonce = pkSender.sealNonce(pk)
  val box = TweetNaclFast.Box(pkSender, sk)

  return box.open(cipherTextWitHmac, nonce) ?: byteArrayOf()
}


@kotlin.jvm.Throws(
  GeneralSecurityException::class, IllegalArgumentException::class
)
internal fun ByteArray.seal(pubKey: ByteArray): ByteArray {
  val ephKeyPair = TweetNaclFast.Box.keyPair()
  val ephPubKey = ephKeyPair.publicKey
  val nonce = ephPubKey.sealNonce(pubKey)
  val box = TweetNaclFast.Box(pubKey, ephKeyPair.secretKey)
  val cipherText = box.box(this, nonce) ?: byteArrayOf()

  if (cipherText.isEmpty()) {
    throw GeneralSecurityException("encryption error")
  }

  val sealedBox = ByteArray(cipherText.size + PUBLICKEYBYTES)
  (0 until PUBLICKEYBYTES).forEach { sealedBox[it] = ephPubKey[it] }
  cipherText.indices.forEach { sealedBox[it + PUBLICKEYBYTES] = cipherText[it] }

  return sealedBox
}


@kotlin.jvm.Throws(IllegalArgumentException::class)
private fun ByteArray.sealNonce(pubKey: ByteArray): ByteArray {
  val blake2b = Blake2bDigest(NONCEBYTES)
  val nonce = ByteArray(NONCEBYTES)

  forEach { blake2b.update(it) }
  pubKey.forEach { blake2b.update(it) }
  blake2b.doFinal(nonce, 0)

  if (nonce.isEmpty() || nonce.size != NONCEBYTES) {
    throw IllegalArgumentException("hashing failed")
  }

  return nonce
}
