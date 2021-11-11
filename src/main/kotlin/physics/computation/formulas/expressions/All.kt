package physics.computation.formulas.expressions

import physics.noop
import physics.values.PhysicalDouble
import println

class All(
    val genericExpression: Expression,
    val collectorName: String,
    private val maximalIndiceNotation: String = "n",
) : Expression() {
    constructor(
        genericVariableName: String,
        collectorName: String,
        maximalIndiceNotation: String = "n"
    ): this(Var(genericVariableName), collectorName, maximalIndiceNotation)

    companion object {
        val collectors: Map<String, (List<PhysicalDouble>) -> PhysicalDouble> = mapOf(
            "sum" to { it.reduce(PhysicalDouble::plus) },
            "product" to { it.reduce(PhysicalDouble::times) },
            "mid" to { it.reduce(PhysicalDouble::plus) / it.size },
            "max" to { it.maxOrNull() ?: PhysicalDouble(0.0) },
            "min" to { it.minOrNull() ?: PhysicalDouble(0.0) },
            "size" to { PhysicalDouble(it.size.toDouble()) }
        )
    }

    override val members: Collection<Expression> = emptyList()
    private val collector: (List<PhysicalDouble>) -> PhysicalDouble = collectors.getValue(collectorName)
    private val genericVariablesNames = genericExpression.allVariables().filter { "#" in it }

    private fun expression(i: Int) = expression(i.toString())
    private fun expression(name: String): Expression {
        val variablesNamesForIteration = genericVariablesNames.associateWith { it.replace("#", name) }
        var resultingExpression = genericExpression
        for ((genericVariable, iterationVariable) in variablesNamesForIteration) {
            resultingExpression = resultingExpression.substitute(Var(genericVariable), Var(iterationVariable))
        }
        return resultingExpression
    }

    override fun evaluate(args: Map<String, PhysicalDouble>): PhysicalDouble {
        var i = 1
        val collected = mutableListOf<PhysicalDouble>()

        while (genericVariablesNames.first().replace("#", i.toString()) in args) {
            val expression = expression(i++)
            collected.add(expression.evaluate(args))
        }

        return collector(collected)
    }

    override fun isolateDirectMember(member: Expression): (Expression) -> Expression {
        return ::noop
    }

    override fun simplifyImpl(): Expression {
        return this
    }

    override fun withMembers(members: List<Expression>): Expression {
        return this
    }

    override fun toString(): String {
        val variables = listOf(expression(1), expression(2), "...", expression(maximalIndiceNotation))
        return when (collectorName) {
            "sum" -> variables.joinToString("+")
            "product" -> variables.joinToString("*")
            "mid" -> "(${variables.joinToString("+")})/$maximalIndiceNotation"
            "max" -> "max(${variables.joinToString(", ")})"
            "min" -> "min(${variables.joinToString(", ")})"
            "size" -> maximalIndiceNotation
            else -> throw NoWhenBranchMatchedException()
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is All && collector == other.collector && genericExpression == other.genericExpression
    }

    override fun hashCode(): Int {
        return All::class.hashCode() * 7 + genericExpression.hashCode() * 13
    }
}