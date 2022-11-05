package physics.quantities.expressions

import physics.quantities.Quantity
import physics.quantities.PDouble


class Equation(val left: Expression, val right: Expression, ) {
    fun isolateVariable(variable: String): Equation = when (val variableAsExpression = Var(variable)) {
        this.left -> this
        in this.left -> Equation(variableAsExpression, left.getVariableIsoler(variable).invoke(this.right))
        in this.right -> Equation(variableAsExpression, right.getVariableIsoler(variable).invoke(this.left))
        else -> throw IllegalArgumentException("Variable '$variable' can't be found in equality $this.")
    }

    override fun equals(other: Any?): Boolean {
        return other is Equation && left == other.left && right == other.right
    }

    override fun hashCode(): Int {
        var result = left.hashCode()
        result = 31 * result + right.hashCode()
        return result
    }

    override fun toString(): String {
        return if (right is Div) left.toString() + " = " + right.toStringAsFraction(offset = left.toString().length + 3)
        else toFlatString()
    }

    fun compute(variable: String, arguments: Map<String, VariableValue<*>>): Quantity<PDouble> {
        return isolateVariable(variable).right.evaluateExhaustively(arguments, counters = emptyMap())
    }

    fun computeOrDefault(variable: String, arguments: Map<String, VariableValue<*>>, default: Quantity<PDouble>): Quantity<PDouble> {
        return try {
            compute(variable, arguments)
        } catch (e: UnsolvableVariable) {
            default
        }
    }

    fun composeWith(equation: Equation): Equation {
        return Equation(this.left, right.substitute(equation.left, equation.right))
    }


    fun allVariables(): Set<String> {
        return left.allVariables() + right.allVariables()
    }

    fun toFlatString(): String {
        return "$left = $right"
    }

    operator fun component1(): Expression {
        return left
    }

    operator fun component2(): Expression {
        return right
    }
}
