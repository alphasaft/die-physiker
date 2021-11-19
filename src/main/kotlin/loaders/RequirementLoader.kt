package loaders

import loaders.base.Ast
import loaders.base.AstNode
import loaders.base.DataLoader
import physics.alwaysTrue
import physics.components.Component
import physics.components.ComponentClass
import physics.computation.ComponentRequirement
import physics.computation.Location


class RequirementLoader(
    private val loadedComponentClasses: Map<String, ComponentClass>,
    private val checks: Map<String, (Component, Map<String, Component>) -> Boolean>
) : DataLoader<RequirementParser, ComponentRequirement>(RequirementParser) {
    override fun generateFrom(ast: Ast): ComponentRequirement {
        val alias = ast["alias"]
        val selectAll = "#" in alias
        val type = loadedComponentClasses.getValue(ast["type"])
        val location = ast.getOrNull("location")?.let { Location.At(it) } ?: Location.Any
        val condition = (ast.getOrNull("checkFunctionRef")?.let { checks.getValue(it) } ?: ::alwaysTrue)
        val variables = generateVariablesFrom(ast.getNodeOrNull("variables") ?: AstNode())

        return if (selectAll) ComponentRequirement.allRemaining(alias, type, location, variables, condition)
        else ComponentRequirement.single(alias, type, location, variables, condition)
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