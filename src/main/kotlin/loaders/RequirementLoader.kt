package loaders

import loaders.base.Ast
import loaders.base.AstNode
import loaders.base.BaseFunctionsRegister
import loaders.base.DataLoader
import physics.alwaysTrue
import physics.components.Component
import physics.components.ComponentClass
import physics.components.ComponentRequirement
import physics.components.Location


class RequirementLoader(
    private val loadedComponentClasses: Map<String, ComponentClass>,
    private val functionRegister: FunctionsRegister,
) : DataLoader<RequirementParser, ComponentRequirement>(RequirementParser) {
    open class FunctionsRegister : BaseFunctionsRegister {
        private val requirementPredicates = mutableMapOf<String, (Component, Map<String, Component>) -> Boolean>()

        fun addRequirementPredicate(predicateRef: String, predicateImpl: (Component, Map<String, Component>) -> Boolean) {
            requirementPredicates[predicateRef] = predicateImpl
        }

        fun getRequirementPredicate(predicateRef: String): (Component, Map<String, Component>) -> Boolean {
            return requirementPredicates.getValue(predicateRef)
        }
    }

    override fun generateFrom(ast: Ast): ComponentRequirement {
        val alias = ast["alias"]
        val selectAll = "#" in alias
        val type = loadedComponentClasses.getValue(ast["type"])
        val location = ast.getOrNull("location")?.let { Location.At(it) } ?: Location.Any
        val predicate = ast.getOrNull("checkFunctionRef")?.let { functionRegister.getRequirementPredicate(it) } ?: ::alwaysTrue
        val variables = generateVariablesFrom(ast.getNodeOrNull("variables") ?: AstNode())

        return if (selectAll) ComponentRequirement.allRemaining(alias, type, location, variables, predicate)
        else ComponentRequirement.single(alias, type, location, variables, predicate)
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