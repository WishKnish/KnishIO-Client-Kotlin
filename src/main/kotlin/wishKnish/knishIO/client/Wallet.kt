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
import wishKnish.knishIO.client.libraries.*
import wishKnish.knishIO.client.data.graphql.types.TokenUnit
import wishKnish.knishIO.client.data.graphql.types.Molecule
import java.security.PrivateKey
import java.security.PublicKey
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
  
  // Post-quantum cryptography keys (ML-KEM768)
  @JvmField var pqPrivateKey: PrivateKey? = null
  @JvmField var pqPublicKey: PublicKey? = null
  @JvmField var encryptionMode: EncryptionMode = EncryptionMode.HYBRID
  @JvmField var supportedMethods: Set<EncryptionMethod> = setOf(
    EncryptionMethod.NACL_BOX, 
    EncryptionMethod.NACL_SEALED_BOX,
    EncryptionMethod.POST_QUANTUM
  )

  @JvmField var createdAt: String? = null
  @JvmField var tokenName: String? = null
  @JvmField var tokenAmount: String? = null
  @JvmField var tokenSupply: String? = null
  @JvmField var tokenFungibility: String? = null

  init {
    // Set default characters to BASE64 to match JS SDK
    characters = characters ?: "BASE64"
    
    bundle = secret?.let {
      // Validate position or generate a new one
      position = when {
        position == null -> Crypto.generateWalletPosition()
        position!!.matches(Regex("^[a-f0-9]{64}$")) -> position
        else -> Crypto.generateWalletPosition() // Invalid position, generate new one
      }
      val bundleHash = Crypto.generateBundleHash(it)
      prepareKeys(it)
      // Re-enable post-quantum key generation
      preparePostQuantumKeys(it)
      bundleHash
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

      // Normalize secret to hex if it's not already valid hex
      val secretHex = try {
        // Test if secret is valid hex
        BigInteger(secret, 16)
        secret
      } catch (e: NumberFormatException) {
        // If not valid hex, hash it to create a valid hex string
        Shake256.hash(secret, 128) // 128 bytes = 256 hex chars for 2048-char compatibility
      }

      // Converting secret to bigInt
      val bigIntSecret = BigInteger(secretHex, 16)

      // Convert position to hex if it's not already valid hex
      val positionHex = try {
        // Test if position is valid hex
        BigInteger(position, 16)
        position
      } catch (e: NumberFormatException) {
        // If not valid hex, hash it to create a valid hex string
        Shake256.hash(position, 32)
      }

      // Adding new position to the user secret to produce the indexed key
      val indexedKey = bigIntSecret.add(BigInteger(positionHex, 16))

      // Hashing the indexed key to produce the intermediate key
      val intermediateKeySponge = Shake256.create()

      // Convert to hex string without padding (matches JS implementation)
      val indexedKeyHex = indexedKey.toString(16).lowercase()
      
      intermediateKeySponge.absorb(indexedKeyHex)
      intermediateKeySponge.absorb(token)

      // Hashing the intermediate key to produce the private key
      return Shake256.hash(intermediateKeySponge.hexString(1024), 1024)
    }

    @JvmStatic
    fun isBundleHash(code: String): Boolean {
      return code.length == 64 && code.matches(Regex("^(?:[a-fA-F0-9]+)?$"))
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
      // Note: pubkey is now set by preparePostQuantumKeys() for ML-KEM compatibility
    }
  }

  /**
   * Derives a private key for encrypting data with this wallet's key
   */
  @Throws(IllegalArgumentException::class)
  fun getMyEncPrivateKey(): String? {
    if (privkey == null) {
      key?.let {
        privkey = Crypto.generateEncPrivateKey(it, characters ?: "BASE64")
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

    // Always generate Base58-encoded encryption public key for encryption compatibility
    // Even if pubkey is set by post-quantum initialization (which uses Base64)
    return privateKey?.let {
      Crypto.generateEncPublicKey(it, characters ?: "BASE64")
    }
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
      encrypt[Crypto.hashShare(it, characters ?: "BASE64")] = Crypto.encryptMessage(
        message, it, characters ?: "BASE64"
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
      encrypt[Crypto.hashShare(it, characters ?: "BASE64")] = Crypto.encryptMessage(
        message, it, characters ?: "BASE64"
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
  ): String? {
    val json = Json {
      isLenient = true
      coerceInputValues = true
      encodeDefaults = true
    }

    return try {
      try {
        val message = json.decodeFromString<Map<String, String>>(data)
        when (val result = decryptMyMessage(message)) {
          is kotlinx.serialization.json.JsonPrimitive -> result.content
          is String -> result
          else -> result?.toString() ?: fallbackValue
        }
      } catch (e: SerializationException) {
        when (val result = decryptMyMessage(data)) {
          is kotlinx.serialization.json.JsonPrimitive -> result.content
          is String -> result
          else -> result?.toString() ?: fallbackValue
        }
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

    return Crypto.decryptMessage(message, privateKey, publicKey, characters ?: "BASE64")
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
    val encrypt = message[Crypto.hashShare(publicKey, characters ?: "BASE64")] ?: ""
    return Crypto.decryptMessage(encrypt, privateKey, publicKey, characters ?: "BASE64")
  }
  
  /**
   * Generate post-quantum keys from the wallet secret
   */
  @Throws(Exception::class)
  private fun preparePostQuantumKeys(secret: String) {
    // Generate post-quantum seed using the same approach as JavaScript:
    // SHAKE256(wallet.key) with 64-byte output (matches generateSecret(this.key, 64))
    val pqSeedHex = key?.let { Shake256.hash(it, 64) }
    if (pqSeedHex != null) {
      // Convert hex string to byte array (same as JavaScript conversion)
      val pqSeed = org.bouncycastle.util.encoders.Hex.decode(pqSeedHex)
      val keyPair = PostQuantumCrypto.generateMLKEMKeyPairFromSeed(pqSeed)
      
      pqPrivateKey = keyPair.private
      pqPublicKey = keyPair.public
      
      // Set pubkey field to raw ML-KEM768 public key (1580 chars) for quantum resistance
      pubkey = pqPublicKey?.let { PostQuantumCrypto.rawMLKEMPublicKeyToBase64(it) }
    }
  }
  
  /**
   * Encrypts a string using hybrid cryptography (supports both classical and post-quantum)
   */
  @Throws(IllegalArgumentException::class, GeneralSecurityException::class)
  fun encryptStringHybrid(
    message: String,
    vararg publicKeys: String
  ): Map<String, String> {
    return try {
      // Try to use the first public key as post-quantum if available
      if (publicKeys.isNotEmpty() && pqPublicKey != null) {
        HybridCrypto.encryptMessage(
          message, 
          publicKeys[0], 
          encryptionMode,
          getMyEncPrivateKey()
        )
      } else {
        // Fallback to classical encryption
        val classicalResult = encryptString(message, *publicKeys)
        // Convert string result to map format for consistency
        mapOf("data" to classicalResult, "version" to "1", "algorithm" to "NaCl-Box")
      }
    } catch (e: Exception) {
      // Fallback to classical encryption
      val classicalResult = encryptString(message, *publicKeys)
      mapOf("data" to classicalResult, "version" to "1", "algorithm" to "NaCl-Box")
    }
  }
  
  /**
   * Decrypts a message using hybrid cryptography
   */
  @Throws(IllegalArgumentException::class, GeneralSecurityException::class)
  fun decryptStringHybrid(
    encryptedMessage: Map<String, String>,
    fallbackValue: String? = null
  ): String? {
    return try {
      HybridCrypto.decryptMessage(
        encryptedMessage,
        getMyEncPrivateKey() ?: "",
        pqPrivateKey
      )
    } catch (e: Exception) {
      // Try classical decryption as fallback
      try {
        encryptedMessage["data"]?.let { data ->
          decryptString(data, fallbackValue) as? String
        } ?: fallbackValue
      } catch (e2: Exception) {
        fallbackValue
      }
    }
  }
  
  /**
   * Get the post-quantum public key as hex string
   */
  fun getPostQuantumPublicKey(): String? {
    return pqPublicKey?.let { PostQuantumCrypto.publicKeyToHex(it) }
  }
  
  /**
   * Get the post-quantum private key as hex string (use with extreme caution)
   */
  fun getPostQuantumPrivateKey(): String? {
    return pqPrivateKey?.let { PostQuantumCrypto.privateKeyToHex(it) }
  }
  
  /**
   * Check if wallet supports a specific encryption method
   */
  fun supportsEncryptionMethod(method: EncryptionMethod): Boolean {
    return supportedMethods.contains(method)
  }
  
  /**
   * Get the best available encryption method for this wallet
   */
  fun getBestEncryptionMethod(): EncryptionMethod {
    return when {
      supportedMethods.contains(EncryptionMethod.POST_QUANTUM) -> EncryptionMethod.POST_QUANTUM
      supportedMethods.contains(EncryptionMethod.NACL_BOX) -> EncryptionMethod.NACL_BOX
      supportedMethods.contains(EncryptionMethod.NACL_SEALED_BOX) -> EncryptionMethod.NACL_SEALED_BOX
      else -> EncryptionMethod.UNKNOWN
    }
  }
  
  /**
   * Set encryption mode preference
   */
  fun setEncryptionMode(mode: EncryptionMode) {
    encryptionMode = mode
  }
}
