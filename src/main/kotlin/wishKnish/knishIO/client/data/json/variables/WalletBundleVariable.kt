@file:JvmName("WalletBundleVariable")

package wishKnish.knishIO.client.data.json.variables

import kotlinx.serialization.Serializable
import wishKnish.knishIO.client.data.MetaData

@Serializable data class WalletBundleVariable @JvmOverloads constructor(
  @JvmField val bundleHash: String? = null,
  @JvmField val bundleHashes: List<String> = listOf(),
  @JvmField val unspent: Boolean? = null,
  @JvmField val tokenSlug: String? = null,
  @JvmField val key: String? = null,
  @JvmField val keys: List<String> = listOf(),
  @JvmField val value: String? = null,
  @JvmField val values: List<String> = listOf(),
  @JvmField val keys_values: List<MetaData> = listOf(),
  @JvmField val latest: Boolean? = null,
  @JvmField val limit: Int? = null,
  @JvmField val skip: Int? = null,
  @JvmField val order: String? = null
) : IVariable
