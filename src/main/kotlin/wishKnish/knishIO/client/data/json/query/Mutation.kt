@file:JvmName("Mutation")

package wishKnish.knishIO.client.data.json.query

import kotlinx.serialization.Serializable
import wishKnish.knishIO.client.data.json.variables.IVariable

@Serializable data class Mutation(@JvmField val variables: IVariable?) : QueryInterface {
  override var query = """""".trimIndent()
}
