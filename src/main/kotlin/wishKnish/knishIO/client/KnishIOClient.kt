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
@file:JvmName("KnishIOClient")

package wishKnish.knishIO.client

import kotlinx.serialization.encodeToString
import wishKnish.knishIO.client.data.MetaData
import wishKnish.knishIO.client.data.graphql.types.*
import wishKnish.knishIO.client.data.json.variables.*
import wishKnish.knishIO.client.httpClient.HttpClient
import wishKnish.knishIO.client.libraries.Crypto
import wishKnish.knishIO.client.mutation.*
import wishKnish.knishIO.client.query.*
import wishKnish.knishIO.client.response.*
import java.net.URI
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import kotlinx.serialization.json.Json
import wishKnish.knishIO.client.exception.*
import kotlin.jvm.Throws
import wishKnish.knishIO.client.storage.*

/**
 * One destination of a multi-recipient stackable transfer (see KnishIOClient.transferTokens).
 * Provide EITHER units (stackable/NFT: amount = units.size) OR amount (fungible), not both.
 * batchId makes the recipient a claimable shadow under that batch.
 */
data class TransferRecipient(
  val bundleHash: String,
  val units: List<String> = emptyList(),
  val amount: Number? = null,
  val batchId: String? = null
)

/**
 * Base client class providing a powerful but user-friendly wrapper
 * around complex Knish.IO ledger transactions.
 */
class KnishIOClient @JvmOverloads constructor(
  @JvmField val uris: List<URI>,
  @JvmField val serverSdkVersion: Int = 3,
  @JvmField val logging: Boolean = false,
  encrypt: Boolean = false,
  insecureTls: Boolean = false,
  secretStorage: SecretStorageProvider? = null,
  @JvmField val mlkemParameterSet: Int = 1024
) {
  @JvmField var secretStorage: SecretStorageProvider? = secretStorage
  @JvmField var authTokenObjects = mutableMapOf<String, AuthToken?>()
  private var authToken: AuthToken? = null
  @JvmField var authInProcess: Boolean = false
  private val client = HttpClient(getRandomUri(), insecureTls = insecureTls)
  private var secret = ""
  @JvmField var bundle = ""
  @JvmField var remainderWallet: Wallet? = null
  @JvmField var cellSlug: String? = null
  @JvmField var lastMoleculeQuery: Mutation? = null

  companion object {
    private val jsonFormat: Json
      get() = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        coerceInputValues = true
      }
  }

  init {
    require(mlkemParameterSet in listOf(1024, 768)) {
      "KnishIO: unsupported ML-KEM parameter set $mlkemParameterSet; expected 1024 or 768."
    }
  }
  init {
    uris.forEach {
      authTokenObjects[it.toASCIIString()] = null
    }

    if (encrypt) {
      enableEncryption()
    }
  }

  private fun switchEncryption(encrypt: Boolean): Boolean {
    if(hasEncryption() == encrypt) {
      return false
    }

    if(encrypt) {
      enableEncryption()
    }
    else {
      disableEncryption()
    }
    return true
  }

  /**
   *  Enables end-to-end encryption protocol.
   */
  fun enableEncryption() {
    client.encrypt = true
  }

  /**
   *  Disables end-to-end encryption protocol.
   */
  fun disableEncryption() {
    client.encrypt = false
  }

  /**
   * Returns whether or not the end-to-end encryption protocol is enabled
   */
  fun hasEncryption(): Boolean {
    return client.encrypt
  }

  /**
   * Get random uri from specified uris
   */
  fun getRandomUri(): URI {
    return uris.random()
  }

  /**
   * Reset common properties
   */
  fun reset() {
    secret = ""
    bundle = ""
    secretStorage = null
    remainderWallet = null
  }

  /**
   * Deinitializes the Knish.IO client session so that a new session can replace it
   */
  fun deinitialize() {
    reset()
  }

  /**
   * Retrieves the endpoint URI for this session
   */
  fun uri(): String {
    return client.uri.toASCIIString()
  }

  /**
   * Returns the HTTP client class session
   */
  fun client(): HttpClient {
    if (! authInProcess) {
      val randomUri = getRandomUri()
      client.setUri(randomUri)

      // Try to get stored auth token object
      val authDataObj = authTokenObjects[randomUri.toASCIIString()]

      // Not authorized - try to do it
      authDataObj?.let {
        client.setAuthData(it.getAuthData())
      } ?: authorize(getSecret(), cellSlug(), client.encrypt)
    }

    return client
  }

  /**
   * Returns whether or not a secret is being stored for this session
   */
  fun hasSecret(): Boolean {
    return secret.isNotEmpty() || (secretStorage != null && bundle.isNotEmpty())
  }

  /**
   * Returns whether or not a bundle hash is being stored for this session
   */
  fun hasBundle(): Boolean {
    return bundle.isNotEmpty()
  }

  /**
   * Set the client's secret
   */
  fun setSecret(value: String): KnishIOClient {
    secret = value
    bundle = Crypto.generateBundleHash(value)
    if (secretStorage == null) {
      val memStorage = MemorySecretStorageProvider()
      memStorage.storeSecret(bundle, value)
      secretStorage = memStorage
    } else {
      secretStorage?.storeSecret(bundle, value)
    }
    return this
  }

  /**
   * Sets the secret storage provider and optionally associates an active bundle hash
   */
  @JvmOverloads
  fun setSecretStorage(storage: SecretStorageProvider, bundleHash: String? = null): KnishIOClient {
    secretStorage = storage
    if (bundleHash != null) {
      bundle = bundleHash
    }
    return this
  }

  /**
   * Returns the current secret storage provider
   */
  fun getSecretStorage(): SecretStorageProvider? {
    return secretStorage
  }

  /**
   * Asynchronously or synchronously retrieves the secret from storage or returns in-memory secret
   */
  @JvmOverloads
  fun retrieveSecret(options: StorageOptions = StorageOptions()): String? {
    if (secret.isNotEmpty()) {
      return secret
    }
    if (secretStorage != null && bundle.isNotEmpty()) {
      return secretStorage?.retrieveSecret(bundle, options)
    }
    return null
  }

  /**
   * Retrieves the stored secret for this session
   */
  @Throws(UnauthenticatedException::class)
  fun getSecret(): String {
    if (secret.isEmpty()) {
      throw UnauthenticatedException("KnishIOClient::getSecret() - Unable to find a stored secret!")
    }
    return secret
  }

  /**
   * Returns the bundle hash for this session
   */
  @Throws(UnauthenticatedException::class)
  fun bundle(): String {
    if (bundle.isEmpty()) {
      throw UnauthenticatedException("KnishIOClient::bundle() - Unable to find a stored bundle!")
    }

    return bundle
  }

  /**
   * Retrieves this session's remainder wallet
   */
  fun remainderWallet(): Wallet? {
    return remainderWallet
  }

  /**
   * Builds a new instance of the provided Query class
   */
  fun <T : KClass<*>> createQuery(queryClass: T): IQuery {
    return queryClass.primaryConstructor?.call(client()) as? IQuery ?: throw CodeException("invalid Query")
  }

  /**
   * Requests an authorization token from the node endpoint
   */
  @JvmOverloads
  fun requestAuthToken(
    secret: String? = null,
    seed: String? = null,
    cellSlug: String? = null,
    encrypt: Boolean = false
  ): AuthToken {
    val _secret = secret ?: seed?.let { Crypto.generateSecret(it) } ?: retrieveSecret()
    val slug = cellSlug ?: cellSlug()

    return authorize(_secret, slug, encrypt)
  }

  fun setAuthToken(authToken: AuthToken) {
    authTokenObjects[uri()] = authToken
    client().setAuthData(authToken.getAuthData())
    this.authToken = authToken
  }

  fun getAuthToken(): AuthToken? {
    return authToken
  }

  fun setCellSlug(cellSlug: String?) {
    this.cellSlug = cellSlug
  }

  private fun getGuestAuthToken(cellSlug: String?, encrypt: Boolean = false): AuthToken {
    setCellSlug(cellSlug)

    val wallet = Wallet(Crypto.generateSecret(), "AUTH", mlkemParameterSet = mlkemParameterSet)
    val query = createQuery(MutationRequestAuthorizationGuest::class) as MutationRequestAuthorizationGuest
    val response = query.execute(AccessTokenMutationVariable(this.cellSlug, wallet.pubkey, encrypt)) as ResponseRequestAuthorizationGuest

    val payload = response.payload()?.takeIf { response.success() }
      ?: throw UnauthenticatedException("Authorization attempt rejected by ledger. Reason: ${response.reason()}")

    return AuthToken.create(payload, wallet, encrypt)
  }

  private fun getProfileAuthToken(secret: String, encrypt: Boolean = false): AuthToken {
    setSecret(secret)

    // A returning identity signs from its ContinuID pointer with the USER wallet registered there,
    // so validator 0.5.0+ marks the token proven (unproven tokens are guests on permissioned and
    // private cells). No usable pointer → genesis/first login from a fresh AUTH wallet.
    val pointerWallet = continuIdAuthWallet(secret)
    if (pointerWallet != null) {
      val response = proposeAuthorization(secret, pointerWallet, encrypt)
      if (response.success()) {
        log("KnishIOClient::getProfileAuthToken() - token pointer-signed from ContinuID position ${pointerWallet.position}")
        return AuthToken.create(response.payload()!!, response.wallet(), encrypt)
      }
      // One fallback only: testnet allows 3 auths/min/IP, so a login sends at most two molecules.
      System.err.println("KnishIOClient::getProfileAuthToken() - WARNING: pointer-signed authorization rejected (${response.reason()}); falling back to an AUTH wallet")
    }

    val response = proposeAuthorization(secret, Wallet(secret, "AUTH", mlkemParameterSet = mlkemParameterSet), encrypt)
    log("KnishIOClient::getProfileAuthToken() - token signed from a fresh AUTH wallet (${if (pointerWallet == null) "no ContinuID pointer" else "pointer fallback"})")

    val payload = response.payload()?.takeIf { response.success() }
      ?: throw UnauthenticatedException("Authorization attempt rejected by ledger. Reason: ${response.reason()}")

    return AuthToken.create(payload, response.wallet(), encrypt)
  }

  /**
   * The USER wallet registered at the bundle's ContinuID pointer, derived from [secret], or null
   * when there is no USER pointer or the derived address differs from the one the ledger reports.
   */
  private fun continuIdAuthWallet(secret: String): Wallet? {
    val response = queryContinuId(bundle(), "USER")
    if (!response.success()) {
      throw InvalidResponseException("KnishIOClient::getProfileAuthToken() - ContinuId query failed: ${response.status()}")
    }
    val pointer = response.payload()?.takeIf { it.token == "USER" } ?: return null
    val position = pointer.position?.takeIf { it.isNotEmpty() } ?: return null
    val wallet = Wallet(secret, "USER", position, mlkemParameterSet = mlkemParameterSet)
    val addressMatches = wallet.position == position && (pointer.address.isNullOrEmpty() || pointer.address == wallet.address)
    return wallet.takeIf { addressMatches }
  }

  private fun proposeAuthorization(secret: String, wallet: Wallet, encrypt: Boolean): ResponseRequestAuthorization {
    // Explicit USER remainder (mirror JS createMolecule), so the ContinuID I-atom added by
    // initAuthorization is USER-token with previousPosition = the signer's position. Without it,
    // createMolecule auto-derives the remainder from an AUTH source token → a wrong-token I-atom.
    val molecule = createMolecule(secret, wallet, Wallet.create(secret, "USER", mlkemParameterSet = mlkemParameterSet))
    val query = createMoleculeMutation(MutationRequestAuthorization::class, molecule) as MutationRequestAuthorization

    // PQ-transport (cycle 162): convey the source wallet's ML-KEM public key as a SIGNED
    // `walletPubkey` meta on the U-atom (initAuthorization → finalMetas → sign), so the
    // validator can encrypt CipherHash responses back to THIS wallet (the one the client
    // decrypts with). Signed → tamper-proof. Only when present (PQ-capable wallet).
    val authMeta = mutableListOf(MetaData("encrypt", if (encrypt) "true" else "false"))
    wallet.pubkey?.takeIf { it.isNotEmpty() }?.let {
      authMeta.add(MetaData("walletPubkey", it))
    }
    query.fillMolecule(authMeta)

    val response = query.execute(MoleculeMutationVariable(query.molecule() !!)) as ResponseRequestAuthorization
    lastMoleculeQuery = null

    return response
  }

  private fun log(message: String) {
    if (logging) {
      println(message)
    }
  }

  @JvmOverloads
  fun authorize(
    secret: String? = null,
    cellSlug: String? = null,
    encrypt: Boolean = false
  ): AuthToken {
    authInProcess = true

    try {
      val authToken = when (secret) {
        null -> getGuestAuthToken(cellSlug, encrypt)
        else -> getProfileAuthToken(secret, encrypt)
      }

      setAuthToken(authToken)
      switchEncryption(encrypt)

      return authToken
    } finally {
      authInProcess = false
    }
  }

  /**
   * Uses the supplied Mutation class to build a new tailored Molecule
   */
  @JvmOverloads
  fun <T : KClass<*>> createMoleculeMutation(
    mutationClass: T,
    molecule: Molecule? = null
  ): MutationProposeMolecule {

    // If you don't supply the molecule, we'll generate one for you
    val newOrExistingMolecule = molecule ?: createMolecule()

    val mutation =
      mutationClass.primaryConstructor?.call(client(), newOrExistingMolecule) ?: throw CodeException("invalid Mutation")

    if (mutation !is MutationProposeMolecule) {
      throw CodeException("${mutationClass.simpleName}::createMoleculeMutation() - This method only accepts MutationProposeMolecule!")
    }

    lastMoleculeQuery = mutation

    return mutation
  }

  /**
   * Instantiates a new Molecule and prepares this client session to operate on it
   */
  @JvmOverloads
  fun createMolecule(
    secret: String? = null,
    sourceWallet: Wallet? = null,
    remainderWallet: Wallet? = null
  ): Molecule {
    val currentSecret = secret ?: (if (this.secret.isNotEmpty()) this.secret else retrieveSecret()) ?: getSecret()
    var signingWallet = sourceWallet

    // Sets the source wallet as the last remainder wallet (to maintain ContinuID). Only a USER
    // remainder continues the chain (JS createMolecule): after a transfer, fusion or buffer
    // operation the remainder is a token wallet, and signing a C/M molecule with it is rejected.
    if (sourceWallet == null && remainderWallet()?.token == "USER" && lastMoleculeQuery != null && lastMoleculeQuery !!.response != null && lastMoleculeQuery !!.response !!.success()) {
      signingWallet = remainderWallet()
    }

    // Unable to use last remainder wallet; Figure out what wallet to use:
    if (signingWallet == null) {
      signingWallet = sourceWallet()
    }

    // Set the remainder wallet for the next transaction
    this.remainderWallet = remainderWallet ?: Wallet.create(
      currentSecret, signingWallet.token, signingWallet.batchId, signingWallet.characters, mlkemParameterSet = mlkemParameterSet
    )

    val molecule = Molecule(
      currentSecret, signingWallet, remainderWallet(), cellSlug, mlkemParameterSet = mlkemParameterSet
    )
    if (hasBundle()) {
      molecule.bundle = bundle()
    }
    return molecule
  }

  /**
   * Retrieves this session's wallet used for signing the next Molecule
   */
  fun sourceWallet(): Wallet {
    // Resolve the bundle's live USER ContinuID position (mirror C++/Python: ContinuId needs the
    // token arg, else the validator can't resolve the chain head and returns null). Null ->
    // genesis fallback (Wallet(getSecret()) is a USER wallet by default).
    val fallbackWallet = if (hasSecret()) {
      val sec = if (secret.isNotEmpty()) secret else retrieveSecret()
      if (!sec.isNullOrEmpty()) Wallet(sec, mlkemParameterSet = mlkemParameterSet) else Wallet(mlkemParameterSet = mlkemParameterSet)
    } else {
      Wallet(mlkemParameterSet = mlkemParameterSet)
    }
    return queryContinuId(bundle(), "USER").payload() ?: fallbackWallet
  }

  /**
   * Queries the ledger for the next ContinuId wallet
   */
  @JvmOverloads
  fun queryContinuId(bundle: String, token: String = "USER"): ResponseContinuId {
    val query = createQuery(QueryContinuId::class) as QueryContinuId
    return query.execute(ContinuIdVariable(bundle, token)) as ResponseContinuId
  }

  /**
   * Retrieves the balance wallet for a specified Knish.IO identity and token slug. [type]
   * `"buffer"` selects the identity's buffer wallet; null selects its regular wallet.
   */
  @JvmOverloads
  fun queryBalance(
    token: String,
    bundle: String? = null,
    type: String? = null
  ): ResponseBalance {
    // Execute query with either the provided bundle hash or the active client's bundle.
    // Default to the client's own bundle when none is given (the documented contract + cross-SDK
    // parity: JS/TS/Python/PHP/C++ all self-scope a bundle-less queryBalance). Passing null sent
    // bundleHash:null to the validator -> a token-global query that returns an arbitrary wallet
    // (a stale-read footgun; e.g. a multi-recipient claimant read back another bundle's unit).
    val query = createQuery(QueryBalance::class) as QueryBalance
    return query.execute(BalanceVariable(token = token, bundleHash = bundle ?: bundle(), type = type)) as ResponseBalance
  }

  /**
   * Retrieves metadata for the given metaType and provided parameters
   */
  @JvmOverloads
  fun queryMeta(
    metaType: String? = null,
    metaIds: List<String> = listOf(),
    keys: List<String> = listOf(),
    values: List<String> = listOf(),
    latest: Boolean? = null,
    latestMetas: Boolean? = null,
    filter: List<MetaFilter> = listOf(),
    queryArgs: QueryArgs? = null,
    count: String? = null,
    countBy: String? = null
  ): ResponseMetaType.Response? {
    val response = (createQuery(QueryMetaType::class) as QueryMetaType).execute(
      MetaTypeVariable(
        metaType = metaType,
        metaIds = metaIds,
        keys = keys,
        values = values,
        latest = latest,
        latestMetas = latestMetas,
        filter = filter,
        queryArgs = queryArgs,
        count = count,
        countBy = countBy
      )
    ) as ResponseMetaType

    return response.payload()
  }


  /**
   * Retrieves metadata for the given metaType and provided parameters
   */
  @JvmOverloads
  fun queryMetaInstance(
    metaType: String,
    metaId: String? = null,
    key: String? = null,
    value: String? = null,
    latest: Boolean? = null,
    filter: List<MetaFilter>? = null
  ): List<MetaType>? {

    val query = createQuery(QueryMetaType::class) as QueryMetaType
    val variables = MetaTypeVariable(metaType)

    metaId?.let {
      variables.metaIds = listOf(it)
    }
    key?.let {
      variables.keys = listOf(it)
    }
    value?.let {
      variables.values = listOf(it)
    }
    latest?.let {
      variables.latest = it
    }
    filter?.let {
      variables.filter = it
    }

    val response = query.execute(variables) as ResponseMetaType

    return response.data()
  }

  /**
   * Query batch to get cascading meta instances by batchID
   */
  fun queryBatch(batchId: String): ResponseBatch {
    val query = createQuery(QueryBatch::class) as QueryBatch
    return query.execute(BatchVariable(batchId)) as ResponseBatch
  }

  /**
   * Query batch history to get cascading meta instances by batchID
   */
  fun queryBatchHistory(batchId: String): ResponseBatchHistory {
    val query = createQuery(QueryBatchHistory::class) as QueryBatchHistory
    return query.execute(BatchHistoryVariable(batchId)) as ResponseBatchHistory
  }

  /**
   * Queries the ledger to retrieve a list of active sessions for the given MetaType
   */
  @JvmOverloads
  fun queryActiveSession(
    bundleHash: String? = null,
    metaType: String? = null,
    metaId: String? = null
  ): ResponseActiveSession {
    val query = createQuery(QueryActiveSession::class) as QueryActiveSession
    return query.execute(ActiveSessionVariable(bundleHash, metaType, metaId)) as ResponseActiveSession
  }

  /**
   * Retrieves a list of your active wallets (unspent)
   */
  @JvmOverloads
  fun queryWallets(
    bundle: String? = null,
    token: String? = null,
    unspent: Boolean? = null
  ): List<Wallet> {
    val walletQuery = createQuery(QueryWalletList::class) as QueryWalletList

    val response = walletQuery.execute(
      WalletListVariable(bundleHash = bundle, token = token, unspent = unspent)
    ) as ResponseWalletList

    return response.getWallets()
  }

  /**
   * Retrieves a list of your shadow wallets (balance, but no keys)
   */
  @JvmOverloads
  fun queryShadowWallets(
    token: String = "KNISH",
    bundle: String? = null
  ): List<Wallet> {
    val shadowWalletQuery = createQuery(QueryWalletList::class) as QueryWalletList
    val response = shadowWalletQuery.execute(
      WalletListVariable(bundleHash = bundle ?: bundle(), token = token)
    ) as ResponseWalletList

    return response.payload()
  }

  @JvmOverloads
  fun queryBundleRaw(
    bundle: String? = null,
    key: String? = null,
    value: String? = null,
    latest: Boolean = true
  ): ResponseWalletBundle {
    val query = createQuery(QueryWalletBundle::class) as QueryWalletBundle

    return query.execute(
      WalletBundleVariable(
        bundleHash = bundle ?: bundle(), key = key, value = value, latest = latest
      )
    ) as ResponseWalletBundle
  }

  /**
   * Retrieves your wallet bundle's metadata from the ledger
   */
  @JvmOverloads
  fun queryBundle(
    bundle: String? = null,
    key: String? = null,
    value: String? = null,
    latest: Boolean = true
  ): Map<String, WalletBundle> {
    return queryBundleRaw(bundle, key, value, latest).payload()
  }

  /**
   * Builds and executes a molecule to issue a new Wallet on the ledger
   */
  fun createWallet(token: String): ResponseProposeMolecule {
    val newWallet = Wallet(getSecret(), token, mlkemParameterSet = mlkemParameterSet)
    val query = createMoleculeMutation(MutationCreateWallet::class) as MutationCreateWallet

    query.fillMolecule(newWallet)

    return submit(query)
  }

  /**
   * Builds and executes a molecule to issue a new token on the ledger
   */
  @JvmOverloads
  fun createToken(
    token: String,
    amount: Number? = null,
    meta: MutableList<MetaData> = mutableListOf(),
    batchId: String? = null,
    units: MutableList<TokenUnit> = mutableListOf()
  ): ResponseProposeMolecule {
    val newOrExistingBatchId = batchId ?: Crypto.generateBatchId()
    var tokenAmount = amount ?: 0

    // Stackable tokens need a new batch for every transfer
    meta.firstOrNull { it.key == "fungibility" }?.let { _ ->

      // No batch ID specified? Create a random one
      meta.firstOrNull { it.key == "batchId" }?.let {
        it.value = newOrExistingBatchId
      } ?: meta.add(MetaData("batchId", newOrExistingBatchId))

      // Adding unit IDs to the token
      if (units.isNotEmpty()) {

        // Stackable tokens with Unit IDs must not use decimals
        meta.firstOrNull { it.key == "decimals" }?.let {
          if (it.value !!.toDouble() > 0) {
            throw StackableUnitDecimalsException()
          }
        }

        // Can't create stackable units AND provide amount
        amount?.let {
          if (it.toDouble() > 0) {
            throw StackableUnitAmountException()
          }
        }

        // Calculating amount based on Unit IDs
        tokenAmount = units.size
        meta.firstOrNull { it.key == "splittable" }?.let {
          it.value = "1"
        } ?: meta.add(MetaData("splittable", "1"))

        meta.firstOrNull { it.key == "tokenUnits" }?.let {
          it.value = jsonFormat.encodeToString(units)
        } ?: meta.add(MetaData("tokenUnits", jsonFormat.encodeToString(units)))
      }
    }

    // Creating the wallet that will receive the new tokens
    val recipientWallet = Wallet(getSecret(), token, newOrExistingBatchId, mlkemParameterSet = mlkemParameterSet)
    val query = createMoleculeMutation(MutationCreateToken::class) as MutationCreateToken

    query.fillMolecule(recipientWallet, tokenAmount, meta)

    return submit(query)
  }

  /**
   * Builds and executes a molecule to convey new metadata to the ledger
   */
  @JvmOverloads
  fun createMeta(
    metaType: String,
    metaId: String,
    meta: MutableList<MetaData> = mutableListOf()
  ): ResponseProposeMolecule {
    val query = createMoleculeMutation(
      MutationCreateMeta::class, createMolecule(getSecret(), sourceWallet())
    ) as MutationCreateMeta

    query.fillMolecule(metaType, metaId, meta)

    return submit(query)
  }

  /**
   * Builds and executes a molecule to create a new identifier on the ledger
   */
  fun createIdentifier(
    type: String,
    contact: String,
    code: String
  ): ResponseProposeMolecule {
    val query = createMoleculeMutation(MutationCreateIdentifier::class) as MutationCreateIdentifier

    query.fillMolecule(type, contact, code)

    return submit(query)
  }

  /**
   * Builds and executes a Molecule that requests token payment from the node
   */
  @JvmOverloads
  fun requestTokens(
    token: String,
    to: Wallet? = null,
    amount: Number? = null,
    units: MutableList<TokenUnit> = mutableListOf(),
    meta: MutableList<MetaData> = mutableListOf(),
    batchId: String? = null
  ): ResponseProposeMolecule {
    val specification = getSpecificationRequestTokens(to, token)
    val metaType: String? = specification["metaType"]
    val metaId: String? = specification["metaId"]

    // Are we specifying a specific recipient?
    to?.let {
      meta.firstOrNull { it.key == "position" }?.let {
        it.value = to.position
      } ?: meta.add(MetaData("position", to.position))

      meta.firstOrNull { it.key == "bundle" }?.let {
        it.value = to.bundle
      } ?: meta.add(MetaData("position", to.bundle))
    }

    return requestTokensQuery(token, amount, metaType, metaId, units, meta, batchId)
  }

  /**
   * Builds and executes a Molecule that requests token payment from the node
   */
  fun requestTokens(
    token: String,
    to: String? = null,
    amount: Number? = null,
    units: MutableList<TokenUnit> = mutableListOf(),
    meta: MutableList<MetaData> = mutableListOf(),
    batchId: String? = null
  ): ResponseProposeMolecule {
    val specification = getSpecificationRequestTokens(to, token)
    val metaType: String? = specification["metaType"]
    val metaId: String? = specification["metaId"]

    return requestTokensQuery(token, amount, metaType, metaId, units, meta, batchId)
  }

  private fun requestTokensQuery(
    token: String,
    amount: Number? = null,
    metaType: String? = null,
    metaId: String? = null,
    units: MutableList<TokenUnit> = mutableListOf(),
    meta: MutableList<MetaData> = mutableListOf(),
    batchId: String? = null
  ): ResponseProposeMolecule {
    var requestedAmount = amount ?: 0

    // Calculate amount & set meta key
    if (units.isNotEmpty()) {

      // Can't move stackable units AND provide amount
      if (requestedAmount.toDouble() > 0) {
        throw StackableUnitAmountException()
      }

      // Calculating amount based on Unit IDs
      requestedAmount = units.size
      meta.firstOrNull { it.key == "tokenUnits" }?.let {
        it.value = jsonFormat.encodeToString(units)
      } ?: meta.add(MetaData("tokenUnits", jsonFormat.encodeToString(units)))
    }

    val query = createMoleculeMutation(MutationRequestTokens::class) as MutationRequestTokens

    query.fillMolecule(token, requestedAmount, metaType, metaId, meta, batchId)

    return submit(query)
  }

  private fun <T> getSpecificationRequestTokens(
    sender: T,
    token: String
  ): Map<String, String?> {
    return when (sender) {
      // If recipient is a Wallet, we need to help the node triangulate
      // the transfer by providing position and bundle hash
      is Wallet -> mapOf("metaType" to "wallet", "metaId" to sender.address)
      // If the recipient is provided as an object, try to figure out the actual recipient
      is String -> run {
        if (Wallet.isBundleHash(sender)) {
          mapOf("metaType" to "walletBundle", "metaId" to sender)
        } else {
          val wallet = Wallet.create(sender, token, mlkemParameterSet = mlkemParameterSet)
          mapOf("metaType" to "wallet", "metaId" to wallet.address)
        }
      }
      // No recipient, so request tokens for ourselves
      else -> mapOf("metaType" to "walletBundle", "metaId" to bundle())
    }
  }

  /**
   * Creates and executes a Molecule that assigns keys to an unclaimed shadow wallet. Without a
   * [batchId] it claims the first shadow wallet `queryWallets` lists for [token] in this bundle.
   *
   * @throws WalletShadowException when no [batchId] is given and the bundle has no shadow wallet
   *   for [token].
   */
  @JvmOverloads
  @Throws(WalletShadowException::class)
  fun claimShadowWallet(
    token: String,
    batchId: String? = null,
    molecule: Molecule? = null
  ): ResponseProposeMolecule {
    val claimBatchId = batchId ?: queryWallets(bundle = bundle(), token = token).firstOrNull { it.isShadow() }?.batchId
      ?: throw WalletShadowException("KnishIOClient::claimShadowWallet() - No shadow wallets found for token $token")
    val query = createMoleculeMutation(MutationClaimShadowWallet::class, molecule) as MutationClaimShadowWallet

    query.fillMolecule(token, claimBatchId)

    return submit(query)
  }

  /**
   * Creates and executes a Molecule that moves tokens from one user to another
   */
  @JvmOverloads
  fun transferToken(
    recipient: Wallet,
    token: String,
    amount: Number,
    units: MutableList<TokenUnit> = mutableListOf(),
    batchId: String? = null,
    sourceWallet: Wallet? = null
  ): ResponseProposeMolecule {
    val signingWallet = sourceWallet ?: queryBalance(token).payload()
    var transferAmount = amount

    // Calculate amount & set meta key
    if (units.isNotEmpty()) {

      // Can't move stackable units AND provide amount
      if (transferAmount.toDouble() > 0) {
        throw StackableUnitAmountException()
      }

      transferAmount = units.size
    }

    // Do you have enough tokens?
    if (signingWallet == null || signingWallet.balance < transferAmount.toDouble()) {
      throw TransferBalanceException()
    }

    // Compute the batch ID for the recipient
    // (typically used by stackable tokens)
    batchId?.let {
      recipient.batchId = batchId
    } ?: recipient.initBatchId(signingWallet)

    remainderWallet = Wallet.create(
      getSecret(), token, characters = signingWallet.characters, mlkemParameterSet = mlkemParameterSet
    )

    remainderWallet !!.initBatchId(signingWallet, true)

    // Token units splitting
    signingWallet.splitUnits(units, remainderWallet !!, recipient)

    // Build the molecule itself
    val molecule = createMolecule(sourceWallet = signingWallet, remainderWallet = remainderWallet)
    val query = createMoleculeMutation(MutationTransferTokens::class, molecule) as MutationTransferTokens

    query.fillMolecule(recipient, transferAmount)

    return submit(query)
  }

  /**
   * Creates and executes a Molecule that moves tokens from one user to another
   */
  @JvmOverloads
  fun transferToken(
    recipient: String,
    token: String,
    amount: Number,
    units: MutableList<TokenUnit> = mutableListOf(),
    batchId: String? = null,
    sourceWallet: Wallet? = null
  ): ResponseProposeMolecule {
    var recipientWallet = queryBalance(token, recipient).payload()

    if (recipientWallet == null) {
      recipientWallet = Wallet.create(recipient, token, mlkemParameterSet = mlkemParameterSet)
    }

    return transferToken(recipientWallet, token, amount, units, batchId, sourceWallet)
  }

  /**
   * Creates and executes a Molecule that funds N recipients from a single source in ONE molecule
   * (multi-recipient sibling of transferToken). Each recipient gets its own subset of stackable
   * units (or a fungible amount); a remainder returns the rest to the sender.
   */
  @JvmOverloads
  fun transferTokens(
    token: String,
    recipients: List<TransferRecipient>,
    sourceWallet: Wallet? = null
  ): ResponseProposeMolecule {
    val signingWallet = sourceWallet ?: queryBalance(token).payload()
    ?: throw TransferBalanceException()

    // Per-recipient amount: stackable -> unit count; fungible -> explicit amount (never both)
    val amounts: List<Number> = recipients.map { recipient ->
      if (recipient.units.isNotEmpty()) {
        if ((recipient.amount?.toDouble() ?: 0.0) > 0) {
          throw StackableUnitAmountException()
        }
        recipient.units.size
      } else {
        recipient.amount ?: 0
      }
    }
    val total = amounts.sumOf { it.toDouble() }

    // Do you have enough tokens?
    if (signingWallet.balance < total) {
      throw TransferBalanceException()
    }

    // A shadow recipient wallet per destination + a distinct batch id
    val recipientWallets = recipients.map { recipient ->
      Wallet.create(recipient.bundleHash, token, mlkemParameterSet = mlkemParameterSet).also { rw ->
        recipient.batchId?.let { rw.batchId = it } ?: rw.initBatchId(signingWallet)
      }
    }

    remainderWallet = Wallet.create(
      getSecret(), token, characters = signingWallet.characters, mlkemParameterSet = mlkemParameterSet
    )
    remainderWallet !!.initBatchId(signingWallet, true)

    // Stackable (NFT): partition the source's tokenUnits across source (SENT union), each recipient
    // (its subset), and remainder (KEPT) before the molecule is built. No-op for fungible.
    if (recipients.any { it.units.isNotEmpty() }) {
      signingWallet.splitUnitsMulti(recipients.map { it.units }, recipientWallets, remainderWallet !!)
    }

    val molecule = createMolecule(sourceWallet = signingWallet, remainderWallet = remainderWallet)
    val query = createMoleculeMutation(MutationTransferTokens::class, molecule) as MutationTransferTokens

    query.fillMoleculeMulti(recipientWallets, amounts)

    return submit(query)
  }

  /**
   * Builds and executes a molecule to destroy the specified Token units
   */
  @JvmOverloads
  fun burnTokens(
    token: String,
    amount: Number = 0,
    units: MutableList<TokenUnit> = mutableListOf(),
    sourceWallet: Wallet? = null
  ): ResponseProposeMolecule {
    // Resolve the signing wallet ONCE (the passed source, else the on-ledger balance wallet).
    // Every downstream use must be this resolved wallet — referencing the nullable `sourceWallet`
    // param NPEs when the caller omits it (the common burnTokens(token, amount) call). Mirrors
    // transferToken's resolve-once pattern.
    val signingWallet = sourceWallet ?: queryBalance(token).payload()
      ?: throw TransferBalanceException()
    val remainderWallet = Wallet.create(getSecret(), token, characters = signingWallet.characters, mlkemParameterSet = mlkemParameterSet)
    var burnAmount = amount

    remainderWallet.initBatchId(signingWallet, true)

    // Calculate amount & set meta key
    if (units.isNotEmpty()) {

      // Can't burn stackable units AND provide amount
      if (burnAmount.toDouble() > 0) {
        throw StackableUnitAmountException()
      }

      // Calculating amount based on Unit IDs
      burnAmount = units.size

      // Token units splitting
      signingWallet.splitUnits(units, remainderWallet)
    }

    // Burn tokens
    val molecule = createMolecule(null, signingWallet, remainderWallet)

    molecule.burnToken(burnAmount)
    molecule.sign()

    return submit(MutationProposeMolecule(client(), molecule))
  }

  /**
   * Replenishes the supply of [token], a token this identity created with supply `infinite` or
   * `replenishable` (contract 9.1): a C atom with `action` = `add` plus the ContinuID atom, signed
   * by the USER wallet. Fungible: [amount] > 0. Stackable: the new [units] (their count is the
   * amount). The identity's wallet for [token] is credited, or a new one when it holds none.
   *
   * @throws StackableUnitAmountException for a stackable wallet without [units], or [units] with a
   *   different [amount].
   * @throws NegativeAmountException when the replenished amount is not positive.
   */
  @JvmOverloads
  @Throws(StackableUnitAmountException::class, NegativeAmountException::class)
  fun replenishToken(
    token: String,
    amount: Number? = null,
    units: MutableList<TokenUnit> = mutableListOf()
  ): ResponseProposeMolecule {
    val creditedWallet = queryBalance(token).payload()
      ?: Wallet.create(getSecret(), token, mlkemParameterSet = mlkemParameterSet)

    // A wallet that holds units is stackable: its supply grows by new units, never by a bare amount.
    if (units.isEmpty() && creditedWallet.hasTokenUnits()) {
      throw StackableUnitAmountException("KnishIOClient::replenishToken() - Stackable token $token is replenished with new units")
    }

    val query = createMoleculeMutation(MutationProposeMolecule::class)
    query.molecule() !!.apply {
      replenishTokens(amount, token, creditedWallet, units)
      sign()
    }

    return submit(query)
  }

  /**
   * Fuses the units [fusedTokenUnitIds] (at least two) of the stackable token [tokenSlug] held by
   * this identity into one new unit [newTokenUnit] (contract 9.2), delivered to [bundleHash]: the
   * caller's own bundle by default. The fused units leave circulation; the validator records them
   * in the new unit's `fusedTokenUnits` meta.
   *
   * @throws TransferBalanceException for fewer than two units, a unit this identity does not hold,
   *   a new unit id it already holds, or no wallet for [tokenSlug].
   */
  @JvmOverloads
  @Throws(TransferBalanceException::class)
  fun fuseToken(
    tokenSlug: String,
    newTokenUnit: TokenUnit,
    fusedTokenUnitIds: List<String>,
    bundleHash: String? = null,
    sourceWallet: Wallet? = null
  ): ResponseProposeMolecule {
    val signingWallet = sourceWallet ?: queryBalance(tokenSlug).payload()
      ?: throw TransferBalanceException()

    val recipientBundle = bundleHash ?: bundle()
    val recipientWallet = if (recipientBundle == bundle()) {
      Wallet.create(getSecret(), tokenSlug, mlkemParameterSet = mlkemParameterSet)
    } else {
      queryBalance(tokenSlug, recipientBundle).payload()
        ?: Wallet.create(recipientBundle, tokenSlug, mlkemParameterSet = mlkemParameterSet)
    }
    recipientWallet.initBatchId(signingWallet)

    remainderWallet = Wallet.create(
      getSecret(), tokenSlug, characters = signingWallet.characters, mlkemParameterSet = mlkemParameterSet
    )
    remainderWallet !!.initBatchId(signingWallet, true)

    val molecule = createMolecule(sourceWallet = signingWallet, remainderWallet = remainderWallet)
    val query = createMoleculeMutation(MutationProposeMolecule::class, molecule)

    molecule.fuseToken(fusedTokenUnitIds, newTokenUnit, recipientWallet)
    molecule.sign()

    return submit(query)
  }

  /**
   * Fuses [fusedTokenUnitIds] into a new unit whose id and name are [newTokenUnitId].
   */
  @JvmOverloads
  @Throws(TransferBalanceException::class)
  fun fuseToken(
    tokenSlug: String,
    newTokenUnitId: String,
    fusedTokenUnitIds: List<String>,
    bundleHash: String? = null,
    sourceWallet: Wallet? = null
  ): ResponseProposeMolecule {
    return fuseToken(tokenSlug, TokenUnit(newTokenUnitId, newTokenUnitId, listOf()), fusedTokenUnitIds, bundleHash, sourceWallet)
  }

  /**
   * Withdraws [amount] of [token] from this identity's buffer (B-isotope) wallet back to its own
   * bundle (contract 9.6): source B `-balance`, recipient V `+amount`, remainder B `+(balance -
   * amount)` at a FRESH position (a remainder at the source's consumed signing position would be
   * stranded). The source is [sourceWallet], else `Balance(token, type: "buffer")`.
   *
   * @throws TransferBalanceException when there is no buffer wallet or its balance is below [amount].
   */
  @JvmOverloads
  @Throws(TransferBalanceException::class)
  fun withdrawBufferToken(
    token: String,
    amount: Number,
    sourceWallet: Wallet? = null
  ): ResponseProposeMolecule {
    val source = sourceWallet ?: queryBalance(token, type = "buffer").payload()
    if (source == null || source.balance < amount.toDouble()) {
      throw TransferBalanceException()
    }

    // JS parity: withdraw `amount` from the buffer back to the caller's own bundle.
    val recipients = mapOf(bundle() to amount)

    val remainder = Wallet.create(getSecret(), token, characters = source.characters, mlkemParameterSet = mlkemParameterSet)
    remainder.initBatchId(source, true)

    val molecule = createMolecule(sourceWallet = source, remainderWallet = remainder)
    val query = createMoleculeMutation(MutationWithdrawBufferToken::class, molecule) as MutationWithdrawBufferToken

    query.fillMolecule(recipients)

    return submit(query)
  }

  /**
   * Deposits [amount] of [token] from the caller's regular balance wallet INTO a buffer
   * (B-isotope) wallet.
   *
   * Client-level wrapper over [Molecule.initDepositBuffer] (V-B-V), mirroring JS
   * `depositBufferToken` / Rust `deposit_buffer_token`: the source is the regular balance wallet
   * and the change routes to a FRESH remainder. Pass an explicit [sourceWallet] to override the
   * resolved balance wallet; [tradeRates] rides for cross-SDK API parity.
   */
  @JvmOverloads
  fun depositBufferToken(
    token: String,
    amount: Number,
    tradeRates: Map<String, Any> = emptyMap(),
    sourceWallet: Wallet? = null
  ): ResponseProposeMolecule {
    // Resolve the source (the passed wallet, else the on-ledger balance wallet).
    val source = sourceWallet ?: queryBalance(token).payload()
      ?: throw TransferBalanceException()

    // Deposit routes the change to a FRESH remainder (not the source, unlike withdraw).
    val remainder = Wallet.create(getSecret(), token, characters = source.characters, mlkemParameterSet = mlkemParameterSet)
    remainder.initBatchId(source, true)

    val molecule = createMolecule(sourceWallet = source, remainderWallet = remainder)
    val query = createMoleculeMutation(MutationDepositBufferToken::class, molecule) as MutationDepositBufferToken

    query.fillMolecule(amount, tradeRates)

    return submit(query)
  }

  /**
   * Sends a molecule this client built, after the SDK's own check of the signed molecule
   * ([Molecule.verify], contract 9.7). A molecule that fails it throws the check's exception and
   * is never sent: the validator would reject it after consuming the signing key (for example a
   * USER-signed molecule without its ContinuID atom). A caller-built molecule sent through
   * [MutationProposeMolecule.execute] directly is not checked.
   */
  private fun submit(query: MutationProposeMolecule): ResponseProposeMolecule {
    val molecule = query.molecule() ?: throw CodeException("KnishIOClient::submit() - the mutation holds no molecule")
    Molecule.verify(molecule, molecule.sourceWallet)
    return query.execute(MoleculeMutationVariable(molecule)) as ResponseProposeMolecule
  }

  /**
   * Returns the currently defined Cell identifier for this session
   */
  fun cellSlug(): String? {
    return cellSlug
  }
}
