@file:JvmName("UserActivityVariable")
package wishKnish.knishIO.client.data.json.variables

import kotlinx.serialization.Serializable
import wishKnish.knishIO.client.data.graphql.types.CountByUserActivity
import wishKnish.knishIO.client.data.graphql.types.Span

@Serializable data class UserActivityVariable @JvmOverloads constructor(
  @JvmField val bundleHash: String? = null,
  @JvmField val metaType: String? = null,
  @JvmField val metaId: String? = null,
  @JvmField val ipAddress: String? = null,
  @JvmField val browser: String? = null,
  @JvmField val osCpu: String? = null,
  @JvmField val resolution: String? = null,
  @JvmField val timeZone: String? = null,
  @JvmField val countBy: List<CountByUserActivity>? = null,
  @JvmField val interval: Span = Span.HOUR
): IVariable
