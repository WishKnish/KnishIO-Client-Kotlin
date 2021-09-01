@file:JvmName("KnishIOClient")

package wishKnish.knishIO.client

import kotlinx.serialization.encodeToString
import wishKnish.knishIO.client.data.Clients
import wishKnish.knishIO.client.data.MetaData
import wishKnish.knishIO.client.data.graphql.types.*
import wishKnish.knishIO.client.data.json.variables.*
import wishKnish.knishIO.client.httpClient.HttpClient
import wishKnish.knishIO.client.libraries.Crypto
import wishKnish.knishIO.client.mutation.*
import wishKnish.knishIO.client.query.*
import wishKnish.knishIO.client.response.*
import wishKnish.knishIO.client.response.IResponseRequestAuthorization
import java.net.URI
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import kotlinx.serialization.json.Json
import wishKnish.knishIO.client.exception.*
import kotlin.jvm.Throws

class KnishIOClient @JvmOverloads constructor(
  @JvmField val uris: List<URI>,
  @JvmField val serverSdkVersion: Int = 3,
  @JvmField val logging: Boolean = false,
  encrypt: Boolean = false
) {
  @JvmField var clients = mutableMapOf<String, Clients?>()
  @JvmField var authProcess: Boolean = false
  @JvmField val client = HttpClient(getRandomUri())
  @JvmField var secret = ""
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
      clients[it.toASCIIString()] = null
    }

    if (encrypt) {
      enableEncryption()
    }
  }

  fun enableEncryption() {
    client.encrypt = true
  }

  fun disableEncryption() {
    client.encrypt = false
  }

  fun hasEncryption(): Boolean {
    return client.encrypt
  }

  fun getRandomUri(): URI {
    return uris.random()
  }

  fun reset() {
    secret = ""
    bundle = ""
    remainderWallet = null
  }

  fun deinitialize() {
    reset()
  }

  fun uri(): String {
    return client.uri.toASCIIString()
  }

  fun client(): HttpClient {
    if (!authProcess) {
      val randomUri = getRandomUri()
      client.setUri(randomUri)
      val authDataObj = clients[randomUri.toASCIIString()]
      authDataObj?.let {
        client.setAuthData(it)
      } ?: requestAuthToken(secret = secret(), cellSlug = cellSlug(), encrypt = client.encrypt)
    }

    return client
  }

  fun hasSecret() : Boolean {
    return secret.isNotEmpty()
  }

  fun secret(value: String) {
    secret = value
    bundle = Crypto.generateBundleHash(value)
  }

  @Throws(UnauthenticatedException::class)
  fun secret(): String {
    if (secret.isEmpty()) {
      throw UnauthenticatedException("KnishIOClient::secret() - Unable to find a stored secret!")
    }
    return secret
  }

  @Throws(UnauthenticatedException::class)
  fun bundle(): String {
    if (bundle.isEmpty()) {
      throw UnauthenticatedException("KnishIOClient::bundle() - Unable to find a stored bundle!")
    }

    return bundle
  }

  fun remainderWallet(): Wallet? {
    return remainderWallet
  }

  fun <T : KClass<*>> createQuery(queryClass: T): IQuery {
    return queryClass.primaryConstructor?.call(client()) as? IQuery  ?: throw CodeException("invalid Query")
  }

  @JvmOverloads
  fun requestAuthToken(
    secret: String? = null,
    seed: String? = null,
    cellSlug: String? = null,
    encrypt: Boolean? = null
  ): IResponseRequestAuthorization {
    authProcess = true
    val guestMode = (seed == null) && (seed == secret)
    val enc = encrypt ?: hasEncryption()
    val response: IResponseRequestAuthorization
    val query: Query

    seed?.let {
      secret(Crypto.generateSecret(it))
    } ?: secret?.let {
      secret(it)
    }

    cellSlug?.let {
      this.cellSlug = it
    }

    if (guestMode) {
      val authorizationWallet = Wallet(Crypto.generateSecret(), "AUTH")
      query = createQuery(MutationRequestAuthorizationGuest::class) as MutationRequestAuthorizationGuest
      query.setAuthorizationWallet(authorizationWallet)
      response = query.execute(AccessTokenMutationVariable(this.cellSlug, authorizationWallet.pubkey, enc)) as ResponseRequestAuthorizationGuest
    }
    else{
      val molecule = createMolecule(secret(), Wallet(secret(), "AUTH"))
      query = createMoleculeMutation(MutationRequestAuthorization::class, molecule) as MutationRequestAuthorization
      query.fillMolecule(listOf(MetaData("encrypt", if (enc) "true" else "false")))
      response = query.execute(MoleculeMutationVariable(query.molecule()!!)) as ResponseRequestAuthorization
    }

    if (response.success()) {
      val authObj = Clients(
        response.token(),
        response.pubKey(),
        response.wallet()
      )

      if (hasEncryption() != response.encrypt()) {
        if(response.encrypt()) enableEncryption() else disableEncryption()
      }

      client.setAuthData(authObj)
      clients[uri()] = authObj
      authProcess = false
    }
    else {
      throw UnauthenticatedException(response.reason() ?: "Authorization token missing or invalid.")
    }

    return response
  }

  @JvmOverloads
  fun <T : KClass<*>> createMoleculeMutation(
    mutationClass: T,
    molecule: Molecule? = null
  ): MutationProposeMolecule {
    val _molecule = molecule ?: createMolecule()
    val mutation = mutationClass.primaryConstructor?.call(client(), _molecule) ?: throw CodeException("invalid Mutation")

    if (mutation !is MutationProposeMolecule) {
      throw CodeException("${mutationClass.simpleName}::createMoleculeMutation() - This method only accepts MutationProposeMolecule!")
    }

    lastMoleculeQuery = mutation

    return mutation
  }

  @JvmOverloads
  fun createMolecule(
    secret: String? = null,
    sourceWallet: Wallet? = null,
    remainderWallet: Wallet? = null
  ): Molecule {
    val _secret = secret ?: secret()
    var _sourceWallet = sourceWallet

    if (
      sourceWallet == null &&
      remainderWallet()?.token != "AUTH" &&
      lastMoleculeQuery != null &&
      lastMoleculeQuery !!.response != null &&
      lastMoleculeQuery !!.response!! .success()
    ) {
      _sourceWallet = remainderWallet()
    }

    if (_sourceWallet == null) {
      _sourceWallet = sourceWallet()
    }

    this.remainderWallet = remainderWallet ?: Wallet.create(
      _secret,
      _sourceWallet.token,
      _sourceWallet.batchId,
      _sourceWallet.characters
    )

    return Molecule(
      _secret,
      _sourceWallet,
      remainderWallet(),
      cellSlug
    )
  }

  fun sourceWallet(): Wallet {
    return queryContinuId(bundle()).payload() ?: Wallet(secret())
  }

  fun queryContinuId(bundle: String): ResponseContinuId {
    val query = createQuery(QueryContinuId::class) as QueryContinuId
    return query.execute(ContinuIdVariable(bundle)) as ResponseContinuId
  }

  @JvmOverloads
  fun queryBalance(token: String, bundle: String? = null): ResponseBalance {
    val query = createQuery(QueryBalance::class) as QueryBalance
    return query.execute(BalanceVariable(token = token, bundleHash = bundle)) as ResponseBalance
  }

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

  fun queryBatch(batchId: String): ResponseBatch {
    val query = createQuery(QueryBatch::class) as QueryBatch
    return query.execute(BatchVariable(batchId)) as ResponseBatch
  }

  fun queryBatchHistory(batchId: String): ResponseBatchHistory {
    val query = createQuery(QueryBatchHistory::class) as QueryBatchHistory
    return query.execute(BatchHistoryVariable(batchId)) as ResponseBatchHistory
  }

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
        bundleHash,
        metaType,
        metaId,
        ipAddress,
        browser,
        osCpu,
        resolution,
        timeZone,
        countBy,
        interval
      )
    ) as ResponseUserActivity
  }

  @JvmOverloads
  fun queryWallets(
    bundle: String? = null,
    token: String? = null,
    unspent: Boolean? = null
  ): List<Wallet> {
    val walletQuery = createQuery(QueryWalletList::class) as QueryWalletList

    val response  = walletQuery.execute(
      WalletListVariable(bundleHash = bundle, token = token, unspent = unspent)
    ) as ResponseWalletList

    return response.getWallets()
  }

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
        bundleHash = bundle ?: bundle(),
        key = key,
        value = value,
        latest = latest
      )
    ) as ResponseWalletBundle
  }

  @JvmOverloads
  fun queryBundle(
    bundle: String? = null,
    key: String? = null,
    value: String? = null,
    latest: Boolean = true
  ): Map<String, WalletBundle> {
    return queryBundleRaw(bundle, key, value, latest).payload()
  }

  fun createWallet(token: String): ResponseProposeMolecule {
    val newWallet = Wallet(secret(), token)
    val query = createMoleculeMutation(MutationCreateWallet::class) as MutationCreateWallet

    query.fillMolecule(newWallet)

    return query.execute(MoleculeMutationVariable(query.molecule()!!)) as ResponseProposeMolecule
  }

  @JvmOverloads
  fun createToken(
    token: String,
    amount: Number? = null,
    meta: MutableList<MetaData> = mutableListOf(),
    batchId: String? = null,
    units: MutableList<TokenUnit> = mutableListOf()
  ): ResponseProposeMolecule {
    val _batchId = batchId ?: Crypto.generateBatchId()
    var _amount = amount ?: 0

    meta.firstOrNull { it.key == "fungibility" }?.let { _ ->

      meta.firstOrNull { it.key == "batchId" }?.let {
        it.value = _batchId
      } ?: meta.add(MetaData("batchId", _batchId))


      if (units.isNotEmpty()) {

        meta.firstOrNull { it.key == "decimals" }?.let {
          if (it.value !!.toDouble() > 0) {
            throw StackableUnitDecimalsException()
          }
        }

        amount?.let {
          if (it.toDouble() > 0) {
            throw StackableUnitAmountException()
          }
        }

        _amount = units.size
        meta.firstOrNull { it.key == "splittable" }?.let{
         it.value = "1"
        } ?: meta.add(MetaData("splittable", "1"))

        meta.firstOrNull { it.key == "tokenUnits" }?.let {
          it.value =  jsonFormat.encodeToString(units)
        } ?: meta.add(MetaData("tokenUnits", jsonFormat.encodeToString(units)))
      }
    }

    val recipientWallet = Wallet(secret(), token, _batchId)
    val query = createMoleculeMutation(MutationCreateToken::class) as MutationCreateToken

    query.fillMolecule(recipientWallet, _amount, meta)

    return query.execute(MoleculeMutationVariable(query.molecule()!!)) as ResponseProposeMolecule
  }

  @JvmOverloads
  fun createMeta(
    metaType: String,
    metaId: String,
    meta: MutableList<MetaData> = mutableListOf()
  ): ResponseProposeMolecule {
    val query = createMoleculeMutation(
      MutationCreateMeta::class,
      createMolecule(secret(), sourceWallet())
    ) as MutationCreateMeta

    query.fillMolecule(metaType, metaId, meta)

    return query.execute(MoleculeMutationVariable(query.molecule()!!)) as ResponseProposeMolecule
  }

  fun createIdentifier(
    type: String,
    contact: String,
    code: String
  ): ResponseProposeMolecule {
    val query = createMoleculeMutation(MutationCreateIdentifier::class) as MutationCreateIdentifier

    query.fillMolecule(type, contact, code)

    return query.execute(MoleculeMutationVariable(query.molecule()!!)) as ResponseProposeMolecule
  }

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
        it.value =  to.position
      } ?: meta.add(MetaData("position", to.position))

      meta.firstOrNull { it.key == "bundle" }?.let {
        it.value =  to.bundle
      } ?: meta.add(MetaData("position", to.bundle))
    }

    return requestTokensQuery(token, amount, metaType, metaId, units, meta, batchId)
  }


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
    var _amount = amount ?: 0

    // Calculate amount & set meta key
    if (units.isNotEmpty()) {
      // Can't move stackable units AND provide amount
      if ( _amount.toDouble() > 0 ) {
        throw StackableUnitAmountException()
      }

      // Calculating amount based on Unit IDs
      _amount = units.size
      meta.firstOrNull { it.key == "tokenUnits" }?.let {
        it.value =  jsonFormat.encodeToString(units)
      } ?: meta.add(MetaData("tokenUnits", jsonFormat.encodeToString(units)))
    }

    val query = createMoleculeMutation(MutationRequestTokens::class) as MutationRequestTokens

    query.fillMolecule(token, _amount, metaType, metaId, meta, batchId)

    return query.execute(MoleculeMutationVariable(query.molecule()!!)) as ResponseProposeMolecule
  }

  private fun <T>getSpecificationRequestTokens(sender: T, token: String): Map<String, String?> {
    return when(sender) {
      // If recipient is a Wallet, we need to help the node triangulate
      // the transfer by providing position and bundle hash
      is Wallet -> mapOf("metaType" to "wallet", "metaId" to sender.address)
      // If the recipient is provided as an object, try to figure out the actual recipient
      is String -> run {
        if (Wallet.isBundleHash(sender)) {
          mapOf("metaType" to "walletBundle", "metaId" to sender)
        }
        else{
          val wallet = Wallet.create(sender, token)
          mapOf("metaType" to "wallet", "metaId" to wallet.address)
        }
      }
      // No recipient, so request tokens for ourselves
      else -> mapOf("metaType" to "walletBundle", "metaId" to bundle())
    }
  }

  @JvmOverloads
  fun claimShadowWallet(
    token: String,
    batchId: String? = null,
    molecule: Molecule? = null
  ): ResponseProposeMolecule {
    val query = createMoleculeMutation(MutationClaimShadowWallet::class, molecule) as MutationClaimShadowWallet

    query.fillMolecule(token, batchId)

    return query.execute(MoleculeMutationVariable(query.molecule()!!)) as ResponseProposeMolecule
  }

  @JvmOverloads
  fun transferToken(
    recipient: Wallet,
    token: String,
    amount: Number,
    units: MutableList<TokenUnit> = mutableListOf(),
    batchId: String? = null,
    sourceWallet: Wallet? = null
  ): ResponseProposeMolecule {
    val _sourceWallet = sourceWallet ?: queryBalance(token).payload()
    var _amount = amount

    if (units.isNotEmpty()) {
      if (_amount.toDouble() > 0) {
        throw StackableUnitAmountException()
      }

      _amount = units.size
    }

    if ( _sourceWallet == null || _sourceWallet.balance < _amount.toDouble()) {
      throw TransferBalanceException()
    }

    batchId?.let {
      recipient.batchId = batchId
    } ?: recipient.initBatchId(_sourceWallet)

    remainderWallet = Wallet.create(
      secret(),
      token,
      characters = _sourceWallet.characters
    )

    remainderWallet !!.initBatchId(_sourceWallet, true)
    _sourceWallet.splitUnits(units, remainderWallet !!, recipient)

    val molecule = createMolecule(sourceWallet = _sourceWallet, remainderWallet = remainderWallet)
    val query = createMoleculeMutation(MutationTransferTokens::class, molecule) as MutationTransferTokens

    query.fillMolecule(recipient, _amount)

    return query.execute(MoleculeMutationVariable(query.molecule()!!)) as ResponseProposeMolecule
  }

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

  @JvmOverloads
  fun burnTokens(
    token: String,
    amount: Number = 0,
    units: MutableList<TokenUnit> = mutableListOf(),
    sourceWallet: Wallet? = null
  ): ResponseProposeMolecule {
    val _sourceWallet = sourceWallet ?: queryBalance(token).payload()
    val remainderWallet = Wallet.create(secret(), token, characters = sourceWallet !!.characters)
    var _amount = amount

    remainderWallet.initBatchId(_sourceWallet !!, true)

    if (units.isNotEmpty()) {
      if (_amount.toDouble() > 0) {
        throw StackableUnitAmountException()
      }

      _amount = units.size

      sourceWallet.splitUnits(units, remainderWallet)
    }

    val molecule = createMolecule(null, sourceWallet, remainderWallet)

    molecule.burnToken(_amount)
    molecule.sign()
    molecule.check()

    return MutationProposeMolecule(client(), molecule).execute(MoleculeMutationVariable(molecule)) as ResponseProposeMolecule
  }

  fun cellSlug(): String? {
    return cellSlug
  }
}
