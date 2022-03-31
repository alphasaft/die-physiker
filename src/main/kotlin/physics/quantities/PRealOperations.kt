@file:Suppress("UNCHECKED_CAST")

package physics.quantities.doubles

import physics.quantities.*
import physics.quantities.Function

fun Quantity<PReal>.applyFunction(f: Function): Quantity<PReal> =
    when (this) {
        is ImpossibleQuantity<*> -> ImpossibleQuantity()
        is AnyQuantity<*> -> f.outDomain
        is QuantityUnion<*> -> QuantityUnion.new(items.map { (it as Quantity<PReal>).applyFunction(f) })
        is PRealOperand -> f.invokeExhaustively(this)
        else -> f.outDomain
    }

private fun Quantity<PReal>.applyOperation(
    other: Quantity<PReal>,
    operation: (PRealOperand, PRealOperand) -> Quantity<PReal>,
    commutative: Boolean = false,
): Quantity<PReal> = when {
    this is ImpossibleQuantity<*> || other is ImpossibleQuantity<*> -> ImpossibleQuantity()
    this is AnyQuantity<*> || other is AnyQuantity<*> -> AnyQuantity()
    this is QuantityUnion<PReal> -> mapItems { it.applyOperation(other, operation, commutative) }
    other is QuantityUnion<PReal> -> other.mapItems { this.applyOperation(it, operation, commutative) }
    this is PRealOperand && other is PRealOperand -> operation(this, other).let { if (it is AnyQuantity<*> && commutative) other.applyOperation(this, operation) else it }
    else -> AnyQuantity()
}

operator fun Quantity<PReal>.unaryMinus(): Quantity<PReal> = PReal(0.0) - this

operator fun Quantity<PReal>.plus(other: Quantity<PReal>): Quantity<PReal> =
    applyOperation(other, PRealOperand::plus, commutative = true)

operator fun Quantity<PReal>.minus(other: Quantity<PReal>): Quantity<PReal> =
    applyOperation(other, PRealOperand::minus)

operator fun Quantity<PReal>.times(other: Quantity<PReal>): Quantity<PReal> =
    applyOperation(other, PRealOperand::times, commutative = true)

operator fun Quantity<PReal>.div(other: Quantity<PReal>): Quantity<PReal> =
    applyOperation(other, PRealOperand::div)

fun Quantity<PReal>.pow(other: Quantity<PReal>): Quantity<PReal> =
    applyOperation(other, PRealOperand::pow)
