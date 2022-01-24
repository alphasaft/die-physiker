package physics.quantities.ints

import physics.quantities.PValue
import physics.quantities.booleans.PBoolean
import physics.quantities.doubles.PReal
import physics.quantities.strings.PString
import kotlin.reflect.KClass


class PInt(
    val value: Int,
) : PValue<PInt>(), Comparable<PInt> {
    override val type: KClass<PInt> = PInt::class

    override fun toPBoolean(): PBoolean = PBoolean(value != 0)
    override fun toPInt(): PInt = this
    override fun toPReal(): PReal = PReal(value.toDouble())
    override fun toPString(): PString = PString(value.toString())

    override fun toString(): String {
        return value.toString()
    }

    override fun equals(other: Any?): Boolean {
        return other is PInt && value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun compareTo(other: PInt): Int {
        return value.compareTo(other.value)
    }

    private fun withValue(value: Int) = PInt(value)

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

}
