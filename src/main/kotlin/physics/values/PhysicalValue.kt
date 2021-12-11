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
        fun coerceValue(value: T): T
    }
}
