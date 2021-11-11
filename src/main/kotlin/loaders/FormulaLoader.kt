package loaders

import loaders.base.Ast
import loaders.base.AstNode
import loaders.base.DataLoader
import physics.components.ComponentClass
import physics.computation.ComponentRequirement
import physics.computation.Location
import physics.computation.formulas.Formula
import physics.computation.formulas.FormulaOptions
import physics.computation.formulas.expressions.*


class FormulaLoader(private val loadedComponentClasses: Map<String, ComponentClass>) : DataLoader<FormulaParser, Formula>(FormulaParser) {
    private companion object {
        val operatorPriorities = mapOf(
            "+" to 1,
            "-" to 1,
            "*" to 2,
            "/" to 2,
            "**" to 3,
        )

        val operatorsImplementations = mapOf(
            "+" to Expression::plus,
            "-" to Expression::minus,
            "*" to Expression::times,
            "/" to Expression::div,
            "**" to Expression::pow,
        )
    }

    override fun generateFrom(ast: Ast): Formula {
        val name = ast["name"]
        val implicit = ast.getOrNull("implicit") == "yes"
        val outputVariable = ast["outputVariable"]
        val requirements = generateRequirementsFrom(ast.getNode("requirements")).toMutableList()
        val outputRequirement = requirements.single { outputVariable in it.ownedVariables }
        val outputLocation = Location.At(outputRequirement.alias, outputRequirement.ownedVariables.getValue(outputVariable))
        val expression = generateExpression(ast.getNode("expression"))
        val variablesToRenderSpecifically = generateVariablesToRenderSpecificallyList(ast.getNodeOrNull("adaptableVariables"))

        requirements.replaceAll { if (outputRequirement === it) outputRequirement.withOptionalVariable(outputVariable) else outputRequirement }

        return Formula(
            Formula.ObtentionMethod.Builtin(name),
            requirements,
            outputVariable to outputLocation,
            outputVariable equal expression,
            options = if (implicit) FormulaOptions.Implicit else 0,
            variablesToRenderSpecifically = variablesToRenderSpecifically,
        )
    }

    private fun generateRequirementsFrom(requirementsNode: AstNode): List<ComponentRequirement> {
        return requirementsNode.allNodes("requirement-#").map { generateRequirementFrom(it) }
    }

    private fun generateRequirementFrom(requirementNode: AstNode): ComponentRequirement {
        val alias = requirementNode["alias"]
        val type = loadedComponentClasses.getValue(requirementNode["type"])
        val location = requirementNode.getOrNull("location")?.let { Location.At(it) } ?: Location.Any
        val selectAll = "#" in alias
        val variables = generateVariablesFrom(requirementNode.getNode("variables"))

        return if (selectAll) ComponentRequirement.allRemaining(alias, type, location, variables)
        else ComponentRequirement.single(alias, type, location, variables)
    }

    private fun generateVariablesFrom(variablesNode: AstNode): Map<String, String> {
        return variablesNode.allNodes("variable-#").associate { generateVariableFrom(it) }
    }

    private fun generateVariableFrom(variableNode: AstNode): Pair<String, String> {
        val variableName = variableNode["variableName"]
        val field = variableNode["field"]
        return variableName to field
    }

    private fun generateExpression(expressionNode: AstNode): Expression {
        val operands = expressionNode.allNodes("operand-#").mapTo(mutableListOf()) { generateOperand(it) }
        val operators = expressionNode.allNodes("operator-#").mapTo(mutableListOf()) { it.content!! }
        return generateExpressionFromOperandAndOperatorList(operands, operators)
    }

    private fun generateExpressionFromOperandAndOperatorList(operands: List<Expression>, operators: List<String>): Expression {
        if (operators.isEmpty()) return operands.single()

        var lowestPriorityOperatorIndex = -1
        var lowestPriority = 0
        for ((i, operator) in operators.withIndex()) {
            val priority = operatorPriorities.getValue(operator)
            if (priority >= lowestPriority) {
                lowestPriorityOperatorIndex = i
                lowestPriority = priority
            }
        }

        val operationImpl = operatorsImplementations.getValue(operators[lowestPriorityOperatorIndex])
        val leftOperands = operands.subList(0, lowestPriorityOperatorIndex+1)
        val rightOperands = operands.subList(lowestPriorityOperatorIndex+1, operands.size)
        val leftOperators = operators.subList(0, lowestPriorityOperatorIndex)
        val rightOperators = operators.subList(lowestPriorityOperatorIndex+1, operators.size)
        return operationImpl(
            generateExpressionFromOperandAndOperatorList(leftOperands, leftOperators),
            generateExpressionFromOperandAndOperatorList(rightOperands, rightOperators)
        )
    }

    private fun generateOperand(operandNode: AstNode): Expression {
        return when(operandNode["type"]) {
            "integer" -> Const(operandNode["value"].toInt())
            "double" -> Const(operandNode["value"].toDouble())
            "variable" -> Var(operandNode["variableName"])
            "expression" -> generateExpression(operandNode.getNode("subexpression"))
            "multiVariablesCollector" -> All(generateExpression(operandNode.getNode("genericExpression")), operandNode["collector"])
            else -> throw NoWhenBranchMatchedException()
        }
    }

    private fun generateVariablesToRenderSpecificallyList(formulaNode: AstNode?): List<String> {
        return formulaNode
            ?.allNodes("adaptableVariable-#")
            ?.map { it.content!! }
            ?: emptyList()
    }
}
