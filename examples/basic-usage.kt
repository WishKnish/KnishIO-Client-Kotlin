@file:JvmName("BasicUsage")

package examples.basicusage

import wishKnish.knishIO.client.KnishIOClient
import wishKnish.knishIO.client.exception.UnauthenticatedException
import wishKnish.knishIO.client.libraries.Crypto
import java.net.URI

/**
 * Basic usage: connect to a node, log in, and read the identity's ledger state.
 *
 * Environment: KNISHIO_NODE_URI (GraphQL endpoint), KNISHIO_SECRET (the identity's secret),
 * KNISHIO_CELL (optional cell slug).
 */
fun main() {
    val nodeUri = System.getenv("KNISHIO_NODE_URI") ?: "http://localhost:8000/graphql"
    val secret = System.getenv("KNISHIO_SECRET") ?: demoSecret()

    KnishIOClient(uris = listOf(URI(nodeUri))).use { client ->
        client.setCellSlug(System.getenv("KNISHIO_CELL"))

        // Log in. `encrypt = true` asks for CipherHash (ML-KEM) encrypted transport; the client
        // follows the flag given here after login.
        val authToken = try {
            client.requestAuthToken(secret = secret, encrypt = true)
        } catch (e: UnauthenticatedException) {
            println("Login rejected: ${e.message}")
            return
        }
        println("Logged in; the session token expires in ${authToken.getExpireInterval()} ms")
        println("Bundle: ${client.bundle()}")

        // The USER wallet and its balance (null when the bundle has no USER wallet yet)
        val userWallet = client.queryBalance("USER").payload()
        println("USER balance: ${userWallet?.balance ?: 0.0}")

        // Every wallet of this bundle
        client.queryWallets(bundle = client.bundle()).forEach { wallet ->
            println("${wallet.token}: ${wallet.balance} at ${wallet.address}")
        }

        // Bundle metadata, keyed by bundle hash
        client.queryBundle().forEach { (bundleHash, walletBundle) ->
            println("$bundleHash: created ${walletBundle.createdAt}, ${walletBundle.metas.size} meta entries")
        }

        // ContinuID head: the USER wallet that signs this identity's next molecule
        val head = client.queryContinuId(client.bundle()).payload()
        println("ContinuID position: ${head?.position}")
    }
}

/** A random secret for a throwaway identity. A real identity uses its stored secret. */
fun demoSecret(): String = Crypto.generateSecret()
