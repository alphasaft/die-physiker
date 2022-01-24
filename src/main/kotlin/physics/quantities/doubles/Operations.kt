@file:Suppress("UNCHECKED_CAST")

package physics.quantities.doubles

import physics.quantities.*

fun Quantity<PReal>.applyContinuousFunction(f: MathFunction): Quantity<PReal> =
    when (this) {
        is ImpossibleQuantity<*> -> ImpossibleQuantity()
        is AnyQuantity<*> -> f.outDomain
        is QuantityUnion<*> -> QuantityUnion.new(items.map { (it as Quantity<PReal>).applyContinuousFunction(f) })
        else -> f.outDomain
    }

private fun Quantity<PReal>.applyOperation(
    other: Quantity<PReal>,
    operation: (PRealOperand, PRealOperand) -> Quantity<PReal>,
    commutative: Boolean = false,
): Quantity<PReal> = when {
    this is ImpossibleQuantity<*> || other is ImpossibleQuantity<*> -> ImpossibleQuantity()
    this is AnyQuantity<*> || other is AnyQuantity<*> -> AnyQuantity()
    this is QuantityUnion<*> -> QuantityUnion.new(items.map { (it as Quantity<PReal>).applyOperation(other, operation, commutative) })
    other is QuantityUnion<*> -> QuantityUnion.new(other.items.map { this.applyOperation(it as Quantity<PReal>, operation, commutative) })
    this is PRealOperand && other is PRealOperand -> operation(this, other).let { if (it is AnyQuantity<*> && commutative) operation(other, this) else it }
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
