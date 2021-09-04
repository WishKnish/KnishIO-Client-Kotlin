@file:JvmName("BalanceVariable")

package wishKnish.knishIO.client.data.json.variables

import kotlinx.serialization.Serializable

@Serializable data class BalanceVariable @JvmOverloads constructor(
  @JvmField val address: String? = null,
  @JvmField val addresses: List<String> = listOf(),
  @JvmField val bundleHash: String? = null,
  @JvmField val bundleHashes: List<String> = listOf(),
  @JvmField val token: String? = null,
  @JvmField val tokens: List<String> = listOf(),
  @JvmField val position: String? = null,
  @JvmField val positions: List<String> = listOf()
) : IVariable
