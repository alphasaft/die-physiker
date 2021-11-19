package physics.values

import kotlin.reflect.KClass

operator fun Int.plus(other: PhysicalDouble) = other + this
operator fun Double.plus(other: PhysicalDouble) = other + this

operator fun Int.minus(other: PhysicalDouble) = -(other - this)
operator fun Double.minus(other: PhysicalDouble) = -(other - this)

operator fun Int.times(other: PhysicalDouble) = other * this
operator fun Double.times(other: PhysicalDouble) = other * this

operator fun Int.div(other: PhysicalDouble) = (other / this).divideOneByThis()
operator fun Double.div(other: PhysicalDouble) = (other / this).divideOneByThis()

operator fun Int.plus(other: PhysicalInt) = other + this
operator fun Int.minus(other: PhysicalInt) = -(other - this)
operator fun Int.times(other: PhysicalInt) = other * this

operator fun Double.plus(other: PhysicalInt) = other + this
operator fun Double.minus(other: PhysicalInt) = -(other - this)
operator fun Double.times(other: PhysicalInt) = other * this
operator fun Double.div(other: PhysicalInt) = this / other.value

inline fun <reified T : PhysicalValue<*>> PhysicalValue<*>.castAs() = castAs(T::class)
fun <T : PhysicalValue<*>> PhysicalValue<*>.castAs(kClass: KClass<T>) =
    @Suppress("UNCHECKED_CAST") (when (kClass) {
        PhysicalInt::class -> toPhysicalInt()
        PhysicalDouble::class -> toPhysicalDouble()
        PhysicalString::class -> toPhysicalString()
        else -> throw NoWhenBranchMatchedException()
    } as T)

