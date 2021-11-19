package loaders

import loaders.base.Ast
import loaders.base.AstNode
import loaders.base.DataLoader
import physics.components.Component
import physics.components.ComponentClass
import physics.computation.ComponentRequirement
import physics.computation.Location
import physics.computation.others.ComplexKnowledge
import physics.values.PhysicalValue

class ComplexKnowledgeLoader(
    loadedComponentClasses: Map<String, ComponentClass>,
    checks: Map<String, (Component, Map<String, Component>) -> Boolean> = emptyMap(),
    private val mappers: Map<String, (Map<String, PhysicalValue<*>>) -> PhysicalValue<*>> = emptyMap()
) : DataLoader<ComplexKnowledgeParser, ComplexKnowledge>(ComplexKnowledgeParser) {
    private val requirementsLoader = RequirementLoader(loadedComponentClasses, checks)

    override fun generateFrom(ast: Ast): ComplexKnowledge {
        val name = ast["name"]
        val requirements = generateRequirementsFrom(ast.."requirements")
        val output = generateOutputFrom(ast.."output")
        val usedMappers = generateUsedMappersFrom(ast.."mappers")

        return ComplexKnowledge(
            name,
            requirements,
            output,
            usedMappers
        )
    }

    private fun generateRequirementsFrom(requirementsNode: AstNode): List<ComponentRequirement> {
        return requirementsNode.allNodes("requirement-#").map { requirementsLoader.generateFrom(it.toAst()) }
    }

    private fun generateOutputFrom(outputNode: AstNode): Pair<String, Location.At> {
        return outputNode["variableName"] to Location.At(outputNode["location"])
    }

    private fun generateUsedMappersFrom(mappersNode: AstNode): Map<String, (Map<String, PhysicalValue<*>>) -> PhysicalValue<*>> {
        return mappersNode.allNodes("mapper-#").associate { it["variable"] to mappers.getValue(it["mapperFunctionRef"]) }
    }
}