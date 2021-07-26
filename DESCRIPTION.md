Для передачи молекул на сервер мы будим использовать клиентскую библиотеку
[ktor]([https:/](https://ktor.io/docs/getting-started-ktor-client.html)).
Вы можете использовать абсолютно любую удобную для вас библиотек, но надо учитывать,
что тело запроса должно иметь следующий формат json строки `{"query": String, "variables": Map}`. 

Пример инициализации http клиента:
```kotlin
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
                    serverName = "example.loc"
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
```
Класс `TrustAllX509TrustManager` служит заглушкой проверки сертификатов
сервера (не рекомендуется использовать в рабочем приложении).
Для преобразования объектов молекулы и атома в json мы используем библиотеку
kotlinx-serialization-json поэтому настраиваем сериализатор json http клиента
классом `KotlinxSerializer`.

Пример метода отправки запроса с молекулой:
```kotlin
suspend fun moleculeMutation(
    molecule: Molecule,
    wallet: Wallet? = null
): String {

    if (molecule.check(wallet)) {

        val client = getClient()
        val response: HttpResponse = client.post(endpoint) {
            contentType(ContentType.Application.Json)
            body = MoleculeMutation(molecule)
        }
        val content = response.readText()

        client.close()

        return content
    }

    throw IllegalArgumentException("Molecule check error")
}
```
Прежде чем отправить молекулу ее нужно проверить на соответствия правил.
Для этого у экземпляра молекулы вызывается метод `check(sourceWallet: Wallet? = null): Boolean`.
В случаи ошибки метод возбудить исключение. `sourceWallet` необходим только при переводе токенов.
Перевод токенов осуществляется вызовам метода `initValue(recipientWallet: Wallet, amount: Number): Molecule`.

Вы можете увидеть выше, что мы передаем в body объект `MoleculeMutation`, он служит обверткой
для мутации молекулы который содержит graphql запрос, а так же, чтобы привести body к формату
`{"query": String, "variables": Map}`. `MoleculeMutation` импортируется из пространства имен 
`wishKnish.knishIO.client.data.json.molecule.mutation`

Пример дата класса, что бы привести тело запроса к формату `{"query": String, "variables": Map}`
```kotlin
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
```

Пример общего метода для запросов использующий `GraphqlQueryData` класс:
```kotlin
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
```
Для того чтобы что-то отправлять или запрашивать от сервера нужно получить токен
авторизации. Осуществляется это с помощью молекулы с атомом изотопа 'U'.
Что бы получить такую молекулу необходима вызвать у экземпляра метод 
`initAuthorization(meta: MutableList<MetaData>): Molecule`

Пример метода получения токена авторизации:
```kotlin
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
        //   time=172800000, 
        //   expiresAt=1627136089,
        //   key="DmiNUTlYY0YKnUS0i8FhG4BZkYm5ZFsOSktBuUU2oINI",
        //   encrypt=false
        // }

        print("authorizationToken() - Successful response:\r\n$responseMolecule\r\n");
        return Gson().fromJson(
            responseMolecule.payload, Map::class.java
        )["token"]?.toString() ?: throw IllegalArgumentException("Invalid response format")
    }

    throw IllegalArgumentException("An error occurred during authorization")
}
```
Чтобы создать экземпляр молекулы для получения токена авторизации мы инициализируем конструктор класса `Molecule` секретом,
который является HEX строкой из 2048 символов и `authWallet` кошельком, параметр token которого должен быть установлен 
значением "AUTH".

После этого вызываем у экземпляра метод `initAuthorization` передавая ему список с
метаданными в котором указываем серверу тип общения, с шифрованием или без.

Прежде чем отравлять молекулу на сервер одна должна быть подписана 
`sign(anonymous: Boolean = false, compressed: Boolean = true): String?`. Так же не забудьте проверить молекулу на 
соответствия правилам методом `check()`. Поле чего можно отправлять на сервер.

Ответ сервера на принятую молекулу имеет следующий вид:
```json
{
  "molecularHash": "0367eg429cbf2g2eb8231a0a674bd5a88bac98d92ff9fb91c457gb74a683g7a5",
  "height": 35,
  "depth": 0,
  "status": "accepted",
  "reason": "",
  "payload": "{\"token\":\"d678adb0-49b8-4a60-a973-0678bea693d4\",\"time\":172800000,\"expiresAt\":1627463377,\"key\":\"FGd6aB7QHrWtj06IALDNmTGKS1FIPm255m6Vree33uE6\",\"encrypt\":false}",
  "createdAt": "1627290571268",
  "receivedAt": "1627290575255",
  "processedAt": "1627290575625",
  "broadcastedAt": ""
}
```
Поле `status` может иметь значение _**accepted**_ в случаи принятия и выполнения молекулы или _**rejected**_, если молекула отклонена
сервером. В поле `reason` содержится сообщения от сервера или описание ошибки. Поле `payload` служит для возращения 
клиенту данных если они предусмотрены. Для токена авторизации это JSON строка с полями `token` в котором хранится сам
токен, `key` - открытый ключ шифрования сервера, `encrypt` хранит логическое значение работы с шифрованием трафика, `expiresAt`
метка времени истечения актуальности токена.

Для создания и хранения разнообразной информации или сущностей используется молекула с изотопами 'M'.
Пример создания и отправки молекулы с метаинформацией:
```kotlin
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

  print("createMeta() - Successful response:\r\n$responseMolecule\r\n");
  return true
}
```

Для создания токенов используется молекула изотопа 'C'.
Пример создания токена:
```kotlin
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

  print("createToken() - Successful response:\r\n$responseMolecule\r\n");
  return true
}
```
Перечисление имеющихся токенов осуществляется с использованием молекулы с изотопами 'V'.
Пример создания молекулы для перечисления токенов.
```kotlin
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

  print("transferTokens() - Successful response:\r\n$responseMolecule\r\n");
  return true;
}
```
Для того чтобы перечислить токены, мы должны знать наш балансовый кошелек который содержит остаток имеющихся у нас 
токенов. В примере выше мы его получаем методом `balanceQuery`.

Пример `balanceQuery`:
```kotlin
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
```
Для упрощения работы с ответом сервера для молекул в примерах использовался метод `extractMolecule`:
```kotlin
fun extractMolecule(response: String): ProposeMoleculeData {
  return ResponseMoleculeData.jsonToObject(response).data?.ProposeMolecule ?: throw IllegalArgumentException("Invalid response format")
}
```
`ResponseMoleculeData` можно импортировать из пространства имен `wishKnish.knishIO.client.data.json.molecule.response`
