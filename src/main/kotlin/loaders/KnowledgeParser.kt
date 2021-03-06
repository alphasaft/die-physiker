package loaders

import loaders.base.Parser


object KnowledgeParser : Parser() {
    override fun axiom() {
        oneOrMore("knowledge-#", separator = "\n*") {
            var knowledgeType: String? = null
            choice {
                option {
                    invokeAsSubParser(FormulaParser)
                    knowledgeType = "formula"
                }
                option {
                    invokeAsSubParser(DatabaseParser)
                    knowledgeType = "database"
                }
                option {
                    invokeAsSubParser(StandardKnowledgeParser)
                    knowledgeType = "complexKnowledge"
                }
            }
            knowledgeType!!  [ "type" ]
        }
    }
}
