@file:JvmName("SecureMemory")

package wishKnish.knishIO.client.libraries

import java.security.MessageDigest
import java.util.Arrays

/**
 * Memory hygiene and zeroization utilities for sensitive cryptographic material
 */
object SecureMemory {

  /**
   * Overwrite byte array contents with zeros
   */
  @JvmStatic
  fun zeroize(bytes: ByteArray) {
    Arrays.fill(bytes, 0.toByte())
  }

  /**
   * Overwrite char array contents with null characters
   */
  @JvmStatic
  fun zeroize(chars: CharArray) {
    Arrays.fill(chars, '\u0000')
  }

  /**
   * Execute a block with a byte buffer and guarantee zeroization upon completion
   */
  @JvmStatic
  inline fun <T> withSecureBytes(bytes: ByteArray, block: (ByteArray) -> T): T {
    try {
      return block(bytes)
    } finally {
      zeroize(bytes)
    }
  }

  /**
   * Execute a block with a char buffer and guarantee zeroization upon completion
   */
  @JvmStatic
  inline fun <T> withSecureChars(chars: CharArray, block: (CharArray) -> T): T {
    try {
      return block(chars)
    } finally {
      zeroize(chars)
    }
  }

  /**
   * Constant-time byte array comparison to prevent timing attacks
   */
  @JvmStatic
  fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
    return MessageDigest.isEqual(a, b)
  }

  /**
   * Constant-time string comparison to prevent timing attacks
   */
  @JvmStatic
  fun constantTimeEquals(a: String, b: String): Boolean {
    val bytesA = a.toByteArray(Charsets.UTF_8)
    val bytesB = b.toByteArray(Charsets.UTF_8)
    try {
      return MessageDigest.isEqual(bytesA, bytesB)
    } finally {
      zeroize(bytesA)
      zeroize(bytesB)
    }
  }
}
