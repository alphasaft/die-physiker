package physics.formulas.expressions

import physics.formulas.FormulaArguments
import physics.noop
import physics.values.PhysicalDouble

class AllVars(
    val genericVariableName: String,
    val collectorName: String,
    private val maximalIndiceNotation: String = "n",
) : Expression() {
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

    init {
        require("#" in genericVariableName) { "Expected '#' in the variable name." }
    }

    private fun variable(i: Int) = variable(i.toString())
    private fun variable(name: String) = genericVariableName.replace("#", name)

    override fun evaluate(args: FormulaArguments): PhysicalDouble {
        var i = 1
        val collected = mutableListOf<PhysicalDouble>()
        while (variable(i) in args) {
            collected.add(args.getValue(variable(i)))
            i++
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
        val variables = listOf(variable(1), variable(2), "...", variable(maximalIndiceNotation))
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
        return other is AllVars && collector == other.collector && genericVariableName == other.genericVariableName
    }

    override fun hashCode(): Int {
        return AllVars::class.hashCode() * 7 + genericVariableName.hashCode() * 13
    }
}