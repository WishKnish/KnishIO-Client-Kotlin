
```kotlin=
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
            level = LogLevel.INFO
        }
    }
}
```
