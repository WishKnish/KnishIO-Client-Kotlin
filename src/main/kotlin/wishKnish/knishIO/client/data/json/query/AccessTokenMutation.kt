@file:JvmName("AccessTokenMutation")

package wishKnish.knishIO.client.data.json.query

import kotlinx.serialization.Serializable
import wishKnish.knishIO.client.data.json.variables.AccessTokenMutationVariable


@Serializable data class AccessTokenMutation(@JvmField val variables: AccessTokenMutationVariable) : QueryInterface {
  override val query = """
    mutation( ${'$'}cellSlug: String, ${'$'}pubkey: String, ${'$'}encrypt: Boolean ) {
      AccessToken( cellSlug: ${'$'}cellSlug, pubkey: ${'$'}pubkey, encrypt: ${'$'}encrypt ) {
        token,
        time,
        key,
        pubkey,
        expiresAt,
        encrypt
      }
    }
    """.trimIndent()
}
