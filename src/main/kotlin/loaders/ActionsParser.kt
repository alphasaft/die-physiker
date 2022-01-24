package loaders

import loaders.base.Parser

object ActionsParser : Parser() {
    override fun axiom() {
        header()
        consume("\n")
        specs()
        consume("\n")
        modifyingBlock()
    }

    private fun header() {
        consumeSentence("La réaction")
        string().trim('"')  [ "reactionName" ]
    }

    private fun specs() {
        consumeSentence("a lieu pour : \n")
        node("specs") {
            oneOrMore("spec-#", "\n") {
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
