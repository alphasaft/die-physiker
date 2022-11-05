package physics.quantities

import physics.quantities.units.PUnit


interface PDoubleOperand : Quantity<PDouble> {
    val unit: PUnit
    operator fun unaryMinus(): Quantity<PDouble>
    operator fun plus(other: PDoubleOperand): Quantity<PDouble>
    operator fun minus(other: PDoubleOperand): Quantity<PDouble> = this + (-other)
    operator fun times(other: PDoubleOperand): Quantity<PDouble>
    fun inv(): Quantity<PDouble>
    operator fun div(other: PDoubleOperand): Quantity<PDouble> = this * other.inv()
    fun pow(other: PDoubleOperand): Quantity<PDouble>
}


fun Quantity<PDouble>.applyFunction(f: PFunction): Quantity<PDouble> =
    when (this) {
        is ImpossibleQuantity<PDouble> -> ImpossibleQuantity()
        is AnyQuantity<PDouble> -> f.outDomain
        is QuantityUnion<PDouble> -> QuantityUnion.new(items.map { it.applyFunction(f) })
        is PDoubleOperand -> f.invokeExhaustively(this)
        else -> f.outDomain
    }


private fun Quantity<PDouble>.applyOperation(
    other: Quantity<PDouble>,
    operation: (PDoubleOperand, PDoubleOperand) -> Quantity<PDouble>,
    commutative: Boolean = false,
): Quantity<PDouble> {
    val simplifiedThis = simplify()
    val simplifiedOther = other.simplify()
    return when {
        simplifiedThis is ImpossibleQuantity<*> || simplifiedOther is ImpossibleQuantity<*> -> ImpossibleQuantity()
        simplifiedThis is AnyQuantity<*> || simplifiedOther is AnyQuantity<*> -> AnyQuantity()
        simplifiedThis is QuantityUnion<PDouble> -> simplifiedThis.mapItems { it.applyOperation(simplifiedOther, operation, commutative) }
        simplifiedOther is QuantityUnion<PDouble> -> simplifiedOther.mapItems { simplifiedThis.applyOperation(it, operation, commutative) }
        simplifiedThis is PDoubleOperand && simplifiedOther is PDoubleOperand -> operation(simplifiedThis, simplifiedOther).let {
            if (it is AnyQuantity<*> && commutative) simplifiedOther.applyOperation(
                simplifiedThis,
                operation
            ) else it
        }
        else -> AnyQuantity()
    }
}

operator fun Quantity<PDouble>.unaryMinus(): Quantity<PDouble> = PDouble(0.0) - this

operator fun Quantity<PDouble>.plus(other: Quantity<PDouble>): Quantity<PDouble> =
    applyOperation(other, PDoubleOperand::plus, commutative = true)

operator fun Quantity<PDouble>.minus(other: Quantity<PDouble>): Quantity<PDouble> =
    applyOperation(other, PDoubleOperand::minus)

operator fun Quantity<PDouble>.times(other: Quantity<PDouble>): Quantity<PDouble> =
    applyOperation(other, PDoubleOperand::times, commutative = true)

operator fun Quantity<PDouble>.div(other: Quantity<PDouble>): Quantity<PDouble> =
    applyOperation(other, PDoubleOperand::div)

fun Quantity<PDouble>.pow(other: Quantity<PDouble>): Quantity<PDouble> {
    // Special case : If exponent is an even integer, result can only belong to [0;+oo[
    return applyOperation(other, PDoubleOperand::pow) /*intersect (
            if (other is PReal && (other/PReal(2)).isInt()) PRealInterval.Builtin.positive
            else AnyQuantity()
    )*/
}

