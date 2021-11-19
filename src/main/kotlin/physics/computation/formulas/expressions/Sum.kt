package physics.computation.formulas.expressions

import physics.*
import physics.UnorderedList
import physics.binomialCoefficient
import physics.chain
import physics.isInt
import physics.values.PhysicalDouble


class Sum(terms: List<Expression>): Expression() {
    constructor(vararg terms: Expression): this(terms.toList())

    override val members: Collection<Expression> = UnorderedList(terms)

    override fun toString(): String {
        return members.joinToString(" + ")
    }

    override fun isolateDirectMember(member: Expression): (Expression) -> Expression {
        val parasiteMembers = members.filter { it !== member }
        return { it - Sum(parasiteMembers) }
    }

    override fun simplifyImpl(): Expression {
        val simplifiedMembers = chain(
            members.map { it.simplify() },
            this::distributeProductsDivisionsAndPowers,
            this::flattenMembers,
            this::addUpConstants,
            this::removeZeros,
            this::addUpSimilarMembers,
            this::applyRemarquableIdentities,
            this::writeAsDifference,
            this::zeroIfEmpty,
        )
        return if (simplifiedMembers.size == 1) simplifiedMembers.single() else Sum(simplifiedMembers)
    }

    private fun distributeProductsDivisionsAndPowers(members: List<Expression>): List<Expression> {
        val distributed = mutableListOf<Expression>()
        for (member in members) {
            distributed.add(
                when (member) {
                    is Prod -> member.distribute()
                    is Div -> member.distribute()
                    is Pow -> {
                        if (member.x is Sum && member.exponent is Const && member.exponent.value.toDouble().isInt()) {
                            Prod(List(member.exponent.value.toInt()) { member.x }).distribute()
                        } else member
                    }
                    else -> member
                }
            )
        }
        return distributed
    }

    private fun flattenMembers(members: List<Expression>): List<Expression> {
        val flattened = mutableListOf<Expression>()
        for (member in members) flattened.addAll(flattenMember(member))
        return flattened
    }

    private fun flattenMember(member: Expression): Collection<Expression> =
        when (member) {
            is Sum -> member.members
            is Sub -> member.asSum().members
            is Minus -> flattenMember(member.value).map { Minus(it) }
            else -> listOf(member)
        }

    private fun addUpConstants(members: List<Expression>): List<Expression> {
        return members.filterIsInstanceAndReplace<Const> { listOf(it.reduce(Const::plus))  }
    }

    private fun removeZeros(members: List<Expression>): List<Expression> {
        return members.filterNot { it is Const && it.value.toDouble() == 0.0 }
    }

    private fun addUpSimilarMembers(members: List<Expression>): List<Expression> {
        val expressionsAndCoefficients = mutableMapOf<Expression, Expression>()
        val remainingMembers = members.toMutableList()

        for (member in remainingMembers) {
            val (expression, coefficient) = getUnderlyingExpressionAndCoefficient(member)
            if (expression in expressionsAndCoefficients) {
                expressionsAndCoefficients[expression] = expressionsAndCoefficients.getValue(expression) + coefficient
            } else {
                expressionsAndCoefficients[expression] = coefficient
            }
        }

        val result = mutableListOf<Expression>()
        for ((expression, coefficient) in expressionsAndCoefficients) {
            result.add(when {
                coefficient == Const(0) -> continue
                coefficient == Const(1) -> expression
                coefficient == Const(-1) -> -expression
                expression is Prod -> coefficient * expression
                expression is Minus -> -coefficient * expression.value
                else -> Prod(coefficient, expression)
            })
        }
        return result
    }


    private fun applyRemarquableIdentities(members: List<Expression>): List<Expression> {
        var result = members
        val filtered = members.filterIsInstance<Pow>().filter { it.exponent.let { e -> e is Const && e.value.toDouble().isInt() } }
        // Here, only one member of the sum can be a Const thanks to the `addUpConstants` method.
        val constant = members.singleOrNull { it is Const && it.value >= 0 } as Const?

        main@ for (a in filtered) {
            for (b in filtered - a) {
                if (a.exponent != b.exponent) continue
                val previous = result.toList()
                result = applyIdentityTo(result, a.x, b.x, (a.exponent as Const).value.toInt())
                if (previous != result) continue@main
            }

            if (constant == null) continue
            val rootOfConst = constant.pow(Const(1) /a.exponent)
            result = applyIdentityTo(result, a.x, rootOfConst, (a.exponent as Const).value.toInt())
        }

        return addUpSimilarMembers(result)
    }
    
    private fun generateRemarquableIdentity(a: Expression, b: Expression, n: Int): List<Expression> {
        return List(n+1) { i -> Const(binomialCoefficient(i, n)) * a.pow(Const(i)) * b.pow(Const(n-i)) }
    }

    private fun applyIdentityTo(members: List<Expression>, a: Expression, b: Expression, n: Int): List<Expression> {
        val remarquableIdentity = generateRemarquableIdentity(a, b, n)
        val missingMembers = remarquableIdentity amputatedOf members

        if (missingMembers.size < remarquableIdentity.size / 2) {
            return ((members amputatedOf remarquableIdentity)
                + missingMembers.map { Minus(it) }
                + Sum(a, b).pow(Const(n)))
        }

        return members
    }

    private fun getUnderlyingExpressionAndCoefficient(member: Expression): Pair<Expression, Expression> {
        return when {
            member is Prod && member.members.any { it is Const } -> {
                val const = member.members.single { it is Const } as Const
                val expression = (member.members - const).let { if (it.size == 1) it.single() else Prod(it) }
                expression to const
            }
            member is Minus -> getUnderlyingExpressionAndCoefficient(member.value).let { it.first to -it.second }
            else -> member to Const(1)
        }
    }

    private fun writeAsDifference(members: List<Expression>): List<Expression> {
        val positive = mutableListOf<Expression>()
        val negative = mutableListOf<Expression>()

        for (member in members) {
            when (member) {
                is Prod -> Prod(member.members.map { if (it is Const && it.value < 0) -it else it }).let { if (it != member) negative.add(it) else positive.add(it)  }
                is Minus -> negative.add(member.value)
                is Const -> if (member.value < 0) negative.add(-member) else positive.add(member)
                else -> positive.add(member)
            }
        }
        return when {
            negative.isEmpty() -> positive
            positive.isEmpty() -> listOf(Minus(Sum(negative)))
            else -> listOf(Sub(Sum(positive).simplify(), Sum(negative).simplify()))
        }
    }

    private fun zeroIfEmpty(members: List<Expression>): List<Expression> {
        return members.ifEmpty { listOf(Const(0)) }
    }

    override fun evaluate(args: Map<String, PhysicalDouble>): PhysicalDouble {
        return members.map { it.evaluate(args) }.reduce(PhysicalDouble::plus)
    }

    override fun withMembers(members: List<Expression>): Expression {
        return Sum(members).simplify()
    }
}
