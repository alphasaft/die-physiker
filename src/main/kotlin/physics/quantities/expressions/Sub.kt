package physics.quantities.expressions


class Sub(val left: Expression, val right: Expression) : Sum(left, Minus(right)) {
    override fun withMembers(members: List<Expression>): Expression {
        val (left, right) = members
        return left - right
    }

    override fun toString(): String {
        return when (right) {
            is Sum -> "$left - ($right)"
            else -> "$left - $right"
        }
    }
}
