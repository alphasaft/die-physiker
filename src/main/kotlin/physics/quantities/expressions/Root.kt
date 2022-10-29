package physics.quantities.expressions

import Args
import isInt
import physics.quantities.*

class Root(private val x: Expression, private val rootExponent: Expression = Const(2)) : Alias(Pow(x, Const(1)/rootExponent)) {
    override fun toString(): String {
        return if (rootExponent == Const(2)) "sqrt($x)"
        else "rt<$rootExponent>($x)"
    }

    override fun evaluateExhaustively(arguments: Args<VariableValue<*>>, counters: Args<Int>): Quantity<PDouble> {
        val evaluated = x.evaluateExhaustively(arguments).pow(PDouble(1.0) / rootExponent.evaluateExhaustively(arguments))
        return (
            if (rootExponent is Const && (rootExponent.value.toDouble() / 2.0).isInt()) evaluated union -evaluated
            else evaluated
        )
    }
}
