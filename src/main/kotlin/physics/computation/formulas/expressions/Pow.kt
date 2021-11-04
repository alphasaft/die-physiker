package physics.computation.formulas.expressions

import physics.values.PhysicalDouble


open class Pow(val x: Expression, val exponent: Expression) : Expression() {
    override val members: Collection<Expression> = listOf(x, exponent)

    override fun toString(): String {
        val xAsString = if (x is Const || x is Var) x.toString() else "($x)"
        val exponentAsString = if (exponent is Const || exponent is Var) exponent.toString() else "($exponent)"
        return "$xAsString^$exponentAsString"
    }

    override fun evaluate(args: Map<String, PhysicalDouble>): PhysicalDouble {
        return x.evaluate(args).pow(exponent.evaluate(args))
    }

    override fun isolateDirectMember(member: Expression): (Expression) -> Expression {
        return if (member === x) {{ root(it, exponent) }} else {{ log(it, x) }}
    }

    override fun simplifyImpl(): Expression {
        val x = x.simplify()
        val exponent = exponent.simplify()

        return when {
            exponent is Log && exponent.base == x -> exponent.x
            x is Const && exponent is Const -> Const(x.value.pow(exponent.value))
            x is Pow -> x.x.pow(x.exponent * exponent)
            exponent == Const(1) -> x
            exponent == Const(0) -> Const(1)
            else -> this
        }
    }

    override fun withMembers(members: List<Expression>): Expression {
        val (x, exponent) = members
        return x.pow(exponent)
    }

    fun withNegatedExponent() = x.pow(-exponent)
}