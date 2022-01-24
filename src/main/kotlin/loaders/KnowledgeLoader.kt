package loaders

import Args
import loaders.base.Ast
import loaders.base.AstNode
import loaders.base.DataLoader
import physics.QuantityMapper
import physics.components.Component
import physics.components.ComponentClass
import physics.knowledge.Knowledge


class KnowledgeLoader(
    loadedComponentClasses: Map<String, ComponentClass>,
    functionsRegister: FunctionsRegister
) : DataLoader<KnowledgeParser, List<Knowledge>>(KnowledgeParser) {
    private val formulaLoader = FormulaLoader(loadedComponentClasses, functionsRegister)
    private val databaseLoader = DatabaseLoader(loadedComponentClasses)
    private val standardKnowledgeLoader = StandardKnowledgeLoader(loadedComponentClasses, functionsRegister)

    class FunctionsRegister internal constructor(): RequirementLoader.FunctionsRegister() {
        private val specsPredicates = mutableMapOf<String, (Component, Args<Component>) -> Boolean>()
        private val standardKnowledgeImplementations = mutableMapOf<String, QuantityMapper>()

        fun addStandardKnowledgeImplementation(implementationRef: String, implementation: QuantityMapper) {
            standardKnowledgeImplementations[implementationRef] = implementation
        }

        fun getComplexKnowledgeImplementation(implementationRef: String): QuantityMapper {
            return standardKnowledgeImplementations.getValue(implementationRef)
        }
    }

    companion object {
        fun getFunctionsRegister() = FunctionsRegister()
    }

    override fun generateFrom(ast: Ast): List<Knowledge> {
        return ast.allNodes("knowledge-#").map { generateKnowledgeFrom(it) }
    }

    private fun generateKnowledgeFrom(knowledgeNode: AstNode): Knowledge {
        return when (knowledgeNode["type"]) {
            "formula" -> formulaLoader.generateFrom(knowledgeNode.toAst())
            "database" -> databaseLoader.generateFrom(knowledgeNode.toAst())
            "complexKnowledge" -> standardKnowledgeLoader.generateFrom(knowledgeNode.toAst())
            else -> throw NoWhenBranchMatchedException()
        }
    }
}