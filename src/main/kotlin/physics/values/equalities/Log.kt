package physics.values.equalities

import physics.quantities.AnyQuantity
import physics.quantities.Quantity
import physics.quantities.doubles.*
import kotlin.math.E
import kotlin.math.ln as nativeLn


open class Log(val x: Expression, val base: Expression = Const(10)) : Expression() {
    private val ln = MathFunction("ln", ::nativeLn, PRealInterval.Builtin.strictlyPositive, AnyQuantity())

    override val members: Collection<Expression> = listOf(x, base)

    override fun toString(): String {
        return when (base) {
            Const(10) -> "log($x)"
            else -> "log<$base>($x)"
        }
    }

    override fun evaluate(arguments: Map<String, Quantity<PReal>>): Quantity<PReal> {
        val evaluatedX = x.evaluate(arguments)
        val evaluatedBase = base.evaluate(arguments)
        val coefficient = evaluatedBase.applyContinuousFunction(ln)

        return evaluatedX.applyContinuousFunction(ln) / coefficient
    }

    override fun isolateDirectMember(member: Expression): (Expression) -> Expression {
        return if (member === x) {{ base.pow(it) }} else {{ Const(10).pow(Log(x) /it) }}
    }

    override fun simplifyImpl(): Expression {
        val base = base.simplify()
        val x = x.simplify()

        return when {
            x == Const(1) -> Const(0)
            base == x -> Const(1)
            x is Pow && x.x == base -> x.exponent
            base == Const(E) -> Ln(x)
            else -> Log(x, base)
        }
    }

    override fun withMembers(members: List<Expression>): Expression {
        val (x, base) = members
        return log(x, base)
    }
}