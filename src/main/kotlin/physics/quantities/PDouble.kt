package physics.quantities

import isInt
import physics.UnitException
import physics.quantities.units.PUnit
import remove
import kotlin.math.*
import kotlin.reflect.KClass


class PDouble internal constructor(
    exactValue: Double,
    private val significantDigitsCount: Int = Int.MAX_VALUE,
    val unit: PUnit = PUnit(),
) : PValue<PDouble>(), PRealOperand, Comparable<PDouble> {
    constructor(value: Int) : this(value.toDouble())

    companion object Factory {
        private val pRealStringFormatRegex = run {
            val ws = "\\s*"
            val coefficient = "-?\\d+(.\\d+)?"
            val exponent = "(\\*${ws}10$ws\\^|E)$ws(-?\\d+)"
            val unit = "\\w+(.\\w+(-?\\d+)?)*"
            Regex("($coefficient)$ws($exponent)?$ws($unit)?$ws")
        }

        operator fun invoke(value: String): PDouble {
            val match = pRealStringFormatRegex.matchEntire(value)
                ?: throw NumberFormatException("Invalid value : $value")

            val (coefficient, _, _, _, exponent, unit) = match.destructured
            return PDouble(
                coefficient.toDouble() * 10.0.pow(exponent.ifEmpty { "0" }.toInt()),
                if (coefficient.toDouble() == 0.0) Int.MAX_VALUE else coefficient.remove("-").remove(".").dropWhile { c -> c == '0' }.length,
                PUnit(unit),
            )
        }
    }

    override val type: KClass<PDouble> = PDouble::class

    init {
        if (exactValue.isNaN()) throw IllegalArgumentException("Can't init PReal(NaN).")
    }

    private val lastAccurateDigit = -significantDigitsCount + exactValue.scientificNotation().exponent + 1
    private val valueStorage = exactValue.suppressDwindlingDigits()
    private val isConstant = significantDigitsCount == Int.MAX_VALUE
    override val value = if (isConstant) exactValue else exactValue.roundedAt(lastAccurateDigit)

    data class ScientificNotation(
        val coefficient: Double,
        val exponent: Int
    )

    private fun Double.scientificNotation(): ScientificNotation {
        if (this == 0.0) return ScientificNotation(0.0, exponent = 0)

        val exponent = floor(log(abs(this), base = 10.0).suppressDwindlingDigits()).toInt()
        return ScientificNotation(
            coefficient = (this * 10.0.pow(-exponent)).suppressDwindlingDigits(),
            exponent,
        )
    }

    private fun Double.roundedAt(n: Int): Double {
        val rounded = (this * 10.0.pow(-n)).roundToInt().toDouble()
        return (rounded * 10.0.pow(n)).suppressDwindlingDigits()
    }

    private fun Double.suppressDwindlingDigits(): Double {
        var asString = toString()

        if (asString.split(".").last().length < 10) return this

        val splitOnE = asString.split("E")
        if (splitOnE.size > 1) {
            val (coefficient, exponent) = splitOnE.first() to splitOnE.last().toInt()
            asString = if (exponent < 0) {
                (if (asString.first() == '-') "-" else "") + ("0." + "0".repeat(-exponent - 1) + coefficient.remove("-")
                    .remove(".")).removeSuffix("0")
            } else {
                coefficient.remove(".") + "0".repeat(exponent - 1)
            }
        }

        return (
                if (asString.last().digitToInt() < 5)
                    asString
                        .dropLast(1)
                        .dropLastWhile { it in "0" }
                        .toDouble()
                else
                    asString
                        .dropLast(1)
                        .dropLastWhile { it in "9" }
                        .let { it.trimEnd('.').dropLast(1) + (it.trimEnd('.').last().digitToInt() + 1) }
                        .toDouble()
        )
    }

    private fun Double.significantDigitsCount() = toString().remove("-").remove(".").dropWhile { it == '0' }.length

    override fun contains(value: PDouble): Boolean {
        return this roughlyEquals value
    }

    override fun toPBoolean(): PBoolean = PBoolean(valueStorage != 0.0)
    override fun toPInt() = PInt(valueStorage.toInt())
    override fun toPReal() = this
    override fun toPString(): PString = PString(value.toString())

    private fun toInterval() = PRealInterval.fromPReal(this)
    fun toDouble() = value
    fun toInt() = value.toInt()

    override fun minus(other: PRealOperand): Quantity<PDouble> = this + (-other)
    operator fun minus(other: PDouble): PDouble = this + (-other)

    override fun unaryMinus() = PDouble(-valueStorage, significantDigitsCount, unit)

    override fun plus(other: PRealOperand): Quantity<PDouble> = if (other is PDouble) this + other else other + this
    operator fun plus(other: PDouble): PDouble {
        val sum = valueStorage + other.valueStorage
        val (_, exponent) = sum.scientificNotation()
        val significantDigitsCount = when {
            this.isConstant && other.isConstant -> Int.MAX_VALUE
            this.isConstant -> other.significantDigitsCount
            other.isConstant -> this.significantDigitsCount
            else -> (exponent + 1) - maxOf(this.lastAccurateDigit, other.lastAccurateDigit)
        }
        return PDouble(sum, significantDigitsCount, unit + other.unit)
    }

    override fun times(other: PRealOperand): Quantity<PDouble> = if (other is PDouble) this * other else other * this
    operator fun times(other: PDouble): PDouble = PDouble(
        valueStorage * other.valueStorage,
        min(significantDigitsCount, other.significantDigitsCount),
        unit * other.unit
    )

    override fun div(other: PRealOperand): Quantity<PDouble> = if (other is PDouble) this / other else toInterval() / other
    operator fun div(other: PDouble): PDouble = PDouble(
        valueStorage / other.valueStorage,
        min(significantDigitsCount, other.significantDigitsCount),
        unit / other.unit,
    )

    override fun pow(other: PRealOperand): Quantity<PDouble> =
        if (other is PDouble) this.pow(other) else toInterval().pow(other)

    fun pow(other: PDouble): PDouble {
        require(other.unit.isNeutral()) { "Can't use a PReal with a unit as exponent." }

        return PDouble(
            valueStorage.pow(other.valueStorage),
            min(significantDigitsCount, other.significantDigitsCount),
            unit.pow(other.valueStorage)
        )
    }

    operator fun rem(other: PDouble): PDouble {
        val convertedOther = other.convertInto(unit)
        if ((valueStorage / convertedOther.valueStorage).isInt()) return PDouble(0).withUnit(unit)

        var remainder = valueStorage % convertedOther.valueStorage

        // We want it to return some value from [0;other], not [-other;other]
        // (i personally don't understand why the first is the default behavior of %)
        if (remainder < 0) remainder += convertedOther.valueStorage

        val exponent = remainder.scientificNotation().exponent
        val significantDigitsCount = when {
            this.isConstant && other.isConstant -> Int.MAX_VALUE
            this.isConstant -> other.significantDigitsCount
            other.isConstant -> this.significantDigitsCount
            else -> (exponent + 1) - maxOf(this.lastAccurateDigit, other.lastAccurateDigit)
        }

        return PDouble(
            remainder,
            significantDigitsCount,
            unit,
        )
    }

    fun applyFunction(f: (Double) -> Double): PDouble {
        require(unit.isNeutral()) { "Can't apply a function to a dimensioned quantity." }
        return PDouble(
            f(valueStorage),
            significantDigitsCount,
            PUnit()
        )
    }

    fun floor(): PDouble = PDouble(floor(value), significantDigitsCount, unit)
    fun abs(): PDouble = PDouble(abs(value), significantDigitsCount, unit)

    fun isCompatibleWith(other: PDouble): Boolean {
        return unit.isConvertibleInto(other.unit)
    }

    fun isInt(): Boolean {
        return floor() == this
    }

    fun convertInto(unit: String) = convertInto(PUnit(unit))
    fun convertInto(unit: PUnit): PDouble {
        if (unit == this.unit) return this
        return PDouble(
            this.unit.convert(valueStorage, into = unit)
                ?: throw UnitException("Unit '${this.unit}' cannot be converted into '$unit'."),
            significantDigitsCount,
            unit
        )
    }

    operator fun compareTo(other: Int) = value.compareTo(other.toDouble())
    operator fun compareTo(other: Double) = value.compareTo(other)
    override operator fun compareTo(other: PDouble): Int = this.value.compareTo(other.convertInto(this.unit).value)

    fun withoutUnit(): PDouble = PDouble(valueStorage, significantDigitsCount, PUnit())
    fun withUnit(unit: PUnit) = PDouble(valueStorage, significantDigitsCount, unit)

    override fun equals(other: Any?): Boolean {
        return other is PDouble
                && other.value == this.value
                && other.unit == this.unit
    }

    private infix fun roughlyEquals(other: PDouble): Boolean {
        if (this == other) return true
        if (this.unit != other.unit) return false
        return abs((valueStorage + other.valueStorage) / (valueStorage - other.valueStorage)) > 100.0
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + significantDigitsCount
        result = 31 * result + unit.hashCode()
        return result
    }

    override fun toString(): String {
        val unitDisplay = if (unit.isNeutral()) "" else " $unit"
        if (value.isInt() && unit.isNeutral()) return value.toInt().toString()
        if (abs(value.scientificNotation().exponent) <= 3) return value.toString() + unitDisplay
        if (value == Double.NEGATIVE_INFINITY) return "-oo$unitDisplay"
        if (value == Double.POSITIVE_INFINITY) return "+oo$unitDisplay"

        val (coefficient, exponent) = value.scientificNotation()

        val significantDigitsCount =
            if (isConstant)
                coefficient.significantDigitsCount() + (if (value.isInt()) -1 else 0)
            else
                significantDigitsCount

        val exactCoefficient =
            if (coefficient.significantDigitsCount() <= significantDigitsCount)
                (coefficient.toString() + "0".repeat(significantDigitsCount - coefficient.significantDigitsCount()))
            else
                coefficient.toString().dropLast(coefficient.significantDigitsCount() - significantDigitsCount)
                    .removeSuffix(".")

        return if (exponent == 0) "$exactCoefficient$unitDisplay" else "$exactCoefficient * 10^$exponent$unitDisplay"
    }

    fun isPositiveInfinity(): Boolean = value == Double.POSITIVE_INFINITY
    fun isNegativeInfinity(): Boolean = value == Double.NEGATIVE_INFINITY
    fun isInfinite(): Boolean = isPositiveInfinity() || isNegativeInfinity()
    fun isFinite(): Boolean = !isInfinite()
    fun isZero(): Boolean = value == 0.0
}
