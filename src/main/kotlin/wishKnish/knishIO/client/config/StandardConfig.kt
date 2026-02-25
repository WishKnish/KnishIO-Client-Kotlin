/*
Enhanced Configuration System for Kotlin SDK

Implements JavaScript/TypeScript reference patterns using Kotlin data classes
with comprehensive type safety, validation, and builder patterns.
*/

package wishKnish.knishIO.client.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import wishKnish.knishIO.client.response.ValidationResult
import wishKnish.knishIO.client.response.ResponseError

/**
 * Socket configuration for real-time features
 */
@Serializable
data class SocketConfig(
    @SerialName("socketUri")
    val socketUri: String?,
    val appKey: String? = null
) {
    companion object {
        fun fromMap(map: Map<String, Any?>): SocketConfig {
            return SocketConfig(
                socketUri = map["socketUri"] as? String ?: map["socket_uri"] as? String,
                appKey = map["appKey"] as? String ?: map["app_key"] as? String
            )
        }
    }
}

/**
 * Core client configuration (mirrors JavaScript object pattern)
 */
@Serializable
data class ClientConfig(
    val uri: String,
    @SerialName("cellSlug")
    val cellSlug: String? = null,
    val client: String? = null,  // GraphQL client config as JSON string
    val socket: SocketConfig? = null,
    @SerialName("serverSdkVersion")
    val serverSdkVersion: Int = 3,
    val logging: Boolean = false
) {
    /**
     * Validate configuration with enhanced error messages
     */
    fun validate(): ValidationResult<ClientConfig> {
        val errors = mutableListOf<String>()
        
        if (uri.isBlank()) {
            errors.add("URI cannot be blank")
        }
        
        if (serverSdkVersion < 1) {
            errors.add("Server SDK version must be positive")
        }
        
        // Validate URI format
        try {
            java.net.URI(uri)
        } catch (e: Exception) {
            errors.add("Invalid URI format: ${e.message}")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult(success = true, data = this)
        } else {
            ValidationResult(
                success = false,
                error = ResponseError(
                    message = "ClientConfig validation failed",
                    details = errors,
                    context = "ClientConfig.validate"
                )
            )
        }
    }
    
    /**
     * Builder pattern for fluent configuration
     */
    fun withCellSlug(cellSlug: String): ClientConfig = copy(cellSlug = cellSlug)
    fun withLogging(logging: Boolean): ClientConfig = copy(logging = logging)
    fun withServerSdkVersion(version: Int): ClientConfig = copy(serverSdkVersion = version)
    fun withSocket(socket: SocketConfig): ClientConfig = copy(socket = socket)
    
    companion object {
        /**
         * Create from JavaScript-style Map
         */
        fun fromMap(configMap: Map<String, Any?>): ClientConfig {
            return ClientConfig(
                uri = configMap["uri"] as? String ?: throw IllegalArgumentException("URI is required"),
                cellSlug = configMap["cellSlug"] as? String ?: configMap["cell_slug"] as? String,
                logging = configMap["logging"] as? Boolean ?: false,
                serverSdkVersion = configMap["serverSdkVersion"] as? Int ?: configMap["server_sdk_version"] as? Int ?: 3,
                socket = (configMap["socket"] as? Map<String, Any?>)?.let { SocketConfig.fromMap(it) }
            )
        }
        
        /**
         * Create from JSON string (JavaScript compatibility)
         */
        fun fromJson(json: String): ClientConfig {
            return Json.decodeFromString<ClientConfig>(json)
        }
        
        /**
         * Default configuration factory
         */
        fun default(uri: String): ClientConfig {
            return ClientConfig(uri = uri)
        }
    }
}

/**
 * Authentication configuration
 */
@Serializable
data class AuthTokenConfig(
    val secret: String? = null,
    val seed: String? = null,
    @SerialName("cellSlug")
    val cellSlug: String? = null,
    val encrypt: Boolean = false
) {
    fun validate(): ValidationResult<AuthTokenConfig> {
        // Guest authentication is allowed, so no required fields
        return ValidationResult(success = true, data = this)
    }
    
    companion object {
        fun fromMap(configMap: Map<String, Any?>): AuthTokenConfig {
            return AuthTokenConfig(
                secret = configMap["secret"] as? String,
                seed = configMap["seed"] as? String,
                cellSlug = configMap["cellSlug"] as? String ?: configMap["cell_slug"] as? String,
                encrypt = configMap["encrypt"] as? Boolean ?: false
            )
        }
    }
}

/**
 * Metadata operation configuration
 */
@Serializable
data class MetaConfig(
    @SerialName("metaType")
    val metaType: String,
    @SerialName("metaId") 
    val metaId: String,
    val meta: Map<String, String>,
    val policy: Map<String, kotlinx.serialization.json.JsonElement>? = null
) {
    fun validate(): ValidationResult<MetaConfig> {
        val errors = mutableListOf<String>()
        
        if (metaType.isBlank()) {
            errors.add("MetaType cannot be blank")
        }
        
        if (metaId.isBlank()) {
            errors.add("MetaId cannot be blank")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult(success = true, data = this)
        } else {
            ValidationResult(
                success = false,
                error = ResponseError(
                    message = "MetaConfig validation failed",
                    details = errors,
                    context = "MetaConfig.validate"
                )
            )
        }
    }
    
    companion object {
        fun fromMap(configMap: Map<String, Any?>): MetaConfig {
            val metaData = configMap["meta"] as? Map<String, String> ?: emptyMap()
            
            return MetaConfig(
                metaType = configMap["metaType"] as? String ?: throw IllegalArgumentException("metaType is required"),
                metaId = configMap["metaId"] as? String ?: throw IllegalArgumentException("metaId is required"),
                meta = metaData,
                policy = null // Policy handling can be enhanced later
            )
        }
    }
}

/**
 * Metadata query configuration
 */
@Serializable
data class QueryMetaConfig(
    @SerialName("metaType")
    val metaType: String,
    @SerialName("metaId")
    val metaId: String? = null,
    val key: String? = null,
    val value: String? = null,
    val latest: Boolean = true,
    val filter: Map<String, kotlinx.serialization.json.JsonElement>? = null,
    @SerialName("queryArgs")
    val queryArgs: Map<String, kotlinx.serialization.json.JsonElement>? = null,
    val count: String? = null,
    @SerialName("countBy")
    val countBy: String? = null
) {
    companion object {
        fun fromMap(configMap: Map<String, Any?>): QueryMetaConfig {
            return QueryMetaConfig(
                metaType = configMap["metaType"] as? String ?: throw IllegalArgumentException("metaType is required"),
                metaId = configMap["metaId"] as? String,
                key = configMap["key"] as? String,
                value = configMap["value"] as? String,
                latest = configMap["latest"] as? Boolean ?: true
            )
        }
    }
}

/**
 * Token creation configuration
 */
@Serializable
data class TokenConfig(
    val token: String,
    val amount: Long? = null,
    val meta: Map<String, String>? = null,
    @SerialName("batchId")
    val batchId: String? = null,
    val units: List<String> = emptyList()
) {
    fun validate(): ValidationResult<TokenConfig> {
        val errors = mutableListOf<String>()
        
        if (token.isBlank()) {
            errors.add("Token identifier cannot be blank")
        }
        
        amount?.let { amt ->
            if (amt < 0) {
                errors.add("Token amount cannot be negative")
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult(success = true, data = this)
        } else {
            ValidationResult(
                success = false,
                error = ResponseError(
                    message = "TokenConfig validation failed",
                    details = errors,
                    context = "TokenConfig.validate"
                )
            )
        }
    }
    
    companion object {
        fun fromMap(configMap: Map<String, Any?>): TokenConfig {
            return TokenConfig(
                token = configMap["token"] as? String ?: throw IllegalArgumentException("token is required"),
                amount = (configMap["amount"] as? Number)?.toLong(),
                meta = configMap["meta"] as? Map<String, String>,
                batchId = configMap["batchId"] as? String,
                units = configMap["units"] as? List<String> ?: emptyList()
            )
        }
    }
}

/**
 * Transfer configuration
 */
@Serializable
data class TransferConfig(
    @SerialName("bundleHash")
    val bundleHash: String,
    val token: String,
    val amount: Long,
    val units: List<String> = emptyList(),
    @SerialName("batchId")
    val batchId: String? = null,
    @SerialName("sourceWallet")
    val sourceWalletJson: String? = null  // Wallet serialized as JSON
) {
    fun validate(): ValidationResult<TransferConfig> {
        val errors = mutableListOf<String>()
        
        if (bundleHash.isBlank()) {
            errors.add("Bundle hash cannot be blank")
        }
        
        if (token.isBlank()) {
            errors.add("Token cannot be blank")
        }
        
        if (amount < 0 && units.isEmpty()) {
            errors.add("Either positive amount or units must be provided")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult(success = true, data = this)
        } else {
            ValidationResult(
                success = false,
                error = ResponseError(
                    message = "TransferConfig validation failed",
                    details = errors,
                    context = "TransferConfig.validate"
                )
            )
        }
    }
    
    companion object {
        fun fromMap(configMap: Map<String, Any?>): TransferConfig {
            return TransferConfig(
                bundleHash = configMap["bundleHash"] as? String ?: throw IllegalArgumentException("bundleHash is required"),
                token = configMap["token"] as? String ?: throw IllegalArgumentException("token is required"),
                amount = (configMap["amount"] as? Number)?.toLong() ?: 0L,
                units = configMap["units"] as? List<String> ?: emptyList(),
                batchId = configMap["batchId"] as? String
            )
        }
    }
}

/**
 * Balance query configuration
 */
@Serializable
data class QueryBalanceConfig(
    val token: String,
    val bundle: String? = null,
    val type: String = "regular"
) {
    companion object {
        fun fromMap(configMap: Map<String, Any?>): QueryBalanceConfig {
            return QueryBalanceConfig(
                token = configMap["token"] as? String ?: throw IllegalArgumentException("token is required"),
                bundle = configMap["bundle"] as? String,
                type = configMap["type"] as? String ?: "regular"
            )
        }
    }
}

/**
 * Wallet creation configuration
 */
@Serializable
data class WalletConfig(
    val token: String
) {
    fun validate(): ValidationResult<WalletConfig> {
        return if (token.isNotBlank()) {
            ValidationResult(success = true, data = this)
        } else {
            ValidationResult(
                success = false,
                error = ResponseError(
                    message = "Token cannot be blank",
                    context = "WalletConfig.validate"
                )
            )
        }
    }
    
    companion object {
        fun fromMap(configMap: Map<String, Any?>): WalletConfig {
            return WalletConfig(
                token = configMap["token"] as? String ?: throw IllegalArgumentException("token is required")
            )
        }
    }
}

/**
 * Configuration factory for creating standardized configurations
 */
object ConfigFactory {
    
    fun createClientConfig(
        uri: String,
        cellSlug: String? = null,
        logging: Boolean = false,
        serverSdkVersion: Int = 3
    ): ClientConfig {
        return ClientConfig(
            uri = uri,
            cellSlug = cellSlug,
            logging = logging,
            serverSdkVersion = serverSdkVersion
        )
    }
    
    fun createMetaConfig(
        metaType: String,
        metaId: String,
        meta: Map<String, String>,
        policy: Map<String, kotlinx.serialization.json.JsonElement>? = null
    ): MetaConfig {
        return MetaConfig(metaType, metaId, meta, policy)
    }
    
    fun createTokenConfig(
        token: String,
        amount: Long? = null,
        meta: Map<String, String>? = null
    ): TokenConfig {
        return TokenConfig(token, amount, meta)
    }
    
    fun createTransferConfig(
        bundleHash: String,
        token: String,
        amount: Long,
        units: List<String> = emptyList()
    ): TransferConfig {
        return TransferConfig(bundleHash, token, amount, units)
    }
}

/**
 * Configuration utilities for validation and conversion
 */
object ConfigUtils {
    
    /**
     * Convert JavaScript camelCase keys to Kotlin-appropriate format
     */
    fun normalizeCamelCase(configMap: Map<String, Any?>): Map<String, Any?> {
        val camelCaseMapping = mapOf(
            "cellSlug" to "cellSlug",
            "metaType" to "metaType", 
            "metaId" to "metaId",
            "bundleHash" to "bundleHash",
            "batchId" to "batchId",
            "sourceWallet" to "sourceWallet",
            "serverSdkVersion" to "serverSdkVersion",
            "queryArgs" to "queryArgs",
            "countBy" to "countBy"
        )
        
        val normalized = configMap.toMutableMap()
        
        // Handle snake_case to camelCase conversion for cross-platform compatibility
        camelCaseMapping.forEach { (camelCase, kotlinCase) ->
            val snakeCase = camelCase.replace(Regex("([a-z])([A-Z])")) { match ->
                "${match.groupValues[1]}_${match.groupValues[2].lowercase()}"
            }
            
            if (configMap.containsKey(snakeCase) && !configMap.containsKey(camelCase)) {
                normalized[kotlinCase] = configMap[snakeCase]
                normalized.remove(snakeCase)
            }
        }
        
        return normalized
    }
    
    /**
     * Validate any configuration object using reflection
     */
    inline fun <reified T> validateConfig(config: T): ValidationResult<T> {
        return when (config) {
            is ClientConfig -> config.validate() as ValidationResult<T>
            is MetaConfig -> config.validate() as ValidationResult<T>
            is TokenConfig -> config.validate() as ValidationResult<T>
            is TransferConfig -> config.validate() as ValidationResult<T>
            is WalletConfig -> config.validate() as ValidationResult<T>
            else -> ValidationResult(success = true, data = config)
        }
    }
    
    /**
     * Convert configuration to JSON for cross-platform compatibility
     */
    inline fun <reified T> configToJson(config: T): String {
        return Json.encodeToString(kotlinx.serialization.serializer<T>(), config)
    }
    
    /**
     * Create configuration from JSON (JavaScript compatibility)
     */
    inline fun <reified T> configFromJson(json: String): T {
        return Json.decodeFromString<T>(json)
    }
}