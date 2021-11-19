package loaders

import loaders.base.Parser

object ComplexKnowledgeParser : Parser() {
    override fun axiom() {
        header()
        consume("\n")
        requirements()
        consume("\n")
        output()
        consume("\n")
        mappers()
    }

    private fun header() {
        consumeSentence("La connaissance complexe")
        consumeRegex(string)  [ "name" ]
    }

    private fun requirements() {
        consumeSentence("concerne : \n")
        group("requirements") {
            oneOrMore("requirement-#", separator = "\n") {
                consume("-")
                invokeAsSubParser(RequirementParser)
            }
        }
    }

    private fun output() {
        consumeSentence("renvoie : \n")
        group("output") {
            consumeRegex("$identifier.[\\wéçèàù ]+").trim()  [ "location" ]
            consumeSentence("( '")
            consumeRegex(identifier)  [ "variableName" ]
            consumeSentence("' )")
        }
    }

    private fun mappers() {
        consumeSentence("et déduit : \n")
        group("mappers") {
            oneOrMore("mapper-#", separator = "\n+") {
                consume("-")
                consumeRegex(identifier)  [ "variable" ]
                consume("par")
                consumeRegex(identifier)  [ "mapperFunctionRef" ]
            }
        }
    }
}