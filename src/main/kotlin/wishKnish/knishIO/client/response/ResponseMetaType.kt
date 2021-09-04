@file:JvmName("ResponseMetaType")

package wishKnish.knishIO.client.response


import wishKnish.knishIO.client.data.graphql.types.KeyValueInt
import wishKnish.knishIO.client.data.graphql.types.MetaInstance
import wishKnish.knishIO.client.data.graphql.types.MetaType
import wishKnish.knishIO.client.data.graphql.types.Paginator
import wishKnish.knishIO.client.data.json.response.query.metaType.MetaTypeResponse
import wishKnish.knishIO.client.query.QueryMetaType


class ResponseMetaType(
  query: QueryMetaType,
  json: String,
) : Response(query, json, "data.MetaType") {
  data class Response @JvmOverloads constructor(
    val instances: List<MetaInstance>? = null,
    val instanceCount: List<KeyValueInt>? = null,
    val paginatorInfo: Paginator? = null
  )

  override fun mapping(response: String): MetaTypeResponse {
    return MetaTypeResponse.jsonToObject(response)
  }

  override fun data(): List<MetaType>? {
    @Suppress("UNCHECKED_CAST") return super.data() as List<MetaType>?
  }

  override fun payload(): Response? {
    val metaTypeData = data()

    return metaTypeData?.let {
      val metaData = it.last()
      Response(metaData.instances, metaData.instanceCount, metaData.paginatorInfo)
    }
  }
}
