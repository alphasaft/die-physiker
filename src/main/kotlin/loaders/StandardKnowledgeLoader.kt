package loaders

import loaders.base.Ast
import loaders.base.AstNode
import loaders.base.DataLoader
import physics.PhysicalValuesMapper
import physics.components.ComponentClass
import physics.components.ComponentRequirement
import physics.components.Location
import physics.components.FlexibleRequirementsHandler
import physics.computation.StandardPhysicalKnowledge


class StandardKnowledgeLoader(
    loadedComponentClasses: Map<String, ComponentClass>,
    private val functionsRegister: KnowledgeLoader.FunctionsRegister,
) : DataLoader<StandardKnowledgeParser, StandardPhysicalKnowledge>(StandardKnowledgeParser) {
    private val requirementsLoader = RequirementLoader(loadedComponentClasses, functionsRegister)

    override fun generateFrom(ast: Ast): StandardPhysicalKnowledge {
        val name = ast["name"]
        val requirements = generateRequirementsFrom(ast.."requirements")
        val output = generateOutputFrom(ast.."output")
        val requirementsHandler = FlexibleRequirementsHandler(requirements, output)
        val usedMappers = generateUsedMappersFrom(ast.."mappers")

        return StandardPhysicalKnowledge(
            name,
            requirementsHandler,
            usedMappers,
            emptyMap()
        )
    }

    private fun generateRequirementsFrom(requirementsNode: AstNode): List<ComponentRequirement> {
        return requirementsNode.allNodes("requirement-#").map { requirementsLoader.generateFrom(it.toAst()) }
    }

    private fun generateOutputFrom(outputNode: AstNode): Pair<String, Location.At> {
        return outputNode["variableName"] to Location.At(outputNode["location"])
    }

    private fun generateUsedMappersFrom(mappersNode: AstNode): Map<String, PhysicalValuesMapper> {
        return mappersNode.allNodes("mapper-#").associate { it["variable"] to functionsRegister.getComplexKnowledgeImplementation(it["mapperFunctionRef"]) }
    }
}