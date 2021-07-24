/*
                               (
                              (/(
                              (//(
                              (///(
                             (/////(
                             (//////(                          )
                            (////////(                        (/)
                            (////////(                       (///)
                           (//////////(                      (////)
                           (//////////(                     (//////)
                          (////////////(                    (///////)
                         (/////////////(                   (/////////)
                        (//////////////(                  (///////////)
                        (///////////////(                (/////////////)
                       (////////////////(               (//////////////)
                      (((((((((((((((((((              (((((((((((((((
                     (((((((((((((((((((              ((((((((((((((
                     (((((((((((((((((((            ((((((((((((((
                    ((((((((((((((((((((           (((((((((((((
                    ((((((((((((((((((((          ((((((((((((
                    (((((((((((((((((((         ((((((((((((
                    (((((((((((((((((((        ((((((((((
                    ((((((((((((((((((/      (((((((((
                    ((((((((((((((((((     ((((((((
                    (((((((((((((((((    (((((((
                   ((((((((((((((((((  (((((
                   #################  ##
                   ################  #
                  ################# ##
                 %################  ###
                 ###############(   ####
                ###############      ####
               ###############       ######
              %#############(        (#######
             %#############           #########
            ############(              ##########
           ###########                  #############
          #########                      ##############
        %######

        Powered by Knish.IO: Connecting a Decentralized World

Please visit https://github.com/WishKnish/KnishIO-Client-Kotlin for information.

License: https://github.com/WishKnish/KnishIO-Client-Kotlin/blob/master/LICENSE
*/
@file:JvmName("Wallet")

package wishKnish.knishIO.client

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.bouncycastle.util.encoders.Base64
import wishKnish.knishIO.client.libraries.*
import wishKnish.knishIO.client.data.UnitData
import java.math.BigInteger
import java.security.GeneralSecurityException
import kotlin.jvm.Throws

/**
 * Wallet class represents the set of public and private
 * keys to sign Molecules
 */
class Wallet(
  secret: String? = null, // typically a 2048-character biometric hash
  @JvmField var token: String = "USER", // slug for the token this wallet is intended for
  @JvmField var position: String? = null, // hexadecimal string used to salt the secret and produce one-time signatures
  @JvmField var batchId: String? = null,
  @JvmField var characters: String? = null
) {

  @JvmField var balance: Double = 0.0
  @JvmField var key: String? = null
  @JvmField var address: String? = null
  @JvmField var privkey: String? = null
  @JvmField var pubkey: String? = null
  @JvmField var tokenUnits = arrayListOf<UnitData>()
  @JvmField var bundle: String? = null

  init {
    bundle = secret?.let {
      position = position ?: Crypto.generateWalletPosition()
      prepareKeys(it)
      Crypto.generateBundleHash(it)
    }
  }

  companion object {

    @JvmStatic
    @Throws(NoSuchElementException::class)
    fun create(
      secretOrBundle: String? = null,
      token: String = "USER",
      batchId: String? = null,
      characters: String? = null
    ): Wallet {

      val secret = secretOrBundle?.let {
        if (isBundleHash(it)) {
          null
        } else it
      }

      val position = secret?.let {
        Crypto.generateWalletPosition()
      }

      // Wallet initialization
      return Wallet(
        secret, token, position, batchId, characters
      ).apply {
        bundle = secret?.let {
          Crypto.generateBundleHash(it)
        } ?: secretOrBundle
      }
    }

    /**
     * Get formatted token units from the raw data
     */
    @JvmStatic
    fun getTokenUnits(unitsData: List<List<String>>): List<UnitData> {
      val result = mutableListOf<UnitData>()

      unitsData.forEach {
        result.add(UnitData(it[0], it[1], it))
      }

      return result.toList()
    }

    /**
     * Generates a private key for the given parameters
     */
    @JvmStatic
    @Throws(NumberFormatException::class)
    fun generatePrivateKey(
      secret: String,
      token: String,
      position: String
    ): String {

      // Converting secret to bigInt
      val bigIntSecret = BigInteger(secret, 16)

      // Adding new position to the user secret to produce the indexed key
      val indexedKey = bigIntSecret.add(BigInteger(position, 16))

      // Hashing the indexed key to produce the intermediate key
      val intermediateKeySponge = Shake256.create()

      intermediateKeySponge.absorb(indexedKey.toString(16))
      intermediateKeySponge.absorb(token)

      // Hashing the intermediate key to produce the private key
      return Shake256.hash(intermediateKeySponge.hexString(1024), 1024)
    }

    @JvmStatic
    fun isBundleHash(code: String): Boolean {
      return code.length == 64 && code.matches(Regex("^(?:[a-fA-F0-9]+)?\$"))
    }

    @JvmStatic
    @Throws(NoSuchElementException::class)
    fun generateWalletPosition(saltLength: Int = 64): String {
      return Strings.randomString(saltLength)
    }

    /**
     * Generates a public key (wallet address)
     */
    @JvmStatic
    fun generatePublicKey(key: String): String {

      // Generating wallet digest
      val digestSponge = Shake256.create()

      // Subdivide private key into 16 fragments of 128 characters each
      key.chunked(128).forEach {
        var workingFragment = it

        (1..16).forEach { _ ->
          workingFragment = Shake256.hash(workingFragment, 64)
        }

        digestSponge.absorb(workingFragment)
      }

      // Producing wallet address
      return Shake256.hash(digestSponge.hexString(1024), 32)
    }
  }

  /**
   * Has token units?
   */
  fun hasTokenUnits(): Boolean {
    return tokenUnits.isNotEmpty()
  }

  fun tokenUnitsJson(): String? {
    if (hasTokenUnits()) {
      val result = arrayListOf<List<String>>()
      tokenUnits.forEach {
        result.add(listOf(it.id, it.name) + it.metas)
      }

      return result.toJsonElement().toString()
    }

    return null
  }

  /**
   * Split token units
   */
  fun splitUnits(
    units: List<UnitData>,
    remainderWallet: Wallet,
    recipientWallet: Wallet? = null
  ) {

    // No units supplied, nothing to split
    if (units.isEmpty()) {
      return
    }

    // Init recipient & remainder token units
    val recipientTokenUnits = arrayListOf<UnitData>()
    val remainderTokenUnits = arrayListOf<UnitData>()

    tokenUnits.forEach {

      units.filter { unit ->
        it.id == unit.id
      }.forEach { unit ->
        recipientTokenUnits.add(unit)
      }

      units.filter { unit ->
        it.id != unit.id
      }.forEach { unit ->
        remainderTokenUnits.add(unit)
      }
    }

    // Reset token units to the sending value
    tokenUnits = recipientTokenUnits

    // Set token units to recipient & remainder
    recipientWallet?.let {
      it.tokenUnits = recipientTokenUnits
    }
    remainderWallet.tokenUnits = remainderTokenUnits
  }

  fun isShadow(): Boolean {
    return position == null && address == null
  }

  /**
   * Sets up a batch ID - either using the sender's, or a new one
   */
  fun initBatchId(
    sourceWallet: Wallet,
    remainder: Boolean = false
  ) {
    sourceWallet.batchId?.let {
      batchId = if (remainder) it else Crypto.generateBatchId()
    }
  }

  /**
   * Prepares wallet for signing by generating all required keys
   */
  private fun prepareKeys(secret: String) {
    if (key == null && address == null) {

      key = generatePrivateKey(
        secret = secret, token = token, position = position as String
      )
      address = generatePublicKey(key = key as String)
    }
  }

  /**
   * Derives a private key for encrypting data with this wallet's key
   */
  @Throws(IllegalArgumentException::class)
  fun getMyEncPrivateKey(): String? {
    if (privkey == null) {
      key?.let {
        privkey = Crypto.generateEncPrivateKey(it, characters ?: "GMP")
      }
    }

    return privkey
  }

  /**
   * Derives a public key for encrypting data for this wallet's consumption
   */
  @Throws(IllegalArgumentException::class, NumberFormatException::class)
  fun getMyEncPublicKey(): String? {
    val privateKey = getMyEncPrivateKey()

    if (pubkey == null) {
      privateKey?.let {
        pubkey = Crypto.generateEncPublicKey(it, characters ?: "GMP")
      }
    }

    return pubkey
  }

  /**
   * Encrypts a message for this wallet instance
   */
  @Throws(IllegalArgumentException::class, GeneralSecurityException::class)
  fun encryptMyMessage(
    message: List<*>,
    vararg publicKeys: String
  ): Map<String, String> {
    val encrypt = mutableMapOf<String, String>()

    publicKeys.forEach {
      encrypt[Crypto.hashShare(it, characters ?: "GMP")] = Crypto.encryptMessage(
        message, it, characters ?: "GMP"
      )
    }

    return encrypt.toMap()
  }

  /**
   * Encrypts a message for this wallet instance
   */
  @Throws(IllegalArgumentException::class, GeneralSecurityException::class)
  fun encryptMyMessage(
    message: Map<*, *>,
    vararg publicKeys: String
  ): Map<String, String> {
    val encrypt = mutableMapOf<String, String>()

    publicKeys.forEach {
      encrypt[Crypto.hashShare(it, characters ?: "GMP")] = Crypto.encryptMessage(
        message, it, characters ?: "GMP"
      )
    }

    return encrypt.toMap()
  }

  /**
   * Encrypts a string for the given public keys
   */
  @Throws(IllegalArgumentException::class, GeneralSecurityException::class)
  fun encryptString(
    message: String,
    vararg publicKeys: String
  ): String {
    val keys = publicKeys.toMutableList()
    val encrypt = mutableMapOf<String, String>()

    // Retrieving sender's encryption public key
    getMyEncPublicKey()?.let {
      keys.add(it)
    }

    // Encrypting message
    keys.forEach {
      encrypt[Crypto.hashShare(it, characters ?: "GMP")] = Crypto.encryptMessage(
        message, it, characters ?: "GMP"
      )
    }
    return Base64.toBase64String(encrypt.toJsonElement().toString().toByteArray())
  }

  /**
   * Attempts to decrypt the given string
   */
  @Throws(
    IllegalArgumentException::class, GeneralSecurityException::class, SerializationException::class
  )
  fun decryptString(
    data: String,
    fallbackValue: String? = null
  ): Any? {
    return try {

      val message = Json.parseToJsonElement(Base64.decode(data).joinToString("") { "${it.toInt().toChar()}" }).decode()
      @Suppress("UNCHECKED_CAST") val decrypt = (message as? Map<String, String>)?.let { decryptMyMessage(it) }
      decrypt ?: fallbackValue

    } catch (e: Exception) {

      // Probably not actually encrypted
      fallbackValue

    }
  }

  /**
   * Uses the current wallet's private key to decrypt the given message
   */
  @Throws(
    IllegalArgumentException::class, GeneralSecurityException::class, SerializationException::class
  )
  fun decryptMyMessage(message: String): Any? {
    val privateKey = getMyEncPrivateKey() ?: ""
    val publicKey = getMyEncPublicKey() ?: ""

    return Crypto.decryptMessage(message, privateKey, publicKey, characters ?: "GMP")
  }

  /**
   * Uses the current wallet's private key to decrypt the given message
   */
  @Throws(
    IllegalArgumentException::class, GeneralSecurityException::class, SerializationException::class
  )
  fun decryptMyMessage(message: Map<String, String>): Any? {
    val publicKey = getMyEncPublicKey() ?: ""
    val privateKey = getMyEncPrivateKey() ?: ""
    val encrypt = message[Crypto.hashShare(publicKey)] ?: ""

    return Crypto.decryptMessage(encrypt, privateKey, publicKey, characters ?: "GMP")
  }

}
