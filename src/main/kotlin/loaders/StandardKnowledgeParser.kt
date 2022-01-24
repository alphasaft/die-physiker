package loaders

import loaders.base.Parser

object StandardKnowledgeParser : Parser() {
    override fun axiom() {
        header()
        consume("\n")
        specs()
        consume("\n")
        output()
        consume("\n")
        mappers()
    }

    private fun header() {
        consumeSentence("La connaissance complexe")
        string()["name"]
    }

    private fun specs() {
        consumeSentence("concerne : \n")
        node("specs") {
            oneOrMore("spec-#", separator = "\n") {
                consume("-")
                invokeAsSubParser(RequirementParser)
            }
        }
    }

    private fun output() {
        consumeSentence("renvoie : \n")
        node("output") {
            consumeRegex("$identifier.[\\wéçèàù ]+").trim()["location"]
            consumeSentence("( '")
            identifier()["variableName"]
            consumeSentence("' )")
        }
    }

    private fun mappers() {
        consumeSentence("et déduit : \n")
        node("mappers") {
            oneOrMore("mapper-#", separator = "\n+") {
                consume("-")
                identifier()["variable"]
                consume("par")
                identifier()["mapperFunctionRef"]
            }
        }
    }
}