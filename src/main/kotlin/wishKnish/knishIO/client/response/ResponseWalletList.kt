@file:JvmName("ResponseWalletList")
package wishKnish.knishIO.client.response

import wishKnish.knishIO.client.data.graphql.types.Wallet as GraphqlWallet
import wishKnish.knishIO.client.Wallet
import wishKnish.knishIO.client.data.json.response.query.walletList.WalletListResponse
import wishKnish.knishIO.client.query.QueryWalletList


class ResponseWalletList(
  query: QueryWalletList,
  json: String,
): Response(query, json, "data.Wallet") {
  companion object {
    fun toClientWallet(data: GraphqlWallet, secret: String? = null): Wallet {
      val wallet: Wallet

      if (data.position == null) {
        wallet = Wallet.create(data.bundleHash, data.tokenSlug as String, data.batchId, data.characters)
      }
      else {
        wallet = Wallet(secret, data.tokenSlug as String, data.position as String?, data.batchId, data.characters).apply {
          address = data.address
          bundle = data.bundleHash
        }
      }

      data.token?.let {
        wallet.apply {
          tokenName = it.name
          tokenAmount = it.amount
          tokenSupply = it.supply
          tokenFungibility = it.fungibility
        }
      }

      wallet.apply {
        tokenUnits = data.tokenUnits.toMutableList()
        molecules = data.molecules
        balance = data.amount!!.toDouble()
        pubkey = data.pubkey
        createdAt = data.createdAt
      }

      return wallet
    }
  }

  fun getWallets(secret: String? = null): List<Wallet> {
    val list = data()
    val wallets = mutableListOf<Wallet>()

    list?.let {
      it.forEach { wallet ->
        wallets.add(toClientWallet(wallet, secret))
      }
    }

    return wallets.toList()
  }

  override fun payload(): List<Wallet> {
    return getWallets()
  }

  override fun mapping(response: String): WalletListResponse {
    return WalletListResponse.jsonToObject(response)
  }

  override fun data(): List<GraphqlWallet>? {
    @Suppress("UNCHECKED_CAST") return super.data() as? List<GraphqlWallet>
  }
}
