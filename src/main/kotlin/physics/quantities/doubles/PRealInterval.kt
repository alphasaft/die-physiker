package physics.quantities.doubles

import physics.quantities.AnyQuantity
import physics.quantities.ImpossibleQuantity
import physics.quantities.PInterval
import physics.quantities.Quantity
import kotlin.math.PI
import kotlin.reflect.KClass


class PRealInterval private constructor(
    isLowerBoundClosed: Boolean,
    lowerBound: PReal,
    upperBound: PReal,
    isUpperBoundClosed: Boolean
) : PInterval<PReal>(
    type = PReal::class,
    isLowerBoundClosed && !lowerBound.value.isNegativeInfinity(),
    lowerBound,
    upperBound,
    isUpperBoundClosed && !upperBound.value.isPositiveInfinity()
), PRealOperand {

    init {
        require(!lowerBound.value.isPositiveInfinity()) { "Can't specify +oo as lower bound." }
        require(!upperBound.value.isNegativeInfinity()) { "Can't specify -oo as upper bound." }
        require(lowerBound.isCompatibleWith(upperBound)) { "Bounds should be interconvertible." }
    }

    override val type: KClass<PReal> = PReal::class

    companion object Factory {
        fun new(
            isLowerBoundClosed: Boolean,
            lowerBound: PReal,
            upperBound: PReal,
            isUpperBoundClosed: Boolean
        ): Quantity<PReal> =
            PRealInterval(isLowerBoundClosed, lowerBound, upperBound, isUpperBoundClosed).simplify()

        fun withOrderedBounds(
            isBound1Closed: Boolean,
            bound1: PReal,
            bound2: PReal,
            isBound2Closed: Boolean,
        ) = PRealInterval(
            if (bound1 <= bound2) isBound1Closed else isBound2Closed,
            if (bound1 <= bound2) bound1 else bound2,
            if (bound2 >= bound1) bound2 else bound1,
            if (bound2 >= bound1) isBound2Closed else isBound1Closed,
        ).simplify()

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

    private operator fun times(other: PReal): Quantity<PReal> = withOrderedBounds(
        isLowerBoundClosed,
        lowerBound * other,
        upperBound * other,
        isUpperBoundClosed,
    )

    private operator fun times(other: PRealInterval): Quantity<PReal> {
        val possibleBounds = listOf(
            Pair(lowerBound * other.lowerBound, isLowerBoundClosed && other.isLowerBoundClosed),
            Pair(lowerBound * other.upperBound, isLowerBoundClosed && other.isUpperBoundClosed),
            Pair(upperBound * other.lowerBound, isUpperBoundClosed && other.isLowerBoundClosed),
            Pair(upperBound * other.upperBound, isUpperBoundClosed && other.isUpperBoundClosed),
        )

        val (lowerBound, isLowerBoundClosed) = possibleBounds.minByOrNull { it.first }!!
        val (upperBound, isUpperBoundClosed) = possibleBounds.maxByOrNull { it.first }!!

        return new(
            isLowerBoundClosed,
            lowerBound,
            upperBound,
            isUpperBoundClosed
        )
    }

    override fun div(other: PRealOperand): Quantity<PReal> = when (other) {
        is PReal -> this / other
        is PRealInterval -> this / other
        else -> AnyQuantity()
    }

    private operator fun div(other: PReal): Quantity<PReal> = withOrderedBounds(
        isLowerBoundClosed,
        lowerBound / other,
        upperBound / other,
        isUpperBoundClosed,
    )

    private operator fun div(other: PRealInterval): Quantity<PReal> {
        val positive = other.coerceLowerBound(PReal(0.0))
        val negative = other.coerceUpperBound(PReal(0.0))
        val dividedByPositivePart = divideByQuantityThatIncludes0OnlyAsBound(positive)
        val dividedByNegativePart = divideByQuantityThatIncludes0OnlyAsBound(negative)
        return dividedByNegativePart union dividedByPositivePart
    }

    private fun divideByQuantityThatIncludes0OnlyAsBound(quantity: Quantity<PReal>): Quantity<PReal> {
        return when (quantity) {
            is ImpossibleQuantity<PReal> -> ImpossibleQuantity()
            is PInterval<PReal> -> this * PRealInterval(
                isLowerBoundClosed = quantity.isUpperBoundClosed,
                lowerBound = PReal(1.0)/quantity.upperBound,
                upperBound = PReal(1.0)/quantity.lowerBound,
                isUpperBoundClosed = quantity.isLowerBoundClosed
            )
            else -> this / quantity
        }
    }

    override fun pow(other: PRealOperand): Quantity<PReal> {
        TODO("Not yet implemented")
        // x ^ y == exp( y * ln(x) ), I guess it's continuous in y (and in x but this is well-known)
    }

    override fun applyContinuousFunction(f: MathFunction): Quantity<PReal> {
        TODO("Not yet implemented")
    }
}
