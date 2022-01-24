package physics.reasoning

import physics.knowledge.Knowledge


sealed interface Step {
    override fun toString(): String

    class UseKnowledge(val knowledge: Knowledge, private val knowledgeRepresentation: String) : Step {
        override fun toString(): String {
            return "On utilise $knowledgeRepresentation."
        }
    }
}
