package physics.computation.expressions

import physics.values.PhysicalDouble


class Equality(
    val left: Expression,
    val right: Expression,
) {
    fun isolateVariable(variable: String): Equality {
        val variableAsExpression = Var(variable)
        return when {
            variableAsExpression == this.left -> this
            variableAsExpression in this.left -> Equality(variableAsExpression, left.isolateVariable(variable).invoke(this.right))
            variableAsExpression in this.right -> Equality(variableAsExpression, right.isolateVariable(variable).invoke(this.left))
            else -> throw IllegalArgumentException("Variable isn't to be found in this equality.")
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is Equality && left == other.left && right == other.right
    }

    override fun toString(): String {
        return if (right is Div) left.toString() + " = " + right.toStringAsFraction(offset = left.toString().length + 3)
        else toFlatString()
    }

    override fun hashCode(): Int {
        return left.hashCode() * 7 + right.hashCode() * 13
    }

    fun compute(arguments: Map<String, PhysicalDouble>): PhysicalDouble {
        return right.evaluate(arguments)
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

infix fun Expression.equal(other: Expression) = Equality(this, other)
infix fun String.equal(other: Expression) = Equality(Var(this), other)
