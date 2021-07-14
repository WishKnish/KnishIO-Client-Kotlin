package wishKnish.knishIO.client.libraries

import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaSetter

internal inline infix fun <reified T : Any> T.merge(other: T): T {
    val nameProperty = T::class.declaredMemberProperties.associateBy { it.name }
    val primaryConstructor = T::class.primaryConstructor!!
    val args = primaryConstructor.parameters.associateWith {
        val property = nameProperty[it.name]!!

        property.get(other) ?: property.get(this)
    }
    val mergedObject = primaryConstructor.callBy(args)

    nameProperty.values.forEach {
        run {
            val property = it as KMutableProperty<*>
            val value = property.javaGetter!!.invoke(other) ?: property.javaGetter!!.invoke(this)
            property.javaSetter!!.invoke(mergedObject, value)
        }
    }

    return mergedObject
}
