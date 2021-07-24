import com.google.gson.Gson
import wishKnish.knishIO.client.Wallet
import wishKnish.knishIO.client.data.MetaData

import wishKnish.knishIO.client.Molecule

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.features.logging.*
import io.ktor.http.*
import io.ktor.network.tls.*
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import wishKnish.knishIO.client.data.json.MoleculeMutationQuery
import wishKnish.knishIO.client.data.json.ProposeMoleculeData
import wishKnish.knishIO.client.data.json.ResponseData
import wishKnish.knishIO.client.libraries.Crypto
import kotlinx.serialization.json.Json as KotlinJson
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager
import java.security.SecureRandom

const val endpoint = "http://knishnode.knishio/graphql"
const val secret =
  "c5da568b4656b5305676cc0ee26c24460ed0c27df013f86717db2f19d9205a35c8e01daeb22bc9f4ded2db7e6ced0dc9c0f5330cc22e4b35f8dcccb7a1b8043468c2aa60762c5a90e272b7a699eb42be7cdb97f522d0595cf1c92b81a9b0c377cfd1c8bd3ed0ff742cab311d24e6a7f2315552f78c6800fd21b24aa3092fd03d570f047948b9181fcf2a3eca919188f9d8c3b65d631379f8e7b02c32deb4f5916faffbc022ceb45c4d9239e548d01d331fb7bf437e679ed679619e2c46161984a21d3ecc63be284147d7f104555f7e1f220ec92a8ef1f6935c1b29ddde51f7b4f1c45c2f51cca084d1e7ae616828543ad1b154df9fb198570af24e351126c0cdd6dc4e5cbefaec82a58016e078a00fa0cd598000b5bb8760cf5120d69b5b2a5f16a7ead6416367080076e85769f56c11a056de2c0c3243bdc4f507c92bcba4f26ff041eafc1999fc05dca8b308f918b00ee0cae74c1f0f539809ca750a21785ee21a1c24bcef2a9727a8b98fe47b5f5010fafe061365331731c69c22c06846167e8cc79bf8613902ea136c96ac775d9f90f99bba404bd4af83bbf3e20d048ccf7304f5e55edbc5d51da7f6148d3e4bb77e3b3f202d4b5c73e69039c9b9ff02ea6d3297512841b73a02c83f6dce061c7e9eb9da79100bad80738a3d2d4a8d0a8fcd9bc9cf4ef432b0e1d74ec8ea3d4a9882a7f6ea159f060b05c9038552a2764d965e4f06f629d62b0b39669839879372c64471697867d9235843affc9bf118744b18de8f021d804bda26c578232a82d91bec493c08202c96d863ebe6a123d452b95e4eb9bd9c23e49293d143fddf011d6d4e36d43d1ba77fc22a9f39dc94d9c1f1e44284df4fca8c2d6e7e2e7e9551256bde7022dc9da4d4b41b8e8a2cf3840558af13be976c06008b1165c03074d2cc7ab3d1604665fa0e8320a81b6d7090afc27c311941ff61bc64c8d90945e99bd64389568cd84733aa6ac085ec15ec82dd746cd791a21c9e38a74b1d6f5e6eca4f0edf6f7c7789895b6a6705310d35fba6d31efa96f7b9d7583f5be3e9457d0e6b8d908423565b4f89798a73b74e5de4cc51b8d6af1e1754f512141cdca3969fa009959ebb96244f3c3e3c774c754e6cef1cca6e992b2e140227fb6dec536fdf9e84e40ba8ce59a723c1f08a01039fc8726705a59d2f4ec5a07c61ef8232b0b19647b19bb4992c17db1f57904dc64ea73a45f892f6d50821df7f40aa629f0659a9fa78c9cad282758c4a8c6cfd8b0b5d91134249b8c0072c10216171a82875454ab75b096b420ac25c6bd1cac93e9b1ade5da37dbcf0f939314ffa102cfda5ae3e98a4db0999338b4df39887e973ac7d8d1ca5b772728267b372a518587854b1a01387509717c6f6ba17b5dc02019ff24edb3759ec84e22eb615f5510614acbda0bca1164390cc0a061b87c3437d3ee0f0"
const val secret2 =
  "509d8d7eb52af57c17c01c6882b1599b2d4c9eed5fe99e33300c6c54a2b22cfe1c3bba65a63bca1a53436fadc59e19a65f75b8be4e0d71ea523691f00a9ea0df5a4b1d25d018b1a35b2ba9e29b897ce42db441902b56b7dbce5e3288c70e4bd3da3d0db97182bce4e9f2e5edde58a475cf5c5acb07c97efd5fcd876694e969d456a910b36b109915df47186b00d1ac3a8fd3bdd5a670947dc7c4cd9d575c960f4a60991c4c65fae9670a3f5f4e45339ea7380357161b44cb898c93b9b5ca4fdeebe10667cec1d299a104fe6429838e921cd1504db935caccb4ac65208b89527bfa2020844b55f8b1a53e5913fa9c03ab18605d18d8ab48e9283595c6a7ea727410cbf8b06dd988998c2634ef49f22d792a3aedf6bd55b1bf593f02d13c61a88799d610c6a10b498ed1c8843b67faa5d199de44a0a70e55ff3003c302f5fe3d043ddd54b0c09e4fbcc51eba2b76c2ada99b1eea3e58142505631c9a987c93689ab3421f2d2e6f4085870aee3efd4284e5459d654841ee7ca6002119d61386655ecae1852ed66cfac7259560db472cbeffc472f5e75f7756e6b6915072152cc81ebb1cc4ef57cbf245666bdf14f72196146e7301cd4ddefb9e759b7d25dbd0b08260710368c26d68c0774139284071c6e829d417f0b7c02a87a38d980ecfabfe10791239aef1567d13bd11309245ab1b851a7734c751fdc9448d9da95262ee246ffc09bf5559aaa47947ab0d25c0213c2646a1cc0fc3faebbc461d51eafcb0782ae611002c7c86511fbde3c6017328a9753c3066172ed2d49c6465c157384be279e65a180b242ecd1e3b8dc37e005fd3df90e62943d36732a1996739b770f6f7c614d46b17e8924d245640cdb59b8033e0c468ec68a966373f222c1b298a67819268fd9c9f59331c8bf788e8d63db1706ce9cf1d4c4b335c59a21893b9866fd70c54bc180fa9828053499505b3048e9aaee619cc605adfe329ded4fd0c08b17e1b2d840b446390c75186fd9e4b6b03b345b91ee7d3b0b855c1bc9a6223d1d38f98fa96c37120674082fba8aaa17766fdaf50e89a9b3d9dfc26ac9e420b46a3bc523a5634f60e28de683cad562d2aa4edbd2d9c3e755ce776fe85a80a6ff3943beec445a02a95c69f44aeca4e401a0eb5f08915812c08831e535383934eef9101feedd28e9b4f02b08291c33e279a783cc2a5eaed92368d02a9da929433ae43edb64383b36a938c360e85f0339c3410342d2eb901e65498e1e89782561a4f961cae3d2d3063a48f2dfd08c27ec5ff4ed103c4c77ae7803ad386a477d2b5e0eef1b51db3909f95ccb921669053a2cb51f6233f3b6933db50f81e747a2e3045b93b8c5cdf5f3b17110c8a47a0f9f9d0558096833717fb7dcfcaefd0406982e51cdc1aaf91d9c149a29fbc2f7e9a0c682d42e4be211b1eb0a82d6ce1117ade59c3890f"
const val secret3 =
  "4c562299fc74f0efa958cb3cdfea4cf40a7f7b32adf9c8c14e7705f0a54a33a5bc71c9bc33c92d5b731e9d4cf1620899de3069cf81897cdcf572a98c562adb8ccb55b4d3c1e991617ecb501f56e651464cda1e3741b0ad9aa48108b958f46d1953015a5462cdf9ef9a11ecada441155739e511a5945b26d3687b88f3aef6a26f1de3171dc9d3b93784f57058f797be504bdcf69ee8f75dacb20f41a15328d9f3eb6c87adc289d3ffe1fd3bb6b47b37a7bbbca02f566b458f64ba14c92a64d6ef03fd8c15416d6bcd6068d54aa7737cedaa4979ed6a0b164e33bdcf71fd37bcaf0eb5aca4caa76565d91c10c7270083c215e7659f52ac2257522ed959a2941bb9eda2de912bb321145daffeda23a9472a809a2616fb6b2d58f2782d8b1575770b2f28920e8a39bcaec6e1e1d24500b441ae39be8585ec4f7f65d0015739eb63b84ed147e625265c68bfb66e45609010b1fd924d334c81f74d5384c4ca1621324b7f19bac494df248a635bae493d287018da5c9a1a364f1f406ed8baa78426cc42ea92c45a331b2a2ede6835098948251561d209aeef3d92335b0d020cc4837bd98ca977007434240f69823c856b0b1452cb6cf9d5714e740d527ca29895a45a4786f817e4363fc466cb0bf5c0a9e2f9ce4763ed16fed3a260b0aea861c061a78bb97c73baf5335a5b90e817ae9345564318fab0ab38718a1d55170fd0ff73487e06f468fd73f99b89c6c06c6285ae2af34a48116878fb46d469750184d30ad800842c6809761f04cca23f9bad12b60e37e3b58ca8d8f443a51fc49548906d09ebaf016888516be9c5fe68a4e62b8996796f624ffccbbd7fcfdd8aac09dda216b4a2c06c67ee07e19ce722dc3daa566cc5fd4b24293f02552e9881e576a694915aa9e0e1a4b0d3ef90ba873fe63b1151c4e9cdfffd7d93196349c08dab2cd615ea93b051e35e7967e7b119a903ed70a25c49ede190d1cfc6f2b4bcfc8fbd4f6b89a8f09a294bbb7abf45f039bb1dca5e0ebe59bb56afbf432f18d336a54e99c552aa2717067364f9d7e43f7568b9bade7d6685ba69d06c18260316d870ceb9514cb574f4d68fda7f5bef8e56c4de11aaf35b9e6647e330c9cb56282f4da1bcbcebcc1023bd9805322b365bc5da1741778e9d4f7f2d28f0c4557e99d25c9297079852c80a2994fe1753f7995163dd7de0d370fc0c3449f7bb2a57eac0c41dacfee157fe5980d11da756c66ac703abc50468d24cbd88f34b1fd4704de8700b3df934bac38c3d1f3dbb9a69fe2848316f301d3dd1639caf146c05f9d2fb86a414b4eb69aa3ab816b05f248f001846c52114373da5dfe3e416c654cc328b3c091e3f6228142bbed32edd883618fce30d6f3be47a41b54409ffa92fa8c0c2cc2805d272053cf95161a1d63c9e1d7e9573b9aa0c2b535fde8e502d6b372f99a4344256e9"
const val cellSlug = "TESTCELL"
const val tokenSlug = "TOKENTEST3"
const val tokenAmount = 10000
var authToken: String? = null

suspend fun main(args: Array<String>) {

  print("#### AUTHORIZING ####\r\n")
  authToken = authorizationToken()
  print("main() - Authorization Token: [$authToken]\r\n")

  // print("\r\n#### CREATING META ####\r\n")
  // createMeta()

  // print("\r\n#### CREATING TOKEN ####\r\n")
  // createToken()

  print("\r\n#### TRANSFERRING TOKEN ####\r\n")
  transferTokens()

}

suspend fun transferTokens(): Boolean {

  // Designating sender wallet
  val sourceWallet = balanceQuery(
    Crypto.generateBundleHash(secret), tokenSlug
  ) ?: throw IllegalArgumentException("You do not have a token $tokenSlug balance")

  // Designating recipient wallet
  val recipientWallet = Wallet(secret2, tokenSlug);

  // Creating molecule
  val molecule = Molecule(secret, sourceWallet, null, cellSlug)
  molecule.initValue(recipientWallet, 10)

  // Signing molecule
  molecule.sign()
  print("transferTokens() - Signed molecule:\r\n$molecule\r\n");

  // Getting broadcast response
  val response = moleculeMutation(molecule, sourceWallet);
  val responseMolecule = extractMolecule(response)

  // Verifying status
  if (responseMolecule.status.lowercase() != "accepted") {
    print("transferTokens() - Error response:\r\n$responseMolecule\r\n");
    throw IllegalArgumentException(responseMolecule.reason)
  }

  return true;
}

suspend fun createToken(): Boolean {

  // Token metadata
  val meta = mutableListOf(
    MetaData("name", "$tokenSlug token"),
    MetaData("fungibility", "fungible"),
    MetaData("supply", "replenishable"),
    MetaData("decimals", "0")
  )

  // Defining signing wallet
  val sourceWallet = Wallet(secret)

  // Creating wallet to receive tokens
  val recipientWallet = Wallet(secret, tokenSlug)

  // Creating molecule mutation
  val molecule = Molecule(secret, sourceWallet, null, cellSlug)
  molecule.initTokenCreation(recipientWallet, tokenAmount, meta)

  // Signing molecule
  molecule.sign()
  print("createToken() - Signed molecule:\r\n$molecule\r\n");

  // Getting broadcast response
  val response = moleculeMutation(molecule);
  val responseMolecule = extractMolecule(response)

  // Verifying status
  if (responseMolecule.status.lowercase() != "accepted") {
    print("createToken() - Error response:\r\n$responseMolecule\r\n");
    throw IllegalArgumentException(responseMolecule.reason)
  }

  return true
}

suspend fun createMeta(): Boolean {

  // Defining meta asset parameters
  val metaType = "artifact"
  val metaId = "1"
  val meta = mutableListOf(
    MetaData("logo", "data:image/jpeg;base64,*")
  )

  // Defining signing wallet
  val sourceWallet = Wallet(secret);

  // Creating molecule mutation
  val molecule = Molecule(secret, sourceWallet, null, cellSlug)
  molecule.initMeta(meta, metaType, metaId)

  // Signing molecule
  molecule.sign()
  print("createMeta() - Signed molecule:\r\n$molecule\r\n");

  // Getting broadcast response
  val response = moleculeMutation(molecule);
  val responseMolecule = extractMolecule(response)

  // Verifying status
  if (responseMolecule.status.lowercase() != "accepted") {
    throw IllegalArgumentException(responseMolecule.reason)
  }

  return true
}

suspend fun authorizationToken(): String {

  // Defining authorization parameters
  val meta = mutableListOf(
    MetaData("encrypt", "false")
  )

  // Creating wallet for authorization
  val authWallet = Wallet(secret, "AUTH")

  // Creating molecule mutation
  val molecule = Molecule(secret, authWallet, null, cellSlug)
  molecule.initAuthorization(meta)

  // Signing molecule
  molecule.sign()
  print("authorizationToken() - Signed molecule:\r\n$molecule\r\n");

  // Getting broadcast response
  val response = moleculeMutation(molecule)
  val responseMolecule = extractMolecule(response)

  // Verifying status
  if (responseMolecule.status.lowercase() == "accepted") {

    // proposeMolecule.payload = {
    //   token="8d2c5c44-9700-48b1-8d35-3ccf956a51ab",
    //   time=172800000, expiresAt=1627136089,
    //   key="DmiNUTlYY0YKnUS0i8FhG4BZkYm5ZFsOSktBuUU2oINI",
    //   encrypt=false
    // }
    return Gson().fromJson(
      responseMolecule.payload, Map::class.java
    )["token"]?.toString() ?: throw IllegalArgumentException("Invalid response format")
  }

  throw IllegalArgumentException("An error occurred during authorization")
}

fun extractMolecule(response: String): ProposeMoleculeData {
  return ResponseData.jsonToObject(response).data?.ProposeMolecule ?: throw IllegalArgumentException("Invalid response format")
}

suspend fun balanceQuery(
  bundleHash: String,
  token: String
): Wallet? {
  val query = """
    query( ${'$'}address: String, ${'$'}bundleHash: String, ${'$'}token: String, ${'$'}position: String ) {
      Balance( address: ${'$'}address, bundleHash: ${'$'}bundleHash, token: ${'$'}token, position: ${'$'}position ) {
        address,
        bundleHash,
        tokenSlug,
        batchId,
        position,
        amount,
        characters,
        pubkey,
        createdAt,
        tokenUnits {
          id,
          name,
          metas
        }
      }
    }
    """.trimIndent()

  // Defining query parameters
  val variables = mapOf(
    "bundleHash" to bundleHash, "token" to token
  )

  // Getting query response
  val responseJson = graphqlQuery(query, variables);
  print("balanceQuery() - JSON response:\r\n$responseJson\r\n");

  // Converting to GSON
  val responseGson = Gson().fromJson(responseJson, Map::class.java)

  // Mapping to wallet objects
  val responseMapped = responseGson["data"]?.let { item ->
    (item as Map<*, *>)["Balance"]?.let {
      val walletMap = it as Map<*, *>

      val bundleHash = walletMap["bundleHash"] as String
      val tokenSlug = walletMap["tokenSlug"] as String
      val batchId = walletMap["batchId"] as String?
      val characters = walletMap["characters"] as String?
      val address = walletMap["address"] as String?
      val position = walletMap["position"] as String?
      val amount = (walletMap["amount"] as String).toDouble()
      val pubkey = walletMap["pubkey"] as String?

      val wallet = Wallet.create(bundleHash, tokenSlug, batchId, characters)
      wallet.address = address
      wallet.position = position
      wallet.balance = amount
      wallet.pubkey = pubkey

      wallet
    }
  }
  print("balanceQuery() - Mapped response: \r\n$responseMapped\r\n");

  return responseMapped
}

suspend fun graphqlQuery(
  query: String,
  variables: Map<String, String>
): String {

  val client = getClient()
  val response: HttpResponse = client.post(endpoint) {
    contentType(ContentType.Application.Json)
    body = GraphqlQueryData(query, variables)
  }
  val content = response.readText()
  client.close()
  return content

}

suspend fun moleculeMutation(
  molecule: Molecule,
  wallet: Wallet? = null
): String {

  if (molecule.check(wallet)) {

    val client = getClient()
    val response: HttpResponse = client.post(endpoint) {
      contentType(ContentType.Application.Json)
      body = MoleculeMutationQuery(molecule)
    }
    val content = response.readText()

    client.close()

    return content
  }

  throw IllegalArgumentException("Molecule check error")
}

fun getClient(): HttpClient {

  // Certificate Authentication Stub
  class TrustAllX509TrustManager : X509TrustManager {
    override fun getAcceptedIssuers(): Array<X509Certificate?> = arrayOfNulls(0)
    override fun checkClientTrusted(
      certs: Array<X509Certificate?>?,
      authType: String?
    ) {
    }

    override fun checkServerTrusted(
      certs: Array<X509Certificate?>?,
      authType: String?
    ) {
    }
  }

  val jsonFormat = KotlinJson {
    encodeDefaults = true
    ignoreUnknownKeys = true
  }

  return HttpClient(CIO) {
    engine {
      headersOf("X-Auth-Token", authToken ?: "")
      maxConnectionsCount = 1000
      endpoint {
        maxConnectionsPerRoute = 100
        pipelineMaxSize = 20
        keepAliveTime = 5000
        connectTimeout = 5000
        connectAttempts = 5
      }
      https {
        // Certificate Authentication Stub
        https {
          serverName = "lumen.loc"
          cipherSuites = CIOCipherSuites.SupportedSuites
          trustManager = TrustAllX509TrustManager()
          random = SecureRandom()
        }
      }
    }
    install(JsonFeature) {
      serializer = KotlinxSerializer(jsonFormat)
    }
    install(Logging) {
      logger = Logger.DEFAULT
      level = LogLevel.NONE
    }
  }
}

@Serializable data class GraphqlQueryData(
  @JvmField var query: String,
  @JvmField var variables: Map<String, String>
) {
  companion object {
    private val jsonFormat: kotlinx.serialization.json.Json
      get() = kotlinx.serialization.json.Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        coerceInputValues = true
      }

    @JvmStatic
    fun create(
      query: String,
      variables: Map<String, String>
    ): GraphqlQueryData {
      return GraphqlQueryData(query, variables)
    }

    @JvmStatic
    fun jsonToObject(json: String): GraphqlQueryData {
      return jsonFormat.decodeFromString(json)
    }
  }

  private fun toJson(): String {
    return jsonFormat.encodeToString(this)
  }

  override fun toString(): String {
    return toJson()
  }
}
