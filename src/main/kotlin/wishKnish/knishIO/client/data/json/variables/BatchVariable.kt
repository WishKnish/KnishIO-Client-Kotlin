@file:JvmName("BatchVariable")

package wishKnish.knishIO.client.data.json.variables

import kotlinx.serialization.Serializable

@Serializable data class BatchVariable @JvmOverloads constructor(@JvmField val batchId: String? = null) : IVariable
