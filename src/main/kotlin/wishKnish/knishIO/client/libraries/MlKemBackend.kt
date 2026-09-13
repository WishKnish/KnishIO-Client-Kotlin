package wishKnish.knishIO.client.libraries

import org.bouncycastle.pqc.crypto.mlkem.MLKEMExtractor
import org.bouncycastle.pqc.crypto.mlkem.MLKEMGenerator
import org.bouncycastle.pqc.crypto.mlkem.MLKEMParameters
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPrivateKeyParameters
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPublicKeyParameters
import java.security.SecureRandom

/** Raw-bytes ML-KEM key wrappers. Sizes are FIPS 203: ek 1184/1568, dk 2400/3168 (s‖t‖rho‖H(ek)‖z). */
class MlKemRawPublicKey(val bytes: ByteArray) : java.security.PublicKey {
  override fun getAlgorithm(): String = if (bytes.size >= 1500) "ML-KEM-1024" else "ML-KEM-768"
  override fun getFormat(): String = "RAW"
  override fun getEncoded(): ByteArray = bytes
}

class MlKemRawPrivateKey(val bytes: ByteArray) : java.security.PrivateKey {
  override fun getAlgorithm(): String = if (bytes.size >= 3000) "ML-KEM-1024" else "ML-KEM-768"
  override fun getFormat(): String = "RAW"
  override fun getEncoded(): ByteArray = bytes
}

interface MlKemBackend {
  val name: String                                                            // "noble" | "bouncycastle"
  fun keygenFromSeed(seed: ByteArray, parameterSet: Int): Pair<ByteArray, ByteArray>   // (ek, dk); seed = 64-byte d‖z
  fun encapsulate(publicKey: ByteArray): Pair<ByteArray, ByteArray>          // (sharedSecret 32, cipherText 1088/1568)
  fun decapsulate(cipherText: ByteArray, privateKey: ByteArray): ByteArray   // sharedSecret 32

  companion object {
    const val BACKEND_PROPERTY = "knishio.mlkem.backend"
    val instance: MlKemBackend by lazy { select() }
    fun select(): MlKemBackend {
      val prop = System.getProperty(BACKEND_PROPERTY)
      if (!prop.isNullOrBlank()) {
        return when (prop) {
          "bouncycastle" -> BouncyCastleMlKemBackend
          "noble" -> NobleMlKemBackend
          else -> throw IllegalArgumentException("KnishIO: unknown $BACKEND_PROPERTY '$prop'; expected 'noble' or 'bouncycastle'")
        }
      }
      return try {
        Class.forName("org.graalvm.polyglot.Context", false, MlKemBackend::class.java.classLoader)
        NobleMlKemBackend
      } catch (e: ClassNotFoundException) {
        BouncyCastleMlKemBackend
      } catch (e: LinkageError) {
        BouncyCastleMlKemBackend
      }
    }
  }
}

@Suppress("DEPRECATION")
object BouncyCastleMlKemBackend : MlKemBackend {
  override val name: String = "bouncycastle"

  private fun paramsFor(parameterSet: Int): MLKEMParameters = when (parameterSet) {
    1024 -> MLKEMParameters.ml_kem_1024
    768 -> MLKEMParameters.ml_kem_768
    else -> throw IllegalArgumentException("KnishIO: unsupported ML-KEM parameter set $parameterSet; expected 1024 or 768.")
  }

  private fun paramsForPublicKey(bytes: ByteArray): MLKEMParameters = when (bytes.size) {
    1568 -> MLKEMParameters.ml_kem_1024
    1184 -> MLKEMParameters.ml_kem_768
    else -> throw IllegalArgumentException("KnishIO: ML-KEM public key must be 1184 or 1568 bytes, got ${bytes.size}")
  }

  private fun paramsForPrivateKey(bytes: ByteArray): MLKEMParameters = when (bytes.size) {
    3168 -> MLKEMParameters.ml_kem_1024
    2400 -> MLKEMParameters.ml_kem_768
    else -> throw IllegalArgumentException("KnishIO: ML-KEM private key must be 2400 or 3168 bytes, got ${bytes.size}")
  }

  override fun keygenFromSeed(seed: ByteArray, parameterSet: Int): Pair<ByteArray, ByteArray> {
    require(seed.size == 64) { "KnishIO: ML-KEM seed must be 64 bytes (d‖z), got ${seed.size}" }
    val sk = MLKEMPrivateKeyParameters(paramsFor(parameterSet), seed)
    return Pair(sk.publicKeyParameters.encoded, sk.encoded)
  }

  override fun encapsulate(publicKey: ByteArray): Pair<ByteArray, ByteArray> {
    val swe = MLKEMGenerator(SecureRandom())
      .generateEncapsulated(MLKEMPublicKeyParameters(paramsForPublicKey(publicKey), publicKey))
    return Pair(swe.secret, swe.encapsulation)
  }

  override fun decapsulate(cipherText: ByteArray, privateKey: ByteArray): ByteArray {
    return MLKEMExtractor(
      MLKEMPrivateKeyParameters(paramsForPrivateKey(privateKey), privateKey)
    ).extractSecret(cipherText)
  }
}
