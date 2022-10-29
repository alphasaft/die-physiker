package physics.quantities.expressions

import Args
import physics.quantities.Quantity
import physics.quantities.PDouble
import physics.quantities.unaryMinus

class Minus(val value: Expression) : Expression() {
    override val members: Collection<Expression> = listOf(value)

    override fun toString(): String {
        return if (value is Const || value is Var || value is Pow || value is Prod || value is Div) "-$value" else "-($value)"
    }

    override fun evaluateExhaustively(arguments: Args<VariableValue<*>>, counters: Map<String, Int>): Quantity<PDouble> {
        return -value.evaluateExhaustively(arguments, counters)
    }

    override fun evaluate(arguments: Args<VariableValue<PDouble>>, counters: Args<Int>): PDouble {
        return -value.evaluate(arguments, counters)
    }

    override fun derive(variable: String): Expression {
        return -value.derive(variable)
    }

    override fun getDirectMemberIsoler(member: Expression): (Expression) -> Expression {
        return { Minus(it) }
    }

    override fun simplifyImpl(): Expression {
        return when (val simplifiedValue = value.simplify()) {
            is Const -> Const(-simplifiedValue.value)
            is Minus -> simplifiedValue.value
            else -> this
        }
    }


    override fun mayBeDiscontinuousImpl(): Boolean {
        return value.mayBeDiscontinuous()
    }

    override fun withMembers(members: List<Expression>): Expression {
        val value = members.single()
        return -value
    }
}
