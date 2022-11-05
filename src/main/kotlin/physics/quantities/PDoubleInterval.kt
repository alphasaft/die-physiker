@file:OptIn(ExperimentalStdlibApi::class)

package physics.quantities

import buildArray
import physics.quantities.units.PUnit
import kotlin.math.PI


class PDoubleInterval private constructor(
    isLowerBoundClosed: Boolean,
    lowerBound: PDouble,
    upperBound: PDouble,
    isUpperBoundClosed: Boolean
) : PInterval<PDouble>(
    type = PDouble::class,
    isLowerBoundClosed && !lowerBound.isNegativeInfinity(),
    lowerBound,
    upperBound,
    isUpperBoundClosed && !upperBound.isPositiveInfinity()
), PDoubleOperand {

    init {
        require(!lowerBound.isPositiveInfinity()) { "Can't specify +oo as lower bound." }
        require(!upperBound.isNegativeInfinity()) { "Can't specify -oo as upper bound." }
        require(lowerBound.isCompatibleWith(upperBound)) { "Bounds should be interconvertible." }
    }

    override val unit: PUnit = upperBound.unit

    companion object Factory {
        fun raw(
            isLowerBoundClosed: Boolean,
            lowerBound: PDouble,
            upperBound: PDouble,
            isUpperBoundClosed: Boolean
        ): PDoubleInterval = PDoubleInterval(isLowerBoundClosed, lowerBound, upperBound, isUpperBoundClosed)

        fun new(
            isLowerBoundClosed: Boolean,
            lowerBound: PDouble,
            upperBound: PDouble,
            isUpperBoundClosed: Boolean
        ): Quantity<PDouble> = raw(isLowerBoundClosed, lowerBound, upperBound, isUpperBoundClosed).simplify()

        fun newUsingBounds(vararg bounds: Pair<PDouble, Boolean>): Quantity<PDouble> {
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

        fun fromPReal(value: PDouble): PDoubleInterval = PDoubleInterval(
            isLowerBoundClosed = true,
            value,
            value,
            isUpperBoundClosed = true
        )
    }

    object Builtin {
        val positive = PDoubleInterval(
            isLowerBoundClosed = true,
            PDouble(0.0),
            PDouble(Double.POSITIVE_INFINITY),
            isUpperBoundClosed = false,
        )

        val negative = -positive

        val strictlyPositive = PDoubleInterval(
            isLowerBoundClosed = false,
            PDouble(0.0),
            PDouble(Double.POSITIVE_INFINITY),
            isUpperBoundClosed = false,
        )

        val strictlyNegative = -strictlyPositive

        val fromMinus1To1 = PDoubleInterval(
            isLowerBoundClosed = true,
            PDouble(-1.0),
            PDouble(1.0),
            isUpperBoundClosed = true
        )

        val fromMinusHalfPiToHalfPi = PDoubleInterval(
            isLowerBoundClosed = true,
            PDouble(-PI / 2),
            PDouble(PI / 2),
            isUpperBoundClosed = true
        )
    }

    override fun new(
        isLowerBoundClosed: Boolean,
        lowerBound: PDouble,
        upperBound: PDouble,
        isUpperBoundClosed: Boolean
    ) = Factory.new(
        isLowerBoundClosed,
        lowerBound,
        upperBound,
        isUpperBoundClosed
    )


    val amplitude = upperBound - lowerBound

    private fun strictlyNegativePart() =
        coerceUpperBound(PDouble(0.0, Int.MAX_VALUE, unit = upperBound.unit), closed = false)

    private fun positivePart() = coerceLowerBound(PDouble(0.0, Int.MAX_VALUE, unit = lowerBound.unit), closed = true)

    fun hasFiniteAmplitude() = amplitude.isFinite()
    fun hasInfiniteAmplitude() = !hasFiniteAmplitude()

    fun containsZero() = PDouble(0.0, Int.MAX_VALUE, unit = lowerBound.unit) in this
    fun hasStrictlyNegativePart() = strictlyNegativePart() !is ImpossibleQuantity
    fun hasPositivePart() = positivePart() !is ImpossibleQuantity

    fun convertInto(unit: PUnit) = PDoubleInterval(
        isLowerBoundClosed,
        lowerBound.convertInto(unit),
        upperBound.convertInto(unit),
        isUpperBoundClosed
    )

    override fun simplify(): Quantity<PDouble> {
        if (lowerBound.isNegativeInfinity() && upperBound.isPositiveInfinity()) return AnyQuantity()
        return super.simplify()
    }

    fun integersOnly(unit: PUnit): Iterable<PDouble> {
        val first = lowerBound.convertInto(unit)
            .let { (if (it.isInt() && isLowerBoundClosed) it else it + PDouble(1.0, unit = unit)).floor() }

        return object : Iterable<PDouble> {
            override fun iterator() = object : Iterator<PDouble> {
                private var current = first

                override fun hasNext(): Boolean = current in this@PDoubleInterval
                override fun next(): PDouble = current.also { current += PDouble(1.0, unit = unit) }
            }
        }
    }

    override fun unaryMinus(): Quantity<PDouble> = new(
        isUpperBoundClosed,
        -upperBound,
        -lowerBound,
        isLowerBoundClosed,
    )

    override fun plus(other: PDoubleOperand): Quantity<PDouble> = when (other) {
        is PDouble -> this + other
        is PDoubleInterval -> this + other
        else -> AnyQuantity()
    }

    private operator fun plus(other: PDouble): Quantity<PDouble> = new(
        isLowerBoundClosed,
        lowerBound + other,
        upperBound + other,
        isUpperBoundClosed,
    )

    private operator fun plus(other: PDoubleInterval): Quantity<PDouble> = new(
        isLowerBoundClosed && other.isLowerBoundClosed,
        lowerBound + other.lowerBound,
        upperBound + other.upperBound,
        isUpperBoundClosed && other.isUpperBoundClosed,
    )

    override fun times(other: PDoubleOperand): Quantity<PDouble> = when (other) {
        is PDouble -> this * other
        is PDoubleInterval -> this * other
        else -> AnyQuantity()
    }

    private operator fun times(other: PDouble): Quantity<PDouble> = newUsingBounds(
        Pair(lowerBound * other, isLowerBoundClosed),
        Pair(upperBound * other, isUpperBoundClosed),
    )

    private operator fun times(other: PDoubleInterval): Quantity<PDouble> = newUsingBounds(
        Pair(lowerBound * other.lowerBound, isLowerBoundClosed && other.isLowerBoundClosed),
        Pair(lowerBound * other.upperBound, isLowerBoundClosed && other.isUpperBoundClosed),
        Pair(upperBound * other.lowerBound, isUpperBoundClosed && other.isLowerBoundClosed),
        Pair(upperBound * other.upperBound, isUpperBoundClosed && other.isUpperBoundClosed),
    )

    override fun inv(): Quantity<PDouble> {
        return when {
            containsZero() -> QuantityUnion.assertReduced(
                PDoubleInterval(
                    isLowerBoundClosed = false,
                    lowerBound = PDouble(Double.NEGATIVE_INFINITY, Int.MAX_VALUE, unit = unit.inv()),
                    upperBound = lowerBound.inv(),
                    isUpperBoundClosed = isLowerBoundClosed
                ),
                PDoubleInterval(
                    isLowerBoundClosed = isUpperBoundClosed,
                    lowerBound = upperBound.inv(),
                    upperBound = PDouble(Double.POSITIVE_INFINITY, Int.MAX_VALUE, unit = unit.inv()),
                    isUpperBoundClosed = false
                )
            )
            else -> {
                newUsingBounds(
                    lowerBound.inv() to isLowerBoundClosed,
                    upperBound.inv() to isUpperBoundClosed,
                )
            }
        }
    }

    override fun pow(other: PDoubleOperand): Quantity<PDouble> {
        return when (other) {
            is PDouble -> this.pow(other)
            is PDoubleInterval -> AnyQuantity()
            else -> AnyQuantity()
        }
    }

    private fun pow(other: PDouble): Quantity<PDouble> {
        if (other < PDouble(1) && strictlyNegativePart() !is ImpossibleQuantity) return positivePart().pow(other)

        return when {
            other.withoutUnit() < PDouble(0) -> PDouble(1) / (this.pow(-other))
            other.isZero() -> PDouble(1)
            else -> {
                newUsingBounds(*buildArray {
                    add(Pair(lowerBound.pow(other), isLowerBoundClosed))
                    add(Pair(upperBound.pow(other), isUpperBoundClosed))
                    if (this@PDoubleInterval.containsZero()) add(Pair(lowerBound.pow(other) * PDouble(0), true))
                })
            }
        }
    }

    operator fun rem(period: PDouble): PDoubleInterval {
        val one = PDouble(1).withUnit(period.unit)
        return PDoubleInterval(
            isLowerBoundClosed,
            lowerBound % period,
            (upperBound - lowerBound) / one + lowerBound % period,
            isUpperBoundClosed
        )
    }

    private operator fun ((Double) -> Double).invoke(x: PDouble) = x.applyFunction(this)

    fun applyMonotonousFunction(f: (Double) -> Double): Quantity<PDouble> {
        return newUsingBounds(
            f(lowerBound) to isLowerBoundClosed,
            f(upperBound) to isUpperBoundClosed
        )
    }

    fun applyPeriodicalFunction(
        f: (Double) -> Double,
        t: PDouble,
        extremasOnIntervalFrom0ToT: Map<PDouble, PDouble>
    ): Quantity<PDouble> {
        val withOffsetRemoved = this % t
        val withSizeReducedToTAtMost = withOffsetRemoved.coerceUpperBound(withOffsetRemoved.lowerBound + t, closed = false) as PInterval
        val intervalToT = withSizeReducedToTAtMost.coerceUpperBound(t, closed = true)
        val intervalFrom0 = withSizeReducedToTAtMost.coerceLowerBound(t, closed = true) - t
        val completeInterval = intervalFrom0 union intervalToT

        val bounds = mutableSetOf<Pair<PDouble, Boolean>>()

        fun addBounds(q: Quantity<PDouble>) {
            when (q) {
                is PDouble -> bounds.add(Pair(f(q), true))
                is PDoubleInterval -> {
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
