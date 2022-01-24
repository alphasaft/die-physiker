package loaders

import alwaysTrue
import loaders.base.Ast
import loaders.base.AstNode
import loaders.base.BaseFunctionsRegister
import loaders.base.DataLoader
import physics.components.Component
import physics.components.ComponentClass
import physics.components.ComponentSpec
import physics.components.Location


class RequirementLoader(
    private val loadedComponentClasses: Map<String, ComponentClass>,
    private val functionRegister: FunctionsRegister,
) : DataLoader<RequirementParser, ComponentSpec>(RequirementParser) {
    open class FunctionsRegister : BaseFunctionsRegister {
        private val specPredicates = mutableMapOf<String, (Component, Map<String, Component>) -> Boolean>()

        fun addRequirementPredicate(predicateRef: String, predicateImpl: (Component, Map<String, Component>) -> Boolean) {
            specPredicates[predicateRef] = predicateImpl
        }

        fun getRequirementPredicate(predicateRef: String): (Component, Map<String, Component>) -> Boolean {
            return specPredicates.getValue(predicateRef)
        }
    }

    override fun generateFrom(ast: Ast): ComponentSpec {
        val alias = ast["alias"]
        val selectAll = "#" in alias
        val type = loadedComponentClasses.getValue(ast["type"])
        val location = ast.getOrNull("location")?.let { Location.At(it) } ?: Location.Any
        val predicate = ast.getOrNull("checkFunctionRef")?.let { functionRegister.getRequirementPredicate(it) } ?: ::alwaysTrue
        val variables = generateVariablesFrom(ast.getNodeOrNull("variables") ?: AstNode())

        return if (selectAll) ComponentSpec.allRemaining(alias, type, location, variables, predicate)
        else ComponentSpec.single(alias, type, location, variables, predicate)
    }

    private fun generateVariablesFrom(variablesNode: AstNode): Map<String, String> {
        return variablesNode.allNodes("variable-#").associate { generateVariableFrom(it) }
    }

    private fun generateVariableFrom(variableNode: AstNode): Pair<String, String> {
        val variableName = variableNode["variableName"]
        val field = variableNode["field"]
        return variableName to field
    }
}