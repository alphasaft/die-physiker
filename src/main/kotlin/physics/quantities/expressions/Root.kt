package physics.quantities.expressions

import Args
import isInt
import physics.quantities.Quantity
import physics.quantities.PReal
import physics.quantities.doubles.div
import physics.quantities.doubles.pow
import physics.quantities.doubles.unaryMinus
import physics.quantities.union

class Root(private val x: Expression, private val rootExponent: Expression = Const(2)) : Alias(Pow(x, Const(1)/rootExponent)) {
    override fun toString(): String {
        return if (rootExponent == Const(2)) "sqrt($x)"
        else "rt<$rootExponent>($x)"
    }

    override fun evaluateExhaustively(arguments: Args<VariableValue<*>>, counters: Args<Int>): Quantity<PReal> {
        val evaluated = x.evaluateExhaustively(arguments).pow(PReal(1.0) / rootExponent.evaluateExhaustively(arguments))
        return (
            if (rootExponent is Const && (rootExponent.value.toDouble() / 2.0).isInt()) evaluated union -evaluated
            else evaluated
        )
    }
}
