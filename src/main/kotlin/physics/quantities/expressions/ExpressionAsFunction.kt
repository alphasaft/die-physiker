package physics.quantities.expressions

import physics.quantities.Quantity
import physics.quantities.Function
import physics.quantities.PReal


class ExpressionAsFunction(
    private val expression: Expression,
    private val parameter: String = "x",
) : Function {
    init {
        require(expression.allVariables().all { it == parameter }) { "Expected $parameter as sole variable, or no variable." }
    }

    override val outDomain: Quantity<PReal> get() = expression.outDomain
    override val reciprocal: ExpressionAsFunction get() = expression.getVariableIsoler(parameter).invoke(Var("y")).asFunction("y")

    override fun invoke(x: String): String {
        return expression.substitute(v(parameter), v(x)).toString()
    }

    override fun invoke(x: PReal): PReal {
        return expression.evaluate(arguments = mapOf(parameter to VariableValue.Single(x)))
    }

    override fun invokeExhaustively(x: Quantity<PReal>): Quantity<PReal> {
        return expression.evaluateExhaustively(arguments = mapOf(parameter to VariableValue.Single(x)))
    }

    operator fun invoke(x: Expression): Expression {
        return expression.substitute(Var(parameter), x)
    }

    override fun compose(f: Function): Function {
        return when (f) {
            is ExpressionAsFunction -> {
                val composedExpression = expression.substitute(v(parameter), f.expression)
                return ExpressionAsFunction(composedExpression, f.parameter)
            }
            else -> Function.defaultComposition(this, f)
        }
    }
}
