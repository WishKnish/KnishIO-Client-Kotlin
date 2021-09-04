@file:JvmName("UserActivity")

package wishKnish.knishIO.client.data.json.query

import kotlinx.serialization.Serializable
import wishKnish.knishIO.client.data.json.variables.UserActivityVariable

@Serializable data class UserActivity(@JvmField val variables: UserActivityVariable) : QueryInterface {
  override val query = """
    query UserActivity (
      ${'$'}bundleHash:String,
      ${'$'}metaType: String,
      ${'$'}metaId: String,
      ${'$'}ipAddress: String,
      ${'$'}browser: String,
      ${'$'}osCpu: String,
      ${'$'}resolution: String,
      ${'$'}timeZone: String,
      ${'$'}countBy: [CountByUserActivity],
      ${'$'}interval: span
    ) {
      UserActivity (
        bundleHash: ${'$'}bundleHash,
        metaType: ${'$'}metaType,
        metaId: ${'$'}metaId,
        ipAddress: ${'$'}ipAddress,
        browser: ${'$'}browser,
        osCpu: ${'$'}osCpu,
        resolution: ${'$'}resolution,
        timeZone: ${'$'}timeZone,
        countBy: ${'$'}countBy,
        interval: ${'$'}interval
      ) {
        createdAt,
        bundleHash,
        metaType,
        metaId,
        instances {
          bundleHash,
          metaType,
          metaId,
          jsonData,
          createdAt,
          updatedAt
        },
        instanceCount {
          ...SubFields,
          ...Recursive
        }
      }
    }

    fragment SubFields on InstanceCountType {
      id,
      count
    }

    fragment Recursive on InstanceCountType {
      instances {
        ...SubFields
        instances {
          ...SubFields,
          instances {
            ...SubFields
            instances {
              ...SubFields
              instances {
                ...SubFields
                instances {
                  ...SubFields
                  instances {
                    ...SubFields
                    instances {
                      ...SubFields
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  """.trimIndent()
}
