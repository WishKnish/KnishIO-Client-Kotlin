@file:JvmName("WalletManagement")

package examples.walletmanagement

import wishKnish.knishIO.client.KnishIOClient
import wishKnish.knishIO.client.Wallet
import wishKnish.knishIO.client.exception.UnauthenticatedException
import wishKnish.knishIO.client.exception.WalletShadowException
import wishKnish.knishIO.client.libraries.Crypto
import java.net.URI

/**
 * Wallets: how a wallet is derived from a secret, declaring wallets on the ledger, listing them,
 * claiming shadow wallets, and ML-KEM encryption between two wallets.
 *
 * Environment: KNISHIO_NODE_URI, KNISHIO_SECRET.
 */
fun main() {
    val nodeUri = System.getenv("KNISHIO_NODE_URI") ?: "http://localhost:8000/graphql"
    val secret = System.getenv("KNISHIO_SECRET") ?: Crypto.generateSecret()

    walletDerivation(secret)
    walletEncryption(secret)

    KnishIOClient(uris = listOf(URI(nodeUri))).use { client ->
        try {
            client.requestAuthToken(secret = secret)
        } catch (e: UnauthenticatedException) {
            println("Login rejected: ${e.message}")
            return
        }
        ledgerWallets(client)
    }
}

/** Local derivation only; no node involved. */
fun walletDerivation(secret: String) {
    // One secret = one bundle: every wallet derived from it carries the same bundle hash
    val bundleHash = Crypto.generateBundleHash(secret)
    println("Bundle: $bundleHash")

    // A wallet is (secret, token, position). The same three inputs always give the same keys.
    val position = Wallet.generatePosition()
    val wallet = Wallet(secret = secret, token = "FOO", position = position, otsVersion = 2)
    val again = Wallet(secret = secret, token = "FOO", position = position, otsVersion = 2)
    println("FOO wallet ${wallet.address}, same inputs give the same address: ${wallet.address == again.address}")
    println("Bundle matches: ${wallet.bundle == bundleHash}")

    // Another position gives an unrelated one-time key and address
    val next = Wallet(secret = secret, token = "FOO", position = Wallet.generatePosition(), otsVersion = 2)
    println("Next position address: ${next.address}")

    // The address is derived from the one-time signing key
    val key = Wallet.generatePrivateKey(secret = secret, token = "FOO", position = position, otsVersion = 2)
    println("Address from key matches: ${Wallet.generatePublicKey(key) == wallet.address}")
    println("OTS version of that address: ${Wallet.detectOtsVersion(secret, "FOO", position, wallet.address)}")
}

/** ML-KEM (FIPS 203) encryption to another wallet's public key. */
fun walletEncryption(secret: String) {
    val sender = Wallet(secret = secret, token = "USER")
    val recipient = Wallet(secret = Crypto.generateSecret(), token = "USER")

    // { cipherText, encryptedMessage }: the envelope every KnishIO SDK reads
    val envelope = sender.encryptMessage("hello", recipient.pubkey!!)
    println("Recipient decrypts: ${recipient.decryptMessage(envelope)}")
}

/** Wallets on the ledger. */
fun ledgerWallets(client: KnishIOClient) {
    // Declare a wallet for a token so transfers to this bundle land in it, not in a shadow wallet
    val declared = client.createWallet("FOO")
    println("createWallet FOO: ${if (declared.success()) "accepted" else declared.reason()}")

    // Every wallet of the bundle, grouped by token
    client.queryWallets(bundle = client.bundle()).groupBy { it.token }.forEach { (token, wallets) ->
        println("$token: ${wallets.size} wallet(s), balance ${wallets.sumOf { it.balance }}")
    }

    // Shadow wallets hold tokens sent to this bundle before it declared a wallet for them; they
    // have a balance but no position or address
    val shadows = client.queryWallets(bundle = client.bundle(), token = "FOO").filter { it.isShadow() }
    println("Shadow FOO wallets: ${shadows.size}")
    try {
        val claimed = client.claimShadowWallet(token = "FOO")
        println("claimShadowWallet FOO: ${if (claimed.success()) "accepted" else claimed.reason()}")
    } catch (e: WalletShadowException) {
        println("Nothing to claim: ${e.message}")
    }

    // The ContinuID head: the USER wallet that signs the next molecule. Its position moves
    // after every accepted molecule.
    println("ContinuID position: ${client.queryContinuId(client.bundle()).payload()?.position}")
}
