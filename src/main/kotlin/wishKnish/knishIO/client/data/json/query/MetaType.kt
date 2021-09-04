@file:JvmName("MetaType")

package wishKnish.knishIO.client.data.json.query

import kotlinx.serialization.Serializable
import wishKnish.knishIO.client.data.json.variables.MetaTypeVariable

@Serializable data class MetaType(@JvmField val variables: MetaTypeVariable) : QueryInterface {
  override val query = """
    query( ${'$'}metaType: String, ${'$'}metaTypes: [ String! ], ${'$'}metaId: String, ${'$'}metaIds: [ String! ], ${'$'}key: String, ${'$'}keys: [ String! ], ${'$'}value: String, ${'$'}values: [ String! ], ${'$'}count: String, ${'$'}latest: Boolean, ${'$'}filter: [ MetaFilter! ], ${'$'}latestMetas: Boolean, ${'$'}queryArgs: QueryArgs, ${'$'}countBy: String ) {
      MetaType( metaType: ${'$'}metaType, metaTypes: ${'$'}metaTypes, metaId: ${'$'}metaId, metaIds: ${'$'}metaIds, key: ${'$'}key, keys: ${'$'}keys, value: ${'$'}value, values: ${'$'}values, count: ${'$'}count, filter: ${'$'}filter, latestMetas: ${'$'}latestMetas, queryArgs: ${'$'}queryArgs, countBy: ${'$'}countBy ) {
        metaType,
        instanceCount {
          key,
          value
        },
        instances {
          metaType,
          metaId,
          createdAt,
          metas(latest:${'$'}latest) {
            molecularHash,
            position,
            key,
            value,
            createdAt
          }
        },
        paginatorInfo {
          currentPage,
          total
        }
      }
    }
  """.trimIndent()
}
