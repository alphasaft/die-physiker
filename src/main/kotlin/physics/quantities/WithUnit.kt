package physics.quantities

import physics.quantities.units.PUnit
import kotlin.reflect.KClass


class WithUnit(override val unit: PUnit) : PDoubleOperand {
    constructor(unit: String): this(PUnit(unit))

    override val type: KClass<PDouble> = PDouble::class
    
    override fun toString(): String {
        return "<?> $unit"
    }

    override fun simpleUnion(quantity: Quantity<PDouble>): Quantity<PDouble> {
        return when {
            quantity is WithUnit && quantity.unit.isConvertibleInto(this.unit) -> this
            else -> QuantityUnion.assertReduced(this, quantity)
        }
    }

    override fun simpleIntersect(quantity: Quantity<PDouble>): Quantity<PDouble> {
        return when (quantity) {
            is WithUnit -> if (quantity.unit.isConvertibleInto(this.unit)) this else ImpossibleQuantity()
            else -> QuantityIntersection.assertReduced(this, quantity)
        }
    }

    override fun contains(value: PDouble): Boolean {
        return value.unit.isConvertibleInto(this.unit)
    }

    override fun equals(other: Any?): Boolean {
        return other is WithUnit && other.unit.isConvertibleInto(this.unit)
    }

    override fun hashCode(): Int {
        return 0  // TODO : No idea how to generate a good hashcode.
    }

    override fun simplify(): Quantity<PDouble> {
        return this
    }

    override fun plus(other: PDoubleOperand): Quantity<PDouble> {
        require(other.unit.isConvertibleInto(this.unit)) { "Can't add quantities whose units aren't interconvertible." }

        return this
    }

    override fun unaryMinus(): Quantity<PDouble> {
        return this
    }

    override fun times(other: PDoubleOperand): Quantity<PDouble> {
        return WithUnit(this.unit*other.unit)
    }

    override fun inv(): Quantity<PDouble> {
        return WithUnit(this.unit.inv())
    }

    override fun pow(other: PDoubleOperand): Quantity<PDouble> {
        return when {
            !other.unit.isNeutral() -> throw IllegalArgumentException("Can't write x^y if y has a dimension.")
            other is PDouble -> WithUnit(unit.pow(other.toDouble()))
            else -> AnyQuantity()
        }
    }
}
