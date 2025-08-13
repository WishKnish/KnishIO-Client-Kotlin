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
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.future.future
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import wishKnish.knishIO.client.Wallet
import wishKnish.knishIO.client.data.ClientTokenData
import wishKnish.knishIO.client.data.QueryData
import wishKnish.knishIO.client.data.json.query.CipherHash
import wishKnish.knishIO.client.data.json.response.query.cipherHash.CipherHash as RCipherHash
import wishKnish.knishIO.client.data.json.query.QueryInterface
import wishKnish.knishIO.client.data.json.query.ContinuId
import wishKnish.knishIO.client.data.json.query.MetaType
import wishKnish.knishIO.client.data.json.query.Balance
import wishKnish.knishIO.client.data.json.query.WalletList
import wishKnish.knishIO.client.data.json.query.Batch
import wishKnish.knishIO.client.data.json.query.BatchHistory
import wishKnish.knishIO.client.data.json.query.ActiveSession
import wishKnish.knishIO.client.data.json.mutation.MoleculeMutation
import wishKnish.knishIO.client.data.json.variables.CipherHashVariable
import wishKnish.knishIO.client.data.json.variables.ContinuIdVariable
import wishKnish.knishIO.client.data.json.variables.MetaTypeVariable
import wishKnish.knishIO.client.data.json.variables.BalanceVariable
import wishKnish.knishIO.client.data.json.variables.WalletListVariable
import wishKnish.knishIO.client.data.json.variables.BatchVariable
import wishKnish.knishIO.client.data.json.variables.BatchHistoryVariable
import wishKnish.knishIO.client.data.json.variables.ActiveSessionVariable
import wishKnish.knishIO.client.data.json.variables.MoleculeMutationVariable
import wishKnish.knishIO.client.exception.CodeException
// Removed insecure TrustAllX509TrustManager
import java.net.URI
import java.security.GeneralSecurityException
import kotlin.jvm.Throws
import kotlinx.serialization.json.Json as KotlinJson


class HttpClient @JvmOverloads constructor(
  @JvmField var uri: URI,
  @JvmField var encrypt: Boolean = false
) {

  companion object {
    // Shared Json instances to avoid repeated creation - performance optimization
    private val contentNegotiationJson = KotlinJson {
      encodeDefaults = true
      ignoreUnknownKeys = true
      coerceInputValues = true
    }
    
    private val decryptionJson = KotlinJson {
      isLenient = true
      coerceInputValues = true
      encodeDefaults = true
    }
  }

  @JvmField var authToken = ""
  @JvmField var pubkey: String? = null
  @JvmField var wallet: Wallet? = null
  private val ktorClient: io.ktor.client.HttpClient
    get() = io.ktor.client.HttpClient(CIO) {
      expectSuccess = false
      engine {
        endpoint {
          connectAttempts = 5
          connectTimeout = 10000
          requestTimeout = 30000
          socketTimeout = 30000
        }
        https {
          serverName = uri.host
          // Use default secure trust manager instead of TrustAllX509TrustManager
          // For production, consider implementing certificate pinning
        }
      }
      install(UserAgent) {
        agent = "KnishIO/0.1"
      }
      install(ContentNegotiation) {
        json(contentNegotiationJson)
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
      ktorClient.use { client ->
        // Convert request to QueryData format
        val queryData = QueryData(request.query, when (request) {
          is CipherHash -> Json.encodeToString(CipherHashVariable.serializer(), request.variables)
          is ContinuId -> Json.encodeToString(ContinuIdVariable.serializer(), request.variables)
          is MetaType -> Json.encodeToString(MetaTypeVariable.serializer(), request.variables)
          is Balance -> Json.encodeToString(BalanceVariable.serializer(), request.variables)
          is WalletList -> Json.encodeToString(WalletListVariable.serializer(), request.variables)
          is Batch -> Json.encodeToString(BatchVariable.serializer(), request.variables)
          is BatchHistory -> Json.encodeToString(BatchHistoryVariable.serializer(), request.variables)
          is ActiveSession -> Json.encodeToString(ActiveSessionVariable.serializer(), request.variables)
          is MoleculeMutation -> Json.encodeToString(MoleculeMutationVariable.serializer(), request.variables)
          else -> null
        })
        
        val requestBody = Json.encodeToString(QueryData.serializer(), queryData)
        
        val finalBody = if (encrypt) {
          encryptBody(requestBody)
        } else {
          requestBody
        }
        
        val response = client.post(uri.normalize().toASCIIString()) {
          headers {
            append("X-Auth-Token", authToken)
          }
          contentType(ContentType.Application.Json)
          setBody(finalBody)
        }
        
        val responseText = response.bodyAsText()
        
        if (encrypt) {
          decryptBody(responseText) ?: responseText
        } else {
          responseText
        }
      }
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
  private fun encryptBody(body: String): String {
    val queryData = QueryData.jsonToObject(body)
    val requestName = operationName(queryData.query)
    val requestType = operationType(queryData.query)
    val isMoleculeMutation = requestName == "ProposeMolecule" && requestType == "mutation"
    val condition = listOf(
      requestType == "query" && listOf("__schema", "ContinuId").contains(requestName),
      requestType == "mutation" && requestName == "AccessToken",
      isMoleculeMutation && queryData.molecule?.atoms?.get(0)?.isotope == 'U'
    )

    if (condition.any { it }) {
      return body
    }

    val cipherHash = CipherHash(CipherHashVariable(wallet().encryptString(body, pubkey())))
    return cipherHash.toJson()
  }

  @Throws(
    IllegalArgumentException::class, GeneralSecurityException::class, SerializationException::class
  )
  private fun decryptBody(body: String): String? {
    RCipherHash.jsonToObject(body).data?.cipherHash?.hash?.let {
      val message = decryptionJson.decodeFromString<Map<String, String>>(it)

      wallet().decryptMyMessage(message)?.let { decrypt ->
        return when (decrypt) {
          is JsonObject -> decrypt.toString()
          is String -> decrypt
          else -> decrypt.toString()
        }
      }
    }
    return null
  }
}
