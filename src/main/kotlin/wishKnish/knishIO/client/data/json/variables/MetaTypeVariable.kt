@file:JvmName("MetaTypeVariable")

package wishKnish.knishIO.client.data.json.variables

import kotlinx.serialization.Serializable
import wishKnish.knishIO.client.data.graphql.types.MetaFilter
import wishKnish.knishIO.client.data.graphql.types.QueryArgs

@Serializable data class MetaTypeVariable @JvmOverloads constructor(
  @JvmField val metaType: String? = null,
  @JvmField var metaIds: List<String> = listOf(),
  @JvmField var keys: List<String> = listOf(),
  @JvmField var values: List<String> = listOf(),
  @JvmField val latestMetas: Boolean? = null,
  @JvmField var filter: List<MetaFilter> = listOf(),
  @JvmField val queryArgs: QueryArgs? = null,
  @JvmField val countBy: String? = null,
  @JvmField val cellSlug: String? = null,
  @JvmField val cellSlugs: List<String> = listOf(),
  @JvmField var latest: Boolean? = null,
  @JvmField val count: String? = null
) : IVariable
