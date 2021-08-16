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
@file:JvmName("HttpClient")

package wishKnish.knishIO.client.httpClient

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.features.logging.*
import io.ktor.http.*
import io.ktor.network.tls.*
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.*
import kotlinx.coroutines.withContext
import wishKnish.knishIO.client.Wallet
import wishKnish.knishIO.client.data.Clients
import wishKnish.knishIO.client.data.json.query.QueryInterface
import wishKnish.knishIO.client.libraries.TrustAllX509TrustManager
import java.net.URI
import kotlinx.serialization.json.Json as KotlinJson
import java.security.SecureRandom


class HttpClient @JvmOverloads constructor(@JvmField var uri: URI, @JvmField var encrypt: Boolean = false) {

  @JvmField var authToken = ""
  @JvmField var pubkey: String? = null
  @JvmField var wallet: Wallet? = null
  @JvmField var client = creatClient()

  suspend fun query(request: QueryInterface): String {
    val response: HttpResponse  = client.post(uri.normalize().toASCIIString()) {
      headers {
        append("X-Auth-Token", authToken)
      }
      contentType(ContentType.Application.Json)
      body = request
    }

    return response.readText()
  }

  suspend fun mutate(request: QueryInterface): String {
    return query(request)
  }

  fun setAuthData(data: Clients) {
    pubkey = data.pubkey
    wallet = data.wallet
    authToken = data.token
  }

  fun setUri(uri: URI) {
    this.uri = uri
    client = creatClient()
  }

  private fun creatClient(): HttpClient {

    return HttpClient(CIO) {
      engine {
        endpoint {
          connectAttempts = 5
        }
        https {
          // Certificate Authentication Stub
          serverName = uri.host
          cipherSuites = CIOCipherSuites.SupportedSuites
          trustManager = TrustAllX509TrustManager()
          random = SecureRandom()
        }
      }
      install(JsonFeature) {
        serializer = KotlinxSerializer(
          KotlinJson {
            encodeDefaults = true
            ignoreUnknownKeys = true
            coerceInputValues = true
          }
        )
      }
      install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.NONE
      }
    }
  }
}
