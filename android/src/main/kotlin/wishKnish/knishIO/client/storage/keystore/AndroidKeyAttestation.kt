package wishKnish.knishIO.client.storage.keystore

import org.bouncycastle.asn1.ASN1Enumerated
import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.ASN1OctetString
import org.bouncycastle.asn1.ASN1Sequence
import wishKnish.knishIO.client.exception.SecretStorageException
import java.security.cert.X509Certificate

/**
 * Parser for Android Keystore key attestation extension (OID 1.3.6.1.4.1.11129.2.1.17).
 *
 * KeyDescription ::= SEQUENCE {
 *   attestationVersion         INTEGER,
 *   attestationSecurityLevel   SecurityLevel,
 *   keyMintVersion             INTEGER,
 *   keyMintSecurityLevel       SecurityLevel,
 *   attestationChallenge       OCTET_STRING,
 *   uniqueId                   OCTET_STRING,
 *   softwareEnforced           AuthorizationList,
 *   hardwareEnforced           AuthorizationList
 * }
 */
object AndroidKeyAttestation {
  const val EXTENSION_OID = "1.3.6.1.4.1.11129.2.1.17"
  const val LEVEL_SOFTWARE = 0
  const val LEVEL_TRUSTED_ENVIRONMENT = 1
  const val LEVEL_STRONGBOX = 2

  data class Summary(
    val attestationVersion: Int,
    val attestationSecurityLevel: Int,
    val keyMintVersion: Int,
    val keyMintSecurityLevel: Int,
    val challenge: ByteArray
  ) {
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is Summary) return false
      return attestationVersion == other.attestationVersion &&
        attestationSecurityLevel == other.attestationSecurityLevel &&
        keyMintVersion == other.keyMintVersion &&
        keyMintSecurityLevel == other.keyMintSecurityLevel &&
        challenge.contentEquals(other.challenge)
    }

    override fun hashCode(): Int {
      var result = attestationVersion
      result = 31 * result + attestationSecurityLevel
      result = 31 * result + keyMintVersion
      result = 31 * result + keyMintSecurityLevel
      result = 31 * result + challenge.contentHashCode()
      return result
    }
  }

  /**
   * Parse the key attestation extension from an X.509 leaf certificate
   */
  fun parse(leaf: X509Certificate): Summary {
    val extBytes = leaf.getExtensionValue(EXTENSION_OID)
      ?: throw SecretStorageException("certificate carries no Android key attestation extension")

    val octets = (ASN1OctetString.getInstance(extBytes)).octets
    val seq = ASN1Sequence.getInstance(octets)

    val attestationVersion = (seq.getObjectAt(0) as ASN1Integer).value.toInt()
    val attestationSecurityLevel = (seq.getObjectAt(1) as ASN1Enumerated).value.toInt()
    val keyMintVersion = (seq.getObjectAt(2) as ASN1Integer).value.toInt()
    val keyMintSecurityLevel = (seq.getObjectAt(3) as ASN1Enumerated).value.toInt()
    val challenge = (seq.getObjectAt(4) as ASN1OctetString).octets

    return Summary(
      attestationVersion = attestationVersion,
      attestationSecurityLevel = attestationSecurityLevel,
      keyMintVersion = keyMintVersion,
      keyMintSecurityLevel = keyMintSecurityLevel,
      challenge = challenge
    )
  }

  /**
   * Map security level to KnishIO providerType
   */
  fun providerTypeFor(level: Int): String? = when (level) {
    LEVEL_STRONGBOX -> AndroidKeystoreSecretStorageProvider.PROVIDER_TYPE_STRONGBOX
    LEVEL_TRUSTED_ENVIRONMENT -> AndroidKeystoreSecretStorageProvider.PROVIDER_TYPE_TEE
    else -> null
  }
}
