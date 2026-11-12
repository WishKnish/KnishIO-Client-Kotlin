@file:JvmName("TokenOperations")

package examples.tokenoperations

import wishKnish.knishIO.client.KnishIOClient
import wishKnish.knishIO.client.data.MetaData
import wishKnish.knishIO.client.data.graphql.types.TokenUnit
import wishKnish.knishIO.client.exception.UnauthenticatedException
import wishKnish.knishIO.client.libraries.Crypto
import wishKnish.knishIO.client.response.ResponseProposeMolecule
import java.net.URI

/**
 * Token operations: issue a fungible token, transfer and burn it, then issue a stackable token
 * and transfer, replenish and fuse its units. The recipient claims what it received in a shadow
 * wallet.
 *
 * Environment: KNISHIO_NODE_URI, KNISHIO_SECRET (issuer), KNISHIO_RECIPIENT_SECRET (recipient).
 */
fun main() {
    val nodeUri = URI(System.getenv("KNISHIO_NODE_URI") ?: "http://localhost:8000/graphql")
    val issuerSecret = System.getenv("KNISHIO_SECRET") ?: Crypto.generateSecret()
    val recipientSecret = System.getenv("KNISHIO_RECIPIENT_SECRET") ?: Crypto.generateSecret()
    val recipientBundle = Crypto.generateBundleHash(recipientSecret)

    // A fresh slug per run: a slug can only be issued once
    val fungibleSlug = "DEMO${(1000..9999).random()}"
    val stackableSlug = "UNIT${(1000..9999).random()}"

    KnishIOClient(uris = listOf(nodeUri)).use { client ->
        try {
            client.requestAuthToken(secret = issuerSecret)
        } catch (e: UnauthenticatedException) {
            println("Login rejected: ${e.message}")
            return
        }

        // 1. Issue a fungible token with a limited supply
        val created = client.createToken(
            token = fungibleSlug,
            amount = 1000000,
            meta = mutableListOf(
                MetaData("name", "Demo Token"),
                MetaData("fungibility", "fungible"),
                MetaData("supply", "limited"),
                MetaData("decimals", "2")
            )
        )
        report("createToken $fungibleSlug", created)
        if (!created.success()) return

        println("Issuer balance: ${client.queryBalance(fungibleSlug).payload()?.balance}")

        // 2. Transfer to another identity by bundle hash. A bundle without a declared wallet for
        //    this token receives it in a shadow wallet.
        report("transferToken", client.transferToken(recipient = recipientBundle, token = fungibleSlug, amount = 10000))

        // 3. Burn part of the remaining supply
        report("burnTokens", client.burnTokens(token = fungibleSlug, amount = 500))

        // 4. Issue a stackable token made of named units (no decimals, no bare amount)
        val units = mutableListOf(
            TokenUnit("unit-1", "Unit One"),
            TokenUnit("unit-2", "Unit Two"),
            TokenUnit("unit-3", "Unit Three")
        )
        report(
            "createToken $stackableSlug",
            client.createToken(
                token = stackableSlug,
                meta = mutableListOf(
                    MetaData("name", "Demo Units"),
                    MetaData("fungibility", "stackable"),
                    MetaData("supply", "replenishable"),
                    MetaData("decimals", "0")
                ),
                units = units
            )
        )

        // 5. Move one unit: units are given instead of an amount
        report(
            "transferToken (unit-1)",
            client.transferToken(recipient = recipientBundle, token = stackableSlug, amount = 0, units = mutableListOf(units[0]))
        )

        // 6. Add a new unit to the replenishable supply
        report("replenishToken", client.replenishToken(token = stackableSlug, units = mutableListOf(TokenUnit("unit-4", "Unit Four"))))

        // 7. Fuse two held units into one new unit
        report(
            "fuseToken",
            client.fuseToken(
                tokenSlug = stackableSlug,
                newTokenUnit = TokenUnit("unit-5", "Fused Unit"),
                fusedTokenUnitIds = listOf("unit-2", "unit-3")
            )
        )
    }

    // 8. The recipient logs in and claims the shadow wallet that received the fungible token
    KnishIOClient(uris = listOf(nodeUri)).use { recipient ->
        recipient.requestAuthToken(secret = recipientSecret)
        report("claimShadowWallet", recipient.claimShadowWallet(token = fungibleSlug))
        println("Recipient balance: ${recipient.queryBalance(fungibleSlug).payload()?.balance}")
    }
}

private fun report(step: String, response: ResponseProposeMolecule) {
    if (response.success()) {
        println("$step: accepted (${response.data()?.molecularHash})")
    } else {
        println("$step: ${response.status()} - ${response.reason()}")
    }
}
