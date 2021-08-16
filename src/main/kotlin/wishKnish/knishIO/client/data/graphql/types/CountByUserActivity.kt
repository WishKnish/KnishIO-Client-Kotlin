@file:JvmName("CountByUserActivity")
package wishKnish.knishIO.client.data.graphql.types

enum class CountByUserActivity(val value: String) {
  CREATEDAT("createdAt"),
  BUNDLEHASH("bundleHash"),
  METATYPE("metaType"),
  METAID("metaId"),
  IPADDRESS("ipAddress"),
  BROWSER("browser"),
  OSCPU("osCpu"),
  RESOLUTION("resolution"),
  TIMEZONE("timeZone")
}
