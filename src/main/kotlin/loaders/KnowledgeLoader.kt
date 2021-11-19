package loaders

import loaders.base.Ast
import loaders.base.AstNode
import loaders.base.DataLoader
import physics.components.Component
import physics.components.ComponentClass
import physics.computation.PhysicalKnowledge
import physics.values.PhysicalValue


class KnowledgeLoader(
    loadedComponentClasses: Map<String, ComponentClass>,
    requirementsPredicates: Map<String, (Component, Map<String, Component>) -> Boolean>,
    complexKnowledgeMappers: Map<String, (Map<String, PhysicalValue<*>>) -> PhysicalValue<*>>,
) : DataLoader<KnowledgeParser, List<PhysicalKnowledge>>(KnowledgeParser) {
    private val formulaLoader = FormulaLoader(loadedComponentClasses, requirementsPredicates)
    private val databaseLoader = DatabaseLoader(loadedComponentClasses)
    private val complexKnowledgeLoader = ComplexKnowledgeLoader(loadedComponentClasses, requirementsPredicates, complexKnowledgeMappers)

    override fun generateFrom(ast: Ast): List<PhysicalKnowledge> {
        return ast.allNodes("knowledge-#").map { generateKnowledgeFrom(it) }
    }

    private fun generateKnowledgeFrom(knowledgeNode: AstNode): PhysicalKnowledge {
        return when (knowledgeNode["type"]) {
            "formula" -> formulaLoader.generateFrom(knowledgeNode.toAst())
            "database" -> databaseLoader.generateFrom(knowledgeNode.toAst())
            "complexKnowledge" -> complexKnowledgeLoader.generateFrom(knowledgeNode.toAst())
            else -> throw NoWhenBranchMatchedException()
        }
    }
}