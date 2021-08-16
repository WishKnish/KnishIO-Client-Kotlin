@file:JvmName("Span")
package wishKnish.knishIO.client.data.graphql.types

enum class Span (val value: String) {
  SECOND("SECOND"),
  MINUTE("MINUTE"),
  HOUR("HOUR"),
  DAY("DAY"),
  WEEK("WEEK"),
  MONTH("MONTH"),
  YEAR("YEAR")
}
