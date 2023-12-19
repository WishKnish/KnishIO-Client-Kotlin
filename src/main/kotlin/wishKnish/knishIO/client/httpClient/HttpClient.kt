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

import graphql.language.Document
import graphql.language.Field
import graphql.language.OperationDefinition
import graphql.parser.InvalidSyntaxException
import graphql.parser.Parser
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.content.*
import io.ktor.http.*
import io.ktor.network.tls.*
import io.ktor.utils.io.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.future.future
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import io.ktor.serialization.kotlinx.cbor.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.*
import wishKnish.knishIO.client.Wallet
import wishKnish.knishIO.client.data.ClientTokenData
import wishKnish.knishIO.client.data.QueryData
import wishKnish.knishIO.client.data.json.query.CipherHash
import wishKnish.knishIO.client.data.json.response.query.cipherHash.CipherHash as RCipherHash
import wishKnish.knishIO.client.data.json.query.QueryInterface
import wishKnish.knishIO.client.data.json.variables.CipherHashVariable
import wishKnish.knishIO.client.exception.CodeException
import wishKnish.knishIO.client.libraries.TrustAllX509TrustManager
import java.net.URI
import java.security.GeneralSecurityException
import java.security.SecureRandom
import kotlin.jvm.Throws
import kotlinx.serialization.json.Json as KotlinJson


class HttpClient @JvmOverloads constructor(
  @JvmField var uri: URI,
  @JvmField var encrypt: Boolean = false
) {

  @JvmField var authToken = ""
  @JvmField var pubkey: String? = null
  @JvmField var wallet: Wallet? = null
  @OptIn(ExperimentalSerializationApi::class) val client: HttpClient
    get() = HttpClient(CIO) {
      expectSuccess = false
      engine {
        endpoint {
          connectAttempts = 5
          requestTimeout = 30000
        }
        https {
          // Certificate Authentication Stub
          serverName = uri.host
          cipherSuites = CIOCipherSuites.SupportedSuites
          trustManager = TrustAllX509TrustManager()
          random = SecureRandom()
        }
      }
      install(UserAgent) {
        agent = "KnishIO/0.1"
      }
      install(ContentNegotiation) {
        json (KotlinJson {
          encodeDefaults = true
          ignoreUnknownKeys = true
          coerceInputValues = true
        })
        cbor(Cbor {
          ignoreUnknownKeys = true
        })
      }
      install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.NONE
      }
    }

  @Throws(CodeException::class)
  fun wallet(): Wallet {
    return wallet ?: throw CodeException("Authorized wallet missing.")
  }

  @Throws(CodeException::class)
  fun pubkey(): String {
    return pubkey ?: throw CodeException("Server public key missing.")
  }

  @OptIn(DelicateCoroutinesApi::class)
  @Throws(
    TimeoutCancellationException::class,
    IllegalArgumentException::class,
    GeneralSecurityException::class,
    SerializationException::class
  )
  fun query(request: QueryInterface): String {
    val completable = GlobalScope.future {
      val response: HttpResponse = client.use {
        if (encrypt) {
          encrypt(it)
          decode(it)
        }

        it.post(uri.normalize().toASCIIString()) {
          headers {
            append("X-Auth-Token", authToken)
          }
          contentType(ContentType.Application.Json)
          setBody(request)
        }
      }

      response.bodyAsText()
    }

    return completable.get()
  }

  @Throws(
    TimeoutCancellationException::class,
    IllegalArgumentException::class,
    GeneralSecurityException::class,
    SerializationException::class
  )
  fun mutate(request: QueryInterface): String {
    return query(request)
  }

  fun setAuthData(data: ClientTokenData) {
    pubkey = data.pubkey
    wallet = data.wallet
    authToken = data.token
  }

  fun setUri(uri: URI) {
    this.uri = uri
  }

  @Throws(InvalidSyntaxException::class)
  private fun operationType(query: String): String? {
    return operationDefinition(query)?.operation?.name?.lowercase()
  }

  @Throws(InvalidSyntaxException::class)
  private fun operationDefinition(query: String): OperationDefinition? {
    require(query.isNotEmpty())
    val graphql = Parser.parse(query) as Document
    return graphql.getFirstDefinitionOfType(OperationDefinition::class.java)?.get()
  }

  @Throws(InvalidSyntaxException::class)
  private fun operationName(query: String): String? {
    return (operationDefinition(query)?.selectionSet?.selections?.get(0) as? Field)?.name
  }

  @Throws(
    InvalidSyntaxException::class, IllegalArgumentException::class, GeneralSecurityException::class
  )
  private fun encrypt(client: HttpClient) {
    client.sendPipeline.intercept(HttpSendPipeline.State) {
      val body = context.body as TextContent
      val queryData = QueryData.jsonToObject(body.text)
      val requestName = operationName(queryData.query)
      val requestType = operationType(queryData.query)
      val isMoleculeMutation = requestName == "ProposeMolecule" && requestType == "mutation"
      val condition = listOf(
        requestType == "query" && listOf("__schema", "ContinuId").contains(requestName),
        requestType == "mutation" && requestName == "AccessToken",
        isMoleculeMutation && queryData.molecule?.atoms?.get(0)?.isotope == 'U'
      )

      if (condition.any { it }) {
        return@intercept
      }

      val cipherHash = CipherHash(CipherHashVariable(wallet().encryptString(body.text, pubkey())))
      context.setBody(TextContent(cipherHash.toJson(), body.contentType, body.status))

      proceedWith(context.body)
    }
  }

  @Throws(
    IllegalArgumentException::class, GeneralSecurityException::class, SerializationException::class
  )
  private fun decode(client: HttpClient) {
    client.responsePipeline.intercept(HttpResponsePipeline.Receive) { (type, content) ->
      if (content !is ByteReadChannel) return@intercept

      val decodeJson =  KotlinJson {
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
      }
      val byteArray = ByteArray(content.availableForRead)
      content.readAvailable(byteArray)
      var result = ByteReadChannel(byteArray)

      RCipherHash.jsonToObject(byteArray.toString(Charsets.UTF_8)).data?.CipherHash?.hash?.let {
        val message = decodeJson.decodeFromString<Map<String, String>>(it)

        wallet().decryptMyMessage(message)?.let { decrypt ->
          val preform = when (decrypt) {
            is JsonObject -> decrypt.toString()
            else -> decrypt
          }
          result = ByteReadChannel((preform as String).toByteArray())
        }
      }

      val response = HttpResponseContainer(type, result)

      proceedWith(response)
    }
  }
}
