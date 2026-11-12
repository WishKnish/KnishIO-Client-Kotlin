@file:JvmName("MetadataManagement")

package examples.metadatamanagement

import wishKnish.knishIO.client.KnishIOClient
import wishKnish.knishIO.client.data.MetaData
import wishKnish.knishIO.client.data.graphql.types.MetaFilter
import wishKnish.knishIO.client.data.graphql.types.MetaInstance
import wishKnish.knishIO.client.exception.UnauthenticatedException
import wishKnish.knishIO.client.libraries.Crypto
import java.net.URI

/**
 * Metadata: write key/value records to Meta Assets (metaType + metaId) and query them back.
 *
 * Environment: KNISHIO_NODE_URI, KNISHIO_SECRET.
 */
fun main() {
    val nodeUri = System.getenv("KNISHIO_NODE_URI") ?: "http://localhost:8000/graphql"
    val secret = System.getenv("KNISHIO_SECRET") ?: Crypto.generateSecret()

    KnishIOClient(uris = listOf(URI(nodeUri))).use { client ->
        try {
            client.requestAuthToken(secret = secret)
        } catch (e: UnauthenticatedException) {
            println("Login rejected: ${e.message}")
            return
        }

        // 1. Write one record per product
        val products = listOf(
            Triple("PROD001", "Laptop Pro", "2499.99"),
            Triple("PROD002", "Wireless Mouse", "29.99"),
            Triple("PROD003", "USB-C Hub", "49.99")
        )
        products.forEach { (sku, name, price) ->
            val response = client.createMeta(
                metaType = "Product",
                metaId = sku,
                meta = mutableListOf(
                    MetaData("name", name),
                    MetaData("price", price),
                    MetaData("currency", "USD"),
                    MetaData("inStock", "true")
                )
            )
            println("createMeta Product/$sku: ${if (response.success()) "accepted" else response.reason()}")
        }

        // 2. A later write to the same asset adds new values; older values stay on the ledger
        client.createMeta(metaType = "Product", metaId = "PROD002", meta = mutableListOf(MetaData("price", "24.99")))

        // 3. Every instance of a metaType, latest value per key
        val all = client.queryMeta(metaType = "Product", latest = true)
        all?.instances.orEmpty().forEach { printInstance(it) }

        // 4. Selected instances and keys
        val some = client.queryMeta(metaType = "Product", metaIds = listOf("PROD001", "PROD003"), keys = listOf("price"))
        some?.instances.orEmpty().forEach { printInstance(it) }

        // 5. Search by value
        val inStock = client.queryMeta(metaType = "Product", keys = listOf("inStock"), values = listOf("true"))
        println("In stock: ${inStock?.instances.orEmpty().map { it.metaId }}")

        // 6. Filter with a comparison operator (=, !=, <, >, <=, >=, LIKE, NOT LIKE, ILIKE, NOT ILIKE)
        val hubs = client.queryMeta(
            metaType = "Product",
            filter = listOf(MetaFilter(key = "name", value = "hub", comparison = "ILIKE")),
            latest = true
        )
        println("Name contains 'hub': ${hubs?.instances.orEmpty().map { it.metaId }}")

        // 7. One instance as the raw MetaType list the node returned
        val raw = client.queryMetaInstance(metaType = "Product", metaId = "PROD002", latest = true)
        println("Raw: $raw")
    }
}

private fun printInstance(instance: MetaInstance) {
    val values = instance.metas.orEmpty().joinToString { "${it.key}=${it.value}" }
    println("${instance.metaType}/${instance.metaId}: $values")
}
