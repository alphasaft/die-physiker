package physics.values

import kotlin.reflect.KClass


sealed interface PhysicalValue<V : Any> {
    val value: V

    fun toPhysicalInt(): PhysicalInt
    fun toPhysicalDouble(): PhysicalDouble
    fun toPhysicalString(): PhysicalString

    sealed interface Factory<T : PhysicalValue<*>> {
        val of: KClass<T>
        fun fromString(value: String): T
    }
}

inline fun <reified T : PhysicalValue<*>> PhysicalValue<*>.castAs() = castAs(T::class)
fun <T : PhysicalValue<*>> PhysicalValue<*>.castAs(kClass: KClass<T>) =
    @Suppress("UNCHECKED_CAST") (when (kClass) {
        PhysicalInt::class -> toPhysicalInt()
        PhysicalDouble::class -> toPhysicalDouble()
        PhysicalString::class -> toPhysicalString()
        else -> throw NoWhenBranchMatchedException()
    } as T)
