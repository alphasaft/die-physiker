package physics.quantities.expressions

import Args
import physics.quantities.Quantity
import physics.quantities.PDouble


abstract class Alias(val expression: Expression) : Expression() {
    override val members: Collection<Expression> = expression.members

    abstract override fun toString(): String

    override fun simplifyImpl(): Expression {
        return expression.simplify()
    }

    override fun mayBeDiscontinuousImpl(): Boolean {
        return expression.mayBeDiscontinuous()
    }

    override fun evaluate(arguments: Args<VariableValue<PDouble>>, counters: Args<Int>): PDouble {
        return expression.evaluate(arguments, counters)
    }

    override fun evaluateExhaustively(arguments: Args<VariableValue<*>>, counters: Args<Int>): Quantity<PDouble> {
        return expression.evaluateExhaustively(arguments, counters)
    }

    override fun withMembers(members: List<Expression>): Expression {
        return expression.withMembers(members)
    }

    override fun differentiate(variable: String): Expression {
        return expression.differentiate(variable)
    }

    override fun getDirectMemberIsoler(member: Expression): (Expression) -> Expression {
        return expression.getDirectMemberIsoler(member)
    }
}
