@file:JvmName("BatchVariable")
package wishKnish.knishIO.client.data.json.variables

import kotlinx.serialization.Serializable

@Serializable data class BatchVariable(@JvmField val batchId: String? = null): IVariable
