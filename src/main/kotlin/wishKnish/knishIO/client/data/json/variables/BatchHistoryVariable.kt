@file:JvmName("BatchHistoryVariable")

package wishKnish.knishIO.client.data.json.variables

import kotlinx.serialization.Serializable

@Serializable data class BatchHistoryVariable @JvmOverloads constructor(@JvmField val batchId: String? = null) : IVariable
