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

/**
 * Base client class providing a powerful but user-friendly wrapper
 * around complex Knish.IO ledger transactions.
 */
class KnishIOClient @JvmOverloads constructor(
  @JvmField val uris: List<URI>,
  @JvmField val serverSdkVersion: Int = 3,
  @JvmField val logging: Boolean = false,
  encrypt: Boolean = false
) {
  @JvmField var authTokenObjects = mutableMapOf<String, AuthToken?>()
  private var authToken: AuthToken? = null
  @JvmField var authInProcess: Boolean = false
  private val client = HttpClient(getRandomUri())
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
    return secret.isNotEmpty()
  }

  /**
   * Set the client's secret
   */
  fun setSecret(value: String) {
    secret = value
    bundle = Crypto.generateBundleHash(value)
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
    val _secret = secret ?: seed?.let { Crypto.generateSecret(it) }
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

    val wallet = Wallet(Crypto.generateSecret(), "AUTH")
    val query = createQuery(MutationRequestAuthorizationGuest::class) as MutationRequestAuthorizationGuest
    val response = query.execute(AccessTokenMutationVariable(this.cellSlug, wallet.pubkey, encrypt)) as ResponseRequestAuthorizationGuest

    return AuthToken.create(response.payload()!!, wallet, encrypt)
  }

  private fun getProfileAuthToken(secret: String, encrypt: Boolean = false): AuthToken {
    setSecret(secret)

    val wallet = Wallet(secret, "AUTH")
    val molecule = createMolecule(secret, wallet)
    val query = createMoleculeMutation(MutationRequestAuthorization::class, molecule) as MutationRequestAuthorization

    query.fillMolecule(listOf(MetaData("encrypt", if (encrypt) "true" else "false")))

    val response = query.execute(MoleculeMutationVariable(query.molecule() !!)) as ResponseRequestAuthorization

    return AuthToken.create(response.payload()!!, response.wallet(), encrypt)
  }

  @JvmOverloads
  fun authorize(
    secret: String? = null,
    cellSlug: String? = null,
    encrypt: Boolean = false
  ): AuthToken {
    authInProcess = true

    val authToken = when (secret) {
      null -> getGuestAuthToken(cellSlug, encrypt)
      else -> getProfileAuthToken(secret, encrypt)
    }

    setAuthToken(authToken)
    switchEncryption(encrypt)

    authInProcess = false

    return authToken
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
    val currentSecret = secret ?: getSecret()
    var signingWallet = sourceWallet

    // Sets the source wallet as the last remainder wallet (to maintain ContinuID)
    if (sourceWallet == null && remainderWallet()?.token != "AUTH" && lastMoleculeQuery != null && lastMoleculeQuery !!.response != null && lastMoleculeQuery !!.response !!.success()) {
      signingWallet = remainderWallet()
    }

    // Unable to use last remainder wallet; Figure out what wallet to use:
    if (signingWallet == null) {
      signingWallet = sourceWallet()
    }

    // Set the remainder wallet for the next transaction
    this.remainderWallet = remainderWallet ?: Wallet.create(
      currentSecret, signingWallet.token, signingWallet.batchId, signingWallet.characters
    )

    return Molecule(
      currentSecret, signingWallet, remainderWallet(), cellSlug
    )
  }

  /**
   * Retrieves this session's wallet used for signing the next Molecule
   */
  fun sourceWallet(): Wallet {
    return queryContinuId(bundle()).payload() ?: Wallet(getSecret())
  }

  /**
   * Queries the ledger for the next ContinuId wallet
   */
  fun queryContinuId(bundle: String): ResponseContinuId {
    val query = createQuery(QueryContinuId::class) as QueryContinuId
    return query.execute(ContinuIdVariable(bundle)) as ResponseContinuId
  }

  /**
   * Retrieves the balance wallet for a specified Knish.IO identity and token slug
   */
  @JvmOverloads
  fun queryBalance(
    token: String,
    bundle: String? = null
  ): ResponseBalance {
    // Execute query with either the provided bundle hash or the active client's bundle
    val query = createQuery(QueryBalance::class) as QueryBalance
    return query.execute(BalanceVariable(token = token, bundleHash = bundle)) as ResponseBalance
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

  @JvmOverloads
  fun queryUserActivity(
    bundleHash: String? = null,
    metaType: String? = null,
    metaId: String? = null,
    ipAddress: String? = null,
    browser: String? = null,
    osCpu: String? = null,
    resolution: String? = null,
    timeZone: String? = null,
    countBy: List<CountByUserActivity>? = null,
    interval: Span = Span.HOUR
  ): ResponseUserActivity {
    val query = createQuery(QueryUserActivity::class) as QueryUserActivity

    return query.execute(
      UserActivityVariable(
        bundleHash, metaType, metaId, ipAddress, browser, osCpu, resolution, timeZone, countBy, interval
      )
    ) as ResponseUserActivity
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
    val newWallet = Wallet(getSecret(), token)
    val query = createMoleculeMutation(MutationCreateWallet::class) as MutationCreateWallet

    query.fillMolecule(newWallet)

    return query.execute(MoleculeMutationVariable(query.molecule() !!)) as ResponseProposeMolecule
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
    val recipientWallet = Wallet(getSecret(), token, newOrExistingBatchId)
    val query = createMoleculeMutation(MutationCreateToken::class) as MutationCreateToken

    query.fillMolecule(recipientWallet, tokenAmount, meta)

    return query.execute(MoleculeMutationVariable(query.molecule() !!)) as ResponseProposeMolecule
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

    return query.execute(MoleculeMutationVariable(query.molecule() !!)) as ResponseProposeMolecule
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

    return query.execute(MoleculeMutationVariable(query.molecule() !!)) as ResponseProposeMolecule
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

    return query.execute(MoleculeMutationVariable(query.molecule() !!)) as ResponseProposeMolecule
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
          val wallet = Wallet.create(sender, token)
          mapOf("metaType" to "wallet", "metaId" to wallet.address)
        }
      }
      // No recipient, so request tokens for ourselves
      else -> mapOf("metaType" to "walletBundle", "metaId" to bundle())
    }
  }

  /**
   * Creates and executes a Molecule that assigns keys to an unclaimed shadow wallet
   */
  @JvmOverloads
  fun claimShadowWallet(
    token: String,
    batchId: String? = null,
    molecule: Molecule? = null
  ): ResponseProposeMolecule {
    val query = createMoleculeMutation(MutationClaimShadowWallet::class, molecule) as MutationClaimShadowWallet

    query.fillMolecule(token, batchId)

    return query.execute(MoleculeMutationVariable(query.molecule() !!)) as ResponseProposeMolecule
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
      getSecret(), token, characters = signingWallet.characters
    )

    remainderWallet !!.initBatchId(signingWallet, true)

    // Token units splitting
    signingWallet.splitUnits(units, remainderWallet !!, recipient)

    // Build the molecule itself
    val molecule = createMolecule(sourceWallet = signingWallet, remainderWallet = remainderWallet)
    val query = createMoleculeMutation(MutationTransferTokens::class, molecule) as MutationTransferTokens

    query.fillMolecule(recipient, transferAmount)

    return query.execute(MoleculeMutationVariable(query.molecule() !!)) as ResponseProposeMolecule
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
      recipientWallet = Wallet.create(recipient, token)
    }

    return transferToken(recipientWallet, token, amount, units, batchId, sourceWallet)
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
    val signingWallet = sourceWallet ?: queryBalance(token).payload()
    val remainderWallet = Wallet.create(getSecret(), token, characters = sourceWallet !!.characters)
    var burnAmount = amount

    remainderWallet.initBatchId(signingWallet !!, true)

    // Calculate amount & set meta key
    if (units.isNotEmpty()) {

      // Can't burn stackable units AND provide amount
      if (burnAmount.toDouble() > 0) {
        throw StackableUnitAmountException()
      }

      // Calculating amount based on Unit IDs
      burnAmount = units.size

      // Token units splitting
      sourceWallet.splitUnits(units, remainderWallet)
    }

    // Burn tokens
    val molecule = createMolecule(null, sourceWallet, remainderWallet)

    molecule.burnToken(burnAmount)
    molecule.sign()
    molecule.check()

    return MutationProposeMolecule(
      client(),
      molecule
    ).execute(MoleculeMutationVariable(molecule)) as ResponseProposeMolecule
  }

  /**
   * Returns the currently defined Cell identifier for this session
   */
  fun cellSlug(): String? {
    return cellSlug
  }
}
