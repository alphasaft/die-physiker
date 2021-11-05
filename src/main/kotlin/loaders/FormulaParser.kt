package loaders

import loaders.base.Parser

class FormulaParser : Parser() {
    override fun axiom() {
        header()
        consume("\n")
        group("requirements") { requirements() }
        consume("\n")
        equality()
    }

    private fun header() {
        consume("La"); consume("formule")
        consumeRegex(string).trim('"')  [ "name" ]
    }

    private fun requirements() {
        consume("concerne"); consume(":"); consume("\n")
        oneOrMore("requirement-#", separator = "\n") {
            consume("-")
            consumeRegex("[\\w#]+")  [ "alias" ]
            consumeRegex("(une|un|des)")
            consumeRegex(identifier)  [ "type" ]
            optional { consume("-s") }

            optional {
                consume("de")
                consumeRegex("$identifier.$identifier")  [ "location" ]
            }

            consume(",")
            group("variables") { variables() }
        }
    }

    private fun variables() {
        consume("avec")
        oneOrMore("variable-#", separator = ",") {
            consumeRegex("[\\w#]+")  [ "variableName" ]
            consumeRegex("(son|sa|leurs|leur) ")
            consumeRegex("[\\w ]+")  [ "field" ]

            optional { consume("-s") }
        }
    }

    private fun equality() {
        consume("on"); consume("a"); consume(":"); consume("\n")

        consumeRegex(identifier)  [ "outputVariable" ]
        consume("=")
        expression()
    }

    // TODO : Improve the AllVars class to allow it to perform complexer operations
    // TODO : Tell the anecdote about the odds of getting sick with or without the vaccines
    private fun expression() {
        group("expression") {
            group("operand-1") { operand() }

            var i = 2
            oneOrMore {
                group("operator-${i-1}") { operator() }
                group("operand-$i") { operand() }
                i++
            }
        }
    }

    private fun operand() {
        choice {
            option { multiVariablesCollector() }
            option { consumeRegex(identifier) [ "variable" ] }
            option { consumeRegex(float)      [ "float" ]    }
            option { consumeRegex(integer)    [ "integer" ]  }
            option { consume("(") ; expression() ; consume(")") }
        }
    }

    private fun multiVariablesCollector() {
        group("multiVariablesCollector") {
            consumeRegex("[\\w#]+")  [ "variableName" ]
            consume("}")
            consumeRegex(identifier)  [ "collector" ]
        }
    }

    private fun operator() {
        consumeRegex("([\\-+*/]|\\*\\*)")
    }
}