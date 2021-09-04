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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import wishKnish.knishIO.client.libraries.*
import wishKnish.knishIO.client.data.graphql.types.TokenUnit
import wishKnish.knishIO.client.data.graphql.types.Molecule
import java.math.BigInteger
import java.security.GeneralSecurityException
import kotlin.jvm.Throws

/**
 * Wallet class represents the set of public and private
 * keys to sign Molecules
 */
class Wallet @JvmOverloads constructor(
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
  @JvmField var tokenUnits = mutableListOf<TokenUnit>()
  @JvmField var bundle: String? = null
  @JvmField var molecules: List<Molecule> = listOf()

  @JvmField var createdAt: String? = null
  @JvmField var tokenName: String? = null
  @JvmField var tokenAmount: String? = null
  @JvmField var tokenSupply: String? = null
  @JvmField var tokenFungibility: String? = null

  init {
    bundle = secret?.let {
      position = position ?: Crypto.generateWalletPosition()
      prepareKeys(it)
      Crypto.generateBundleHash(it)
    }
  }

  companion object {

    @JvmStatic
    @JvmOverloads
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
    fun getTokenUnits(unitsData: List<List<String>>): List<TokenUnit> {
      val result = mutableListOf<TokenUnit>()

      unitsData.forEach {
        result.add(TokenUnit(it[0], it[1], it))
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
    @JvmOverloads
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
  @JvmOverloads
  fun splitUnits(
    units: List<TokenUnit>,
    remainderWallet: Wallet,
    recipientWallet: Wallet? = null
  ) {

    // No units supplied, nothing to split
    if (units.isEmpty()) {
      return
    }

    // Init recipient & remainder token units
    val recipientTokenUnits = arrayListOf<TokenUnit>()
    val remainderTokenUnits = arrayListOf<TokenUnit>()

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
  @JvmOverloads
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
      getMyEncPublicKey()
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
  fun <T : Collection<*>> encryptMyMessage(
    message: T,
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
    return encrypt.toJsonElement().toString()
  }

  /**
   * Attempts to decrypt the given string
   */
  @JvmOverloads
  @Throws(
    IllegalArgumentException::class, GeneralSecurityException::class, SerializationException::class
  )
  fun decryptString(
    data: String,
    fallbackValue: String? = null
  ): Any? {
    val json = Json {
      isLenient = true
      coerceInputValues = true
      encodeDefaults = true
    }

    return try {
      try {
        val message = json.decodeFromString<Map<String, String>>(data)
        decryptMyMessage(message) ?: fallbackValue
      } catch (e: SerializationException) {
        decryptMyMessage(data) ?: fallbackValue
      }
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
    val encrypt = message[Crypto.hashShare(publicKey, characters ?: "GMP")] ?: ""
    return Crypto.decryptMessage(encrypt, privateKey, publicKey, characters ?: "GMP")
  }
}
