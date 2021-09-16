@file:JvmName("ClientTokenData")

package wishKnish.knishIO.client.data

import wishKnish.knishIO.client.Wallet

data class ClientTokenData(
  @JvmField val token: String,
  @JvmField val pubkey: String,
  @JvmField val wallet: Wallet
)
