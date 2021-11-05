package physics.values

import physics.values.units.PhysicalUnit

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
operator fun Int.div(other: PhysicalInt) = PhysicalInt(this / other.value)

operator fun Double.plus(other: PhysicalInt) = other + this
operator fun Double.minus(other: PhysicalInt) = -(other - this)
operator fun Double.times(other: PhysicalInt) = other * this
operator fun Double.div(other: PhysicalInt) = this / other.value

fun Int.toPhysicalInt() = PhysicalInt(this)
fun Double.toPhysicalDouble() = PhysicalDouble(this)
fun Double.toPhysicalDouble(significantDigits: Int, unit: PhysicalUnit) = PhysicalDouble(this, significantDigits, unit)
