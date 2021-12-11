package loaders

import loaders.base.Parser

object RequirementParser : Parser() {
    override fun axiom() {
        consumeRegex("[\\w#]+")  [ "alias" ]
        consumeRegex("(une|un|des)")
        identifier()  [ "type" ]
        optional { consume("-s") }

        optional {
            consume("de")
            consumeRegex("$identifier\\.[\\wéçàèù ]+").trim()  [ "location" ]
        }

        optional {
            consume("respectant")
            identifier()  [ "checkFunctionRef" ]
        }

        optional {
            consume(",")
            node("variables") { variables() }
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