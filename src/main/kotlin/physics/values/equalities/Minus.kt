package physics.values.equalities

import physics.quantities.Quantity
import physics.quantities.doubles.PReal
import physics.quantities.doubles.unaryMinus

class Minus(val value: Expression) : Expression() {
    override val members: Collection<Expression> = listOf(value)

    override fun toString(): String {
        return if (value is Const || value is Var || value is Pow) "-$value" else "-($value)"
    }

    override fun evaluate(arguments: Map<String, Quantity<PReal>>): Quantity<PReal> {
        return -value.evaluate(arguments)
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
