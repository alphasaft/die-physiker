@file:OptIn(ExperimentalStdlibApi::class)

package physics.quantities.doubles

import buildArray
import physics.quantities.*
import physics.quantities.units.PUnit
import println
import kotlin.math.PI


class PRealInterval private constructor(
    isLowerBoundClosed: Boolean,
    lowerBound: PReal,
    upperBound: PReal,
    isUpperBoundClosed: Boolean
) : PInterval<PReal>(
    type = PReal::class,
    isLowerBoundClosed && !lowerBound.isNegativeInfinity(),
    lowerBound,
    upperBound,
    isUpperBoundClosed && !upperBound.isPositiveInfinity()
), PRealOperand {

    init {
        require(!lowerBound.isPositiveInfinity()) { "Can't specify +oo as lower bound." }
        require(!upperBound.isNegativeInfinity()) { "Can't specify -oo as upper bound." }
        require(lowerBound.isCompatibleWith(upperBound)) { "Bounds should be interconvertible." }
    }

    companion object Factory {
        fun new(
            isLowerBoundClosed: Boolean,
            lowerBound: PReal,
            upperBound: PReal,
            isUpperBoundClosed: Boolean
        ): Quantity<PReal> =
            PRealInterval(isLowerBoundClosed, lowerBound, upperBound, isUpperBoundClosed).simplify()

        fun newUsingBounds(vararg bounds: Pair<PReal, Boolean>): Quantity<PReal> {
            require(bounds.size >= 2) { "Expected at least two bounds." }

            val (lowerBound, isLowerBoundClosed) = bounds.minByOrNull { it.first }!!
            val (upperBound, isUpperBoundClosed) = bounds.maxByOrNull { it.first }!!

            return new(
                isLowerBoundClosed,
                lowerBound,
                upperBound,
                isUpperBoundClosed,
            )
        }

        fun fromPReal(value: PReal): PRealInterval = PRealInterval(
            isLowerBoundClosed = true,
            value,
            value,
            isUpperBoundClosed = true
        )
    }

    object Builtin {
        val positive = PRealInterval(
            isLowerBoundClosed = true,
            PReal(0.0),
            PReal(Double.POSITIVE_INFINITY),
            isUpperBoundClosed = false,
        )

        val negative = -positive

        val strictlyPositive = PRealInterval(
            isLowerBoundClosed = false,
            PReal(0.0),
            PReal(Double.POSITIVE_INFINITY),
            isUpperBoundClosed = false,
        )

        val strictlyNegative = -strictlyPositive

        val fromMinus1To1 = PRealInterval(
            isLowerBoundClosed = true,
            PReal(-1.0),
            PReal(1.0),
            isUpperBoundClosed = true
        )

        val fromMinusHalfPiToHalfPi = PRealInterval(
            isLowerBoundClosed = true,
            PReal(-PI/2),
            PReal(PI/2),
            isUpperBoundClosed = true
        )
    }

    override fun new(
        isLowerBoundClosed: Boolean,
        lowerBound: PReal,
        upperBound: PReal,
        isUpperBoundClosed: Boolean
    ) = Factory.new(
        isLowerBoundClosed,
        lowerBound,
        upperBound,
        isUpperBoundClosed
    )

    private fun containsZero() = PReal(0.0, Int.MAX_VALUE, unit = lowerBound.unit) in this
    private fun negativePart() = coerceUpperBound(PReal(0.0, Int.MAX_VALUE, unit = upperBound.unit))
    private fun positivePart() = coerceLowerBound(PReal(0.0, Int.MAX_VALUE, unit = lowerBound.unit))

    fun amplitude() = upperBound - lowerBound
    fun hasFiniteAmplitude() = amplitude().isFinite()
    fun hasInfiniteAmplitude() = !hasFiniteAmplitude()

    fun integersOnly(unit: PUnit): Iterable<PReal> {
        val first = lowerBound.convertInto(unit).let { (if (it.isInt() && isLowerBoundClosed) it else it+PReal(1.0, unit = unit)).floor() }

        return object : Iterable<PReal> {
            override fun iterator() = object : Iterator<PReal> {
                private var current = first

                override fun hasNext(): Boolean = current in this@PRealInterval
                override fun next(): PReal = current.also { current += PReal(1.0, unit = unit) }
            }
        }
    }

    override fun unaryMinus(): Quantity<PReal> = new(
        isUpperBoundClosed,
        -upperBound,
        -lowerBound,
        isLowerBoundClosed,
    )

    override fun plus(other: PRealOperand): Quantity<PReal> = when (other) {
        is PReal -> this + other
        is PRealInterval -> this + other
        else -> AnyQuantity()
    }

    private operator fun plus(other: PReal): Quantity<PReal> = new(
        isLowerBoundClosed,
        lowerBound + other,
        upperBound + other,
        isUpperBoundClosed,
    )

    private operator fun plus(other: PRealInterval): Quantity<PReal> = new(
        isLowerBoundClosed && other.isLowerBoundClosed,
        lowerBound + other.lowerBound,
        upperBound + other.upperBound,
        isUpperBoundClosed && other.isUpperBoundClosed,
    )

    override fun times(other: PRealOperand): Quantity<PReal> = when (other) {
        is PReal -> this * other
        is PRealInterval -> this * other
        else -> AnyQuantity()
    }

    private operator fun times(other: PReal): Quantity<PReal> = newUsingBounds(
        Pair(lowerBound * other, isLowerBoundClosed),
        Pair(upperBound * other, isUpperBoundClosed),
    )

    private operator fun times(other: PRealInterval): Quantity<PReal> = newUsingBounds(
        Pair(lowerBound * other.lowerBound, isLowerBoundClosed && other.isLowerBoundClosed),
        Pair(lowerBound * other.upperBound, isLowerBoundClosed && other.isUpperBoundClosed),
        Pair(upperBound * other.lowerBound, isUpperBoundClosed && other.isLowerBoundClosed),
        Pair(upperBound * other.upperBound, isUpperBoundClosed && other.isUpperBoundClosed),
    )

    override fun div(other: PRealOperand): Quantity<PReal> = when (other) {
        is PReal -> this / other
        is PRealInterval -> this / other
        else -> AnyQuantity()
    }

    private operator fun div(other: PReal): Quantity<PReal> = this * (PReal(1.0)/other)

    private operator fun div(other: PRealInterval): Quantity<PReal> {
        val positive = other.positivePart()
        val negative = other.negativePart()
        val dividedByPositivePart = divideByQuantityThatIncludes0OnlyAsBound(positive)
        val dividedByNegativePart = divideByQuantityThatIncludes0OnlyAsBound(negative)
        return dividedByNegativePart union dividedByPositivePart
    }

    private fun divideByQuantityThatIncludes0OnlyAsBound(quantity: Quantity<PReal>): Quantity<PReal> {
        return when (quantity) {
            is ImpossibleQuantity<PReal> -> ImpossibleQuantity()
            is PInterval<PReal> -> this * PRealInterval(
                isLowerBoundClosed = quantity.isUpperBoundClosed,
                lowerBound = (PReal(1.0)/quantity.upperBound).let { if (it.isPositiveInfinity()) -it else it },
                upperBound = (PReal(1.0)/quantity.lowerBound).let { if (it.isNegativeInfinity()) -it else it },
                isUpperBoundClosed = quantity.isLowerBoundClosed
            )
            else -> this / quantity
        }
    }

    override fun pow(other: PRealOperand): Quantity<PReal> {
        return when (other) {
            is PReal -> this.pow(other)
            is PRealInterval -> AnyQuantity()
            else -> AnyQuantity()
        }
    }

    private fun pow(other: PReal): Quantity<PReal> {
        return when {
            other.withoutUnit() < PReal(0) -> PReal(1)/(this.pow(-other))
            other.withoutUnit() == PReal(0) -> PReal(1)
            else -> {
                newUsingBounds(*buildArray {
                    add(Pair(lowerBound.pow(other), isLowerBoundClosed))
                    add(Pair(upperBound.pow(other), isUpperBoundClosed))
                    if (this@PRealInterval.containsZero()) add(Pair(PReal(0).withUnit(lowerBound.unit), true))
                })
            }
        }
    }

    private operator fun ((Double) -> Double).invoke(x: PReal) = x.applyFunction(this)

    fun applyMonotonousFunction(f: (Double) -> Double): Quantity<PReal> {
        return newUsingBounds(
            f(lowerBound) to isLowerBoundClosed,
            f(upperBound) to isUpperBoundClosed
        )
    }

    fun applyPeriodicalFunction(f: (Double) -> Double, t: PReal, extremasOnIntervalFrom0ToT: Map<PReal, PReal>): Quantity<PReal> {
        val offset = (lowerBound/t).floor()*t
        val withOffsetRemoved = (this - offset) as PRealInterval
        val withSizeReducedToTAtMost = withOffsetRemoved.coerceUpperBound(withOffsetRemoved.lowerBound+t) as PInterval
        val intervalToT = withSizeReducedToTAtMost.coerceUpperBound(t)
        val intervalFrom0 = withSizeReducedToTAtMost.coerceLowerBound(t) - t
        val completeInterval = intervalFrom0 union intervalToT

        val bounds = mutableSetOf<Pair<PReal, Boolean>>()

        fun addBounds(q: Quantity<PReal>) {
            when (q) {
                is PReal -> bounds.add(Pair(f(q), true))
                is PRealInterval -> {
                    bounds.add(Pair(f(q.lowerBound), q.isLowerBoundClosed))
                    bounds.add(Pair(f(q.upperBound), q.isUpperBoundClosed))
                }
                else -> {}
            }
        }

        addBounds(intervalFrom0)
        addBounds(intervalToT)

        for ((extremaX, extremaY) in extremasOnIntervalFrom0ToT) {
            if (extremaX in completeInterval) {
                bounds.add(Pair(extremaY, true))
            }
        }

        return newUsingBounds(*bounds.toTypedArray())
    }
}
