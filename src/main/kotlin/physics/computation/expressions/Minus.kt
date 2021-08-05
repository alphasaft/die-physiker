package physics.computation.expressions

import physics.values.PhysicalDouble

class Minus(val value: Expression) : Expression() {
    override val members: Collection<Expression> = listOf(value)

    override fun toString(): String {
        return if (value is Const || value is Var || value is Pow) "-$value" else "-($value)"
    }

    override fun evaluate(args: Map<String, PhysicalDouble>): PhysicalDouble {
        return -value.evaluate(args)
    }

    override fun isolateDirectMember(member: Expression): (Expression) -> Expression {
        return { Minus(it) }
    }

    override fun simplifyImpl(): Expression {
        return when (val simplifiedValue = value.simplify()) {
            is Const -> Const(-simplifiedValue.value)
            is Minus -> simplifiedValue.value
            else -> this
        }
    }

    override fun withMembers(members: List<Expression>): Expression {
        val value = members.single()
        return -value
    }
}