package physics.quantities.expressions

import physics.quantities.Quantity
import physics.quantities.PReal


class Equality(val left: Expression, val right: Expression, ) {
    fun isolateVariable(variable: String): Equality = when (val variableAsExpression = Var(variable)) {
        this.left -> this
        in this.left -> Equality(variableAsExpression, left.getVariableIsoler(variable).invoke(this.right))
        in this.right -> Equality(variableAsExpression, right.getVariableIsoler(variable).invoke(this.left))
        else -> throw IllegalArgumentException("Variable isn't to be found in this equality.")
    }

    override fun equals(other: Any?): Boolean {
        return other is Equality && left == other.left && right == other.right
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

    fun compute(variable: String, arguments: Map<String, VariableValue<*>>): Quantity<PReal> {
        return isolateVariable(variable).right.evaluateExhaustively(arguments, counters = emptyMap())
    }

    fun composeWith(equality: Equality, joiningVariable: String): Equality {
        return Equality(this.left, right.substitute(Var(joiningVariable), equality.right))
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
