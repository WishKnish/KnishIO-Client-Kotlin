@file:JvmName("Clients")
package wishKnish.knishIO.client.data

import wishKnish.knishIO.client.Wallet

data class Clients(
  @JvmField val token: String,
  @JvmField val pubkey: String,
  @JvmField val wallet: Wallet
)
