package loaders

import loaders.base.Parser


object FormulaParser : Parser() {
    override fun axiom() {
        header()
        consume("\n")
        requirements()
        consume("\n")
        output()
        consume("\n")
        equality()
        optional {
            consume("\n")
            adaptableVariables()
        }
    }

    private fun header() {
        consumeSentence("La formule")
        optional { consume("implicite") ; "yes" [ "implicit" ] }
        consumeRegex(string).trim('"')  [ "name" ]
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
            consumeRegex("$identifier.[$letter ]+").trim()  [ "location" ]
            consumeSentence("( '")
            consumeRegex(identifier)  [ "variableName" ]
            consumeSentence("' )")
        }
    }

    private fun equality() {
        consumeSentence("on a : \n")

        group("equality") {
            group("left") {
                expression()
            }

            consume("=")

            group("right") {
                expression()
            }
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