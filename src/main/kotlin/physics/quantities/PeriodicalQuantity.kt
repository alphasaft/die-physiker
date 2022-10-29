package physics.quantities

import kotlin.reflect.KClass

class PeriodicalQuantity private constructor(
    interval: PRealInterval,
    private val period: PDouble,
) : PRealOperand {
    companion object Factory {
        private class AffineFunction(val a: PDouble, val b: PDouble) : Function {
            override val outDomain: Quantity<PDouble> = AnyQuantity()
            override val reciprocal: Function get() = AffineFunction(a = PDouble(1) / a, b = -b / a)
            override fun invoke(x: String): String = "$a$x+$b"
            override fun invoke(x: PDouble): PDouble = a * x + b
            override fun invokeExhaustively(x: Quantity<PDouble>): Quantity<PDouble> = a * x + b
        }

        fun new(quantity: Quantity<PDouble>, period: PDouble): Quantity<PDouble> {
            return when (val simplifiedQuantity = quantity.simplify()) {
                is AnyQuantity -> AnyQuantity()
                is ImpossibleQuantity -> ImpossibleQuantity()
                is PDouble -> IntegersComprehension(simplifiedQuantity.unit, IntegersComprehension.InDomain.Z, AffineFunction(a = period, b = simplifiedQuantity))
                is PRealInterval -> if (simplifiedQuantity.amplitude > period) AnyQuantity() else PeriodicalQuantity(simplifiedQuantity, period)
                is QuantityUnion -> simplifiedQuantity.mapItems { new(it, period) }
                is QuantityIntersection -> simplifiedQuantity.mapItems { new(it, period) }
                else -> AnyQuantity()
            }
        }
    }

    override val type: KClass<PDouble> = PDouble::class
    private val standardizedInterval: PRealInterval = interval % period

    override fun simpleIntersect(quantity: Quantity<PDouble>): Quantity<PDouble> {
        return when (val simplifiedQuantity = quantity.simplify()) {
            is PRealInterval -> this stdIntersect simplifiedQuantity
            is PeriodicalQuantity -> this stdIntersect simplifiedQuantity
            else -> QuantityIntersection.assertReduced(this, simplifiedQuantity)
        }
    }

    // -(-0.3, 0.7, 1.7, ...)

    private infix fun stdIntersect(quantity: PRealInterval): Quantity<PDouble> {
        val lb = quantity.lowerBound
        var lowerBoundOfCurrentInterval = lb - (lb % period)
        var asUnion = QuantityUnion.new<PDouble>()
        while (lowerBoundOfCurrentInterval < quantity.upperBound) {
            asUnion = asUnion union (standardizedInterval + lowerBoundOfCurrentInterval)
            lowerBoundOfCurrentInterval += period
        }
        return asUnion intersect quantity
    }

    override fun plus(other: PRealOperand): Quantity<PDouble> {
        return new(standardizedInterval + other, period)
    }

    override fun times(other: PRealOperand): Quantity<PDouble> {
        if (other is PDouble) {
            if (other.isZero()) return other
            return new(standardizedInterval * other, period*other)
        }
        return AnyQuantity()

    }

    override fun pow(other: PRealOperand): Quantity<PDouble> {
        return AnyQuantity()
    }

    override fun div(other: PRealOperand): Quantity<PDouble> {
        if (other !is PDouble) return AnyQuantity()
        return new(standardizedInterval/other, period/other)
    }

    override fun unaryMinus(): Quantity<PDouble> {
        return new(-standardizedInterval, period)
    }

    private infix fun stdIntersect(quantity: PeriodicalQuantity): Quantity<PDouble> {
        return when {
            (period / quantity.period).isInt() -> new(this.standardizedInterval intersect quantity, period)
            (quantity.period / period).isInt() -> new(this.standardizedInterval intersect quantity, quantity.period)
            else -> QuantityIntersection.assertReduced(this, quantity)
        }
    }

    override fun simpleUnion(quantity: Quantity<PDouble>): Quantity<PDouble> {
        return when {
            quantity includedIn this -> this
            this includedIn quantity -> quantity
            else -> QuantityUnion.assertReduced(this, quantity)
        }
    }

    override fun contains(value: PDouble): Boolean {
        val rightCenteredValue = value % period
        val leftCenteredValue = rightCenteredValue - period
        return rightCenteredValue in standardizedInterval || leftCenteredValue in standardizedInterval
    }

    override fun simplify(): Quantity<PDouble> {
        return this
    }

    override fun equals(other: Any?): Boolean {
        return other is PeriodicalQuantity
                && standardizedInterval == other.standardizedInterval
                && period == other.period
    }

    override fun hashCode(): Int {
        var result = standardizedInterval.hashCode()
        result = result * 31 + period.hashCode()
        return result
    }

    override fun toString(): String {
        return "{ x ~ $standardizedInterval mod. $period }"
    }
}
