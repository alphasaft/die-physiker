package physics.computation.expressions

import physics.UnorderedList
import physics.chain
import physics.values.PhysicalDouble


class Prod(factors: List<Expression>) : Expression() {
    constructor(vararg factors: Expression): this(factors.toList())

    override val members: Collection<Expression> = UnorderedList(factors)

    override fun toString(): String {
        val constants = members.filterIsInstance<Const>()
        val variables = members.filter { it is Var || it is Pow && it.x is Var }
        val sumAndSubs = members.filter { it is Sum || it is Sub || it is All && it.collectorName == "sum" }
        val others = members.filterOut(sumAndSubs + constants + variables)
        val buffer = StringBuffer()

        constants.joinTo(buffer, "*")
        variables.joinTo(buffer, "*")
        if (sumAndSubs.isNotEmpty()) sumAndSubs.joinTo(buffer, separator = ")(", prefix = "(", postfix = ")")
        if (others.isNotEmpty()) {
            if (buffer.toString().isNotBlank()) buffer.append("*")
            others.joinTo(buffer, separator = "*")
        }

        return buffer.toString()
    }

    override fun isolateDirectMember(member: Expression): (Expression) -> Expression {
        val parasiteMembers = members.filter { it !== member }
        return { it / Prod(parasiteMembers) }
    }

    override fun simplifyImpl(): Expression {
        val simplifiedMembers = chain(
            members.map { it.simplify() },
            this::writeDivisionsAsProducts,
            this::flattenNestedProducts,
            this::multiplyConstants,
            this::regroupAsPowers,
            this::removeProductsByOne,
            this::zeroIfAnyMemberIsZero,
            this::oneIfEmpty,
            this::applyTheSignsRule,
            this::writeAsFraction,
        )
        return if (simplifiedMembers.size == 1) simplifiedMembers.single() else Prod(simplifiedMembers)
    }
    
    private fun Expression.isConstWithValue(value: Int) = isConstWithValue(value.toDouble())
    private fun Expression.isConstWithValue(value: Double) = this is Const && this.value.toDouble() == value

    private fun writeDivisionsAsProducts(members: List<Expression>): List<Expression> {
        return members.map { if (it is Div) it.writeAsProduct() else it }
    }

    private fun flattenNestedProducts(members: List<Expression>): List<Expression> {
        val flattened = mutableListOf<Expression>()
        for (member in members) {
            if (member is Prod)
                flattened.addAll(member.members)
            else
                flattened.add(member)
        }
        return flattened
    }

    private fun multiplyConstants(members: List<Expression>): List<Expression> {
        return members.filterIsInstanceAndReplace<Const> { listOf(it.reduce(Const::times)) }
    }

    private fun applyTheSignsRule(members: List<Expression>): List<Expression> {
        if (members.size <= 1) return members

        val minuses = members.count { it is Minus || it.isConstWithValue(-1) }
        val unsignedMembers = members.map { if (it is Minus) it.value else it }.filterNot { it.isConstWithValue(-1) }
        return if (minuses % 2 == 0)
            unsignedMembers
        else
            listOf(-Prod(unsignedMembers).simplify())
    }

    private fun regroupAsPowers(members: List<Expression>): List<Expression> {
        val expressionsToCoefficients = mutableMapOf<Expression, Expression>()
        val remainingMembers = members.toMutableList()

        for (member in remainingMembers) {
            val (expression, coefficient) = getUnderlyingExpressionAndCoefficient(member)
            if (expression in expressionsToCoefficients) {
                expressionsToCoefficients[expression] = expressionsToCoefficients.getValue(expression) + coefficient
            } else {
                expressionsToCoefficients[expression] = coefficient
            }
        }

        val result = mutableListOf<Expression>()
        for ((expression, coefficient) in expressionsToCoefficients) {
            result.add(when {
                coefficient.isConstWithValue(0) -> continue
                coefficient.isConstWithValue(1) -> expression
                expression is Div && expression.dividend.isConstWithValue(1) -> Pow(expression.divider, -coefficient).simplify()
                else -> Pow(expression, coefficient).simplify()
            })
        }
        return result
    }

    private fun getUnderlyingExpressionAndCoefficient(member: Expression): Pair<Expression, Expression> {
        return when {
            member is Pow -> member.x to member.exponent
            member is Div && member.dividend.isConstWithValue(1) -> getUnderlyingExpressionAndCoefficient(member.divider).let { it.first to -it.second }
            else -> member to Const(1)
        }
    }

    private fun removeProductsByOne(members: List<Expression>): List<Expression> {
        return members.filterNot { it.isConstWithValue(1)  }
    }

    private fun zeroIfAnyMemberIsZero(members: List<Expression>): List<Expression> {
        val zero = members.firstOrNull { it.isConstWithValue(0) }
        return if (zero != null) listOf(zero) else members
    }

    private fun oneIfEmpty(members: List<Expression>): List<Expression> {
        return members.ifEmpty { listOf(Const(1)) }
    }

    private fun writeAsFraction(members: List<Expression>): List<Expression> {
        val divider = members.filter { it is Div || it is Pow && (it.exponent is Const && it.exponent.value < 0.0 || it.exponent is Minus) }
        val dividend = members.filterOut(divider)

        val dividendAsExpression = asExpression(dividend) ?: return listOf(asExpression(divider)!!)
        val dividerAsExpression = asExpression(divider.map { (it as Pow).withNegatedExponent() }) ?: return listOf(asExpression(dividend)!!)

        return listOf(Div(dividendAsExpression, dividerAsExpression))
    }

    private fun asExpression(members: List<Expression>): Expression? {
        return when {
            members.isEmpty() -> null
            members.size == 1 -> members.single()
            else -> Prod(members)
        }
    }

    override fun evaluate(args: Map<String, PhysicalDouble>): PhysicalDouble {
        return members.map { it.evaluate(args) }.reduce(PhysicalDouble::times)
    }

    override fun withMembers(members: List<Expression>): Expression {
        return Prod(members).simplify()
    }

    fun distribute(): Expression {
        if (members.none { it is Sum || it is Sub }) return this

        val resultingProducts = mutableListOf(listOf<Expression>())
        for (member in members) {
            when (member) {
                is Sum -> {
                    for (product in resultingProducts.toList()) {
                        for (term in member.members) {
                            resultingProducts.add(product + term)
                        }
                        resultingProducts.remove(product)
                    }
                }

                is Sub -> {
                    for (product in resultingProducts.toList()) {
                        resultingProducts.add(product + member.left)
                        resultingProducts.add(product + Minus(member.right))
                        resultingProducts.remove(product)
                    }
                }

                else -> {
                    for (product in resultingProducts.toList()) {
                        resultingProducts.add(product + member)
                        resultingProducts.remove(product)
                    }
                }
            }
        }

        return Sum(resultingProducts.map { Prod(it).simplify() })
    }
}
