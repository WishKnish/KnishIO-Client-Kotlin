@file:JvmName("Batch")
package wishKnish.knishIO.client.data.json.query

import kotlinx.serialization.Serializable
import wishKnish.knishIO.client.data.json.variables.BatchVariable

@Serializable data class Batch(@JvmField val variables: BatchVariable): QueryInterface {
  companion object {
    @JvmStatic
    fun getFields(): String {
      return """
      batchId,
      molecularHash,
      type,
      status,
      createdAt,
      wallet {
        address,
        bundleHash,
        amount,
        tokenSlug,
        token {
          name,
          amount
        },
        tokenUnits {
          id,
          name,
          metas
        }
      },
      fromWallet {
        address,
        bundleHash,
        amount,
        batchId
      },
      toWallet {
        address,
        bundleHash,
        amount,
        batchId
        },
        sourceTokenUnits {
          id,
          name,
          metas
        },
        transferTokenUnits {
          id,
          name,
          metas
        },
        metas {
          key,
          value,
        },
        throughMetas {
          key,
          value
        }
    """.trimIndent()
    }
  }

  override val query = """
    query( ${'$'}batchId: String ) {
      Batch( batchId: ${'$'}batchId ) {
        ${ getFields() },
        children {
          ${ getFields() }
        }
      }
    }
  """.trimIndent()
}
