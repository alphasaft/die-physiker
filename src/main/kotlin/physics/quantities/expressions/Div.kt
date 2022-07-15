package physics.quantities.expressions

import Args
import physics.quantities.Quantity
import physics.quantities.PReal
import physics.quantities.div

class Div(val dividend: Expression, val divider: Expression) : Expression() {
    override val members: Collection<Expression> = listOf(dividend, divider)

    override fun toString(): String {
        val dividendAsString = if (dividend is Sum || dividend is Sub || dividend is Prod || dividend is Div) "($dividend)" else dividend.toString()
        val dividerAsString = if (divider is Sum || divider is Sub || divider is Prod || divider is Div) "($divider)" else divider.toString()
        return "$dividendAsString / $dividerAsString"
    }

    override fun getDirectMemberIsoler(member: Expression): (Expression) -> Expression {
        return if (member === dividend) {{ it * divider }} else {{ dividend / it }}
    }

    override fun derive(variable: String): Expression {
        return (dividend.derive(variable) * divider - dividend * divider.derive(variable))/divider.square()
    }

    override fun evaluateExhaustively(arguments: Args<VariableValue<*>>, counters: Map<String, Int>): Quantity<PReal> {
        return dividend.evaluateExhaustively(arguments, counters) / divider.evaluateExhaustively(arguments, counters)
    }

    override fun evaluate(arguments: Args<VariableValue<PReal>>, counters: Args<Int>): PReal {
        return dividend.evaluate(arguments, counters) / divider.evaluate(arguments, counters)
    }

    override fun mayBeDiscontinuousImpl(): Boolean {
        return dividend.mayBeDiscontinuous() || PReal(0) in divider.outDomain
    }

    override fun simplifyImpl(): Expression {
        val dividend = dividend.simplify()
        val divider = divider.simplify()

        return when {
            dividend == divider -> Const(1)
            dividend == Const(1) -> divider.pow(Const(-1))
            divider == Const(1) -> dividend
            dividend is Const && divider is Const -> Const(dividend.value/divider.value)
            dividend is Div -> (dividend.dividend / (dividend.divider * divider))
            divider is Div -> (dividend * divider.divider) / divider.dividend
            divider is Pow -> dividend * divider.withNegatedExponent()
            divider is Exp -> dividend * divider.withNegatedExponent()
            dividend is Log && divider is Log && dividend.base == divider.base -> Log(x = dividend.x, base = divider.x)
            else -> writeAsProduct().simplify()
        }}


    override fun withMembers(members: List<Expression>): Expression {
        val (dividend, divider) = members
        return dividend / divider
    }

    fun distribute(): Expression {
        if (dividend !is Sum) return this

        val result = mutableListOf<Expression>()
        for (term in dividend.members) result.add(Div(term, divider).simplify())
        return Sum(*result.toTypedArray()).simplify()
    }

    fun writeAsProduct(): Prod {
        val dividendAsProduct = if (dividend is Prod) dividend.members else listOf(dividend)
        val dividerAsProduct = if (divider is Prod) divider.members.map { Const(1) / it } else listOf(Div(Const(1), divider))
        return Prod(dividerAsProduct + dividendAsProduct)
    }

    fun toStringAsFraction(offset: Int = 0, includeTextAtItsRight: String = ""): String {
        val dividendAsString = dividend.toString()
        val dividerAsString = divider.toString()
        val length = dividerAsString.length.coerceAtLeast(dividendAsString.length+2)
        val underscores = "_".repeat((length-dividendAsString.length)/2)
        val spaces = " ".repeat((length-dividerAsString.length)/2)
        return underscores + dividendAsString + underscores + includeTextAtItsRight + "\n" + " ".repeat(offset) + spaces + dividerAsString + spaces
    }
}
