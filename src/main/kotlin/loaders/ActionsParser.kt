package loaders

import loaders.base.Parser

object ActionsParser : Parser() {
    override fun axiom() {
        header()
        consume("\n")
        requirements()
        consume("\n")
        modifyingBlock()
    }

    private fun header() {
        consumeSentence("La réaction")
        string().trim('"')  [ "reactionName" ]
    }

    private fun requirements() {
        consumeSentence("a lieu pour : \n")
        node("requirements") {
            oneOrMore("requirement-#", "\n") {
                consume("-")
                invokeAsSubParser(RequirementParser)
            }
        }
    }

    private fun modifyingBlock() {
        consume("modifiant")
        identifier()  [ "modifiedComponent" ]
        consume("par")
        identifier()  [ "modifierFunctionRef" ]
    }
}
