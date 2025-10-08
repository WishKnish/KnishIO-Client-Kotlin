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

package wishKnish.knishIO.client.response

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant

/**
 * Universal response interface matching JavaScript SDK pattern
 */
interface UniversalResponse<T> {
    fun success(): Boolean
    fun payload(): T?
    fun reason(): String?
    fun data(): Any?
}

/**
 * Enhanced validation result pattern for Kotlin type safety
 */
@Serializable
data class ValidationResult<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ResponseError? = null,
    val warnings: List<String> = emptyList()
)

/**
 * Enhanced error information with detailed context
 */
@Serializable
data class ResponseError(
    val message: String,
    val code: String? = null,
    val details: List<String> = emptyList(),
    val context: String? = null,
    val timestamp: String = Instant.now().toString(),
    val operation: String? = null
)

/**
 * Response metadata for enhanced debugging and monitoring
 */
@Serializable
data class ResponseMetadata(
    val timestamp: String = Instant.now().toString(),
    val operation: String,
    val duration: Long? = null,
    val requestId: String? = null,
    val serverVersion: String? = null,
    val clientVersion: String = "1.0.0-RC1"
)

/**
 * Enhanced response interface with functional programming support
 */
interface EnhancedResponse<T> : UniversalResponse<T> {
    // Validation result conversion
    fun toValidationResult(): ValidationResult<T>
    
    // Functional programming combinators
    fun <U> map(mapper: (T) -> U): EnhancedResponse<U>
    fun <U> flatMap(mapper: (T) -> EnhancedResponse<U>): EnhancedResponse<U>
    fun filter(predicate: (T) -> Boolean): EnhancedResponse<T>
    
    // Error handling enhancements
    fun onSuccess(callback: (T) -> Unit): EnhancedResponse<T>
    fun onFailure(callback: (String) -> Unit): EnhancedResponse<T>
    
    // Enhanced debugging
    fun debug(label: String? = null): EnhancedResponse<T>
    
    // Coroutine integration
    fun toFlow(): Flow<T>
}

/**
 * Standard response implementation with sealed class pattern for type safety
 */
sealed class StandardResponse<T> : EnhancedResponse<T> {
    
    data class Success<T>(
        val payload: T,
        val rawData: Any? = null,
        val metadata: ResponseMetadata
    ) : StandardResponse<T>() {
        override fun success(): Boolean = true
        override fun payload(): T = payload
        override fun reason(): String? = null
        override fun data(): Any? = rawData
        
        override fun toValidationResult(): ValidationResult<T> = ValidationResult(
            success = true,
            data = payload,
            warnings = emptyList()
        )
    }
    
    data class Failure<T>(
        val errorMessage: String,
        val rawData: Any? = null,
        val metadata: ResponseMetadata,
        private val errorDetails: ResponseError? = null
    ) : StandardResponse<T>() {
        override fun success(): Boolean = false
        override fun payload(): T? = null
        override fun reason(): String = errorMessage
        override fun data(): Any? = rawData
        
        override fun toValidationResult(): ValidationResult<T> = ValidationResult(
            success = false,
            error = errorDetails ?: ResponseError(
                message = errorMessage,
                context = metadata.operation,
                timestamp = metadata.timestamp
            )
        )
    }
    
    // Functional programming combinators
    override fun <U> map(mapper: (T) -> U): StandardResponse<U> {
        return when (this) {
            is Success -> try {
                Success(mapper(payload), rawData, metadata)
            } catch (e: Exception) {
                Failure(
                    "Mapping failed: ${e.message}",
                    rawData,
                    metadata.copy(operation = "${metadata.operation}_map_failed")
                )
            }
            is Failure -> Failure(errorMessage, rawData, metadata)
        }
    }
    
    override fun <U> flatMap(mapper: (T) -> EnhancedResponse<U>): StandardResponse<U> {
        return when (this) {
            is Success -> try {
                when (val mapped = mapper(payload)) {
                    is StandardResponse -> mapped
                    else -> Failure(
                        "FlatMap returned non-StandardResponse", 
                        rawData,
                        metadata.copy(operation = "${metadata.operation}_flatmap_failed")
                    )
                }
            } catch (e: Exception) {
                Failure(
                    "FlatMap failed: ${e.message}",
                    rawData,
                    metadata.copy(operation = "${metadata.operation}_flatmap_error")
                )
            }
            is Failure -> Failure(errorMessage, rawData, metadata)
        }
    }
    
    override fun filter(predicate: (T) -> Boolean): StandardResponse<T> {
        return when (this) {
            is Success -> try {
                if (predicate(payload)) {
                    this
                } else {
                    Failure(
                        "Filter predicate failed",
                        rawData,
                        metadata.copy(operation = "${metadata.operation}_filter_failed")
                    )
                }
            } catch (e: Exception) {
                Failure(
                    "Filter failed: ${e.message}",
                    rawData, 
                    metadata.copy(operation = "${metadata.operation}_filter_error")
                )
            }
            is Failure -> this
        }
    }
    
    // Enhanced error handling with callbacks
    override fun onSuccess(callback: (T) -> Unit): StandardResponse<T> {
        if (this is Success) {
            try {
                callback(payload)
            } catch (e: Exception) {
                println("StandardResponse.onSuccess callback failed: ${e.message}")
            }
        }
        return this
    }
    
    override fun onFailure(callback: (String) -> Unit): StandardResponse<T> {
        if (this is Failure) {
            try {
                callback(errorMessage)
            } catch (e: Exception) {
                println("StandardResponse.onFailure callback failed: ${e.message}")
            }
        }
        return this
    }
    
    // Enhanced debugging with optional labels
    override fun debug(label: String?): StandardResponse<T> {
        val debugPrefix = label ?: this::class.simpleName ?: "Response"
        
        when (this) {
            is Success -> println("[$debugPrefix] Success: payload=$payload, metadata=$metadata")
            is Failure -> println("[$debugPrefix] Failure: error=$errorMessage, metadata=$metadata")
        }
        
        return this
    }
    
    // Coroutine integration
    override fun toFlow(): Flow<T> = flow {
        when (this@StandardResponse) {
            is Success -> emit(payload)
            is Failure -> throw Exception(errorMessage)
        }
    }
    
    // Kotlin-specific enhancements
    
    /**
     * Convert to Result type for idiomatic Kotlin error handling
     */
    fun toResult(): Result<T> {
        return when (this) {
            is Success -> Result.success(payload)
            is Failure -> Result.failure(Exception(errorMessage))
        }
    }
    
    /**
     * Enhanced pattern matching with fold
     */
    inline fun <R> fold(
        onSuccess: (T) -> R,
        onFailure: (String) -> R
    ): R {
        return when (this) {
            is Success -> onSuccess(payload)
            is Failure -> onFailure(errorMessage)
        }
    }
    
    /**
     * Enhanced pattern matching with optional handlers
     */
    fun <R> match(
        onSuccess: ((T) -> R)? = null,
        onFailure: ((String) -> R)? = null
    ): R? {
        return when (this) {
            is Success -> onSuccess?.invoke(payload)
            is Failure -> onFailure?.invoke(errorMessage)
        }
    }
    
    companion object {
        /**
         * Factory methods for creating standardized responses
         */
        fun <T> success(
            payload: T,
            operation: String,
            rawData: Any? = null,
            duration: Long? = null
        ): StandardResponse<T> {
            return Success(
                payload = payload,
                rawData = rawData,
                metadata = ResponseMetadata(
                    operation = operation,
                    duration = duration
                )
            )
        }
        
        fun <T> failure(
            errorMessage: String,
            operation: String,
            rawData: Any? = null,
            duration: Long? = null,
            errorDetails: ResponseError? = null
        ): StandardResponse<T> {
            return Failure(
                errorMessage = errorMessage,
                rawData = rawData,
                metadata = ResponseMetadata(
                    operation = operation,
                    duration = duration
                ),
                errorDetails = errorDetails
            )
        }
        
        /**
         * Convert from legacy Kotlin response format
         */
        fun <T> fromLegacyResponse(
            legacyResponse: Any,
            operation: String
        ): StandardResponse<T> {
            return try {
                // Use reflection to check for success method
                val successMethod = legacyResponse::class.java.getMethod("success")
                val isSuccessful = successMethod.invoke(legacyResponse) as Boolean
                
                if (isSuccessful) {
                    val payloadMethod = legacyResponse::class.java.getMethod("payload")
                    val payload = payloadMethod.invoke(legacyResponse) as T
                    success(payload, operation, legacyResponse)
                } else {
                    val reasonMethod = try {
                        legacyResponse::class.java.getMethod("reason")
                    } catch (e: NoSuchMethodException) {
                        legacyResponse::class.java.getMethod("error")
                    }
                    val errorMessage = reasonMethod.invoke(legacyResponse) as? String
                    failure(errorMessage ?: "Unknown error", operation, legacyResponse)
                }
            } catch (e: Exception) {
                failure("Legacy response conversion failed: ${e.message}", operation, legacyResponse)
            }
        }
        
        /**
         * Convert from ValidationResult
         */
        fun <T> fromValidationResult(
            validationResult: ValidationResult<T>,
            operation: String
        ): StandardResponse<T> {
            return if (validationResult.success && validationResult.data != null) {
                success(validationResult.data, operation)
            } else {
                failure(
                    validationResult.error?.message ?: "Validation failed",
                    operation,
                    errorDetails = validationResult.error
                )
            }
        }
    }
}

// Type aliases for specific response types
typealias MetaResponse = StandardResponse<Any>
typealias TokenResponse = StandardResponse<Any>
typealias TransferResponse = StandardResponse<Any>
typealias BalanceResponse = StandardResponse<Any>
typealias WalletResponse = StandardResponse<Any>
typealias AuthResponse = StandardResponse<Any>

/**
 * Response factory for creating standardized responses
 */
object ResponseFactory {
    
    fun createSuccessResponse(
        payload: Any,
        operation: String,
        rawData: Any? = null,
        duration: Long? = null
    ): StandardResponse<Any> {
        return StandardResponse.success(payload, operation, rawData, duration)
    }
    
    fun createErrorResponse(
        errorMessage: String,
        operation: String,
        rawData: Any? = null,
        duration: Long? = null
    ): StandardResponse<Any> {
        return StandardResponse.failure(errorMessage, operation, rawData, duration)
    }
    
    /**
     * Enhanced error response with detailed context
     */
    fun createDetailedErrorResponse(
        error: ResponseError,
        operation: String,
        rawData: Any? = null
    ): StandardResponse<Any> {
        return StandardResponse.failure(
            errorMessage = error.message,
            operation = operation,
            rawData = rawData,
            errorDetails = error
        )
    }
}

/**
 * Response utilities for enhanced operations
 */
object ResponseUtils {
    
    /**
     * Combine multiple responses into a single response
     */
    fun <T> combineResponses(responses: List<StandardResponse<T>>): StandardResponse<List<T>> {
        val successful = responses.all { it.success() }
        
        return if (successful) {
            val payloads = responses.mapNotNull { it.payload() }
            StandardResponse.success(payloads, "combineResponses")
        } else {
            val errors = responses.filter { !it.success() }.joinToString("; ") { it.reason() ?: "Unknown error" }
            StandardResponse.failure("Combined operation failed: $errors", "combineResponses", responses)
        }
    }
    
    /**
     * Execute operations in sequence, stopping on first failure
     */
    suspend fun <T> sequenceResponses(
        operations: List<suspend () -> StandardResponse<T>>
    ): StandardResponse<List<T>> {
        val results = mutableListOf<StandardResponse<T>>()
        
        for (operation in operations) {
            try {
                val result = operation()
                results.add(result)
                
                if (!result.success()) {
                    return StandardResponse.failure(
                        "Sequence failed at operation ${results.size}: ${result.reason()}",
                        "sequenceResponses",
                        results
                    )
                }
            } catch (e: Exception) {
                return StandardResponse.failure(
                    "Sequence failed with exception: ${e.message}",
                    "sequenceResponses",
                    results
                )
            }
        }
        
        val payloads = results.mapNotNull { it.payload() }
        return StandardResponse.success(payloads, "sequenceResponses")
    }
}