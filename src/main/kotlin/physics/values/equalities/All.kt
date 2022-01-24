package physics.values.equalities

import noop
import physics.quantities.Quantity
import physics.quantities.doubles.PReal
import physics.quantities.doubles.div
import physics.quantities.doubles.plus
import physics.quantities.doubles.times


class All(
    val genericExpression: Expression,
    val collectorName: String,
    private val maximalIndiceNotation: String = "n",
) : Expression() {
    constructor(
        genericVariableName: String,
        collectorName: String,
        maximalIndiceNotation: String = "n"
    ) : this(Var(genericVariableName), collectorName, maximalIndiceNotation)

    private companion object {
        val collectors: Map<String, (List<Quantity<PReal>>) -> Quantity<PReal>> = mapOf(
            "sum" to { it.reduce { a, b -> a + b } },
            "product" to { it.reduce { a, b -> a * b } },
            "mid" to { it.reduce { a, b -> a * b } / PReal(it.size.toDouble()) },
            "size" to { PReal(it.size.toDouble()) }
        )
    }

    override val members: Collection<Expression> = listOf(genericExpression)
    private val collector: (List<Quantity<PReal>>) -> Quantity<PReal> = collectors.getValue(collectorName)
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

    override fun transformBy(allVariablesMappings: Map<String, String>): Expression {
        val variables = allVariables()
        val inlinedMembers = mutableListOf<Expression>()
        var i = 1

        mainLoop@ while (true) {
            var inlinedMember = genericExpression
            for (variable in variables) {
                val indexedVariable = variable.replace("#", i.toString())
                if (indexedVariable !in allVariablesMappings) {
                    variables.map { it.replace("#", i.toString()) }.find { it in allVariablesMappings }?.let { throw IllegalArgumentException("Variable $indexedVariable is missing, but inlining of the All expr should continue because variable $it was provided.") }
                    break@mainLoop
                }
                inlinedMember = inlinedMember.substitute(Var(variable), Var(allVariablesMappings.getValue(indexedVariable)))
            }
            inlinedMembers.add(inlinedMember)
            i++
        }

        return when (collectorName) {
            "sum" -> Sum(inlinedMembers)
            "product" -> Prod(inlinedMembers)
            "mid" -> Div(Sum(inlinedMembers), Const(inlinedMembers.size))
            "size" -> Const(inlinedMembers.size)
            else -> throw NoWhenBranchMatchedException()
        }
    }

    override fun evaluate(arguments: Map<String, Quantity<PReal>>): Quantity<PReal> {
        var i = 1
        val collected = mutableListOf<Quantity<PReal>>()

        while (genericVariablesNames.first().replace("#", i.toString()) in arguments) {
            val expression = expression(i++)
            collected.add(expression.evaluate(arguments))
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
        val genericExpression = members.single()
        return All(genericExpression, collectorName, maximalIndiceNotation)
    }

    override fun toString(): String {
        val variables = listOf(expression(1), expression(2), "...", expression(maximalIndiceNotation))
        return when (collectorName) {
            "sum" -> variables.joinToString(" + ")
            "product" -> variables.joinToString("*")
            "mid" -> "(${variables.joinToString("+")})/$maximalIndiceNotation"
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