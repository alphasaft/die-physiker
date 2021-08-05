package physics.computation.expressions

import physics.values.PhysicalDouble
import kotlin.math.E
import kotlin.math.log
import kotlin.math.min


class Log(val x: Expression, val base: Expression = Const(10)) : Expression() {
    override val members: Collection<Expression> = listOf(x, base)

    override fun toString(): String {
        return when (base) {
            Const(E) -> "ln($x)"
            Const(10) -> "log($x)"
            else -> "log<$base>($x)"
        }
    }

    override fun evaluate(args: Map<String, PhysicalDouble>): PhysicalDouble {
        val evaluatedX = x.evaluate(args)
        val evaluatedBase = base.evaluate(args)

        return PhysicalDouble(
            log(evaluatedX.value, evaluatedBase.value),
            min(evaluatedX.significantDigitsCount, evaluatedBase.significantDigitsCount),
            evaluatedX.unit
        )
    }

    override fun isolateDirectMember(member: Expression): (Expression) -> Expression {
        return if (member === x) {{ base.pow(it) }} else {{ Const(10).pow(Log(x)/it) }}
    }

    override fun simplifyImpl(): Expression {
        val base = base.simplify()
        val x = x.simplify()

        return when {
            base is Const && x is Const -> Const(log(x.value.toDouble(), base.value.toDouble()))
            base == x -> Const(1)
            x is Pow && x.x == base -> x.exponent
            else -> Log(x, base)
        }
    }

    override fun withMembers(members: List<Expression>): Expression {
        val (x, base) = members
        return log(x, base)
    }
}