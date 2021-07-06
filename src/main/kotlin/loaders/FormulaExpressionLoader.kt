package loaders

import loaders.base.Ast
import loaders.base.AstNode
import loaders.base.DataLoader
import physics.FormulaArguments
import physics.FormulaExpression
import kotlin.math.*


// Rappel : le truc ICA de hitman coincidence avec le fond d'écran stppppp je sais que c chiant mais c incroyable


object FormulaExpressionLoader : DataLoader<FormulaExpressionParser, FormulaExpression>(FormulaExpressionParser) {
    private val precedence = mapOf("+" to 1, "-" to 1, "*" to 2, "/" to 2, "^" to 3)
    private val operatorsCallbacks: Map<String, (Double, Double) -> Double> = mapOf(
        "+" to { x, y -> x+y },
        "-" to { x, y -> x-y },
        "*" to { x, y -> x*y },
        "/" to { x, y -> x/y },
        "^" to { x, y -> x.pow(y) }
    )

    private val collectors: Map<String, (List<Double>) -> Double> = mapOf(
        "sum" to List<Double>::sum,
        "average" to List<Double>::average,
        "max" to { l -> l.maxOrNull() ?: throw IllegalArgumentException("Collection is empty, can't successfully collect it with collector 'max'. Use maxOr0 instead") },
        "maxOr0" to { l -> l.minOrNull() ?: 0.0 },
        "min" to { l -> l.minOrNull() ?: throw IllegalArgumentException("Collection is empty, can't successfully collect it with collector 'min'. Use minOr0 instead") },
        "minOr0" to { l -> l.minOrNull() ?: 0.0 }
    )

    private val functions: Map<String, (Double) -> Double> = mapOf(
        "cos" to ::cos,
        "acos" to ::acos,
        "sin" to ::sin,
        "asin" to ::asin,
        "tan" to ::tan,
        "atan" to ::atan,
        "cotan" to { x -> 1/tan(x) },
        "exp" to ::exp,
        "abs" to ::abs,
        "sqrt" to ::sqrt,
    )

    private val constants: Map<String, Double> = mapOf(
        "PI" to PI,
        "E" to E,
    )

    override fun generateFrom(ast: Ast): FormulaExpression {
        return { args -> evaluateExpression(args, ast.children.toList()) }
    }

    private fun evaluateExpression(args: FormulaArguments, nodes: List<Pair<String, AstNode>>): Double {
        if (nodes.size == 1) return evaluateOperand(args, nodes.single().second)

        val operatorNodes = nodes.filter { (name, _) -> name.startsWith("operator") }
        val highestPrecedenceOperator = operatorNodes.reversed().minByOrNull { precedence.getValue(it.second.content!!) }!!
        val highestPrecedenceOperatorPositionInExpression = operatorNodes.indexOf(highestPrecedenceOperator)*2+1

        return operatorsCallbacks.getValue(highestPrecedenceOperator.second.content!!)(
            evaluateExpression(args, nodes.subList(0, highestPrecedenceOperatorPositionInExpression)),
            evaluateExpression(args, nodes.subList(highestPrecedenceOperatorPositionInExpression+1, nodes.size))
        )
    }

    private fun evaluateOperand(args: FormulaArguments, operand: AstNode): Double {
        return when (operand["type"]){
            "variable" -> args[operand["value"]]
            "float" -> operand["value"].toDouble()
            "collector" -> collectors.getValue(operand["collector"]).invoke(args.getAll(operand["collection"]))
            "constant" -> constants.getValue(operand["constName"])
            "function" -> functions.getValue(operand["functionName"]).invoke(evaluateExpression(args, operand.getNode("value").children.toList()))
            "subExpr" -> evaluateExpression(args, operand.getNode("value").children.toList())
            else -> throw IllegalStateException()
        }
    }
}
