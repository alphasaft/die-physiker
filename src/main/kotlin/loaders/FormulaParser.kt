package loaders

import loaders.base.Parser


object FormulaParser : Parser() {
    override fun axiom() {
        header()
        consume("\n")
        group("requirements") { requirements() }
        consume("\n")
        equality()
        optional {
            consume("\n")
            adaptableVariables()
        }
    }

    private fun header() {
        consume("La"); consume("formule")
        optional { consume("implicite") ; "yes" [ "implicit" ] }
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
            variable()
        }
    }

    private fun variable() {
        consumeRegex("[\\w#]+")  [ "variableName" ]
        consumeRegex("(son|sa|leurs|leur) ")
        consumeRegex("[\\w ]+")  [ "field" ]

        optional { consume("-s") }
    }

    private fun equality() {
        consume("on"); consume("a"); consume(":"); consume("\n")

        consumeRegex(identifier)  [ "outputVariable" ]
        consume("=")

        group("expression") {
            expression()
        }
    }

    private fun expression() {
        group("operand-1") { operand() }

        var i = 2
        zeroOrMore {
            group("operator-${i-1}") { operator() }
            group("operand-$i") { operand() }
            i++
        }
    }

    private fun operand() {
        var type: String? = null

        choice {
            option {
                multiVariablesCollector()
                type = "multiVariablesCollector"
            }
            option {
                consumeRegex("[a-zA-Z_#][a-zA-Z0-9_#]*") [ "variableName" ]
                type = "variable"
            }
            option {
                consumeRegex(double)  [ "value" ]
                type = "double"
            }
            option {
                consumeRegex(integer)  [ "value" ]
                type = "integer"
            }
            option {
                consume("(")
                group("subexpression") { expression() }
                consume(")")
                type = "expression"
            }
        }

        type!!  [ "type" ]
    }

    private fun multiVariablesCollector() {
        consume("(")
        group("genericExpression") { expression() }
        consume("}")
        consumeRegex(identifier)  [ "collector" ]
    }

    private fun operator() {
        consumeRegex("([\\-+*/]|\\*\\*)")
    }

    private fun adaptableVariables() {
        consumeSentence("variables adaptables :")
        group("adaptableVariables") {
            oneOrMore("adaptableVariable-#", ",") { consumeRegex(identifier) }
        }
    }
}