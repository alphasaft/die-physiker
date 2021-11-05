package physics.values

import physics.roundAt
import physics.IncompatibleUnitsException
import physics.isInt
import physics.values.units.PhysicalUnit

import remove
import suppressDwindlingDigits
import kotlin.math.*
import kotlin.reflect.KClass


class PhysicalDouble(
    private val exactValue: Double,
    val significantDigitsCount: Int,
    val unit: PhysicalUnit,
) : PhysicalNumber<Double>, Comparable<PhysicalDouble> {
    constructor(constant: Double): this(constant, Int.MAX_VALUE, PhysicalUnit())
    constructor(value: Double, significantDigitsCount: Int): this(value, significantDigitsCount, PhysicalUnit())
    constructor(constant: Double, unit: PhysicalUnit): this(constant, Int.MAX_VALUE, unit)

    private val Double.significantDigitsCount get() = toString().remove("-").remove(".").length
    private val lastAccurateDigit = -significantDigitsCount + scientificNotationOf(exactValue).second + 1
    private val isConstant = significantDigitsCount == Int.MAX_VALUE

    override val value = run {
        return@run when {
            isConstant -> exactValue
            exactValue >= 1 -> exactValue.roundAt(lastAccurateDigit)
            else -> {
                val (coefficient, exponent) = scientificNotationOf(exactValue)
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

        val (coefficient, exponent) = scientificNotationOf(value)
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

    operator fun times(other: Int) = this * other.toDouble()
    operator fun times(other: Double) = this * PhysicalDouble(other)
    operator fun times(other: PhysicalDouble) = PhysicalDouble(exactValue * other.exactValue, min(significantDigitsCount, other.significantDigitsCount), unit*other.unit)

    operator fun div(other: Int) = this * (1.0 / other)
    operator fun div(other: Double) = this * (1.0 / other)
    operator fun div(other: PhysicalDouble) = PhysicalDouble(exactValue / other.exactValue, min(significantDigitsCount, other.significantDigitsCount), unit/other.unit)

    operator fun plus(other: Int) = this + other.toDouble()
    operator fun plus(other: Double): PhysicalDouble = this + PhysicalDouble(other)
    operator fun plus(other: PhysicalDouble): PhysicalDouble {
        val sum = exactValue + other.exactValue
        val (_, exponent) = scientificNotationOf(sum)
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
    fun pow(other: Double) = this.pow(PhysicalDouble(other))
    fun pow(other: PhysicalDouble) = PhysicalDouble(exactValue.pow(other.exactValue), min(significantDigitsCount, other.significantDigitsCount), unit)

    internal fun divideOneByThis(): PhysicalDouble = PhysicalDouble(1.0/exactValue, significantDigitsCount, unit)

    operator fun compareTo(other: Int) = compareTo(other.toDouble())
    operator fun compareTo(other: Double) = compareTo(PhysicalDouble(other))
    override operator fun compareTo(other: PhysicalDouble) = value.compareTo(other.value)

    override fun toPhysicalInt() = PhysicalInt(value.toInt())
    override fun toPhysicalDouble(): PhysicalDouble = this
    override fun toPhysicalString() = PhysicalString(value.toString())

    fun convertInto(unit: String) = convertInto(PhysicalUnit(unit))
    fun convertInto(unit: PhysicalUnit): PhysicalDouble {
        if (unit == this.unit) return this
        return PhysicalDouble(
            PhysicalUnit.convert(this.unit, unit, exactValue) ?: throw IncompatibleUnitsException(this.unit, unit),
            significantDigitsCount,
            unit
        )
    }

    infix fun roughlyEquals(other: PhysicalDouble): Boolean {
        return (this - other).let { other / it + this / it } > 1000
    }

    companion object FactoryProvider {
        private val stringFormatRegex = run {
            val ws = "\\s*"
            val coefficient = "-?\\d+(.\\d+)?"
            val exponent = "(\\*${ws}10$ws\\^|E)$ws(-?\\d+)"
            val unit = "\\w+(.\\w+(-?\\d+)?)*"
            Regex("($coefficient)$ws($exponent)?$ws($unit)?$ws")
        }

        private val String.significantDigitsCount get() = remove("-").remove(".").dropWhile { it == '0' }.length

        private fun isFramedBetween1And10(value: Double) = value >= 1.0 && value < 10.0

        private fun scientificNotationOf(value: Double): Pair<Double, Int> {
            var coefficient = value
            var exponent = 0
            val action = when {
                value == 0.0 -> return 0.0 to 0
                value > -1.0 && value < 1.0 -> fun() { coefficient *= 10 ; exponent -= 1 }
                value <= -10.0 || value >= 10.0 -> fun() { coefficient /= 10 ; exponent += 1 }
                else -> return coefficient to exponent
            }
            while (!isFramedBetween1And10(abs(coefficient))) action()
            return coefficient.suppressDwindlingDigits() to exponent
        }

        // SECONDARY CONSTRUCTOR
        operator fun invoke(asString: String): PhysicalDouble {
            val match = stringFormatRegex.matchEntire(asString) ?: throw NumberFormatException("Invalid value : $asString")
            val (coefficient, _, _, _, exponent, unit) = match.destructured
            return PhysicalDouble(
                coefficient.toDouble() * 10.0.pow(exponent.ifEmpty { "0" }.toInt()),
                coefficient.significantDigitsCount,
                PhysicalUnit(unit),
            )
        }

        fun withUnit(unit: String): Factory = Factory(PhysicalUnit(unit))
        fun withUnit(unit: PhysicalUnit): Factory = Factory(unit)

        class Factory(private val standardUnit: PhysicalUnit): PhysicalValue.Factory<PhysicalDouble> {
            override val of: KClass<PhysicalDouble> = PhysicalDouble::class

            override fun fromString(value: String): PhysicalDouble {
                return PhysicalDouble(value).convertInto(standardUnit)
            }
        }
    }
}
