@file:JvmName("MetaTypeVariable")

package wishKnish.knishIO.client.data.json.variables

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import wishKnish.knishIO.client.data.graphql.types.MetaFilter
import wishKnish.knishIO.client.data.graphql.types.QueryArgs

// `latest` is always encoded: the validator's `metas(latest:)` takes a non-null Boolean, and an
// omitted `$latest` reaches it as null ("Expected input type \"Boolean\", found null").
@OptIn(ExperimentalSerializationApi::class)
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
  @EncodeDefault @JvmField var latest: Boolean = false,
  @JvmField val count: String? = null
) : IVariable
