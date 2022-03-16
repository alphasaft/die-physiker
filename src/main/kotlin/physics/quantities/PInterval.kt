package physics.quantities

import kotlin.reflect.KClass


abstract class PInterval<T>(
    override val type: KClass<T>,
    val isLowerBoundClosed: Boolean,
    val lowerBound: T,
    val upperBound: T,
    val isUpperBoundClosed: Boolean,
) : Quantity<T> where T : PValue<T>, T : Comparable<T> {

    protected abstract fun new(
        isLowerBoundClosed: Boolean,
        lowerBound: T,
        upperBound: T,
        isUpperBoundClosed: Boolean
    ): Quantity<T>

    override fun stdIntersect(quantity: Quantity<T>): Quantity<T> {
        return when (quantity) {
            is PInterval<T> -> intersectWithInterval(quantity)
            else -> QuantityIntersection.assertReduced(type, this, quantity)
        }
    }

    private fun intersectWithInterval(interval: PInterval<T>): Quantity<T> {
        val (lowerInterval, upperInterval) = listOf(this, interval).sortedBy { it.lowerBound }
        val lowerBound = upperInterval.lowerBound
        val upperBound = lowerInterval.upperBound

        val isLowerBoundClosed =
            if (lowerBound == lowerInterval.lowerBound) lowerInterval.isLowerBoundClosed && upperInterval.isLowerBoundClosed
            else upperInterval.isLowerBoundClosed

        val isUpperBoundClosed =
            if (upperBound == upperInterval.upperBound) upperInterval.isUpperBoundClosed && lowerInterval.isUpperBoundClosed
            else lowerInterval.isUpperBoundClosed

        return new(
            isLowerBoundClosed,
            lowerBound,
            upperBound,
            isUpperBoundClosed
        )
    }

    override fun stdUnion(quantity: Quantity<T>): Quantity<T> {
        return when (quantity) {
            is PInterval<T> -> unionWithInterval(quantity)
            else -> QuantityUnion.assertReduced(type, this, quantity)
        }
    }

    private fun unionWithInterval(interval: PInterval<T>): Quantity<T> {
        if (this stdIntersect interval is ImpossibleQuantity<*>) return QuantityUnion.assertReduced(type, this, interval)

        val (lowerInterval, upperInterval) = listOf(this, interval).sortedBy { it.lowerBound }

        val lowerBound = lowerInterval.lowerBound
        val upperBound = upperInterval.upperBound

        val isLowerBoundClosed =
            if (lowerBound == lowerInterval.lowerBound) lowerInterval.isLowerBoundClosed && upperInterval.isLowerBoundClosed
            else upperInterval.isLowerBoundClosed

        val isUpperBoundClosed =
            if (upperBound == upperInterval.upperBound) upperInterval.isUpperBoundClosed && lowerInterval.isUpperBoundClosed
            else lowerInterval.isUpperBoundClosed

        return new(
            isLowerBoundClosed,
            lowerBound,
            upperBound,
            isUpperBoundClosed
        )
    }

    fun coerceLowerBound(min: T): Quantity<T> {
        return if (lowerBound < min) new(
            isLowerBoundClosed = true,
            min,
            upperBound,
            isUpperBoundClosed,
        ) else this
    }

    fun coerceUpperBound(max: T): Quantity<T> {
        return if (upperBound > max) new(
            isLowerBoundClosed,
            lowerBound,
            max,
            isUpperBoundClosed = true
        ) else this
    }

    override fun simplify(): Quantity<T> = when {
        lowerBound > upperBound -> ImpossibleQuantity(type)
        lowerBound == upperBound && !(isLowerBoundClosed && isUpperBoundClosed) -> ImpossibleQuantity(type)
        lowerBound == upperBound && (isLowerBoundClosed && isUpperBoundClosed) -> lowerBound
        else -> this
    }

    override fun contains(value: T): Boolean = when (value) {
        lowerBound -> isLowerBoundClosed
        upperBound -> isUpperBoundClosed
        in lowerBound..upperBound -> true
        else -> false
    }

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append(if (isLowerBoundClosed) "[" else "]")
        builder.append(lowerBound)
        builder.append(" ; ")
        builder.append(upperBound)
        builder.append(if (isUpperBoundClosed) "]" else "[")
        return builder.toString()
    }

    override fun equals(other: Any?): Boolean {
        return other is PInterval<*>
                && (!isLowerBoundClosed xor other.isLowerBoundClosed)
                && (!isUpperBoundClosed xor other.isUpperBoundClosed)
                && (lowerBound == other.lowerBound)
                && (upperBound == other.upperBound)
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + isLowerBoundClosed.hashCode()
        result = 31 * result + lowerBound.hashCode()
        result = 31 * result + upperBound.hashCode()
        result = 31 * result + isUpperBoundClosed.hashCode()
        return result
    }
}