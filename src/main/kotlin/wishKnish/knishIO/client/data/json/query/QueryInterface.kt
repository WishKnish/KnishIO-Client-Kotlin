@file:JvmName("QueryInterface")

package wishKnish.knishIO.client.data.json.query

import kotlinx.serialization.Serializable

@Serializable
sealed interface QueryInterface {
  val query: String
}
