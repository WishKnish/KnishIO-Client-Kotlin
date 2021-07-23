Для передачи молекул на сервер мы будим использовать клиентскую библиотеку
[ktor]([https:/](https://ktor.io/docs/getting-started-ktor-client.html)).
Вы можете использовать абсолютно любую удобную для вас библиотек, но надо учитывать,
что тело запроса должно иметь следующий формат json строки `{"query": String, "variables": Map}`. 

Пример инициализации http клиента:
```kotlin
fun getClient(token: String? = null): HttpClient {
    // Certificate Authentication Stub
    class TrustAllX509TrustManager : X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate?> = arrayOfNulls(0)
        override fun checkClientTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
        override fun checkServerTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
    }

    val jsonFormat = KotlinJson {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    return HttpClient(CIO) {
        engine {
            headersOf("X-Auth-Token", token ?: "")
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
            level = LogLevel.INFO
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
suspend fun moleculeQuery(molecule: Molecule, token: String? = null, wallet: Wallet? = null): String {

    if (molecule.check(wallet)) {

        val client = getClient(token)
        val response: HttpResponse = client.post("https://example.loc/graphql") {
            contentType(ContentType.Application.Json)
            body = MoleculeMutationQuery(molecule)
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

Вы можете увидеть выше, что мы передаем в body объект `MoleculeMutationQuery`, он служит обверткой
для мутации молекулы который содержит graphql запрос, а так же, чтобы привести body к формату
`{"query": String, "variables": Map}`. `MoleculeMutationQuery` импортируется из пространства имен 
`wishKnish.knishIO.client.data.json`

Пример дата класса, что бы привести тело запроса к формату `{"query": String, "variables": Map}`
```kotlin
@Serializable
data class GraphqlQueryData(@JvmField var query: String, @JvmField var variables: Map<String, String>) {
    companion object {
        private val jsonFormat: kotlinx.serialization.json.Json
            get() = kotlinx.serialization.json.Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
                coerceInputValues = true
            }

        @JvmStatic
        fun create(query: String, variables: Map<String, String>): GraphqlQueryData {
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
suspend fun graphqlQuery(query: String, variables: Map<String, String>, authToken: String? = null): String {

    val client = getClient(authToken)
    val response: HttpResponse = client.post("https://example.loc/graphql") {
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
Что бы получить такую молекулу нужно вызвать у экземпляра метод 
`initAuthorization(meta: MutableList<MetaData>): Molecule`

Пример метода получения токена авторизации:
```kotlin
suspend fun authorizationToken(): String {
    val molecule = Molecule(secret = secret, sourceWallet = Wallet(secret = secret, token = "AUTH"))

    molecule.initAuthorization(mutableListOf(MetaData(key = "encrypt", value = "false")))
    molecule.sign()

    val proposeMolecule = extractMolecule(moleculeQuery(molecule))

    if (proposeMolecule.status.lowercase() == "accepted") {

        // proposeMolecule.payload = {
        //   token="8d2c5c44-9700-48b1-8d35-3ccf956a51ab",
        //   time=172800000, 
        //   expiresAt=1627136089,
        //   key="DmiNUTlYY0YKnUS0i8FhG4BZkYm5ZFsOSktBuUU2oINI",
        //   encrypt=false
        // }

        return Gson().fromJson(proposeMolecule.payload, Map::class.java)["token"]?.toString()
            ?: throw IllegalArgumentException("Invalid response format")
    }

    throw IllegalArgumentException("An error occurred during authorization")
}
```
Чтобы создать экземпляр молекулы для получения токена авторизации мы инициализируем конструктор класса секретом,
который является HEX строкой из 2048 символов и `sourceWallet` кошельком, конструктор которого инициализируется секретом
и указанием параметра token "AUTH".

После этого вызываем у экземпляра метод `initAuthorization` передавая ему список с
метаданными в котором указываем будет ли для данного токена применятся шифрования.

Прежде чем отравлять молекулу на сервер одна должна быть подписана методом экземпляра 
`sign(anonymous: Boolean = false, compressed: Boolean = true): String?`.
