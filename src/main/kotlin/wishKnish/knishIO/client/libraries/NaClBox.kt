package wishKnish.knishIO.client.libraries

import org.bouncycastle.crypto.engines.XSalsa20Engine
import org.bouncycastle.crypto.macs.Poly1305
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import org.bouncycastle.math.ec.rfc7748.X25519
import java.security.SecureRandom

/**
 * NaCl `crypto_box` (public-key authenticated encryption) implemented on
 * BouncyCastle primitives (Maven Central), byte-identical to the reference
 * `tweetnacl-java` it replaces — which resolved from JitPack (a mutable-rebuild
 * source we no longer ship). Construction (NaCl / libsodium):
 *
 *   scalarmult_base : X25519(sk, basepoint 9)                    [RFC 7748]
 *   crypto_box      : k = HSalsa20(0^16, X25519(sk, peerPk), σ)  ("beforenm")
 *                     then crypto_secretbox(msg, nonce24, k)
 *   crypto_secretbox: keystream = XSalsa20(k, nonce24);
 *                     poly1305 one-time key = keystream[0..32];
 *                     ct = keystream[32..] XOR msg;
 *                     tag = Poly1305(ct, key); output = tag(16) ‖ ct
 *
 * Byte-parity with tweetnacl is pinned by the `nacl` cross-platform vectors and
 * (at migration time) a differential test against tweetnacl itself.
 */
internal object NaClBox {
  const val SECRET_KEY_LENGTH = 32
  const val PUBLIC_KEY_LENGTH = 32
  const val NONCE_LENGTH = 24
  const val MAC_LENGTH = 16

  /** NaCl "expand 32-byte k" σ constant, little-endian words folded into bytes. */
  private val SIGMA = byteArrayOf(
    'e'.code.toByte(), 'x'.code.toByte(), 'p'.code.toByte(), 'a'.code.toByte(),
    'n'.code.toByte(), 'd'.code.toByte(), ' '.code.toByte(), '3'.code.toByte(),
    '2'.code.toByte(), '-'.code.toByte(), 'b'.code.toByte(), 'y'.code.toByte(),
    't'.code.toByte(), 'e'.code.toByte(), ' '.code.toByte(), 'k'.code.toByte()
  )

  private val rng = SecureRandom()

  /** X25519 base-point scalar mult: 32-byte secret → 32-byte public (RFC 7748 clamping). */
  fun scalarMultBase(secretKey: ByteArray): ByteArray {
    require(secretKey.size == SECRET_KEY_LENGTH) { "secret key must be 32 bytes" }
    val out = ByteArray(PUBLIC_KEY_LENGTH)
    X25519.scalarMultBase(secretKey, 0, out, 0)
    return out
  }

  /** Fresh ephemeral X25519 keypair → (secretKey, publicKey). */
  fun keyPair(): Pair<ByteArray, ByteArray> {
    val sk = ByteArray(SECRET_KEY_LENGTH).also { rng.nextBytes(it) }
    return sk to scalarMultBase(sk)
  }

  /** crypto_box: authenticated-encrypt `message` to `peerPublicKey` under `secretKey`. Returns tag‖ct. */
  fun box(message: ByteArray, nonce: ByteArray, peerPublicKey: ByteArray, secretKey: ByteArray): ByteArray {
    val key = beforenm(peerPublicKey, secretKey)
    return secretbox(message, nonce, key)
  }

  /** crypto_box_open: verify + decrypt. Returns plaintext, or null on auth failure. */
  fun boxOpen(boxed: ByteArray, nonce: ByteArray, peerPublicKey: ByteArray, secretKey: ByteArray): ByteArray? {
    val key = beforenm(peerPublicKey, secretKey)
    return secretboxOpen(boxed, nonce, key)
  }

  /** crypto_box_beforenm: k = HSalsa20(0^16, X25519(sk, peerPk), σ). */
  private fun beforenm(peerPublicKey: ByteArray, secretKey: ByteArray): ByteArray {
    require(peerPublicKey.size == PUBLIC_KEY_LENGTH) { "peer public key must be 32 bytes" }
    require(secretKey.size == SECRET_KEY_LENGTH) { "secret key must be 32 bytes" }
    val shared = ByteArray(32)
    X25519.scalarMult(secretKey, 0, peerPublicKey, 0, shared, 0)
    return hsalsa20(ByteArray(16), shared, SIGMA)
  }

  /** crypto_secretbox: XSalsa20 keystream → Poly1305 one-time key + XOR; output tag(16)‖ct. */
  private fun secretbox(message: ByteArray, nonce: ByteArray, key: ByteArray): ByteArray {
    require(nonce.size == NONCE_LENGTH) { "nonce must be 24 bytes" }
    val engine = XSalsa20Engine().apply { init(true, ParametersWithIV(KeyParameter(key), nonce)) }
    val polyKey = ByteArray(32)
    engine.processBytes(ByteArray(32), 0, 32, polyKey, 0) // consume keystream[0..32]
    val ct = ByteArray(message.size)
    engine.processBytes(message, 0, message.size, ct, 0)  // keystream[32..] XOR msg
    val tag = poly1305(ct, polyKey)
    return tag + ct
  }

  /** crypto_secretbox_open: recompute + constant-time-verify tag, then decrypt. */
  private fun secretboxOpen(boxed: ByteArray, nonce: ByteArray, key: ByteArray): ByteArray? {
    require(nonce.size == NONCE_LENGTH) { "nonce must be 24 bytes" }
    if (boxed.size < MAC_LENGTH) return null
    val engine = XSalsa20Engine().apply { init(true, ParametersWithIV(KeyParameter(key), nonce)) }
    val polyKey = ByteArray(32)
    engine.processBytes(ByteArray(32), 0, 32, polyKey, 0)
    val tag = boxed.copyOfRange(0, MAC_LENGTH)
    val ct = boxed.copyOfRange(MAC_LENGTH, boxed.size)
    if (!constantTimeEquals(poly1305(ct, polyKey), tag)) return null
    val msg = ByteArray(ct.size)
    engine.processBytes(ct, 0, ct.size, msg, 0)
    return msg
  }

  private fun poly1305(data: ByteArray, key: ByteArray): ByteArray {
    val mac = Poly1305()
    mac.init(KeyParameter(key))
    mac.update(data, 0, data.size)
    val tag = ByteArray(MAC_LENGTH)
    mac.doFinal(tag, 0)
    return tag
  }

  private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
    if (a.size != b.size) return false
    var diff = 0
    for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
    return diff == 0
  }

  // ── crypto_core_hsalsa20 (public-domain TweetNaCl construction) ────────────
  // out = HSalsa20(in[16], key[32], c[16]) — 20 rounds, NO feed-forward add.

  private fun ld32(x: ByteArray, off: Int): Int =
    (x[off].toInt() and 0xff) or
    ((x[off + 1].toInt() and 0xff) shl 8) or
    ((x[off + 2].toInt() and 0xff) shl 16) or
    ((x[off + 3].toInt() and 0xff) shl 24)

  private fun st32(x: ByteArray, off: Int, v: Int) {
    x[off] = (v and 0xff).toByte()
    x[off + 1] = ((v ushr 8) and 0xff).toByte()
    x[off + 2] = ((v ushr 16) and 0xff).toByte()
    x[off + 3] = ((v ushr 24) and 0xff).toByte()
  }

  private fun rotl(u: Int, c: Int): Int = (u shl c) or (u ushr (32 - c))

  private fun hsalsa20(input: ByteArray, key: ByteArray, c: ByteArray): ByteArray {
    var x0 = ld32(c, 0)
    var x1 = ld32(key, 0)
    var x2 = ld32(key, 4)
    var x3 = ld32(key, 8)
    var x4 = ld32(key, 12)
    var x5 = ld32(c, 4)
    var x6 = ld32(input, 0)
    var x7 = ld32(input, 4)
    var x8 = ld32(input, 8)
    var x9 = ld32(input, 12)
    var x10 = ld32(c, 8)
    var x11 = ld32(key, 16)
    var x12 = ld32(key, 20)
    var x13 = ld32(key, 24)
    var x14 = ld32(key, 28)
    var x15 = ld32(c, 12)

    var i = 0
    while (i < 20) {
      // column round
      x4 = x4 xor rotl(x0 + x12, 7)
      x8 = x8 xor rotl(x4 + x0, 9)
      x12 = x12 xor rotl(x8 + x4, 13)
      x0 = x0 xor rotl(x12 + x8, 18)
      x9 = x9 xor rotl(x5 + x1, 7)
      x13 = x13 xor rotl(x9 + x5, 9)
      x1 = x1 xor rotl(x13 + x9, 13)
      x5 = x5 xor rotl(x1 + x13, 18)
      x14 = x14 xor rotl(x10 + x6, 7)
      x2 = x2 xor rotl(x14 + x10, 9)
      x6 = x6 xor rotl(x2 + x14, 13)
      x10 = x10 xor rotl(x6 + x2, 18)
      x3 = x3 xor rotl(x15 + x11, 7)
      x7 = x7 xor rotl(x3 + x15, 9)
      x11 = x11 xor rotl(x7 + x3, 13)
      x15 = x15 xor rotl(x11 + x7, 18)
      // row round
      x1 = x1 xor rotl(x0 + x3, 7)
      x2 = x2 xor rotl(x1 + x0, 9)
      x3 = x3 xor rotl(x2 + x1, 13)
      x0 = x0 xor rotl(x3 + x2, 18)
      x6 = x6 xor rotl(x5 + x4, 7)
      x7 = x7 xor rotl(x6 + x5, 9)
      x4 = x4 xor rotl(x7 + x6, 13)
      x5 = x5 xor rotl(x4 + x7, 18)
      x11 = x11 xor rotl(x10 + x9, 7)
      x8 = x8 xor rotl(x11 + x10, 9)
      x9 = x9 xor rotl(x8 + x11, 13)
      x10 = x10 xor rotl(x9 + x8, 18)
      x12 = x12 xor rotl(x15 + x14, 7)
      x13 = x13 xor rotl(x12 + x15, 9)
      x14 = x14 xor rotl(x13 + x12, 13)
      x15 = x15 xor rotl(x14 + x13, 18)
      i += 2
    }

    val out = ByteArray(32)
    st32(out, 0, x0)
    st32(out, 4, x5)
    st32(out, 8, x10)
    st32(out, 12, x15)
    st32(out, 16, x6)
    st32(out, 20, x7)
    st32(out, 24, x8)
    st32(out, 28, x9)
    return out
  }
}
