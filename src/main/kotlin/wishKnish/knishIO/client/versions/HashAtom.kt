@file:JvmName("HashAtom")

package wishKnish.knishIO.client.versions

import wishKnish.knishIO.client.data.MetaData
import wishKnish.knishIO.client.exception.HashAtomException
import kotlin.reflect.full.memberProperties

abstract class HashAtom {
  companion object {
    @JvmStatic
    inline fun <reified T : HashAtom> create(func: () -> T): T {
      return func()
    }

    @JvmStatic
    fun <T> structure(obj: T): List<Map<String, Any?>> {
      return when(obj) {
        is HashAtom -> structureHashAtom(obj)
        is MetaData -> structureMetaData(obj)
        else -> {
          throw HashAtomException()
        }
      }
    }

    private fun <T>isStructure(obj: T): Boolean {
      return obj is HashAtom || obj is MetaData
    }

    private fun structureHashAtom(obj: HashAtom): List<Map<String, Any?>> {
      val values = mutableMapOf<String, Any?>()
      val properties = obj::class.memberProperties
        .map {
          values[it.name] = it.getter.call(obj)
          it.name
        }
        .sortedWith { first: String, second: String -> first.compareTo(second) }

      if (properties.isEmpty()) {
        throw HashAtomException()
      }

      return properties.map { mapOf(it to if (isStructure(values[it])) structure(values[it]) else values[it]) }
    }

    private fun structureMetaData(obj: MetaData): List<Map<String, String?>> {
      return listOf(mapOf(obj.key to obj.value))
    }
  }
}
