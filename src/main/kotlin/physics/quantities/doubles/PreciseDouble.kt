package physics.quantities.doubles

import remove
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.abs as nativeAbs


class PreciseDouble(d: Double) : Comparable<PreciseDouble> {
    private companion object Util {
        fun suppressDwindlingDigits(d: Double): Double {
            var asString = d.toString()

            if (asString.split(".").last().length < 10) return d

            val splitOnE = asString.split("E")
            if (splitOnE.size > 1) {
                val (coefficient, exponent) = splitOnE.first() to splitOnE.last().toInt()
                asString = if (exponent < 0) {
                    ("0." + "0".repeat(-exponent - 1) + coefficient.remove(".")).removeSuffix("0")
                } else {
                    coefficient.remove(".") + "0".repeat(exponent-1)
                }
            }

            return if (asString.last().digitToInt() < 5) asString.dropLast(1).dropLastWhile { it in ".0" }.toDouble()
            else asString.dropLast(1).dropLastWhile { it in ".9" }.let { it.dropLast(1) + (it.last().digitToInt()+1) }.toDouble()
        }
    }

    private val storage = suppressDwindlingDigits(d)
    val significantDigitsCount get() = toString().remove("-").remove(".").length

    data class ScientificNotation(
        val coefficient: PreciseDouble,
        val exponent: Int
    )

    fun scientificNotation(): ScientificNotation {
        fun isFramedBetween1And10(value: PreciseDouble) = value >= PreciseDouble(1.0) && value < PreciseDouble(10.0)

        var coefficient = this
        var exponent = 0
        val action = when {
            this == PreciseDouble(0.0) -> return ScientificNotation(PreciseDouble(0.0), 0)
            this.abs() < PreciseDouble(1.0) -> fun() { coefficient *= PreciseDouble(10.0); exponent -= 1 }
            this.abs() >= PreciseDouble(10.0) -> fun() { coefficient /= PreciseDouble(10.0); exponent += 1 }
            else -> return ScientificNotation(coefficient, 0)
        }
        while (!isFramedBetween1And10(coefficient.abs())) action()
        return ScientificNotation(coefficient, exponent)
    }

    fun roundedAt(digit: Int): PreciseDouble {
        val rounded = (storage * 10.0.pow(-digit)).roundToInt().toDouble()
        return PreciseDouble(rounded * 10.0.pow(digit))
    }

    operator fun unaryMinus() = PreciseDouble(-storage)

    operator fun plus(y: Double) = PreciseDouble(storage + y)
    operator fun plus(y: PreciseDouble) = PreciseDouble(storage + y.storage)

    operator fun minus(y: Double) = PreciseDouble(storage - y)
    operator fun minus(y: PreciseDouble) = PreciseDouble(storage - y.storage)

    operator fun times(y: Double) = PreciseDouble(storage * y)
    operator fun times(y: PreciseDouble) = PreciseDouble(storage * y.storage)

    operator fun div(y: Double) = PreciseDouble(storage / y)
    operator fun div(y: PreciseDouble) = PreciseDouble(storage / y.storage)

    fun pow(y: Double) = PreciseDouble(storage.pow(y))
    fun pow(y: PreciseDouble) = PreciseDouble(storage.pow(y.storage))

    fun applyF(f: (Double) -> Double) = PreciseDouble(f(storage))
    fun abs(): PreciseDouble = applyF(::nativeAbs)

    fun toInt() = storage.toInt()
    fun toDouble() = storage

    fun isPositiveInfinity(): Boolean = storage == Double.POSITIVE_INFINITY
    fun isNegativeInfinity(): Boolean = storage == Double.NEGATIVE_INFINITY

    override fun compareTo(other: PreciseDouble): Int = storage.compareTo(other.storage)
    override fun equals(other: Any?): Boolean = other is PreciseDouble && storage == other.storage
    override fun toString(): String = storage.toString()
    override fun hashCode(): Int = storage.hashCode()
}
