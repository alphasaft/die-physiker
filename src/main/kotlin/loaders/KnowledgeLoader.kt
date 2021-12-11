package loaders

import loaders.base.Ast
import loaders.base.AstNode
import loaders.base.DataLoader
import physics.Args
import physics.PhysicalValuesMapper
import physics.components.Component
import physics.components.ComponentClass
import physics.computation.BasePhysicalKnowledge


class KnowledgeLoader(
    loadedComponentClasses: Map<String, ComponentClass>,
    functionsRegister: FunctionsRegister,
) : DataLoader<KnowledgeParser, List<BasePhysicalKnowledge>>(KnowledgeParser) {
    private val formulaLoader = FormulaLoader(loadedComponentClasses, functionsRegister)
    private val databaseLoader = DatabaseLoader(loadedComponentClasses)
    private val complexKnowledgeLoader = StandardKnowledgeLoader(loadedComponentClasses, functionsRegister)

    class FunctionsRegister internal constructor(): RequirementLoader.FunctionsRegister() {
        private val requirementsPredicates = mutableMapOf<String, (Component, Args<Component>) -> Boolean>()
        private val standardKnowledgeImplementations = mutableMapOf<String, PhysicalValuesMapper>()

        fun addStandardKnowledgeImplementation(implementationRef: String, implementation: PhysicalValuesMapper) {
            standardKnowledgeImplementations[implementationRef] = implementation
        }

        fun getComplexKnowledgeImplementation(implementationRef: String): PhysicalValuesMapper {
            return standardKnowledgeImplementations.getValue(implementationRef)
        }
    }

    companion object {
        fun getFunctionsRegister() = FunctionsRegister()
    }

    override fun generateFrom(ast: Ast): List<BasePhysicalKnowledge> {
        return ast.allNodes("knowledge-#").map { generateKnowledgeFrom(it) }
    }

    private fun generateKnowledgeFrom(knowledgeNode: AstNode): BasePhysicalKnowledge {
        return when (knowledgeNode["type"]) {
            "formula" -> formulaLoader.generateFrom(knowledgeNode.toAst())
            "database" -> databaseLoader.generateFrom(knowledgeNode.toAst())
            "complexKnowledge" -> complexKnowledgeLoader.generateFrom(knowledgeNode.toAst())
            else -> throw NoWhenBranchMatchedException()
        }
    }
}