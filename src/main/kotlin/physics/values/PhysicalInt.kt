package physics.values

import physics.values.units.UnitScope


class PhysicalInt(
    override val value: Int,
    private val unitScope: UnitScope,
) : PhysicalNumber<Int> {
    private fun withValue(value: Int) = PhysicalInt(value, unitScope)

    operator fun plus(other: Int) = withValue(value + other)
    operator fun minus(other: Int) = withValue(value - other)
    operator fun times(other: Int) = withValue(value * other)
    operator fun div(other: Int) = withValue(value / other)

    operator fun unaryPlus() = withValue(value)
    operator fun unaryMinus() = withValue(-value)

    operator fun plus(other: Double) = value + other
    operator fun minus(other: Double) = value - other
    operator fun times(other: Double) = value * other
    operator fun div(other: Double) = value / other

    override fun toPhysicalInt() = +this
    override fun toPhysicalDouble() = PhysicalValuesFactory(unitScope).double(value.toDouble())
    override fun toPhysicalString() = PhysicalString(value.toString(), unitScope)

    override fun toString(): String {
        return value.toString()
    }

    override fun equals(other: Any?): Boolean {
        return other is PhysicalInt && value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}
