package physics.quantities.doubles

import physics.UnitException
import physics.quantities.ImpossibleQuantity
import physics.quantities.PValue
import physics.quantities.Quantity
import physics.values.units.PUnit
import physics.quantities.booleans.PBoolean
import physics.quantities.ints.PInt
import physics.quantities.strings.PString
import remove
import kotlin.math.*
import kotlin.reflect.KClass


class PReal internal constructor(
    private val exactValue: PreciseDouble,
    val significantDigitsCount: Int,
    val unit: PUnit,
) : PValue<PReal>(), PRealOperand, Comparable<PReal> {
    constructor(exactValue: Double, significantDigitsCount: Int, unit: PUnit): this(PreciseDouble(exactValue), significantDigitsCount, unit)
    constructor(value: PreciseDouble): this(value, Int.MAX_VALUE, PUnit())
    constructor(value: Double): this(PreciseDouble(value))

    companion object Factory {
        private val physicalDoubleStringFormatRegex = run {
            val ws = "\\s*"
            val coefficient = "-?\\d+(.\\d+)?"
            val exponent = "(\\*${ws}10$ws\\^|E)$ws(-?\\d+)"
            val unit = "\\w+(.\\w+(-?\\d+)?)*"
            Regex("($coefficient)$ws($exponent)?$ws($unit)?$ws")
        }

        operator fun invoke(value: String): PReal {
            val match = physicalDoubleStringFormatRegex.matchEntire(value) ?: throw NumberFormatException("Invalid value : $value")
            val (coefficient, _, _, _, exponent, unit) = match.destructured
            return PReal(
                PreciseDouble(coefficient.toDouble() * 10.0.pow(exponent.ifEmpty { "0" }.toInt())),
                coefficient.remove("-").remove(".").dropWhile { it == '0' }.length,
                PUnit(unit),
            )
        }
    }

    override val type: KClass<PReal> = PReal::class

    private val lastAccurateDigit = -significantDigitsCount + exactValue.scientificNotation().exponent + 1
    private val isConstant = significantDigitsCount == Int.MAX_VALUE

    val value = if (isConstant) exactValue else exactValue.roundedAt(lastAccurateDigit)

    override fun toPBoolean(): PBoolean = PBoolean(exactValue != PreciseDouble(0.0))
    override fun toPInt() = PInt(exactValue.toInt())
    override fun toPReal() = this
    override fun toPString(): PString = PString(value.toString())

    fun toInterval() = PRealInterval.fromPReal(this)
    fun toDouble() = value.toDouble()
    fun toInt() = value.toInt()

    override fun minus(other: PRealOperand): Quantity<PReal> = this + (-other)
    operator fun minus(other: PReal): PReal = this + (-other)
    
    override fun unaryMinus() = PReal(-exactValue, significantDigitsCount, unit)

    override fun plus(other: PRealOperand): Quantity<PReal> = if (other is PReal) this + other else toInterval() + other
    operator fun plus(other: PReal): PReal {
        val sum = exactValue + other.exactValue
        val (_, exponent) = sum.scientificNotation()
        val significantDigitsCount = when {
            this.isConstant && other.isConstant -> Int.MAX_VALUE
            this.isConstant -> exponent - other.lastAccurateDigit
            other.isConstant -> exponent - this.lastAccurateDigit
            else -> (exponent+1) - maxOf(this.lastAccurateDigit, other.lastAccurateDigit)
        }
        return PReal(sum, significantDigitsCount, unit+other.unit)
    }

    override fun times(other: PRealOperand): Quantity<PReal> = if (other is PReal) this * other else toInterval() * other
    operator fun times(other: PReal): PReal = PReal(
        exactValue * other.exactValue,
        min(significantDigitsCount, other.significantDigitsCount),
        unit*other.unit
    )

    override fun div(other: PRealOperand): Quantity<PReal> = if (other is PReal) this / other else toInterval() / other
    operator fun div(other: PReal): PReal = PReal(
        exactValue / other.exactValue,
        min(significantDigitsCount, other.significantDigitsCount),
        unit/other.unit
    )

    override fun pow(other: PRealOperand): Quantity<PReal> = if (other is PReal) this.pow(other) else toInterval().pow(other)
    fun pow(other: PReal): PReal = PReal(
        exactValue.pow(other.exactValue),
        min(significantDigitsCount, other.significantDigitsCount),
        unit
    )

    override fun applyContinuousFunction(f: MathFunction): Quantity<PReal> {
        return if (this !in f.inDomain) ImpossibleQuantity()
        else PReal(
            exactValue.applyF { f(it) },
            significantDigitsCount,
            PUnit()
        )
    }

    fun isCompatibleWith(other: PReal): Boolean {
        return unit.isConvertibleInto(other.unit)
    }

    fun isInt(): Boolean {
        return value.toInt().toDouble() == value.toDouble()
    }

    fun convertInto(unit: PUnit): PReal {
        if (unit == this.unit) return this
        return PReal(
            this.unit.convert(exactValue, into = unit) ?: throw UnitException("Unit ${this.unit} cannot be converted into $unit."),
            significantDigitsCount,
            unit
        )
    }

    operator fun compareTo(other: Int) = value.compareTo(PreciseDouble(other.toDouble()))
    operator fun compareTo(other: Double) = value.compareTo(PreciseDouble(other))
    override operator fun compareTo(other: PReal): Int = this.value.compareTo(other.convertInto(this.unit).value)

    override fun equals(other: Any?): Boolean {
        return other is PReal
                && other.value == this.value
                && other.unit == this.unit
    }

    private infix fun roughlyEquals(other: PReal): Boolean {
        if (this == other) return true
        if (this.unit != other.unit) return false
        return ((exactValue + other.exactValue)/(exactValue - other.exactValue)).abs() > PreciseDouble(1000.0)
    }

    override fun hashCode(): Int {
        var result = exactValue.hashCode()
        result = 31 * result + significantDigitsCount
        result = 31 * result + unit.hashCode()
        return result
    }

    override fun toString(): String {
        if (isConstant && value.toInt().toDouble() == value.toDouble()) return value.toInt().toString()
        if (value.isNegativeInfinity()) return "-oo"
        if (value.isPositiveInfinity()) return "+oo"

        val (coefficient, exponent) = value.scientificNotation()
        val significantDigitsCount = if (isConstant) coefficient.significantDigitsCount + if (value.toInt().toDouble() == value.toDouble()) -1 else 0 else significantDigitsCount

        val unitDisplay = if (unit.isNeutral()) "" else " $unit"
        val exactCoefficient =
            if (coefficient.significantDigitsCount <= significantDigitsCount)
                (coefficient.toString() + "0".repeat(significantDigitsCount-coefficient.significantDigitsCount))
            else
                coefficient.toString().dropLast(coefficient.significantDigitsCount - significantDigitsCount).removeSuffix(".")

        return if (exponent == 0) exactCoefficient else "$exactCoefficient * 10^$exponent$unitDisplay"
    }

}
