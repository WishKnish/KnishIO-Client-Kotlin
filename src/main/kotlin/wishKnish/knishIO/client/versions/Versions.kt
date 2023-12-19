@file:JvmName("Versions")

package wishKnish.knishIO.client.versions

import kotlin.reflect.KClass

class Versions {
  companion object {
    @JvmStatic
    val ver = mapOf(
      "4" to Version4::class
    )

    @JvmStatic
    fun make(version: String?): KClass<out HashAtom>? {
      return ver[version] 
    }
  }
}
