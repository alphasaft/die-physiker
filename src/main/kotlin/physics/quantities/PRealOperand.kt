package physics.quantities

import isInt


interface PRealOperand : Quantity<PReal> {
    operator fun unaryMinus(): Quantity<PReal>
    operator fun plus(other: PRealOperand): Quantity<PReal>
    operator fun minus(other: PRealOperand): Quantity<PReal> = this + (-other)
    operator fun times(other: PRealOperand): Quantity<PReal>
    operator fun div(other: PRealOperand): Quantity<PReal>
    fun pow(other: PRealOperand): Quantity<PReal>
}


fun Quantity<PReal>.applyFunction(f: Function): Quantity<PReal> =
    when (this) {
        is ImpossibleQuantity<PReal> -> ImpossibleQuantity()
        is AnyQuantity<PReal> -> f.outDomain
        is QuantityUnion<PReal> -> QuantityUnion.new(items.map { it.applyFunction(f) })
        is PRealOperand -> f.invokeExhaustively(this)
        else -> f.outDomain
    }


private fun Quantity<PReal>.applyOperation(
    other: Quantity<PReal>,
    operation: (PRealOperand, PRealOperand) -> Quantity<PReal>,
    commutative: Boolean = false,
): Quantity<PReal> {
    val simplifiedThis = simplify()
    val simplifiedOther = other.simplify()
    return when {
        simplifiedThis is ImpossibleQuantity<*> || simplifiedOther is ImpossibleQuantity<*> -> ImpossibleQuantity()
        simplifiedThis is AnyQuantity<*> || simplifiedOther is AnyQuantity<*> -> AnyQuantity()
        simplifiedThis is QuantityUnion<PReal> -> simplifiedThis.mapItems { it.applyOperation(simplifiedOther, operation, commutative) }
        simplifiedOther is QuantityUnion<PReal> -> simplifiedOther.mapItems { simplifiedThis.applyOperation(it, operation, commutative) }
        simplifiedThis is PRealOperand && simplifiedOther is PRealOperand -> operation(simplifiedThis, simplifiedOther).let {
            if (it is AnyQuantity<*> && commutative) simplifiedOther.applyOperation(
                simplifiedThis,
                operation
            ) else it
        }
        else -> AnyQuantity()
    }
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

fun Quantity<PReal>.pow(other: Quantity<PReal>): Quantity<PReal> {
    // Special case : If exponent is an even integer, result can only belong to [0;+oo[
    return applyOperation(other, PRealOperand::pow) intersect (
            if (other is PReal && (other/PReal(2)).isInt()) PRealInterval.Builtin.positive
            else AnyQuantity()
    )
}

