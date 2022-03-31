package physics.quantities.expressions

import Args
import physics.quantities.Quantity
import physics.quantities.PReal


abstract class Alias(val expression: Expression) : Expression() {
    override val members: Collection<Expression> = expression.members

    abstract override fun toString(): String

    override fun simplifyImpl(): Expression {
        val simplifiedExpr = expression.simplify()
        if (simplifiedExpr == expression) return this
        return simplifiedExpr
    }

    override fun mayBeDiscontinuousImpl(): Boolean {
        return expression.mayBeDiscontinuous()
    }

    override fun evaluate(arguments: Args<VariableValue<PReal>>, counters: Args<Int>): PReal {
        return expression.evaluate(arguments, counters)
    }

    override fun evaluateExhaustively(arguments: Args<VariableValue<*>>, counters: Args<Int>): Quantity<PReal> {
        return expression.evaluateExhaustively(arguments, counters)
    }

    override fun withMembers(members: List<Expression>): Expression {
        return expression.withMembers(members)
    }

    override fun derive(variable: String): Expression {
        return expression.derive(variable)
    }

    override fun getDirectMemberIsoler(member: Expression): (Expression) -> Expression {
        return expression.getDirectMemberIsoler(member)
    }
}