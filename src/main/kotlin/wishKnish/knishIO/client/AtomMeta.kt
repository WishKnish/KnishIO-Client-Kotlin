package wishKnish.knishIO.client

import wishKnish.knishIO.client.data.MetaData

private const val USE_META_CONTEXT = false;
private  const val DEFAULT_META_CONTEXT = "https://www.schema.org"

class AtomMeta(private var meta: List<MetaData> = mutableListOf()) {

  fun merge(meta: List<MetaData>) {
    this.meta += meta
  }

  fun addContext(context: String? = null): AtomMeta {
    if (USE_META_CONTEXT) {
      merge(listOf(MetaData(key = "context", value = context ?: DEFAULT_META_CONTEXT)))
    }

    return this;
  }

  fun setAtomWallet(wallet: Wallet) {
    val walletMeta = mutableListOf(
      MetaData(key = "pubkey", value = wallet.pubkey),
      MetaData(key = "characters", value = wallet.characters)
    )

    if(wallet.tokenUnits.isNotEmpty()) {
      walletMeta.add(MetaData(key = "tokenUnits", value = null))
    }
  }
}