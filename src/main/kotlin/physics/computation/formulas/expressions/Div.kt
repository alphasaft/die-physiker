package physics.computation.formulas.expressions

import physics.values.PhysicalDouble

class Div(val dividend: Expression, val divider: Expression) : Expression() {
    override val members: Collection<Expression> = listOf(dividend, divider)

    override fun evaluate(args: Map<String, PhysicalDouble>): PhysicalDouble {
        return dividend.evaluate(args) / divider.evaluate(args)
    }

    override fun toString(): String {
        return "$dividend / $divider"
    }

    override fun isolateDirectMember(member: Expression): (Expression) -> Expression {
        return if (member === dividend) {{ it * divider }} else {{ dividend / it }}
    }

    override fun simplifyImpl(): Expression {
        val dividend = dividend.simplify()
        val divider = divider.simplify()

        return when {
            dividend == divider -> Const(1)
            dividend == Const(1) -> divider.pow(Const(-1))
            divider == Const(1) -> dividend
            dividend is Div -> (dividend.dividend / (dividend.divider * divider))
            divider is Div -> (dividend * divider.divider) / divider.dividend
            divider is Pow && divider.exponent.let { it is Const && it.value < 0 } -> dividend * divider.withNegatedExponent()
            dividend is Const && divider is Const -> Const(dividend.value / divider.value)
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
