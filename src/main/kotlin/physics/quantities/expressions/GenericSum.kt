package physics.quantities.expressions

import noop
import physics.quantities.Quantity
import physics.quantities.doubles.PReal
import physics.quantities.doubles.plus


class GenericSum(
    underlyingExpression: Expression,
    counterName: String,
    start: Bound = Bound.Static(1),
    end: Bound,
) : GenericExpression(underlyingExpression, counterName, start, end) {

    constructor(
        variable: String,
        counter: String,
        start: Bound = Bound.Static(1),
        endBound: Bound,
    ) : this(Var(variable), counter, start, endBound)

    override val members: Collection<Expression> = listOf(underlyingExpression)
    override val associatedStandardExpression: (List<Expression>) -> Expression = ::Sum
    override val reducer1: (PReal, PReal) -> PReal = PReal::plus
    override val reducer2: (Quantity<PReal>, Quantity<PReal>) -> Quantity<PReal> = Quantity<PReal>::plus

    override fun getDirectMemberIsoler(member: Expression): (Expression) -> Expression {
        return ::noop
    }

    override fun simplifyImpl(): Expression {
        val simplifiedUnderlyingExpression = term.simplify()

        isZero(simplifiedUnderlyingExpression).let { yes -> if (yes) return Const(0) }
        factorise(simplifiedUnderlyingExpression)?.let { (scalar, expr) -> return Prod(scalar, GenericSum(expr, counterName, start, end).simplify()) }

        return this
    }

    override fun mayBeDiscontinuousImpl(): Boolean {
        return members.any { it.mayBeDiscontinuous() }
    }

    private fun isZero(expression: Expression): Boolean {
        return expression == Const(0)
    }

    private fun factorise(expression: Expression): Pair<Expression, Expression>? {
        if (expression !is Prod) return null

        val scalar = Prod(expression.members.filter { it.allCounters().isEmpty() }).simplify()
        val term = Prod(expression.members.filter { it.allCounters().isNotEmpty() }).simplify()
        if (scalar == Const(1.0)) return null
        return Pair(
            scalar,
            term
        )
    }

    override fun withMembers(members: List<Expression>): Expression {
        val genericExpression = members.single()
        return GenericSum(genericExpression, counterName, start, end)
    }

    override fun derive(variable: String): Expression {
        return GenericSum(term.derive(variable), counterName, start, end)
    }
}