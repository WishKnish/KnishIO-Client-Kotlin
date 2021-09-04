@file:JvmName("AccessTokenMutationVariable")

package wishKnish.knishIO.client.data.json.variables

import kotlinx.serialization.Serializable

@Serializable data class AccessTokenMutationVariable @JvmOverloads constructor(
  @JvmField val cellSlug: String? = null,
  @JvmField val pubkey: String? = null,
  @JvmField val encrypt: Boolean? = null
) : IVariable
