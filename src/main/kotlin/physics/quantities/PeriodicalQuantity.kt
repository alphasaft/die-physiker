package physics.quantities

import physics.quantities.doubles.plus
import physics.quantities.doubles.times
import kotlin.reflect.KClass

class PeriodicalQuantity private constructor(
    interval: PRealInterval,
    private val period: PReal,
) : Quantity<PReal> {
    companion object Factory {
        private class AffineFunction(val a: PReal, val b: PReal) : Function {
            override val outDomain: Quantity<PReal> = AnyQuantity()
            override val reciprocal: Function get() = AffineFunction(a = PReal(1) / a, b = -b / a)
            override fun invoke(x: String): String = "$a$x+$b"
            override fun invoke(x: PReal): PReal = a * x + b
            override fun invokeExhaustively(x: Quantity<PReal>): Quantity<PReal> = a * x + b
        }

        fun new(quantity: Quantity<PReal>, period: PReal): Quantity<PReal> {
            return when (val simplifiedQuantity = quantity.simplify()) {
                is AnyQuantity -> AnyQuantity()
                is ImpossibleQuantity -> ImpossibleQuantity()
                is PReal -> IntegersComprehension(simplifiedQuantity.unit, IntegersComprehension.InDomain.Z, AffineFunction(a = period, b = simplifiedQuantity))
                is PRealInterval -> if (simplifiedQuantity.amplitude > period) AnyQuantity() else PeriodicalQuantity(simplifiedQuantity, period)
                is QuantityUnion -> simplifiedQuantity.mapItems { new(it, period) }
                is QuantityIntersection -> simplifiedQuantity.mapItems { new(it, period) }
                else -> AnyQuantity()
            }
        }
    }

    override val type: KClass<PReal> = PReal::class
    private val standardizedInterval: PRealInterval = interval % period

    override fun stdIntersect(quantity: Quantity<PReal>): Quantity<PReal> {
        return when (val simplifiedQuantity = quantity.simplify()) {
            is PRealInterval -> this stdIntersect simplifiedQuantity
            is PeriodicalQuantity -> this stdIntersect simplifiedQuantity
            else -> QuantityIntersection.assertReduced(this, simplifiedQuantity)
        }
    }

    private infix fun stdIntersect(quantity: PRealInterval): Quantity<PReal> {
        val lb = quantity.lowerBound
        var lowerBoundOfCurrentInterval = lb - (lb % period)
        var asUnion = QuantityUnion.new<PReal>()
        while (lowerBoundOfCurrentInterval < quantity.upperBound) {
            asUnion = asUnion union (standardizedInterval + lowerBoundOfCurrentInterval)
            lowerBoundOfCurrentInterval += period
        }
        return asUnion intersect quantity
    }

    private infix fun stdIntersect(quantity: PeriodicalQuantity): Quantity<PReal> {
        return when {
            (period / quantity.period).isInt() -> new(this.standardizedInterval intersect quantity, period)
            (quantity.period / period).isInt() -> new(this.standardizedInterval intersect quantity, quantity.period)
            else -> QuantityIntersection.assertReduced(this, quantity)
        }
    }

    override fun stdUnion(quantity: Quantity<PReal>): Quantity<PReal> {
        return when {
            quantity includedIn this -> this
            this includedIn quantity -> quantity
            else -> QuantityUnion.assertReduced(this, quantity)
        }
    }

    override fun contains(value: PReal): Boolean {
        val centeredValue = value % period
        val leftCenteredValue = centeredValue - period
        return centeredValue in standardizedInterval || leftCenteredValue in standardizedInterval
    }

    override fun simplify(): Quantity<PReal> {
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
