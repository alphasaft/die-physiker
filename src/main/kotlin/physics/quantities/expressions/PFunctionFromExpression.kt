package physics.quantities.expressions

import physics.quantities.Quantity
import physics.quantities.PFunction
import physics.quantities.PDouble


class PFunctionFromExpression(
    private val expression: Expression,
    private val parameter: String = "x",
) : PFunction {
    init {
        require(expression.allVariables().all { it == parameter }) { "Expected $parameter as sole variable, or no variable." }
    }

    override val outDomain: Quantity<PDouble> get() = expression.outDomain
    override val reciprocal: PFunctionFromExpression get() = (("y" equals expression).isolateVariable("y").right).."y"
    override val derivative: PFunctionFromExpression get() = (expression.differentiate(parameter))..parameter

    override fun invoke(x: String): String {
        return expression.substitute(v(parameter), v(x)).toString()
    }

    override fun invoke(x: PDouble): PDouble {
        return expression.evaluate(arguments = mapOf(parameter to VariableValue.Single(x)))
    }

    override fun invokeExhaustively(x: Quantity<PDouble>): Quantity<PDouble> {
        return expression.evaluateExhaustively(arguments = mapOf(parameter to VariableValue.Single(x)))
    }

    operator fun invoke(x: Expression): Expression {
        return expression.substitute(Var(parameter), x)
    }

    override fun toString(): String {
        return "$parameter --> $expression"
    }
}
