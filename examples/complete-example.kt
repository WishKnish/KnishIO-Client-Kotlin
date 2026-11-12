@file:JvmName("CompleteExample")

package examples.complete

import wishKnish.knishIO.client.KnishIOClient
import wishKnish.knishIO.client.data.MetaData
import wishKnish.knishIO.client.exception.BaseException
import wishKnish.knishIO.client.exception.UnauthenticatedException
import wishKnish.knishIO.client.libraries.Crypto
import wishKnish.knishIO.client.response.ResponseProposeMolecule
import java.net.URI

/**
 * One session end to end: log in with encrypted transport, issue a token, store and read
 * metadata, transfer, and list the bundle's wallets.
 *
 * Environment: KNISHIO_NODE_URI, KNISHIO_SECRET, KNISHIO_CELL (optional).
 * Store real secrets in a SecretStorageProvider (see README "Secret storage"), not in code.
 */
fun main() {
    val nodeUri = System.getenv("KNISHIO_NODE_URI") ?: "http://localhost:8000/graphql"
    val secret = System.getenv("KNISHIO_SECRET") ?: Crypto.generateSecret()

    KnishIOClient(uris = listOf(URI(nodeUri))).use { client ->
        client.setCellSlug(System.getenv("KNISHIO_CELL"))

        // 1. Log in; requestAuthToken throws when the node rejects the login
        val authToken = try {
            client.requestAuthToken(secret = secret, encrypt = true)
        } catch (e: UnauthenticatedException) {
            println("Login rejected: ${e.message}")
            return
        }
        println("Bundle ${client.bundle()}, encrypted transport: ${client.hasEncryption()}, expired: ${authToken.isExpired()}")

        try {
            // 2. Issue a token (a fresh slug per run: a slug can only be issued once)
            val tokenSlug = "DEMO${(1000..9999).random()}"
            report(
                "createToken $tokenSlug",
                client.createToken(
                    token = tokenSlug,
                    amount = 100000,
                    meta = mutableListOf(
                        MetaData("name", "Demo Token"),
                        MetaData("fungibility", "fungible"),
                        MetaData("supply", "limited"),
                        MetaData("decimals", "2")
                    )
                )
            )

            // 3. Store metadata and read it back
            val metaId = "demo-${System.currentTimeMillis()}"
            report("createMeta", client.createMeta(metaType = "DemoMetadata", metaId = metaId, meta = mutableListOf(MetaData("sdk", "Kotlin"))))
            client.queryMeta(metaType = "DemoMetadata", metaIds = listOf(metaId))?.instances.orEmpty().forEach { instance ->
                println("${instance.metaId}: ${instance.metas.orEmpty().joinToString { "${it.key}=${it.value}" }}")
            }

            // 4. Transfer 100 to another identity, addressed by its bundle hash
            val wallet = client.queryBalance(tokenSlug).payload()
            if (wallet != null && wallet.balance >= 100) {
                val recipientBundle = Crypto.generateBundleHash(Crypto.generateSecret())
                report("transferToken", client.transferToken(recipient = recipientBundle, token = tokenSlug, amount = 100))
            }

            // 5. The bundle's wallets, totalled per token
            client.queryWallets(bundle = client.bundle()).groupBy { it.token }.forEach { (token, wallets) ->
                println("$token: ${wallets.size} wallet(s), balance ${wallets.sumOf { it.balance }}")
            }
        } catch (e: BaseException) {
            // Local checks (balance, units, molecule verification) throw before anything is sent
            println("Stopped: ${e::class.simpleName}: ${e.message}")
        }
    }
}

private fun report(step: String, response: ResponseProposeMolecule) {
    println("$step: ${if (response.success()) "accepted" else "${response.status()} - ${response.reason()}"}")
}
