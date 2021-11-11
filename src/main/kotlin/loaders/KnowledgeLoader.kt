package loaders

import loaders.base.Ast
import loaders.base.AstNode
import loaders.base.DataLoader
import physics.components.ComponentClass
import physics.computation.PhysicalKnowledge


class KnowledgeLoader(loadedComponentClasses: Map<String, ComponentClass>) : DataLoader<KnowledgeParser, List<PhysicalKnowledge>>(KnowledgeParser) {
    private val formulaLoader = FormulaLoader(loadedComponentClasses)

    override fun generateFrom(ast: Ast): List<PhysicalKnowledge> {
        return ast.allNodes("knowledge-#").map { generateKnowledgeFrom(it) }
    }

    private fun generateKnowledgeFrom(knowledgeNode: AstNode): PhysicalKnowledge {
        return when (knowledgeNode["type"]) {
            "formula" -> formulaLoader.generateFrom(knowledgeNode.toAst())
            else -> throw NoWhenBranchMatchedException()
        }
    }
}