package physics.computation.expressions

import physics.values.PhysicalDouble

class Sub(val left: Expression, val right: Expression) : Expression() {
    override val members: Collection<Expression> = listOf(left, right)

    override fun toString(): String {
        return when {
            right is Sum -> "$left - ($right)"
            right is AllVars && right.collectorName == "sum" -> "$left - ($right)"
            else -> "$left - $right"
        }
    }

    override fun isolateDirectMember(member: Expression): (Expression) -> Expression {
        return if (member === left) {{ it + right }} else {{ left - it }}
    }

    override fun simplifyImpl(): Expression {
        return asSum().simplify()
    }

    override fun evaluate(args: Map<String, PhysicalDouble>): PhysicalDouble {
        return left.evaluate(args) - right.evaluate(args)
    }

    override fun withMembers(members: List<Expression>): Expression {
        val (left, right) = members
        return left - right
    }

    fun asSum(): Sum {
        return Sum(left, Minus(right))
    }
}
