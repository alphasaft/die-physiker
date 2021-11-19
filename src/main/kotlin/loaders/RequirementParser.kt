package loaders

import loaders.base.Parser

object RequirementParser : Parser() {
    override fun axiom() {
        consumeRegex("[\\w#]+")  [ "alias" ]
        consumeRegex("(une|un|des)")
        consumeRegex(identifier)  [ "type" ]
        optional { consume("-s") }

        optional {
            consume("de")
            consumeRegex("$identifier\\.[\\wéçàèù ]+").trim()  [ "location" ]
        }

        optional {
            consume("respectant")
            consumeRegex(identifier)  [ "checkFunctionRef" ]
        }

        optional {
            consume(",")
            group("variables") { variables() }
        }
    }

    private fun variables() {
        consume("avec")
        oneOrMore("variable-#", separator = ",") {
            variable()
        }
    }

    private fun variable() {
        consumeRegex("[\\w#]+")[ "variableName" ]
        consumeRegex("(son|sa|leurs|leur) ")
        consumeRegex("[\\wéàçùè ]+")[ "field" ]

        optional { consume("-s") }
    }
}