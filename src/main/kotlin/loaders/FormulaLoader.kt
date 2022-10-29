package loaders

import loaders.base.Ast
import loaders.base.AstNode
import loaders.base.DataLoader
import physics.components.ComponentClass
import physics.components.ComponentSpec
import physics.components.Location
import physics.components.ComponentsPickerWithOutput
import physics.knowledge.Formula
import physics.quantities.expressions.*


class FormulaLoader(
    loadedComponentClasses: Map<String, ComponentClass>,
    functionsRegister: KnowledgeLoader.FunctionsRegister,
) : DataLoader<FormulaParser, Formula>(FormulaParser) {
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

    private val specsLoader = RequirementLoader(loadedComponentClasses, functionsRegister)

    override fun generateFrom(ast: Ast): Formula {
        val name = ast["name"]
        val specs = generateSpecsFrom(ast.."specs")
        val output = generateOutputFrom(ast.."output")
        val specsHandler = ComponentsPickerWithOutput(specs, output)
        val equality = generateEquality(ast.."equality", outputVariable = output.first)

        return Formula(
            name,
            specsHandler,
            equality,
        )
    }

    private fun generateSpecsFrom(specsNode: AstNode): List<ComponentSpec> {
        return specsNode.allNodes("spec-#").map { specsLoader.generateFrom(it.toAst()) }
    }

    private fun generateOutputFrom(outputNode: AstNode): Pair<String, Location.At> {
        return outputNode["variableName"] to Location.At(outputNode["location"])
    }

    private fun generateEquality(equalityNode: AstNode, outputVariable: String): Equation {
        val left = generateExpression(equalityNode.."left")
        val right = generateExpression(equalityNode.."right")
        return (left equals right).isolateVariable(outputVariable)
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
            "expression" -> generateExpression(operandNode.."subexpression")
            "multiVariablesCollector" -> GenericSum(generateExpression(operandNode.."genericExpression"), operandNode["counter"], GenericExpression.Bound.Static(1), GenericExpression.Bound.Static(2))
            else -> throw NoWhenBranchMatchedException()
        }
    }
}
