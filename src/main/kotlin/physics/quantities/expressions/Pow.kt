package physics.quantities.expressions

import Args
import physics.quantities.ImpossibleQuantity
import physics.quantities.Quantity
import physics.quantities.PReal
import physics.quantities.PRealInterval
import physics.quantities.doubles.pow


class Pow(val x: Expression, val exponent: Expression) : Expression() {
    override val members: Collection<Expression> = listOf(x, exponent)

    override fun toString(): String {
        val xAsString = if (x is Const || x is Var) x.toString() else "($x)"
        val exponentAsString = if (exponent is Const || exponent is Var) exponent.toString() else "($exponent)"
        return "$xAsString^$exponentAsString"
    }

    override fun evaluateExhaustively(arguments: Args<VariableValue<*>>, counters: Args<Int>): Quantity<PReal> {
        return x.evaluateExhaustively(arguments, counters).pow(exponent.evaluateExhaustively(arguments, counters))
    }

    override fun evaluate(arguments: Args<VariableValue<PReal>>, counters: Args<Int>): PReal {
        return x.evaluate(arguments, counters).pow(exponent.evaluate(arguments, counters))
    }

    override fun getDirectMemberIsoler(member: Expression): (Expression) -> Expression {
        return if (member === x) {{ root(it, exponent) }} else {{ log(it, x) }}
    }

    override fun simplifyImpl(): Expression {
        val x = x.simplify()
        val exponent = exponent.simplify()

        return when {
            exponent is Log && exponent.base == x -> exponent.x
            x is Const && exponent is Const -> Const(x.value.pow(exponent.value))
            x is Exp -> Exp(x.argument * exponent)
            x is Pow -> x.x.pow(x.exponent * exponent)
            exponent == Const(1) -> x
            exponent == Const(0) -> Const(1)
            else -> this
        }
    }

    override fun mayBeDiscontinuousImpl(): Boolean {
        if (PRealInterval.Builtin.negative stdIntersect exponent.outDomain != ImpossibleQuantity<PReal>() && PReal(0) in x.outDomain) return true
        return x.mayBeDiscontinuous() || exponent.mayBeDiscontinuous()
    }

    override fun withMembers(members: List<Expression>): Expression {
        val (x, exponent) = members
        return x.pow(exponent)
    }

    override fun derive(variable: String): Expression {
        return (exponent.derive(variable) * Ln(x) + exponent*x.derive(variable)/x) * this
    }

    fun withNegatedExponent() = x.pow(-exponent)
}