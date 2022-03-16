package loaders

import Collector
import loaders.base.Ast
import loaders.base.AstNode
import loaders.base.DataLoader
import physics.components.ComponentClass
import physics.components.ComponentSpec
import physics.components.Location
import physics.components.ComponentsPickerWithOutput
import physics.knowledge.StandardKnowledge
import physics.quantities.Quantity


class StandardKnowledgeLoader(
    loadedComponentClasses: Map<String, ComponentClass>,
    private val functionsRegister: KnowledgeLoader.FunctionsRegister,
) : DataLoader<StandardKnowledgeParser, StandardKnowledge>(StandardKnowledgeParser) {
    private val specsLoader = RequirementLoader(loadedComponentClasses, functionsRegister)

    override fun generateFrom(ast: Ast): StandardKnowledge {
        val name = ast["name"]
        val specs = generateSpecsFrom(ast.."specs")
        val output = generateOutputFrom(ast.."output")
        val specsHandler = ComponentsPickerWithOutput(specs, output)
        val usedMappers = generateUsedMappersFrom(ast.."mappers")

        return StandardKnowledge(
            name,
            specsHandler,
            usedMappers,
            emptyMap()
        )
    }

    private fun generateSpecsFrom(specsNode: AstNode): List<ComponentSpec> {
        return specsNode.allNodes("spec-#").map { specsLoader.generateFrom(it.toAst()) }
    }

    private fun generateOutputFrom(outputNode: AstNode): Pair<String, Location.At> {
        return outputNode["variableName"] to Location.At(outputNode["location"])
    }

    private fun generateUsedMappersFrom(mappersNode: AstNode): Map<String, Collector<Quantity<*>>> {
        return mappersNode.allNodes("mapper-#").associate { it["variable"] to functionsRegister.getComplexKnowledgeImplementation(it["mapperFunctionRef"]) }
    }
}