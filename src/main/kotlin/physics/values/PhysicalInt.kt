package physics.values

import kotlin.reflect.KClass


class PhysicalInt(override val value: Int) : PhysicalNumber<Int> {
    operator fun plus(other: Int) = PhysicalInt(value + other)
    operator fun minus(other: Int) = PhysicalInt(value - other)
    operator fun times(other: Int) = PhysicalInt(value * other)
    operator fun div(other: Int) = PhysicalInt(value / other)

    operator fun unaryPlus() = PhysicalInt(value)
    operator fun unaryMinus() = PhysicalInt(-value)

    operator fun plus(other: Double) = value + other
    operator fun minus(other: Double) = value - other
    operator fun times(other: Double) = value * other
    operator fun div(other: Double) = value / other

    override fun toPhysicalInt() = this
    override fun toPhysicalDouble() = PhysicalDouble(value.toDouble())
    override fun toPhysicalString() = PhysicalString(value.toString())

    override fun toString(): String {
        return value.toString()
    }

    companion object Factory : PhysicalValue.Factory<PhysicalInt> {
        override val of: KClass<PhysicalInt> = PhysicalInt::class
        override fun fromString(value: String): PhysicalInt {
            return PhysicalInt(value.toInt())
        }
    }
}
