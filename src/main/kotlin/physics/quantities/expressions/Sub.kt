package physics.quantities.expressions

import Args
import physics.quantities.Quantity
import physics.quantities.doubles.PReal
import physics.quantities.doubles.minus

class Sub(val left: Expression, val right: Expression) : Expression() {
    override val members: Collection<Expression> = listOf(left, right)

    fun asSum(): Sum {
        return Sum(left, Minus(right))
    }

    override fun getDirectMemberIsoler(member: Expression): (Expression) -> Expression {
        return if (member === left) {{ it + right }} else {{ left - it }}
    }

    override fun mayBeDiscontinuousImpl(): Boolean {
        return left.mayBeDiscontinuous() || right.mayBeDiscontinuous()
    }

    override fun simplifyImpl(): Expression {
        return asSum().simplify()
    }

    override fun withMembers(members: List<Expression>): Expression {
        val (left, right) = members
        return left - right
    }

    override fun evaluateExhaustively(arguments: Args<VariableValue<*>>, counters: Args<Int>): Quantity<PReal> {
        return left.evaluateExhaustively(arguments, counters) - right.evaluateExhaustively(arguments, counters)
    }

    override fun evaluate(arguments: Args<VariableValue<PReal>>, counters: Args<Int>): PReal {
        return left.evaluate(arguments, counters) - right.evaluate(arguments, counters)
    }

    override fun derive(variable: String): Expression {
        return left.derive(variable) - right.derive(variable)
    }

    override fun toString(): String {
        return when (right) {
            is Sum -> "$left - ($right)"
            else -> "$left - $right"
        }
    }
}
