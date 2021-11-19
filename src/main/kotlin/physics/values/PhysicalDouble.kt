package physics.values

import physics.roundAt
import physics.IncompatibleUnitsException
import physics.isInt
import physics.values.units.PhysicalUnit
import remove
import kotlin.math.*


class PhysicalDouble internal constructor(
    private val exactValue: Double,
    val significantDigitsCount: Int,
    val unit: PhysicalUnit,
) : PhysicalNumber<Double>, Comparable<PhysicalDouble> {
    private val Double.significantDigitsCount get() = toString().remove("-").remove(".").length
    private val lastAccurateDigit = -significantDigitsCount + exactValue.scientificNotation().second + 1
    val isConstant = significantDigitsCount == Int.MAX_VALUE

    override val value = run {
        return@run when {
            isConstant -> exactValue
            exactValue >= 1 -> exactValue.roundAt(lastAccurateDigit)
            else -> {
                val (coefficient, exponent) = exactValue.scientificNotation()
                return@run (coefficient
                    .roundAt(lastAccurateDigit) * 10.0.pow(exponent))
                    .suppressDwindlingDigits()
            }
        }
    }

    fun toDouble() = value
    fun toInt() = value.toInt()

    override fun toString(): String {
        if (isConstant && value.isInt()) return value.toInt().toString()

        val (coefficient, exponent) = value.scientificNotation()
        val significantDigitsCount = if (isConstant) coefficient.significantDigitsCount + if (value.toInt().toDouble() == value) -1 else 0 else significantDigitsCount

        val exactCoefficient =
            if (coefficient.significantDigitsCount <= significantDigitsCount)
                (coefficient.toString() + "0".repeat(significantDigitsCount-coefficient.significantDigitsCount))
            else
                coefficient.toString().dropLast(coefficient.significantDigitsCount - significantDigitsCount).removeSuffix(".")

        val unitDisplay = if (unit.isNeutral()) "" else " $unit"

        return (if (exponent == 0) exactCoefficient
        else when (Settings.scientificNotationDisplayStyle) {
            ScientificNotationDisplayStyle.E -> "${exactCoefficient}E$exponent"
            ScientificNotationDisplayStyle.POWER_OF_TEN -> "$exactCoefficient * 10^$exponent"
        }) + unitDisplay
    }

    override fun equals(other: Any?): Boolean {
        return other is PhysicalDouble
                && other.value == this.value
                && other.significantDigitsCount == significantDigitsCount
                && other.unit == this.unit
    }

    override fun hashCode(): Int {
        return value.hashCode() * 5 + significantDigitsCount.hashCode() * 7 + unit.hashCode() * 13
    }

    private fun fromDouble(value: Double) = PhysicalValuesFactory(unit.getScope()).double(value)

    fun divideOneByThis(): PhysicalDouble = PhysicalDouble(1/exactValue, significantDigitsCount, unit.invert())

    operator fun times(other: Int) = this * other.toDouble()
    operator fun times(other: Double) = this * fromDouble(other)
    operator fun times(other: PhysicalDouble) = PhysicalDouble(exactValue * other.exactValue, min(significantDigitsCount, other.significantDigitsCount), unit*other.unit)

    operator fun div(other: Int) = this * (1.0 / other)
    operator fun div(other: Double) = this * (1.0 / other)
    operator fun div(other: PhysicalDouble) = PhysicalDouble(exactValue / other.exactValue, min(significantDigitsCount, other.significantDigitsCount), unit/other.unit)

    operator fun plus(other: Int) = this + other.toDouble()
    operator fun plus(other: Double): PhysicalDouble = this + fromDouble(other)
    operator fun plus(other: PhysicalDouble): PhysicalDouble {
        val sum = exactValue + other.exactValue
        val (_, exponent) = sum.scientificNotation()
        val significantDigitsCount = when {
            this.isConstant && other.isConstant -> Int.MAX_VALUE
            this.isConstant -> exponent - other.lastAccurateDigit
            other.isConstant -> exponent - this.lastAccurateDigit
            else -> (exponent+1) - maxOf(this.lastAccurateDigit, other.lastAccurateDigit)
        }

        return PhysicalDouble(sum, significantDigitsCount, unit+other.unit)
    }


    operator fun minus(other: Int) = this + -other
    operator fun minus(other: Double) = this + -other
    operator fun minus(other: PhysicalDouble) = this + -other

    operator fun unaryPlus() = PhysicalDouble(exactValue, significantDigitsCount, unit)
    operator fun unaryMinus() = PhysicalDouble(-exactValue, significantDigitsCount, unit)

    fun pow(other: Int) = this.pow(other.toDouble())
    fun pow(other: Double) = this.pow(fromDouble(other))
    fun pow(other: PhysicalDouble) = PhysicalDouble(exactValue.pow(other.exactValue), min(significantDigitsCount, other.significantDigitsCount), unit)

    operator fun compareTo(other: Int) = compareTo(other.toDouble())
    operator fun compareTo(other: Double) = compareTo(fromDouble(other))
    override operator fun compareTo(other: PhysicalDouble) = value.compareTo(other.value)

    override fun toPhysicalDouble(): PhysicalDouble = this
    override fun toPhysicalInt() = PhysicalInt(value.toInt(), unit.getScope())
    override fun toPhysicalString() = PhysicalString(value.toString(), unit.getScope())

    fun convertInto(unit: PhysicalUnit): PhysicalDouble {
        if (unit == this.unit) return this
        return PhysicalDouble(
            this.unit.getScope().convert(this.unit, unit, exactValue) ?: throw IncompatibleUnitsException(this.unit, unit),
            significantDigitsCount,
            unit
        )
    }

    fun isInt(): Boolean = this.isConstant && this.toPhysicalInt().toPhysicalDouble() roughlyEquals this

    private infix fun roughlyEquals(other: PhysicalDouble): Boolean {
        if (this == other) return true
        return (this - other).let { it / other + it / this } < 0.0001
    }
}
