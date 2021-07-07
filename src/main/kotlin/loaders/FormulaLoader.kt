package loaders

import loaders.base.Ast
import loaders.base.AstNode
import loaders.base.DataLoader
import physics.Formula
import physics.FormulaExpression
import physics.generateFormulas
import physics.specs.ComponentAccessSpec
import physics.specs.FieldAccessSpec
import physics.specs.RootComponentAccessSpec

object FormulaLoader : DataLoader<FormulaParser, List<Formula>>(FormulaParser) {
    private val formulaExpressions = mutableMapOf<String, FormulaExpression>()

    fun registerFormulaExpression(name: String, expression: FormulaExpression) {
        formulaExpressions[name] = expression
    }

    override fun generateFrom(ast: Ast): List<Formula> {
        val formulas = mutableListOf<Formula>()
        for (node in ast.allNodes("formula-#")) {
            formulas.addAll(generateFormulasFromNode(node))
        }
        return formulas
    }

    private fun generateFormulasFromNode(formulaNode: AstNode): List<Formula> {
        val rootSpec = RootComponentAccessSpec(formulaNode["rootComponentType"], formulaNode["rootComponentName"])
        val componentSpecs = generateComponentSpecs(formulaNode)
        val fieldSpecs = generateFieldSpecs(formulaNode)
        val computingClauses = generateComputingClauses(formulaNode)
        return generateFormulas(
            rootSpec,
            componentSpecs,
            fieldSpecs,
            computingClauses
        )
    }

    private fun generateComponentSpecs(formulaNode: AstNode): List<ComponentAccessSpec> {
        val componentSpecs = mutableListOf<ComponentAccessSpec>()
        for (specNode in formulaNode.allNodes("componentSpec-#")) {
            componentSpecs.add(ComponentAccessSpec(
                name = specNode["name"],
                formattedLocation = "${specNode["component"]}.${specNode["field"]}",
            ))
        }
        return componentSpecs
    }

    private fun generateFieldSpecs(formulaNode: AstNode): List<FieldAccessSpec> {
        val fieldSpecs = mutableListOf<FieldAccessSpec>()
        for (specNode in formulaNode.allNodes("fieldSpec-#")) {
            fieldSpecs.add(FieldAccessSpec(
                specNode["parentName"],
                specNode["storedIn"]
            ))
        }
        return fieldSpecs
    }

    private fun generateComputingClauses(formulaNode: AstNode): Map<String, FormulaExpression> {
        val clauses = mutableMapOf<String, FormulaExpression>()
        for (clause in formulaNode.allNodes("computingClause-#")) {
            clauses[clause["output"]] = when (clause["type"]) {
                "delegated" -> formulaExpressions[clause["functionRef"]] ?: throw IllegalArgumentException("Function expression ${clause["functionRef"]} wasn't found.")
                "inline" -> FormulaExpressionLoader.loadFrom(clause["expr"])
                else -> throw IllegalStateException()
            }
        }
        return clauses
    }
}
